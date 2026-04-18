---
name: psi-smart-ims-sales
description: >-
  智链进销存销售：通过 psims sales 管理销售订单及付款、发货、收货、取消与统计；对应 HTTP /sales/*。
  当用户处理订单履约状态、或脚本导出销售与统计时使用。
---

# 销售（订单履约）

## ⚠️ 写操作前确认（最高优先级，不可跳过）

**本技能中所有会改动系统数据的操作，必须先暂停 → 向用户确认 → 得到明确同意 → 才可执行。**

### 什么是写操作（必须确认）

| 类别 | 示例 |
|------|------|
| 新增 | `POST /sales/orders`（创建销售订单） |
| 修改 | `PUT /sales/orders/{id}`（修改销售订单） |
| 付款 | `PUT /sales/orders/{id}/payment`（确认付款） |
| 发货 | `POST /sales/orders/{id}/shipping`（确认发货） |
| 收货 | `PUT /sales/orders/{id}/received`（确认收货） |
| 取消 | `PUT /sales/orders/{id}/cancel`（取消订单） |

**可不经确认的只读操作**：销售订单列表/详情、销售统计等纯查询。

### 确认流程（每次写操作前必须完整执行）

```
1. 收集必要参数（客户、商品、数量、价格等必填字段）
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

跳过确认直接执行写操作是严重失误，会导致销售订单/库存/资金状态被错误变更。每次执行前问一句，远胜于事后补救。

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

## 各写操作执行步骤

### 新增销售订单（POST /sales/orders）

```
步骤 1：收集必填字段
  必须字段：customerId（客户）、productId（商品）、quantity（数量）、unitPrice（单价）
  可选字段：warehouseId、payMethod、receiverName、receiverPhone、receiverAddress、remark

步骤 2：⛔ 输出确认卡片，停止等待

步骤 3：用户确认后再执行 POST /sales/orders
```

### 修改销售订单（PUT /sales/orders/{id}）

```
步骤 1：确认要修改的销售订单 id 及变更字段

步骤 2：⛔ 输出确认卡片（含 id、变更前后值），停止等待

步骤 3：用户确认后再执行
```

### 确认付款/收货/取消

```
步骤 1：确认销售订单 id

步骤 2：⛔ 输出确认卡片 + 状态流转说明，停止等待

步骤 3：用户确认后再执行
```

### 确认发货（POST /sales/orders/{id}/shipping）

```
步骤 1：收集必填字段
  必须字段：warehouseId、quantity、logisticsCompany、logisticsNo、receiverName、receiverPhone、receiverAddress
  可选字段：remark

步骤 2：⛔ 输出确认卡片 + ⚠️ 物流信息不可轻易变更提示，停止等待

步骤 3：用户确认后再执行
```

## HTTP 对照

- `GET|POST|PUT`：`/sales/orders` 及子路径 `payment`、`shipping`、`received`、`cancel`
- `GET /sales/stats`
