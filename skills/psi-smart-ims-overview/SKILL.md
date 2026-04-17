---
name: psi-smart-ims-overview
description: >-
  智链进销存 smart-ims 全局说明：在 Linux 环境下使用 psims CLI（安装、全局参数）、JWT、API 基址、统一响应与技能索引；仪表盘业务说明见独立技能 psi-smart-ims-dashboard。
  当用户在 Linux 服务器/容器、OpenClaw 或 CI 中通过命令行操作系统 API、脚本化拉数、或替代手写 curl 调用 /api/v1 时使用本技能。
---

# 智链进销存总览（API + CLI）

## 编辑与写操作前确认（强制）

在**执行任何会改动系统或仓库状态的操作之前**，必须先向用户**说明拟执行动作**（含影响范围、关键参数：路径、环境、`id`/单号、请求体或文件变更摘要等），并得到用户**明确同意**（例如「确认」「可以执行」「按这个来」）后，才可执行。

涵盖但不限于：

- **代码与配置**：创建/修改/删除仓库内文件、批量替换、会改写工作区或生成物的命令。
- **业务写操作**：`psims` 或 HTTP 中一切**非幂等或会改数据**的调用（各域技能的 `create`/`update`/`delete`/`cancel`/`confirm` 等）。
- **环境与依赖**：用户未事先声明可自动执行时，`npm install` 等会写入磁盘的操作；直接改库、清缓存等。

**可不经确认**：只读排查（读文件、`list`/`get`/统计等纯读接口）、纯口头方案及文档说明。

若用户已在**同一条消息**中明确授权某一具体动作（含范围），可视为已确认，但执行前仍应**简短复述**将运行的命令或写入点，避免误操作。

## 能力与作用

业务侧提供 **HTTP JSON API**。**本技能自带 `psims` CLI 全部源码**（与本 `SKILL.md` 同级的 `psi-cli/`），**不依赖仓库其它目录**；单独拷贝 `psi-smart-ims-overview/` 即可安装运行。

- 在终端或自动化脚本中完成与 Web 系统一致的业务操作（需登录后的 JWT）。
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
5. `psims auth logout` 会调用登出接口并删除本地 token 文件。

### 列表与查询参数

多数 `list` 子命令支持 `-q / --query`，值为 **JSON 字符串**，会作为 URL 查询参数发送，例如：

```bash
# Bash 下可用单引号包住 JSON，避免转义双引号
psims products list -q '{"current":1,"size":10}'
```

### GET 查询串编码（Tomcat / Agent 手写 URL）

嵌入式 Tomcat 按 **RFC 7230** 校验 HTTP 请求行：**请求目标里不能出现未做百分号编码的非 ASCII**（例如中文若原样出现在 `?keyword=` 之后）。技能或脚本调接口时若拼出「明文中文查询串」，典型异常为：

```text
java.lang.IllegalArgumentException: Invalid character found in the request target
```

约定与排障：

- **`psims ... list -q '{"keyword":"中文",...}'`**：由 axios 将 `-q` JSON 展开为查询参数并做 UTF-8 百分号编码，**可直接写中文**，无需手工转义。
- **手写 `curl`、把完整 URL 贴进浏览器地址栏、或 Agent 用 `fetch`/`http.get` 且 URL 字符串内含未编码中文**：必须对每个查询值使用 **`encodeURIComponent`**（或等价方式）；否则凡带 `keyword` 等文本筛选的 **GET**（如 `/products`、`/inventory`、`/sales/orders` 等）均可能触发上述错误。
- **Knife4j / Swagger「Try it out」**：若工具发出的请求行未编码，同样会 400；排障时优先用 `psims` 对照。
- **本仓库**：后端 `application.yml` 中 `server.tomcat.relaxed-query-chars` 仅放宽 **部分 ASCII**（如 `[]{}|`），**不能替代中文编码**；前端 `psi-manage-system-front/src/api/index.js` 已对 GET 使用 `URLSearchParams` 序列化。

### 写操作 Body

`create` / `update` 等支持：

- `-d / --data '<json>'`：内联 JSON；
- `-f / --file <path>`：从文件读取 JSON（适合大 Body）。

### 写操作执行前（强制）

凡是会修改进销存数据的命令（如 `create`、`update`、`delete`、`batch-delete`、`cancel`、`confirm`、`inbound`、`outbound`、`payment`、`shipping`、`received`、`handle`、`close`）都必须遵循以下流程：

1. **先识别必填字段**：根据对应子技能和命令参数，列出本次操作的必填项（例如 `id`、数量、仓库/商品/客户/供应商、业务原因或备注等）。
2. **缺字段先补齐**：若用户未提供完整必填信息，先引导用户逐项填写，禁止直接调用写接口。
3. **执行前二次确认**：整理一份“执行摘要”（对象、关键字段、预期影响），明确询问“是否确认执行”。
4. **收到明确确认才执行**：仅在用户给出明确同意（如“确认执行”）后发起写请求；若未确认或改口，停止执行并回到信息补齐。
5. **高风险操作加强确认**：删除、批量删除、取消、关单、出库、扣减库存等不可逆/高影响操作，需再次强调影响范围后再执行。

### 与 HTTP 的对应关系

CLI 子命令名与各业务接口路径一一对应；底层即对 `GET/POST/PUT/DELETE http(s)://<host>:8080/api/v1/...` 发请求。

以下是常用命令入口（除登录类接口外需 Bearer，先 `auth login` 或 `auth sso-login`）；**仪表盘**见技能 **`psi-smart-ims-dashboard`**。

```bash
psims auth navigation
psims auth sso-login --key "<与服务端共享配置一致的密钥>"
psims auth change-password -d "{\"currentPassword\":\"旧\",\"newPassword\":\"新密码至少6位\"}"
psims supplier-industries
psims warehouses options
psims users upload-avatar --file ./avatar.png
```

### 总览技能接口参数清单（本技能覆盖的通用接口）

> 业务域接口（商品/库存/采购/销售等）参数已在对应子技能中完整展开；本节仅列总览中直接示例和通用入口。

| 接口 | 路径参数 | Query 参数 | Body 参数 | 文件参数 |
|------|----------|------------|-----------|----------|
| `POST /auth/sso-login` | 无 | 无 | `key`(必填) | 无 |
| `GET /auth/navigation` | 无 | 无 | 无 | 无 |
| `POST /auth/change-password` | 无 | 无 | `currentPassword`(必填), `newPassword`(必填) | 无 |
| `GET /supplier-industries` | 无 | 无 | 无 | 无 |
| `GET /warehouses/options` | 无 | 无 | 无 | 无 |
| `POST /users/avatar` | 无 | 无 | 无 | `file`(必填, 图片) |

通用 CLI 参数（所有接口适用）：

- 根命令：`--base-url <url>`、`--token <jwt>`、`--timeout <ms>`
- 列表查询：`-q|--query <json>`
- JSON Body：`-d|--data <json>` 或 `-f|--file <path>`
- multipart：`--file <path>`

### CLI 参数自查（字段级完整参数）

为了保证「每个命令对应接口参数可查且完整」，`psims` 提供了参数规范查询命令：

```bash
# 列出全部命令 -> 接口映射
psims spec list

# 关键字筛选（命令名或路径）
psims spec list -q products

# 查看某个命令的完整参数（路径/query/body/file，含必填）
psims spec show products create
psims spec show sales orders shipping
psims spec show inventory warnings handle
```

约定：技能文档与 `psims spec show ...` 输出保持一致；当你需要字段级细节时，以 `spec show` 为准。

### 参数兜底方案（Swagger）

当出现以下情况时，使用 Swagger 文档作为兜底真值源：

- 线上接口字段与技能文档不一致
- `psims spec show ...` 输出与实际返回校验冲突
- 新增接口/字段尚未同步到技能

Swagger 地址（相对服务根）：

```text
/api/v3/api-docs
```

例如服务地址为 `http://localhost:8080` 时，完整地址为：

```text
http://localhost:8080/api/v3/api-docs
```

执行顺序建议：

1. 先看技能文档参数表。
2. 再看 `psims spec show <命令>`。
3. 仍有冲突时，以 `/api/v3/api-docs` 为最终依据。

## 服务地址与路径（直连 HTTP 时）

- **端口**：默认 `8080`
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

成功时业务数据在 **`data`**；`psims` 在终端只打印解包后的 **`data`**。

## 技能索引（按业务）

| 技能目录 | 作用 |
|----------|------|
| `psi-smart-ims-auth` | 登录、SSO 登录、改密、登出、token 路径、`auth navigation`（`GET /auth/navigation`） |
| `psi-smart-ims-dashboard` | 仪表盘聚合数据、`dashboard overview`（`GET /dashboard/overview`） |
| `psi-smart-ims-products` | 商品、分类、图片、以图搜图 |
| `psi-smart-ims-suppliers` | 供应商、`supplier-industries`（`GET /supplier-industries`） |
| `psi-smart-ims-customers` | 客户 |
| `psi-smart-ims-warehouses` | 仓库（含 `GET /warehouses/options` 轻量下拉） |
| `psi-smart-ims-purchase` | 采购订单、采购入库、统计 |
| `psi-smart-ims-sales` | 销售订单、付款发货收货、统计 |
| `psi-smart-ims-inventory` | 库存、调拨、预警、出入库、以图搜图 |
| `psi-smart-ims-aftersales` | 售后 |
| `psi-smart-ims-system` | 用户（含 `users upload-avatar`）、角色权限、日志、工具 |
| `psi-smart-ims-upload-image-followup` | 仅上传商品图时：默认图搜图 → 命中后查库存/库位与销售、采购订单 |

## OpenClaw 使用建议

1. 优先用 **`psims <模块> ...`**，少用手拼 curl。
2. 需要上传图片时用 `psims products upload-image --file <路径>` 或 **`psims users upload-avatar --file <路径>`**（multipart 已由 CLI 处理）。
3. 以图搜图耗时长，保留默认 `--timeout` 或更大。
4. 仪表盘：读 **`psi-smart-ims-dashboard`**，命令 `psims dashboard overview`；动态菜单：`psims auth navigation`（需先登录）。
