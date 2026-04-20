---
name: psi-smart-ims-inventory
description: >-
  智链进销存库存：通过 psims inventory 查询库存、调拨、预警处理、其它出入库、维护安全库存/库位/呆滞天数、以图搜库存；对应 HTTP /inventory/*。
  当用户盘点、移库、处理低库存预警或按图找货时使用。
---

# 库存

## ⚠️ 写操作前确认（最高优先级，不可跳过）

**本技能中所有会改动系统数据的操作，必须先暂停 → 向用户确认 → 得到明确同意 → 才可执行。**

### 什么是写操作（必须确认）

| 类别 | 示例 |
|------|------|
| 调拨创建 | `POST /inventory/transfers`（新建调拨单） |
| 调拨确认 | `PUT /inventory/transfers/{id}/confirm`（确认调拨记账） |
| 预警处理 | `PUT /inventory/warnings/{id}/handle`（处理库存预警） |
| 其它入库 | `POST /inventory/inbound`（简单入库）、`POST /inventory/inbound/new`（新入库） |
| 其它出库 | `POST /inventory/outbound`（简单出库） |
| 属性维护 | `PUT /inventory/{id}/safe-stock`（安全库存）、`PUT /inventory/{id}/location`（库位）、`PUT /inventory/{id}/stagnant-days`（呆滞天数） |

**可不经确认的只读操作**：库存列表/详情、按商品查库存、以图搜库存、统计、预警列表、出库列表等纯查询。

### 确认流程（每次写操作前必须完整执行）

```
1. 收集必要参数（库存id、商品、仓库、数量、目标等必填字段）
2. 向用户输出「拟执行操作」确认卡片（见下方模板）
3. ⛔ 停止，等待用户回复
4. 仅当用户明确回复「确认」「可以」「执行」「OK」等肯定语，才继续第 5 步
5. 执行写操作
```

### 确认卡片模板（必须使用）

```
📋 拟执行操作：【操作类型】
━━━━━━━━━━━━━━━━━━━━
• 接口：HTTP方法 /path
• 关键参数：
  - 字段1: 值1
  - 字段2: 值2
  - ...
• 影响：操作结果简述
━━━━━━━━━━━━━━━━━━━━
请确认后回复「确认」执行。
```

### 违规后果

跳过确认直接执行写操作是严重失误，会导致库存数量被错误增减、调拨/出入库误执行。每次执行前问一句，远胜于事后补救。

## 功能与作用

- **库存行**：按仓库+商品维度展示数量、安全库存、库位、最近出入库时间等。
- **调拨**：仓间/库位间移货，创建调拨单后可 **确认** 完成记账。
- **预警**：低于安全库存等条件产生预警，可 **处理** 并备注。
- **其它出入库**：简单参数走 `inbound-simple` / `outbound-simple`；复杂业务走 `inbound-new`。
- **以图搜图**：与商品类似，对库存维度做向量检索（耗时长）。

适用于库存详情、调拨详情、预警处理与出入库场景。

**GET 含中文的 `keyword` 等**：查询串须 URL 编码，否则 Tomcat 报 `Invalid character found in the request target`；`psims ... list -q` 由 axios 编码。详见 **`psi-smart-ims-overview`** →「GET 查询串编码（Tomcat / Agent 手写 URL）」。

## CLI 调用

### 主数据与检索

```bash
psims inventory list -q "{}"
psims inventory get 1
psims inventory by-product 100
psims inventory search-image --image ./query.jpg -f ./body.json
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

## 接口参数清单（按技能内接口）

### 库存主数据与检索

| 接口 | 路径参数 | Query 参数 | Body 参数 | 文件参数 |
|------|----------|------------|-----------|----------|
| `GET /inventory` | 无 | `page`,`size`,`sort`,`order`,`keyword`,`productId`,`warehouseId`,`customerId`,`supplierId`,`categoryName`,`productStatus`,`stagnantStatus`,`inboundStatus`,`payStatus`,`salesOrderStatus`,`aftersalesStatus`,`lastOutboundStart`,`lastOutboundEnd`,`lastInboundStart`,`lastInboundEnd`,`expectDateStart`,`expectDateEnd`,`createTimeStart`,`createTimeEnd`,`operatorName`（均可选） | 无 | 无 |
| `GET /inventory/{id}` | `id`(必填) | 无 | 无 | 无 |
| `GET /inventory/product/{productId}` | `productId`(必填) | 无 | 无 | 无 |
| `POST /inventory/search-by-image` | 无 | 无 | `page`(可选),`size`(可选),`keyword`(可选),`productId`(可选),`warehouseId`(可选),`stagnantStatus`(可选),`lastOutboundStart`(可选),`lastOutboundEnd`(可选),`lastInboundStart`(可选),`lastInboundEnd`(可选),`similarityThreshold`(可选) | `image`(必填，multipart 文件字段) |
| `GET /inventory/stats` | 无 | 无 | 无 | 无 |
| `GET /inventory/outbounds` | 无 | `page`,`size`,`sort`,`order`,`keyword`,`productId`,`warehouseId`,`customerId`,`supplierId`,`categoryName`,`productStatus`,`stagnantStatus`,`inboundStatus`,`payStatus`,`salesOrderStatus`,`aftersalesStatus`,`lastOutboundStart`,`lastOutboundEnd`,`lastInboundStart`,`lastInboundEnd`,`expectDateStart`,`expectDateEnd`,`createTimeStart`,`createTimeEnd`,`operatorName`（均可选） | 无 | 无 |

### 调拨与预警

| 接口 | 路径参数 | Query 参数 | Body 参数 | 文件参数 |
|------|----------|------------|-----------|----------|
| `GET /inventory/transfers` | 无 | `page`,`size`,`sort`,`order`,`keyword`,`productId`,`warehouseId`,`customerId`,`supplierId`,`categoryName`,`productStatus`,`stagnantStatus`,`inboundStatus`,`payStatus`,`salesOrderStatus`,`aftersalesStatus`,`lastOutboundStart`,`lastOutboundEnd`,`lastInboundStart`,`lastInboundEnd`,`expectDateStart`,`expectDateEnd`,`createTimeStart`,`createTimeEnd`,`operatorName`（均可选） | 无 | 无 |
| `POST /inventory/transfers` | 无 | 无 | `productId`(必填),`fromWarehouseId`(必填),`toWarehouseId`(必填),`quantity`(必填),`remark`(可选) | 无 |
| `PUT /inventory/transfers/{id}/confirm` | `id`(必填) | 无 | 无 | 无 |
| `GET /inventory/warnings` | 无 | `page`,`size`,`sort`,`order`,`keyword`,`productId`,`warehouseId`,`customerId`,`supplierId`,`categoryName`,`productStatus`,`stagnantStatus`,`inboundStatus`,`payStatus`,`salesOrderStatus`,`aftersalesStatus`,`lastOutboundStart`,`lastOutboundEnd`,`lastInboundStart`,`lastInboundEnd`,`expectDateStart`,`expectDateEnd`,`createTimeStart`,`createTimeEnd`,`operatorName`（均可选） | 无 | 无 |
| `PUT /inventory/warnings/{id}/handle` | `id`(必填) | `handleRemark`(必填) | 无 | 无 |

### 出入库与属性维护

| 接口 | 路径参数 | Query 参数 | Body 参数 | 文件参数 |
|------|----------|------------|-----------|----------|
| `POST /inventory/inbound` | 无 | `inventoryId`(必填), `quantity`(必填), `remark`(可选) | 无 | 无 |
| `POST /inventory/outbound` | 无 | `inventoryId`(必填), `quantity`(必填), `remark`(可选) | 无 | 无 |
| `POST /inventory/inbound/new` | 无 | 无 | `productId`(必填),`warehouseId`(必填),`quantity`(必填),`remark`(可选) | 无 |
| `PUT /inventory/{id}/safe-stock` | `id`(必填) | `safeStock`(必填) | 无 | 无 |
| `PUT /inventory/{id}/location` | `id`(必填) | `location`(必填) | 无 | 无 |
| `PUT /inventory/{id}/stagnant-days` | `id`(必填) | `stagnantDays`(必填) | 无 | 无 |

CLI 参数对应：

- 列表：`-q|--query <json>`
- Body：`-d|--data <json>` 或 `-f|--file <path>`
- 预警处理：`psims inventory warnings handle <id> --remark <text>`
- 简单出入库：`--inventory-id`, `--quantity`, `--remark`

字段级完整参数查询（CLI）：

- `psims spec show inventory list`
- `psims spec show inventory search-image`
- `psims spec show inventory transfers create`
- `psims spec show inventory warnings handle`
- `psims spec show inventory inbound-new`

## 各写操作执行步骤

### 创建调拨（POST /inventory/transfers）

```
步骤 1：收集必填字段
  必须字段：productId（商品）、fromWarehouseId（源仓库）、toWarehouseId（目标仓库）、quantity（数量）
  可选字段：remark

步骤 2：⛔ 输出确认卡片，停止等待

步骤 3：用户确认后再执行
```

### 确认调拨（PUT /inventory/transfers/{id}/confirm）

```
步骤 1：确认调拨单 id

步骤 2：⛔ 输出确认卡片 + 库存增减影响，停止等待

步骤 3：用户确认后再执行
```

### 预警处理（PUT /inventory/warnings/{id}/handle）

```
步骤 1：确认预警 id 及处理备注

步骤 2：⛔ 输出确认卡片，停止等待

步骤 3：用户确认后再执行
```

### 其它入库/出库（POST /inventory/inbound / /outbound / /inbound/new）

```
步骤 1：收集必填字段（inventoryId/productId、warehouseId、quantity 等）

步骤 2：⛔ 输出确认卡片 + 库存增减影响，停止等待

步骤 3：用户确认后再执行
```

### 属性维护（safe-stock / location / stagnant-days）

```
步骤 1：确认库存 id 及新值

步骤 2：⛔ 输出确认卡片，停止等待

步骤 3：用户确认后再执行
```

## HTTP 对照（节选）

- `GET /inventory`、`/inventory/{id}`、`/inventory/product/{productId}`
- `POST /inventory/search-by-image`
- `GET|POST /inventory/transfers`、`PUT .../confirm`
- `GET /inventory/warnings`、`PUT .../handle?handleRemark=`
- `POST /inventory/inbound`、`/outbound`、`/inbound/new`
- `GET /inventory/outbounds`
- `PUT /inventory/{id}/safe-stock|location|stagnant-days`
