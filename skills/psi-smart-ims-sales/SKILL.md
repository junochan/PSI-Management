---
name: psi-smart-ims-sales
description: >-
  智链进销存销售：通过 psims sales 管理销售订单及付款、发货、收货、取消与统计；对应 HTTP /sales/*。
  当用户处理订单履约状态、或脚本导出销售与统计时使用。
---

# 销售（订单履约）

## 功能与作用

销售订单典型流程：**创建/修改** → **付款** → **发货**（填写物流等）→ **收货** → 或 **取消**。统计接口用于仪表盘与销售分析。

对应前端：`/sales`、销售单详情。

**说明**：前端 `api/index.js` 中曾定义 `GET /sales/shipping`，当前后端 **未提供** 该路由；发货请使用 **`POST /sales/orders/{id}/shipping`**（CLI：`psims sales orders shipping <id>`）。

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
| `shipping <id>` | 确认发货（Body：`ShippingDTO`） |
| `received <id>` | 确认收货 |
| `cancel <id>` | 取消订单 |

### 统计

```bash
psims sales stats
```

## HTTP 对照

- `GET|POST|PUT`：`/sales/orders` 及子路径 `payment`、`shipping`、`received`、`cancel`
- `GET /sales/stats`
