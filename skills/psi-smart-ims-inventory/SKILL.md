---
name: psi-smart-ims-inventory
description: >-
  智链进销存库存：通过 psims inventory 查询库存、调拨、预警处理、其它出入库、维护安全库存/库位/呆滞天数、以图搜库存；对应 HTTP /inventory/*。
  当用户盘点、移库、处理低库存预警或按图找货时使用。
---

# 库存

## 功能与作用

- **库存行**：按仓库+商品维度展示数量、安全库存、库位、最近出入库时间等。
- **调拨**：仓间/库位间移货，创建调拨单后可 **确认** 完成记账。
- **预警**：低于安全库存等条件产生预警，可 **处理** 并备注。
- **其它出入库**：简单参数走 `inbound-simple` / `outbound-simple`；复杂业务走 `inbound-new`。
- **以图搜图**：与商品类似，对库存维度做向量检索（耗时长）。

对应前端：`/inventory`、库存详情、调拨详情。

## CLI 调用

### 主数据与检索

```bash
psims inventory list -q "{}"
psims inventory get 1
psims inventory by-product 100
psims inventory search-image -f ./body.json
psims inventory stats
```

### 调拨 `psims inventory transfers <子命令>`

```bash
psims inventory transfers list -q "{}"
psims inventory transfers create -f ./transfer.json
psims inventory transfers confirm 1
```

### 预警 `psims inventory warnings <子命令>`

```bash
psims inventory warnings list -q "{}"
psims inventory warnings handle 1 --remark "已补货"
```

### 出入库

```bash
psims inventory inbound-simple --inventory-id 1 --quantity 10 --remark "盘盈"
psims inventory outbound-simple --inventory-id 1 --quantity 2
psims inventory inbound-new -f ./in.json
psims inventory outbounds-list -q "{}"
```

### 属性

```bash
psims inventory set-safe-stock 1 --safe-stock 100
psims inventory set-location 1 --location "A-01"
psims inventory set-stagnant-days 1 --days 90
```

## HTTP 对照（节选）

- `GET /inventory`、`/inventory/{id}`、`/inventory/product/{productId}`
- `POST /inventory/search-by-image`
- `GET|POST /inventory/transfers`、`PUT .../confirm`
- `GET /inventory/warnings`、`PUT .../handle?handleRemark=`
- `POST /inventory/inbound`、`/outbound`、`/inbound/new`
- `GET /inventory/outbounds`
- `PUT /inventory/{id}/safe-stock|location|stagnant-days`
