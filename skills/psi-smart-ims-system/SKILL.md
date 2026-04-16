---
name: psi-smart-ims-system
description: >-
  智链进销存系统管理：通过 psims 管理用户、角色与权限、查看操作日志，以及 util-encode 生成 BCrypt；另有 POST /users/avatar 头像上传（multipart file）。
  当用户开户改权、上传头像、审计操作记录、或初始化测试账号密码哈希时使用。
---

# 系统设置（用户 / 角色 / 日志 / 工具）

## 功能与作用

- **用户**：后台账号的增删改查；`all` 用于下拉选择等不分页场景；**头像** 通过 `POST /users/avatar`（`multipart/form-data`）上传，响应 `data.url` 写入用户资料的 `avatar` 字段。
- **角色与权限**：维护角色，及 **角色-权限** 绑定（权限 id 数组）。
- **操作日志**：按条件分页查询审计日志。
- **util-encode**：服务端提供的 **BCrypt 明文转哈希**（仅测试/运维，返回 **纯文本** 非 JSON Result）。

适用于系统设置、账号权限管理与运维排障场景。

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
