---
name: psi-smart-ims-sales
description: >-
  智链进销存销售：通过 psims sales 管理销售订单及付款、发货、收货、取消与统计；对应 HTTP /sales/*。
  当用户处理订单履约状态、或脚本导出销售与统计时使用。
---

# 销售（订单履约）

## 功能与作用

销售订单典型流程：**创建/修改** → **付款** → **发货**（填写物流等）→ **收货** → 或 **取消**。统计接口用于仪表盘与销售分析。

适用于销售单详情、履约流转与销售统计场景。

**说明**：发货列表无独立 REST 资源；在途/已发状态以 **销售订单** 列表与详情字段为准。确认发货请使用 **`POST /sales/orders/{id}/shipping`**（CLI：`psims sales orders shipping <id>`）。

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
| `POST /sales/orders/{id}/shipping` | `id`(必填) | 无 | `warehouseId`(必填),`quantity`(必填),`logisticsCompany`(可选),`logisticsNo`(可选),`receiverName`(可选),`receiverPhone`(可选),`receiverAddress`(可选),`remark`(可选) | 无 |
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
