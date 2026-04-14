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
- **统计**：采购侧汇总数据（金额、笔数等，以后端 `PurchaseStats` 为准）。

对应前端：`/purchase`、采购单详情/编辑、入库详情。

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
| `inbound <id>` | 对该订单执行采购入库，Body 为入库 DTO |

### 入库单 `psims purchase inbounds <子命令>`

```bash
psims purchase inbounds list -q "{}"
psims purchase inbounds get 1
```

### 统计

```bash
psims purchase stats
```

## HTTP 对照

- 订单：`/purchase/orders`、 `/purchase/orders/{id}`、 `cancel`、`DELETE`、`POST .../inbound`
- 入库：`/purchase/inbounds`、`/purchase/inbounds/{id}`
- 统计：`GET /purchase/stats`
