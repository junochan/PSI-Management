---
name: psi-smart-ims-auth
description: >-
  智链进销存认证：通过 psims CLI 登录获取 JWT、登出、查看 token 文件路径、auth navigation（GET /auth/navigation：菜单、权限码、动态路由）。
  当用户首次在终端访问系统、脚本需持久化 token、排查动态菜单/权限、或 OpenClaw 需代登录再调其它接口时使用。
---

# 认证（登录 / 登出）

## 功能与作用

- **登录**：向后端验证用户名密码，取得 **JWT**。前端存 `localStorage`；CLI 默认写入用户目录下的 **`~/.psi-smart-ims/token`**，供后续 `psims` 子命令自动带 `Authorization`。
- **登出**：通知后端结束会话（若后端实现），并 **删除本地 token 文件**，避免脚本误用过期凭证。
- **token-path**：运维或排错时确认 CLI 实际读取的 token 文件位置。
- **navigation（登录后）**：返回当前用户的 **菜单树、权限码列表、前端动态路由表**（`viewKey` 与 `router/view-loaders.js` 对应），用于侧边栏与按权限注册路由。需 **Bearer**，白名单中仅匿名接口可免登录；本接口 **必须已登录**。
- **sso-login**：中转页用 **共享密钥** `key` 换取与普通登录相同的 `LoginVO`（含 JWT），需与后端 `app.sso-bypass.secret` 一致；成功后 CLI 同样可写 token 文件。
- **change-password（登录后）**：当前用户修改密码，Body 为 `currentPassword`、`newPassword`（新密码至少 6 位）；成功后建议重新登录。

对应前端：`/login`（`Login.vue`）；`authApi.ssoLogin` 用于中转页；`authApi.changePassword` 用于个人改密；登录成功后由 `navigationStore.fetchNavigation()` 调用 `GET /auth/navigation`。

## CLI 调用（推荐）

**前置**：`psims` 可执行文件位于 **`psi-smart-ims-overview/psi-cli/`**（与总览技能打包在一起）；在该目录执行 `npm install` 后，下文用 `psims` 表示 `node bin/psims.mjs`（当前工作目录为 `psi-cli/` 时）或 `npx psims`。

### 全局参数（可选）

与总览一致：`--base-url`、`--token`、`--timeout`。

### 子命令

| 命令 | 作用 |
|------|------|
| `psims auth login <username>` | 调用登录接口；成功后可保存 token |
| `psims auth sso-login` | `POST /auth/sso-login`；`--key` 或 `-d '{"key":"..."}'`；可选 `--no-save` |
| `psims auth logout` | 登出并清除本地 token 文件 |
| `psims auth token-path` | 打印 token 文件绝对路径 |
| `psims auth navigation` | `GET /auth/navigation`（需已登录） |
| `psims auth change-password` | `POST /auth/change-password`（需 Bearer）；`-d` / `-f` 传 JSON |

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

登录后拉取菜单与动态路由：

```bash
psims auth navigation
```

SSO 登录（自动化/中转页脚本）：

```bash
psims auth sso-login --key "your-shared-secret"
```

已登录用户改密：

```bash
psims auth change-password -d "{\"currentPassword\":\"old\",\"newPassword\":\"newpass12\"}"
```

## HTTP 对照（直连 curl 时）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/auth/login` | Body：`{ "username", "password", "remember" }` |
| POST | `/auth/sso-login` | Body：`{ "key" }`，与后端 SSO 配置一致 |
| POST | `/auth/logout` | 需 Bearer |
| GET | `/auth/navigation` | 需 Bearer；`data` 为 `NavigationVO`（菜单、权限、`routes`） |
| POST | `/auth/change-password` | 需 Bearer；Body：`ChangePasswordDTO` |

成功响应体仍为统一 `Result`，登录与 SSO 接口 `data` 中含 `token`、用户信息等字段。
