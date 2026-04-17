---
name: psi-smart-ims-sales
description: >-
  智链进销存销售：通过 psims sales 管理销售订单及付款、发货、收货、取消与统计；对应 HTTP /sales/*。
  当用户处理订单履约状态、或脚本导出销售与统计时使用。
---

# 销售（订单履约）

## 编辑与写操作前确认（强制）

在**执行任何会改动系统或仓库状态的操作之前**，必须先向用户**说明拟执行动作**（含影响范围、关键参数：路径、环境、`id`/单号、请求体或文件变更摘要等），并得到用户**明确同意**（例如「确认」「可以执行」「按这个来」）后，才可执行。

涵盖但不限于：

- **代码与配置**：创建/修改/删除仓库内文件、批量替换、会改写工作区或生成物的命令。
- **业务写操作**：`psims` 或 HTTP 中带 `create`、`update`、`delete`、`cancel`、付款/发货/收货等**会改数据或非纯查询**的调用。
- **环境与依赖**：用户未事先声明可自动执行时，`npm install` 等会写入磁盘的操作；直接改库、清缓存等。

**可不经确认**：只读排查（读文件、`list`/`get`/统计等纯读接口）、纯口头方案及文档说明。

若用户已在**同一条消息**中明确授权某一具体动作（含范围），可视为已确认，但执行前仍应**简短复述**将运行的命令或写入点，避免误操作。

## 功能与作用

销售订单典型流程：**创建/修改** → **付款** → **发货**（填写物流等）→ **收货** → 或 **取消**。统计接口用于仪表盘与销售分析。

适用于销售单详情、履约流转与销售统计场景。

**GET 含中文的 `keyword` 等**：须 URL 编码，否则 Tomcat 报 `Invalid character found in the request target`；`psims sales orders list -q` 由 axios 编码。详见 **`psi-smart-ims-overview`** →「GET 查询串编码（Tomcat / Agent 手写 URL）」。

**说明**：发货列表无独立 REST 资源；在途/已发状态以 **销售订单** 列表与详情字段为准。确认发货请使用 **`POST /sales/orders/{id}/shipping`**（CLI：`psims sales orders shipping <id>`）。

**发货请求体**：`warehouseId`、`quantity`、**`logisticsCompany`、`logisticsNo`、`receiverName`、`receiverPhone`、`receiverAddress` 为必填**（与前端发货表单一致）；服务端对 `ShippingDTO` 做 Bean 校验，缺省或空串会返回校验错误。`receiverPhone` 最长 100 字符（兼容主号与备用号合并）。已有库若仍为 `varchar(20)`，请执行 `docs/database/patch_sales_order_receiver_phone_v100.sql`。

## CLI 调用

### 销售订单 `psims sales orders <子命令>`

```bash
psims sales orders list -q "{\"current\":1,\"size\":10}"
psims sales orders get 1
psims sales orders create -f ./so.json
psims sales orders update 1 -d "{...}"
psims sales orders payment 1
psims sales orders shipping 1 -f ./shipping.json
psims sales orders received 1
psims sales orders cancel 1
```

| 子命令 | 作用 |
|--------|------|
| `list` / `get <id>` | 列表与详情 |
| `create` / `update <id>` | 新建与修改 |
| `payment <id>` | 确认付款 |
| `shipping <id>` | 确认发货（Body：发货请求 JSON） |
| `received <id>` | 确认收货 |
| `cancel <id>` | 取消订单 |

### 统计

```bash
psims sales stats
```

## 接口参数清单（按技能内接口）

### 销售订单

| 接口 | 路径参数 | Query 参数 | Body 参数 | 文件参数 |
|------|----------|------------|-----------|----------|
| `GET /sales/orders` | 无 | `page`,`size`,`sort`,`order`,`keyword`,`productId`,`warehouseId`,`customerId`,`supplierId`,`categoryName`,`productStatus`,`stagnantStatus`,`inboundStatus`,`payStatus`,`salesOrderStatus`,`aftersalesStatus`,`lastOutboundStart`,`lastOutboundEnd`,`lastInboundStart`,`lastInboundEnd`,`expectDateStart`,`expectDateEnd`,`createTimeStart`,`createTimeEnd`,`operatorName`（均可选） | 无 | 无 |
| `GET /sales/orders/{id}` | `id`(必填) | 无 | 无 | 无 |
| `POST /sales/orders` | 无 | 无 | `customerId`(必填),`productId`(必填),`quantity`(必填),`unitPrice`(必填),`warehouseId`(可选),`payMethod`(可选),`receiverName`(可选),`receiverPhone`(可选),`receiverAddress`(可选),`remark`(可选) | 无 |
| `PUT /sales/orders/{id}` | `id`(必填) | 无 | 同 `POST /sales/orders` 字段 | 无 |
| `PUT /sales/orders/{id}/payment` | `id`(必填) | 无 | 无 | 无 |
| `POST /sales/orders/{id}/shipping` | `id`(必填) | 无 | `warehouseId`(必填),`quantity`(必填),`logisticsCompany`(必填),`logisticsNo`(必填),`receiverName`(必填),`receiverPhone`(必填),`receiverAddress`(必填),`remark`(可选) | 无 |
| `PUT /sales/orders/{id}/received` | `id`(必填) | 无 | 无 | 无 |
| `PUT /sales/orders/{id}/cancel` | `id`(必填) | 无 | 无 | 无 |
| `GET /sales/stats` | 无 | 无 | 无 | 无 |

CLI 参数对应：`list` 用 `-q`；`create/update/shipping` 用 `-d` 或 `-f`。

字段级完整参数查询（CLI）：

- `psims spec show sales orders list`
- `psims spec show sales orders create`
- `psims spec show sales orders update`
- `psims spec show sales orders shipping`

## 写操作执行规范（必须）

涉及销售订单状态与数据变更的操作（`create`、`update`、`payment`、`shipping`、`received`、`cancel`）必须遵循：

1. 先确认并补齐必填字段（如客户、商品明细、数量、金额、物流信息等）。
2. 信息不完整时先引导用户补齐，禁止直接执行写命令。
3. 执行前输出变更摘要（订单、关键字段、状态流转与影响）。
4. 仅在用户明确确认后再执行。
5. `shipping`、`received`、`cancel` 等状态推进/终止操作需再次提示业务影响后再执行。

## HTTP 对照

- `GET|POST|PUT`：`/sales/orders` 及子路径 `payment`、`shipping`、`received`、`cancel`
- `GET /sales/stats`
