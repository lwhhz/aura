# @aura/dsh-plugin-remote-pair

Aura 手机端 ↔ DSH 桌面端的**局域网配对桥**，按 DSH 插件形态编写（cordis Service）。

## 现状与安装限制（重要）

DSH 桌面端（0.2.2-rc.3）**没有用户可安装的插件目录**：插件包都打包在
`resources/app.asar` 的 `dsh/node_modules` 里，由桌面运行时统一加载。
因此本包目前只能：

1. 随 DSH 桌面运行时一起打包（需要改官方安装包/上游合并），或
2. 作为一个独立 sidecar 进程接入（把本文件中的 mDNS/配对/中继逻辑抽到独立入口）。

在官方提供插件机制之前，**建议先做 sidecar 形态**，本目录保留插件写法以便后续迁移。

## 能力

- mDNS 广播 `_dsh._tcp`，TXT 包含 `mode` / `path` / `version`
- 配对鉴权二选一（安全开关）：
  - `code`：手机输入 6 位短期码 → 换取长期 token（默认）
  - `token`：只接受长期 token
- 长期 token 校验头：`x-aura-token`
- 短期码校验头：`x-aura-code`，默认 120 秒轮换
- WebSocket 中继占位（后续接 session/job/terminal/workspace-files 代理）

## 配置（schemastery）

| 字段 | 默认 | 说明 |
|---|---|---|
| `port` | 8765 | 配对桥端口 |
| `serviceName` | DSH Desktop | mDNS 名称 |
| `securityMode` | code | code / token |
| `codeTtlSeconds` | 120 | 短期码有效期 |
| `tokenFile` | 空 | token 落盘路径 |
| `dshLocalUrl` | http://127.0.0.1:8187 | 本机 DSH 地址 |

## 待办（未实现）

- 真实 token 落盘与撤销
- 到 DSH 本机 Web 的 WS 透明代理（而不是回显）
- session/job/terminal/workspace-files 方法白名单
- TLS（局域网自签或反向代理）
- 手机端上传文件的落点与大小限制
