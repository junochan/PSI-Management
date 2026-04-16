---
name: psi-smart-ims-purchase
description: >-
  智链进销存采购：通过 psims purchase 管理采购订单、采购入库、入库单查询与采购统计；对应 HTTP /purchase/*。
  当用户从下单到收货入库全流程、或脚本拉采购/入库报表时使用。
---

# 采购（订单与入库）

## 功能与作用

- **采购订单**：向供应商下单，可后续 **取消**、**删除**、或执行 **采购入库**（货物入账）。
- **入库单**：查询由采购产生的入库记录及明细。
- **统计**：采购侧汇总数据（金额、笔数等，以接口返回字段为准）。

适用于采购单详情/编辑、采购入库与采购统计场景。

## CLI 调用

### 采购订单 `psims purchase orders <子命令>`

```bash
psims purchase orders list -q "{\"current\":1,\"size\":10}"
psims purchase orders get 1
psims purchase orders create -f ./po.json
psims purchase orders update 1 -d "{...}"
psims purchase orders cancel 1
psims purchase orders delete 1
psims purchase orders inbound 1 -f ./inbound.json
```

| 子命令 | 作用 |
|--------|------|
| `list` | 订单分页列表 |
| `get <id>` | 订单详情 |
| `create` | 新建订单 |
| `update <id>` | 修改订单 |
| `cancel <id>` | 取消 |
| `delete <id>` | 删除 |
| `inbound <id>` | 对该订单执行采购入库，Body 为入库请求 JSON |

### 入库单 `psims purchase inbounds <子命令>`

```bash
psims purchase inbounds list -q "{}"
psims purchase inbounds get 1
```

### 统计

```bash
psims purchase stats
```

## 接口参数清单（按技能内接口）

### 采购订单

| 接口 | 路径参数 | Query 参数 | Body 参数 | 文件参数 |
|------|----------|------------|-----------|----------|
| `GET /purchase/orders` | 无 | `page`,`size`,`sort`,`order`,`keyword`,`productId`,`warehouseId`,`customerId`,`supplierId`,`categoryName`,`productStatus`,`stagnantStatus`,`inboundStatus`,`payStatus`,`salesOrderStatus`,`aftersalesStatus`,`lastOutboundStart`,`lastOutboundEnd`,`lastInboundStart`,`lastInboundEnd`,`expectDateStart`,`expectDateEnd`,`createTimeStart`,`createTimeEnd`,`operatorName`（均可选） | 无 | 无 |
| `GET /purchase/orders/{id}` | `id`(必填) | 无 | 无 | 无 |
| `POST /purchase/orders` | 无 | 无 | `supplierId`(必填),`productId`(必填),`quantity`(必填),`unitPrice`(必填),`expectDate`(可选),`warehouseId`(可选),`payMethod`(可选),`remark`(可选) | 无 |
| `PUT /purchase/orders/{id}` | `id`(必填) | 无 | 同 `POST /purchase/orders` 字段 | 无 |
| `PUT /purchase/orders/{id}/cancel` | `id`(必填) | 无 | 无 | 无 |
| `DELETE /purchase/orders/{id}` | `id`(必填) | 无 | 无 | 无 |
| `POST /purchase/orders/{id}/inbound` | `id`(必填) | 无 | `warehouseId`(必填),`quantity`(必填),`batchNo`(可选),`remark`(可选) | 无 |

### 入库单与统计

| 接口 | 路径参数 | Query 参数 | Body 参数 | 文件参数 |
|------|----------|------------|-----------|----------|
| `GET /purchase/inbounds` | 无 | `page`,`size`,`sort`,`order`,`keyword`,`productId`,`warehouseId`,`customerId`,`supplierId`,`categoryName`,`productStatus`,`stagnantStatus`,`inboundStatus`,`payStatus`,`salesOrderStatus`,`aftersalesStatus`,`lastOutboundStart`,`lastOutboundEnd`,`lastInboundStart`,`lastInboundEnd`,`expectDateStart`,`expectDateEnd`,`createTimeStart`,`createTimeEnd`,`operatorName`（均可选） | 无 | 无 |
| `GET /purchase/inbounds/{id}` | `id`(必填) | 无 | 无 | 无 |
| `GET /purchase/stats` | 无 | 无 | 无 | 无 |

CLI 参数对应：`list` 用 `-q`；`create/update/inbound` 用 `-d` 或 `-f`。

字段级完整参数查询（CLI）：

- `psims spec show purchase orders list`
- `psims spec show purchase orders create`
- `psims spec show purchase orders update`
- `psims spec show purchase orders inbound`

## 写操作执行规范（必须）

涉及采购数据变更的操作（`create`、`update`、`cancel`、`delete`、`inbound`）必须遵循：

1. 先确认并补齐必填字段（如供应商、商品明细、数量、价格、订单/入库相关字段等）。
2. 信息不完整时先引导用户补齐，禁止直接执行写命令。
3. 执行前输出变更摘要（单据编号或目标对象、关键字段、预期库存影响）。
4. 仅在用户明确确认后再执行。
5. `cancel`、`delete`、`inbound` 属于高影响操作，需再次提示影响范围后再执行。

## HTTP 对照

- 订单：`/purchase/orders`、 `/purchase/orders/{id}`、 `cancel`、`DELETE`、`POST .../inbound`
- 入库：`/purchase/inbounds`、`/purchase/inbounds/{id}`
- 统计：`GET /purchase/stats`
