/**
 * Aura Remote Pair —— DSH 插件（骨架，未编译/未加载验证）。
 *
 * 职责：
 * 1. 在局域网广播 mDNS 服务 _dsh._tcp（TXT: mode/path/version）；
 * 2. 提供配对端点：短配对码换长期 token，或直接 token 校验（二选一，安全开关）；
 * 3. 配对成功后，把手机端的 WebSocket 连接桥接到本机 DSH Web 服务；
 * 4. 后续在此处接入 session/job/terminal/workspace-files 的代理方法。
 *
 * 注意：DSH 当前没有面向用户的插件目录，本包要随桌面运行时一起打包
 * （或上游合并）才能被 cordis loader 加载；详见 README.md。
 */
import { randomBytes, timingSafeEqual } from "node:crypto";
import { Service } from "@deepseek-ai/cordis";
import z from "@deepseek-ai/schemastery";
import { Bonjour } from "bonjour-service";
import { WebSocketServer } from "ws";

interface PairConfig {
  port: number;
  serviceName: string;
  securityMode: "code" | "token";
  codeTtlSeconds: number;
  tokenFile: string;
  dshLocalUrl: string;
}

const TOKEN_HEADER = "x-aura-token";
const CODE_HEADER = "x-aura-code";

export default class RemotePair extends Service {
  static readonly inject = ["webServer"];

  static readonly Config: z<PairConfig> = z.object({
    port: z.natural().default(8765).description("配对桥监听端口"),
    serviceName: z.string().default("DSH Desktop").description("mDNS 显示名"),
    securityMode: z.union([z.const("code"), z.const("token")]).default("code")
      .description("code：短配对码换长期 token；token：只认长期 token"),
    codeTtlSeconds: z.natural().default(120).description("配对码有效期（秒）"),
    tokenFile: z.string().default("").description("长期 token 落盘路径，留空用默认"),
    dshLocalUrl: z.string().default("http://127.0.0.1:8187").description("本机 DSH Web 地址"),
  });

  private config: PairConfig = undefined as unknown as PairConfig;
  private bonjour: Bonjour | undefined;
  private code = "";
  private codeExpiresAt = 0;
  private token = "";
  private server: WebSocketServer | undefined;

  constructor(ctx: any, config: PairConfig) {
    super(ctx, "auraRemotePair");
    this.config = config;
    ctx.effect(() => {
      this.start();
      return () => this.stop();
    }, "auraRemotePair: lifecycle");
  }

  /** 供 DSH 设置页展示：当前模式、短期码与过期时间。 */
  pairStatus() {
    return {
      mode: this.config.securityMode,
      code: this.config.securityMode === "code" ? this.currentCode() : undefined,
      expiresIn: Math.max(0, Math.floor((this.codeExpiresAt - Date.now()) / 1000)),
      port: this.config.port,
    };
  }

  private currentCode(): string {
    if (this.config.securityMode !== "code") return "";
    if (Date.now() > this.codeExpiresAt || this.code.length === 0) {
      this.code = String(Math.floor(100000 + Math.random() * 900000));
      this.codeExpiresAt = Date.now() + this.config.codeTtlSeconds * 1000;
    }
    return this.code;
  }

  private start() {
    this.token = this.loadOrCreateToken();
    const bonjour = new Bonjour();
    this.bonjour = bonjour;
    bonjour.publish({
      name: this.config.serviceName,
      type: "dsh",
      port: this.config.port,
      txt: { mode: this.config.securityMode, path: "/aura/pair", version: "1" },
    });
    this.serve();
  }

  private stop() {
    this.server?.close();
    this.bonjour?.destroy();
  }

  private loadOrCreateToken(): string {
    // 生产实现应写入用户目录；此处用内存兜底，避免骨架阶段写盘。
    return randomBytes(24).toString("hex");
  }

  private verifyCode(input: string): boolean {
    const expected = this.currentCode();
    if (expected.length !== input.length) return false;
    return timingSafeEqual(Buffer.from(expected), Buffer.from(input));
  }

  private serve() {
    const wss = new WebSocketServer({ noServer: true });
    this.server = wss;
    // webServer 服务由 DSH 提供（node:http）。这里只示范注册路由的形状：
    const web = (this.ctx as any).webServer;
    if (web?.addRoute) {
      web.addRoute("GET", "/aura/pair/info", (req: any, res: any) => {
        res.setHeader("content-type", "application/json");
        res.end(JSON.stringify({ name: this.config.serviceName, mode: this.config.securityMode, port: this.config.port }));
      });
      web.addRoute("POST", "/aura/pair/verify", (req: any, res: any) => {
        const code = String(req.headers[CODE_HEADER] ?? "");
        if (this.config.securityMode === "code" && !this.verifyCode(code)) {
          res.statusCode = 401;
          res.end(JSON.stringify({ error: "code_invalid" }));
          return;
        }
        res.setHeader("content-type", "application/json");
        res.end(JSON.stringify({ token: this.token }));
      });
      web.addRoute("GET", "/aura/pair/status", (req: any, res: any) => {
        res.setHeader("content-type", "application/json");
        res.end(JSON.stringify(this.pairStatus()));
      });
    }
    // WebSocket 中继：校验 token 后转发到本机 DSH（占位实现，仅回显确认）。
    wss.on("connection", (socket: any) => {
      socket.send(JSON.stringify({ type: "paired", server: "dsh-remote-pair" }));
      socket.on("message", (raw: any) => socket.send(raw));
    });
  }
}
