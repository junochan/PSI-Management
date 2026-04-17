---
name: psi-smart-ims-customers
description: >-
  智链进销存客户主数据：通过 psims customers 完成列表、详情、增删改、批量删除；对应 HTTP /customers。
  当用户维护销售客户资料、销售单选择客户或批量清理测试数据时使用。
---

# 客户

## 编辑与写操作前确认（强制）

在**执行任何会改动系统或仓库状态的操作之前**，必须先向用户**说明拟执行动作**（含影响范围、关键参数：路径、环境、`id`/单号、请求体或文件变更摘要等），并得到用户**明确同意**（例如「确认」「可以执行」「按这个来」）后，才可执行。

涵盖但不限于：

- **代码与配置**：创建/修改/删除仓库内文件、批量替换、会改写工作区或生成物的命令。
- **业务写操作**：`psims` 或 HTTP 中带 `create`、`update`、`delete`、`batch-delete` 等**会改数据或非纯查询**的调用。
- **环境与依赖**：用户未事先声明可自动执行时，`npm install` 等会写入磁盘的操作；直接改库、清缓存等。

**可不经确认**：只读排查（读文件、`list`/`get` 等纯读接口）、纯口头方案及文档说明。

若用户已在**同一条消息**中明确授权某一具体动作（含范围），可视为已确认，但执行前仍应**简短复述**将运行的命令或写入点，避免误操作。

## 功能与作用

客户是 **销售与售后** 的基础档案，列表支持分页与筛选。

**GET 含中文的 `keyword` 等**：须 URL 编码，否则 Tomcat 报 `Invalid character found in the request target`；`psims customers list -q` 由 axios 编码。详见 **`psi-smart-ims-overview`** →「GET 查询串编码（Tomcat / Agent 手写 URL）」。

## CLI 调用

```bash
psims customers list -q "{\"current\":1,\"size\":10}"
psims customers get 1
psims customers create -d "{\"customerName\":\"某客户\"}"
psims customers update 1 -f ./customer.json
psims customers delete 1
psims customers batch-delete -d "[1,2,3]"
```

| 子命令 | 说明 |
|--------|------|
| `list` | `-q` 查询 JSON |
| `get <id>` | 详情 |
| `create` / `update <id>` / `delete <id>` | 标准 CRUD |
| `batch-delete` | id 数组 |

## 接口参数清单（按技能内接口）

| 接口 | 路径参数 | Query 参数 | Body 参数 | 文件参数 |
|------|----------|------------|-----------|----------|
| `GET /customers` | 无 | `page`,`size`,`sort`,`order`,`keyword`,`productId`,`warehouseId`,`customerId`,`supplierId`,`categoryName`,`productStatus`,`stagnantStatus`,`inboundStatus`,`payStatus`,`salesOrderStatus`,`aftersalesStatus`,`lastOutboundStart`,`lastOutboundEnd`,`lastInboundStart`,`lastInboundEnd`,`expectDateStart`,`expectDateEnd`,`createTimeStart`,`createTimeEnd`,`operatorName`（均可选） | 无 | 无 |
| `GET /customers/{id}` | `id`(必填) | 无 | 无 | 无 |
| `POST /customers` | 无 | 无 | `name`(必填),`code`(可选),`type`(可选),`contactPerson`(可选, 兼容 `contact`),`contactPhone`(可选, 兼容 `phone`),`email`(可选),`address`(可选),`vipLevel`(可选),`remark`(可选) | 无 |
| `PUT /customers/{id}` | `id`(必填) | 无 | 同 `POST /customers` 字段 | 无 |
| `DELETE /customers/{id}` | `id`(必填) | 无 | 无 | 无 |
| `DELETE /customers/batch` | 无 | 无 | `ids`(必填, `number[]`) | 无 |

CLI 参数对应：`list` 用 `-q`；`create/update/batch-delete` 用 `-d` 或 `-f`。

字段级完整参数查询（CLI）：

- `psims spec show customers list`
- `psims spec show customers create`
- `psims spec show customers update`
- `psims spec show customers batch-delete`

## 写操作执行规范（必须）

涉及客户数据变更的操作（`create`、`update`、`delete`、`batch-delete`）必须先完成以下步骤：

1. 先确认并补齐必填字段（如客户名称、联系方式、地址、结算信息等接口要求字段）。
2. 信息不完整时先引导用户补齐，禁止直接执行写命令。
3. 执行前输出变更摘要（目标客户、关键字段、影响范围）。
4. 仅在用户明确确认后再执行。
5. 删除/批量删除前必须再次提示不可逆影响并请求确认。

## HTTP 对照

| 方法 | 路径 |
|------|------|
| GET | `/customers` |
| GET | `/customers/{id}` |
| POST | `/customers` |
| PUT | `/customers/{id}` |
| DELETE | `/customers/{id}` |
| DELETE | `/customers/batch` |
