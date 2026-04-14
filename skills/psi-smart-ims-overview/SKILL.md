---
name: psi-smart-ims-overview
description: >-
  智链进销存 smart-ims 全局说明：在 Linux 环境下使用 psims CLI（安装、全局参数）、JWT、API 基址、统一响应与技能索引；仪表盘业务说明见独立技能 psi-smart-ims-dashboard。
  当用户在 Linux 服务器/容器、OpenClaw 或 CI 中通过命令行操作后端、脚本化拉数、或替代手写 curl 调用 /api/v1 时使用本技能。
---

# 智链进销存总览（API + CLI）

## 能力与作用

业务侧提供 **HTTP JSON API**（Spring Boot）与前端（Vue）。**本技能自带 `psims` CLI 全部源码**（与本 `SKILL.md` 同级的 `psi-cli/`），**不依赖仓库其它目录**；单独拷贝 `psi-smart-ims-overview/` 即可安装运行。

- 在终端或自动化脚本中完成与 Web 前端相同的业务操作（需登录后的 JWT）。
- 由 OpenClaw / Agent 通过「读技能 + 执行命令」完成调用，避免手写 URL 与 Header。

### 目录结构（自包含）

```text
psi-smart-ims-overview/
  SKILL.md
  psi-cli/
    package.json
    package-lock.json
    bin/psims.mjs
    src/
```

第三方依赖（axios、commander、form-data）通过 **`npm install`** 安装；随技能提供的是 **源码与 lockfile**，通常不提交 `node_modules/`（见 `psi-cli/.gitignore`）。

## CLI：`psims`（推荐调用方式，环境以 **Linux** 为主）

### 环境与安装

- **运行时**：Node.js ≥ 18（Linux 服务器、WSL 或容器内均可）。
- 在 **`psi-cli/`** 目录首次安装依赖：

```bash
cd psi-cli && npm install
```

（若你站在技能根目录 `psi-smart-ims-overview/` 下，路径即为上述 `psi-cli/`。）

### 调用入口（任选其一）

```bash
# 在 psi-cli 目录内（相对路径最短）
cd psi-cli && node bin/psims.mjs --help
cd psi-cli && npx psims --help

# 从本仓库根目录（整仓检出时）
node skills/psi-smart-ims-overview/psi-cli/bin/psims.mjs --help
```

### 让 `psims` 成为全局命令（可选）

若希望任意目录直接输入 `psims`：

```bash
cd psi-cli && npm install && npm link
```

确保 **npm 全局 bin**（一般为 `$(npm prefix -g)/bin`）已在 `PATH` 中；否则仍请使用 **`node <技能目录>/psi-cli/bin/psims.mjs`** 的绝对路径，避免依赖 PATH。

### 全局参数（写在子命令之前）

| 参数 | 作用 |
|------|------|
| `--base-url <url>` | API 根，**须含** `/api/v1`，默认 `http://localhost:8080/api/v1`，也可用环境变量 `PSI_API_BASE` |
| `--token <jwt>` | 显式传入 Bearer，覆盖环境变量 `PSI_TOKEN` 与登录保存的文件 |
| `--timeout <ms>` | HTTP 超时，默认 `120000`（以图搜图等长耗时接口建议保持较大值） |

### 登录与 Token 持久化

1. 执行 `psims auth login <用户名> -p <密码>`（不传 `-p` 会在终端询问密码）。
2. 默认将返回的 JWT 写入 **`$HOME/.psi-smart-ims/token`**（与运行用户一致；容器内需保证该目录可写）。
3. 后续子命令自动从该文件读取 token；也可用 `--token` 或 `PSI_TOKEN` 覆盖。
4. `psims auth token-path` 可打印 token 文件绝对路径。
5. `psims auth logout` 会调用后端登出并删除本地 token 文件。

### 列表与查询参数

多数 `list` 子命令支持 `-q / --query`，值为 **JSON 字符串**，会作为 URL 查询参数发送（与前端 `params` 一致），例如：

```bash
# Bash 下可用单引号包住 JSON，避免转义双引号
psims products list -q '{"current":1,"size":10}'
```

### 写操作 Body

`create` / `update` 等支持：

- `-d / --data '<json>'`：内联 JSON；
- `-f / --file <path>`：从文件读取 JSON（适合大 Body）。

### 与 HTTP 的对应关系

CLI 子命令名与 `psi-manage-system-front/src/api/index.js` 中的封装 **大体** 对应；底层即对 `GET/POST/PUT/DELETE http(s)://<host>:8080/api/v1/...` 发请求。

**与前端 `authApi.navigation` / `supplierIndustryApi` / `userApi.uploadAvatar` 对齐的命令**（均需 Bearer，先 `auth login`）；**仪表盘**见技能 **`psi-smart-ims-dashboard`**。

```bash
psims auth navigation
psims supplier-industries
psims users upload-avatar --file ./avatar.png
```

## 后端与路径（直连 HTTP 时）

- **端口**：默认 `8080`（`application.yml`）
- **context-path**：`/api`
- **控制器前缀**：`/v1`
- **完整根路径**：`http://<host>:8080/api/v1`

## 鉴权（HTTP）

- 除白名单接口外需：`Authorization: Bearer <token>`
- 登录：`POST /api/v1/auth/login`，`data.token` 为 JWT
- `401`：需重新登录

## 统一响应

```json
{ "code": 200, "message": "...", "data": ... }
```

成功时业务数据在 **`data`**；`psims` 在终端只打印解包后的 **`data`**（与前端 axios 拦截器行为一致）。

## 技能索引（按业务）

| 技能目录 | 作用 |
|----------|------|
| `psi-smart-ims-auth` | 登录、登出、token 路径、`auth navigation`（`GET /auth/navigation`） |
| `psi-smart-ims-dashboard` | 仪表盘聚合数据、`dashboard overview`（`GET /dashboard/overview`） |
| `psi-smart-ims-products` | 商品、分类、图片、以图搜图 |
| `psi-smart-ims-suppliers` | 供应商、`supplier-industries`（`GET /supplier-industries`） |
| `psi-smart-ims-customers` | 客户 |
| `psi-smart-ims-warehouses` | 仓库 |
| `psi-smart-ims-purchase` | 采购订单、采购入库、统计 |
| `psi-smart-ims-sales` | 销售订单、付款发货收货、统计 |
| `psi-smart-ims-inventory` | 库存、调拨、预警、出入库、以图搜图 |
| `psi-smart-ims-aftersales` | 售后 |
| `psi-smart-ims-system` | 用户（含 `users upload-avatar`）、角色权限、日志、工具 |

## OpenClaw 使用建议

1. 优先用 **`psims <模块> ...`**，少用手拼 curl。
2. 需要上传图片时用 `psims products upload-image --file <路径>` 或 **`psims users upload-avatar --file <路径>`**（multipart 已由 CLI 处理）。
3. 以图搜图耗时长，保留默认 `--timeout` 或更大。
4. 仪表盘：读 **`psi-smart-ims-dashboard`**，命令 `psims dashboard overview`；动态菜单：`psims auth navigation`（需先登录）。
