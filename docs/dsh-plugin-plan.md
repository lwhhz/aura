# DSH 配对桥（插件形态）设计

## 目标

手机 Aura 在局域网内发现 DSH 桌面端、完成配对、查看会话/发指令/上传文件。

## 三层结构

1. **Aura App（已有一部分）**：mDNS 发现 → 手动/扫码配对 → 持有 token → 连接 WS。
2. **配对桥（本目录）**：mDNS 广播、配对码/token 校验、WS 中继到 `127.0.0.1:8187`。
3. **DSH 本体**：现有 Web 服务与 gateway；桥只做鉴权与转发，不改 DSH 协议。

## 为什么必须先有桥

DSH 的 `dsh-host-webserver` 默认只绑回环，且**没有 token/登录**；直接暴露 = 把远程执行能力开放到局域网。桥负责：

- 只监听局域网端口，配 `code` 或 `token`；
- 短期码默认 120 秒轮换；
- 配对成功后签发长期 token，手机端后续免输码；
- 后续可按方法白名单限制手机能调用的 DSH 能力。

## 安装形态的现实约束

- DSH 没有插件目录，插件必须随运行时打包或上游合并；
- 因此在官方插件机制落地前，先用 sidecar 进程跑同一套逻辑；
- 代码已按 cordis Service 写，迁移时只换入口。

## 手机端接口约定（草案）

```
GET  /aura/pair/info                 -> { name, mode, port }
POST /aura/pair/verify  x-aura-code  -> { token }
GET  /aura/pair/status               -> { mode, code, expiresIn, port }
WS   /aura/remote       x-aura-token -> 双向 JSON 帧（后续定义）
```

## 安全开关（用户选择）

| 模式 | 行为 | 适用 |
|---|---|---|
| code | 每次新设备配对输 6 位码，换长期 token | 默认，家里/办公室 |
| token | 只认 token，不再展示码 | 固定设备、长期使用 |

## 下一步

1. sidecar 入口 + mDNS + 配对端点（可直接用 Node 跑）；
2. WS 到 DSH 的透明代理；
3. App 端真正发 verify/连接、拉会话列表；
4. 上传文件（手机 → PC）落点与限额；
5. 官方插件机制可用后，把 sidecar 代码收回插件。
