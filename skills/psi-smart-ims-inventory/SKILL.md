---
name: psi-smart-ims-inventory
description: >-
  智链进销存库存：通过 psims inventory 查询库存、调拨、预警处理、其它出入库、维护安全库存/库位/呆滞天数、以图搜库存；对应 HTTP /inventory/*。
  当用户盘点、移库、处理低库存预警或按图找货时使用。
---

# 库存

## 编辑与写操作前确认（强制）

在**执行任何会改动系统或仓库状态的操作之前**，必须先向用户**说明拟执行动作**（含影响范围、关键参数：路径、环境、`id`/单号、请求体或文件变更摘要等），并得到用户**明确同意**（例如「确认」「可以执行」「按这个来」）后，才可执行。

涵盖但不限于：

- **代码与配置**：创建/修改/删除仓库内文件、批量替换、会改写工作区或生成物的命令。
- **业务写操作**：`psims` 或 HTTP 中带 `create`、`update`、`delete`、`confirm`、调拨确认、预警处理、其它出入库等**会改数据或非纯查询**的调用。
- **环境与依赖**：用户未事先声明可自动执行时，`npm install` 等会写入磁盘的操作；直接改库、清缓存等。

**可不经确认**：只读排查（读文件、`list`/`get`/统计/`search-image` 只读拉数等）、纯口头方案及文档说明。

若用户已在**同一条消息**中明确授权某一具体动作（含范围），可视为已确认，但执行前仍应**简短复述**将运行的命令或写入点，避免误操作。

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

## 接口参数清单（按技能内接口）

### 库存主数据与检索

| 接口 | 路径参数 | Query 参数 | Body 参数 | 文件参数 |
|------|----------|------------|-----------|----------|
| `GET /inventory` | 无 | `page`,`size`,`sort`,`order`,`keyword`,`productId`,`warehouseId`,`customerId`,`supplierId`,`categoryName`,`productStatus`,`stagnantStatus`,`inboundStatus`,`payStatus`,`salesOrderStatus`,`aftersalesStatus`,`lastOutboundStart`,`lastOutboundEnd`,`lastInboundStart`,`lastInboundEnd`,`expectDateStart`,`expectDateEnd`,`createTimeStart`,`createTimeEnd`,`operatorName`（均可选） | 无 | 无 |
| `GET /inventory/{id}` | `id`(必填) | 无 | 无 | 无 |
| `GET /inventory/product/{productId}` | `productId`(必填) | 无 | 无 | 无 |
| `POST /inventory/search-by-image` | 无 | 无 | `page`(可选),`size`(可选),`keyword`(可选),`productId`(可选),`warehouseId`(可选),`stagnantStatus`(可选),`lastOutboundStart`(可选),`lastOutboundEnd`(可选),`lastInboundStart`(可选),`lastInboundEnd`(可选),`imageBase64`(必填),`similarityThreshold`(可选) | 无 |
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

## 写操作执行规范（必须）

涉及库存变更的操作（`transfers create/confirm`、`warnings handle`、`inbound-simple`、`outbound-simple`、`inbound-new`、`set-safe-stock`、`set-location`、`set-stagnant-days`）必须遵循：

1. 先确认并补齐必填字段（如库存/商品/仓库标识、数量、方向、调拨目标、处理备注等）。
2. 信息不完整时先引导用户补齐，禁止直接执行写命令。
3. 执行前输出变更摘要（目标库存行、关键参数、预期增减与影响范围）。
4. 仅在用户明确确认后再执行。
5. 出库、调拨确认、预警处理等高影响操作需再次提示影响后再执行。

## HTTP 对照（节选）

- `GET /inventory`、`/inventory/{id}`、`/inventory/product/{productId}`
- `POST /inventory/search-by-image`
- `GET|POST /inventory/transfers`、`PUT .../confirm`
- `GET /inventory/warnings`、`PUT .../handle?handleRemark=`
- `POST /inventory/inbound`、`/outbound`、`/inbound/new`
- `GET /inventory/outbounds`
- `PUT /inventory/{id}/safe-stock|location|stagnant-days`
