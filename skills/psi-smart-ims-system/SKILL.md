---
name: psi-smart-ims-system
description: >-
  智链进销存系统管理：通过 psims 管理用户、角色与权限、查看操作日志，以及 util-encode 生成 BCrypt；另有 POST /users/avatar 头像上传（multipart file）。
  当用户开户改权、上传头像、审计操作记录、或初始化测试账号密码哈希时使用。
---

# 系统设置（用户 / 角色 / 日志 / 工具）

## 编辑与写操作前确认（强制）

在**执行任何会改动系统或仓库状态的操作之前**，必须先向用户**说明拟执行动作**（含影响范围、关键参数：路径、环境、`id`/单号、请求体或文件变更摘要等），并得到用户**明确同意**（例如「确认」「可以执行」「按这个来」）后，才可执行。

涵盖但不限于：

- **代码与配置**：创建/修改/删除仓库内文件、批量替换、会改写工作区或生成物的命令。
- **业务写操作**：`psims` 或 HTTP 中带用户/角色/权限的增删改、头像上传、改密、`util-encode` 运维写入等**会改数据或非纯查询**的调用。
- **环境与依赖**：用户未事先声明可自动执行时，`npm install` 等会写入磁盘的操作；直接改库、清缓存等。

**可不经确认**：只读排查（读文件、`list`/`get`/操作日志查询等纯读接口）、纯口头方案及文档说明。

若用户已在**同一条消息**中明确授权某一具体动作（含范围），可视为已确认，但执行前仍应**简短复述**将运行的命令或写入点，避免误操作。

## 功能与作用

- **用户**：后台账号的增删改查；`all` 用于下拉选择等不分页场景；**头像** 通过 `POST /users/avatar`（`multipart/form-data`）上传，响应 `data.url` 写入用户资料的 `avatar` 字段。
- **角色与权限**：维护角色，及 **角色-权限** 绑定（权限 id 数组）。
- **操作日志**：按条件分页查询审计日志。
- **util-encode**：服务端提供的 **BCrypt 明文转哈希**（仅测试/运维，返回 **纯文本** 非 JSON Result）。

适用于系统设置、账号权限管理与运维排障场景。

**GET 含中文的 `keyword` 等**（用户列表、操作日志等）：须 URL 编码，否则 Tomcat 报 `Invalid character found in the request target`；`psims ... list -q` 由 axios 编码。详见 **`psi-smart-ims-overview`** →「GET 查询串编码（Tomcat / Agent 手写 URL）」。

## CLI 调用

### 用户 `psims users <子命令>`

```bash
psims users list -q "{}"
psims users all
psims users get 1
psims users create -f ./user.json
psims users update 1 -d "{\"phone\":\"...\"}"
psims users delete 1
psims users upload-avatar --file ./avatar.png
```

头像上传对应 `POST /users/avatar`，`multipart/form-data` 字段 **`file`**，终端打印解包后的 `data`（含 `url`）。

### 角色 `psims roles <子命令>`

```bash
psims roles list
psims roles get 1
psims roles create -f ./role.json
psims roles update 1 -d "{...}"
psims roles delete 1
psims roles permissions 1
psims roles permissions-all
psims roles set-permissions 1 -d "[1,2,3]"
```

### 操作日志

```bash
psims logs -q "{\"current\":1,\"size\":20}"
```

### 密码哈希（非 JSON 输出）

```bash
psims util-encode -p "plain-password"
```

该命令直接打印服务端返回的 **多行文本**（明文 + BCrypt），与统一 `Result` JSON 不同。

## 接口参数清单（按技能内接口）

### 用户

| 接口 | 路径参数 | Query 参数 | Body 参数 | 文件参数 |
|------|----------|------------|-----------|----------|
| `GET /users` | 无 | `page`,`size`,`sort`,`order`,`keyword`,`productId`,`warehouseId`,`customerId`,`supplierId`,`categoryName`,`productStatus`,`stagnantStatus`,`inboundStatus`,`payStatus`,`salesOrderStatus`,`aftersalesStatus`,`lastOutboundStart`,`lastOutboundEnd`,`lastInboundStart`,`lastInboundEnd`,`expectDateStart`,`expectDateEnd`,`createTimeStart`,`createTimeEnd`,`operatorName`（均可选） | 无 | 无 |
| `GET /users/all` | 无 | 无 | 无 | 无 |
| `GET /users/{id}` | `id`(必填) | 无 | 无 | 无 |
| `POST /users` | 无 | 无 | `username`(必填),`name`(必填),`email`(可选),`phone`(可选),`roleId`(可选),`password`(可选),`status`(可选),`avatar`(可选) | 无 |
| `PUT /users/{id}` | `id`(必填) | 无 | 同 `POST /users` 字段 | 无 |
| `DELETE /users/{id}` | `id`(必填) | 无 | 无 | 无 |
| `POST /users/avatar` | 无 | 无 | 无 | `file`(必填, 图片) |

### 角色与权限

| 接口 | 路径参数 | Query 参数 | Body 参数 | 文件参数 |
|------|----------|------------|-----------|----------|
| `GET /roles` | 无 | 无 | 无 | 无 |
| `GET /roles/{id}` | `id`(必填) | 无 | 无 | 无 |
| `POST /roles` | 无 | 无 | `name`(可选),`code`(可选),`description`(可选),`status`(可选) | 无 |
| `PUT /roles/{id}` | `id`(必填) | 无 | 同 `POST /roles` 字段 | 无 |
| `DELETE /roles/{id}` | `id`(必填) | 无 | 无 | 无 |
| `GET /roles/{id}/permissions` | `id`(必填) | 无 | 无 | 无 |
| `GET /roles/permissions` | 无 | 无 | 无 | 无 |
| `PUT /roles/{id}/permissions` | `id`(必填) | 无 | `permissionIds`(必填, `number[]`) | 无 |

### 日志与工具

| 接口 | 路径参数 | Query 参数 | Body 参数 | 文件参数 |
|------|----------|------------|-----------|----------|
| `GET /logs` | 无 | `page`,`size`,`sort`,`order`,`keyword`,`productId`,`warehouseId`,`customerId`,`supplierId`,`categoryName`,`productStatus`,`stagnantStatus`,`inboundStatus`,`payStatus`,`salesOrderStatus`,`aftersalesStatus`,`lastOutboundStart`,`lastOutboundEnd`,`lastInboundStart`,`lastInboundEnd`,`expectDateStart`,`expectDateEnd`,`createTimeStart`,`createTimeEnd`,`operatorName`（均可选） | 无 | 无 |
| `GET /util/encode` | 无 | `password`(必填) | 无 | 无 |

CLI 参数对应：

- 列表：`-q|--query <json>`
- 写入 Body：`-d|--data <json>` 或 `-f|--file <path>`
- 上传头像：`psims users upload-avatar --file <path>`
- 密码加密：`psims util-encode -p <password>`

字段级完整参数查询（CLI）：

- `psims spec show users create`
- `psims spec show users update`
- `psims spec show roles create`
- `psims spec show roles set-permissions`
- `psims spec show logs`

## HTTP 对照

| 区域 | 路径 |
|------|------|
| 用户 | `/users`、`/users/all`、`/users/{id}`、`POST /users/avatar`（multipart） |
| 角色 | `/roles`、`/roles/{id}`、`/roles/{id}/permissions`、`/roles/permissions` |
| 日志 | `GET /logs` |
| 工具 | `GET /util/encode?password=`（纯文本响应） |
