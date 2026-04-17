---
name: psi-smart-ims-auth
description: >-
  智链进销存认证：通过 psims CLI 登录获取 JWT、登出、查看 token 文件路径、auth navigation（GET /auth/navigation：菜单、权限码、动态路由）。
  当用户首次在终端访问系统、脚本需持久化 token、排查动态菜单/权限、或 OpenClaw 需代登录再调其它接口时使用。
---

# 认证（登录 / 登出）

## 编辑与写操作前确认（强制）

在**执行任何会改动系统或仓库状态的操作之前**，必须先向用户**说明拟执行动作**（含影响范围、关键参数：路径、环境、用户名是否写 token 文件、请求体摘要等），并得到用户**明确同意**（例如「确认」「可以执行」「按这个来」）后，才可执行。

涵盖但不限于：

- **代码与配置**：创建/修改/删除仓库内文件、批量替换、会改写工作区或生成物的命令。
- **业务写操作**：`psims auth login`（写 `~/.psi-smart-ims/token`）、`logout`（删 token）、`change-password`、`sso-login` 等**会改远端状态或本地凭证**的调用。
- **环境与依赖**：用户未事先声明可自动执行时，`npm install` 等会写入磁盘的操作。

**可不经确认**：只读排查（`token-path`、`navigation` 等纯读）、纯口头方案及文档说明。

若用户已在**同一条消息**中明确授权某一具体动作（含范围），可视为已确认，但执行前仍应**简短复述**将运行的命令或写入点（尤其 token 文件路径），避免误操作。

## 功能与作用

- **登录**：调用登录接口校验用户名密码，取得 **JWT**。CLI 默认写入用户目录下的 **`~/.psi-smart-ims/token`**，供后续 `psims` 子命令自动带 `Authorization`。
- **登出**：调用登出接口并 **删除本地 token 文件**，避免脚本误用过期凭证。
- **token-path**：运维或排错时确认 CLI 实际读取的 token 文件位置。
- **navigation（登录后）**：返回当前用户的 **菜单树、权限码列表、路由配置**，用于权限控制与导航展示。需 **Bearer**，白名单中仅匿名接口可免登录；本接口 **必须已登录**。
- **sso-login**：中转页用 **共享密钥** `key` 换取与普通登录同等信息（含 JWT）；密钥需与服务端共享配置一致；成功后 CLI 同样可写 token 文件。
- **change-password（登录后）**：当前用户修改密码，Body 为 `currentPassword`、`newPassword`（新密码至少 6 位）；成功后建议重新登录。

适用于登录、SSO 中转与个人改密场景。

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

# 指定服务地址（远程环境）
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

## 接口参数清单（按技能内接口）

| 接口 | 路径参数 | Query 参数 | Body 参数 | 文件参数 |
|------|----------|------------|-----------|----------|
| `POST /auth/login` | 无 | 无 | `username`(必填), `password`(必填), `remember`(可选, 默认 `false`) | 无 |
| `POST /auth/sso-login` | 无 | 无 | `key`(必填, 与服务端共享密钥一致) | 无 |
| `POST /auth/logout` | 无 | 无 | 无 | 无 |
| `GET /auth/navigation` | 无 | 无 | 无 | 无 |
| `POST /auth/change-password` | 无 | 无 | `currentPassword`(必填), `newPassword`(必填, 至少 6 位) | 无 |

CLI 参数对应：

- `psims auth login <username> [-p|--password <pwd>] [--remember] [--no-save]`
- `psims auth sso-login [--key <secret> | -d '{"key":"..."}'] [--no-save]`
- `psims auth change-password <-d <json> | -f <path>>`

字段级完整参数查询（CLI）：

- `psims spec show auth login`
- `psims spec show auth sso-login`
- `psims spec show auth change-password`

## HTTP 对照（直连 curl 时）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/auth/login` | Body：`{ "username", "password", "remember" }` |
| POST | `/auth/sso-login` | Body：`{ "key" }`，与服务端 SSO 配置一致 |
| POST | `/auth/logout` | 需 Bearer |
| GET | `/auth/navigation` | 需 Bearer；`data` 含菜单、权限、`routes` |
| POST | `/auth/change-password` | 需 Bearer；Body：`{ "currentPassword", "newPassword" }` |

成功响应体仍为统一 `Result`，登录与 SSO 接口 `data` 中含 `token`、用户信息等字段。
