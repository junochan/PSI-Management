---
name: psi-smart-ims-purchase
description: >-
  智链进销存采购管理：通过 psims purchase 管理采购订单、入库单查询与采购统计；对应 HTTP /purchase/*。
  涉及采购订单相关的时候使用。
---

# 采购（订单与入库）

## ⚠️ 写操作前确认（最高优先级，不可跳过）

**本技能中所有会改动系统数据的操作，必须先暂停 → 向用户确认 → 得到明确同意 → 才可执行。**

### 什么是写操作（必须确认）

| 类别 | 示例 |
|------|------|
| 新增 | `POST /purchase/orders`（创建采购订单） |
| 修改 | `PUT /purchase/orders/{id}`（修改采购订单） |
| 取消 | `PUT /purchase/orders/{id}/cancel`（取消采购单） |
| 删除 | `DELETE /purchase/orders/{id}`（删除采购单） |
| 入库 | `POST /purchase/orders/{id}/inbound`（采购入库） |

**可不经确认的只读操作**：采购订单列表/详情、入库单列表/详情、采购统计等纯查询。

### 确认流程（每次写操作前必须完整执行）

```
1. 收集必要参数（供应商、商品、数量、价格等必填字段）
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

跳过确认直接执行写操作是严重失误，会导致采购订单/库存被错误创建/修改/删除。每次执行前问一句，远胜于事后补救。

## 功能与作用

- **采购订单**：向供应商下单，可后续 **取消**、**删除**。
- **查询入库单**：查询由采购产生的入库记录及明细。
- **统计**：采购侧汇总数据（金额、笔数等，以接口返回字段为准）。

适用于采购单详情/编辑、入库单查询与采购统计场景。

**GET 含中文的 `keyword` 等**：须 URL 编码，否则 Tomcat 报 `Invalid character found in the request target`；`psims purchase orders list -q` / `psims purchase inbounds list -q` 等由 axios 编码。详见 **`psi-smart-ims-overview`** →「GET 查询串编码（Tomcat / Agent 手写 URL）」。

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
| `POST /purchase/orders` | 无 | 无 | `supplierId`(必填),`productId`(必填),`quantity`(必填),`unitPrice`(必填),`expectDate`(可选),`payMethod`(可选),`remark`(可选) | 无 |
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

## 各写操作执行步骤

### 新增采购订单（POST /purchase/orders）

```
步骤 1：收集必填字段
  必须字段：supplierId（供应商）、productId（商品）、quantity（数量）、unitPrice（单价）
  可选字段：expectDate、payMethod、remark

步骤 2：⛔ 输出确认卡片，停止等待

步骤 3：用户确认后再执行 POST /purchase/orders
```

### 修改采购订单（PUT /purchase/orders/{id}）

```
步骤 1：确认要修改的采购订单 id 及变更字段

步骤 2：⛔ 输出确认卡片（含 id、变更前后值），停止等待

步骤 3：用户确认后再执行
```

### 取消/删除采购订单

```
步骤 1：确认采购订单 id

步骤 2：⛔ 输出确认卡片 + ⚠️ 不可逆提示，停止等待

步骤 3：用户确认后再执行
```

### 采购入库（POST /purchase/orders/{id}/inbound）

```
步骤 1：收集必填字段
  必须字段：warehouseId（仓库）、quantity（入库数量）
  可选字段：batchNo、remark

步骤 2：⛔ 输出确认卡片 + 库存增减影响，停止等待

步骤 3：用户确认后再执行
```

## HTTP 对照

- 订单：`/purchase/orders`、 `/purchase/orders/{id}`、 `cancel`、`DELETE`、`POST .../inbound`
- 入库：`/purchase/inbounds`、`/purchase/inbounds/{id}`
- 统计：`GET /purchase/stats`
