---
name: psi-smart-ims-auth
description: >-
  智链进销存认证：通过 psims CLI 登录获取 JWT、登出、查看 token 文件路径；对应 HTTP POST /auth/login 与 /auth/logout。
  当用户首次在终端访问系统、脚本需持久化 token、或 OpenClaw 需代登录再调其它接口时使用。
---

# 认证（登录 / 登出）

## 功能与作用

- **登录**：向后端验证用户名密码，取得 **JWT**。前端存 `localStorage`；CLI 默认写入用户目录下的 **`~/.psi-smart-ims/token`**，供后续 `psims` 子命令自动带 `Authorization`。
- **登出**：通知后端结束会话（若后端实现），并 **删除本地 token 文件**，避免脚本误用过期凭证。
- **token-path**：运维或排错时确认 CLI 实际读取的 token 文件位置。

对应前端路由：`/login`（`Login.vue`）。

## CLI 调用（推荐）

**前置**：`psims` 可执行文件位于 **`psi-smart-ims-overview/psi-cli/`**（与总览技能打包在一起）；在该目录执行 `npm install` 后，下文用 `psims` 表示 `node bin/psims.mjs`（当前工作目录为 `psi-cli/` 时）或 `npx psims`。

### 全局参数（可选）

与总览一致：`--base-url`、`--token`、`--timeout`。

### 子命令

| 命令 | 作用 |
|------|------|
| `psims auth login <username>` | 调用登录接口；成功后可保存 token |
| `psims auth logout` | 登出并清除本地 token 文件 |
| `psims auth token-path` | 打印 token 文件绝对路径 |

### 示例

```bash
# 交互式密码（推荐，避免把密码写进 shell 历史）
psims auth login admin

# 密码通过参数传入（仅自动化环境）
psims auth login admin -p "your-password"

# 登录成功但不写文件，只打印返回 JSON（便于管道给 jq）
psims auth login admin -p "secret" --no-save

# 指定后端地址（远程环境）
psims --base-url https://example.com/api/v1 auth login admin
```

登出：

```bash
psims auth logout
```

## HTTP 对照（直连 curl 时）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/auth/login` | Body：`{ "username", "password", "remember" }` |
| POST | `/auth/logout` | 需 Bearer |

成功响应体仍为统一 `Result`，`data` 中含 `token` 等字段。
