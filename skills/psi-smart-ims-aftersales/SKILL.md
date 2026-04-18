---
name: psi-smart-ims-aftersales
description: >-
  智链进销存售后:通过 psims aftersales 查询、创建、处理、关闭售后单;对应 HTTP /aftersales。
  当用户跟进退换修、客服闭环或按条件筛选售后记录时使用。
---

# 售后

## ⚠️ 写操作前确认（最高优先级，不可跳过）

**本技能中所有会改动系统数据的操作，必须先暂停 → 向用户确认 → 得到明确同意 → 才可执行。**

### 什么是写操作（必须确认）

| 类别 | 示例 |
|------|------|
| 新增 | `POST /aftersales`（创建售后单） |
| 处理 | `PUT /aftersales/{id}/handle`（处理售后） |
| 关闭 | `PUT /aftersales/{id}/close`（关闭售后） |

**可不经确认的只读操作**：`GET /aftersales`（列表）、`GET /aftersales/{id}`（详情）等纯查询。

### 确认流程（每次写操作前必须完整执行）

```
1. 收集必要参数（售后类型、关联订单、问题描述、处理结果等必填字段）
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

跳过确认直接执行写操作是严重失误，会导致售后单被错误创建/处理/关闭。每次执行前问一句，远胜于事后补救。

## 功能与作用

售后单关联销售订单与客户诉求,支持 **处理**(填写处理结果)与 **关闭**。列表接口支持分页与筛选。

适用于售后列表、售后详情与售后处理闭环场景。

**GET 含中文的 `keyword` 等**:须 URL 编码,否则 Tomcat 报 `Invalid character found in the request target`;`psims aftersales list -q` 由 axios 编码。详见 **`psi-smart-ims-overview`** →「GET 查询串编码(Tomcat / Agent 手写 URL)」。

## CLI 调用

```bash
psims aftersales list -q "{\"current\":1,\"size\":10}"
psims aftersales get 1
psims aftersales create -f ./afs.json
psims aftersales handle 1 -d "{\"handleResult\":\"退款\"}"
psims aftersales close 1
```

| 子命令 | 作用 |
|--------|------|
| `list` | 分页列表 |
| `get <id>` | 详情 |
| `create` | 新建 |
| `handle <id>` | 处理(Body 为处理请求 JSON) |
| `close <id>` | 关闭 |

## 接口参数清单(按技能内接口)

| 接口 | 路径参数 | Query 参数 | Body 参数 | 文件参数 |
|------|----------|------------|-----------|----------|
| `GET /aftersales` | 无 | `page`,`size`,`sort`,`order`,`keyword`,`productId`,`warehouseId`,`customerId`,`supplierId`,`categoryName`,`productStatus`,`stagnantStatus`,`inboundStatus`,`payStatus`,`salesOrderStatus`,`aftersalesStatus`,`lastOutboundStart`,`lastOutboundEnd`,`lastInboundStart`,`lastInboundEnd`,`expectDateStart`,`expectDateEnd`,`createTimeStart`,`createTimeEnd`,`operatorName`(均可选) | 无 | 无 |
| `GET /aftersales/{id}` | `id`(必填) | 无 | 无 | 无 |
| `POST /aftersales` | 无 | 无 | `salesOrderId`(必填),`type`(必填),`content`(必填),`expectHandle`(可选),`refundAmount`(可选),`remark`(可选) | 无 |
| `PUT /aftersales/{id}/handle` | `id`(必填) | 无 | `handleResult`(必填),`refundAmount`(可选),`remark`(可选) | 无 |
| `PUT /aftersales/{id}/close` | `id`(必填) | 无 | 无 | 无 |

CLI 参数对应:`list` 用 `-q`;`create/handle` 用 `-d` 或 `-f`。

字段级完整参数查询(CLI):

- `psims spec show aftersales list`
- `psims spec show aftersales create`
- `psims spec show aftersales handle`

## 各写操作执行步骤

### 创建售后单（POST /aftersales）

```
步骤 1：收集必填字段
  必须字段：salesOrderId（关联订单）、type（售后类型）、content（问题描述）
  可选字段：expectHandle、refundAmount、remark

步骤 2：⛔ 输出确认卡片，停止等待

步骤 3：用户确认后再执行 POST /aftersales
```

### 处理售后（PUT /aftersales/{id}/handle）

```
步骤 1：确认售后单 id 及处理结果
  必须字段：handleResult
  可选字段：refundAmount、remark

步骤 2：⛔ 输出确认卡片（含售后单号、处理结果），停止等待

步骤 3：用户确认后再执行
```

### 关闭售后（PUT /aftersales/{id}/close）

```
步骤 1：确认售后单 id

步骤 2：⛔ 输出确认卡片 + ⚠️ "本次将结束该售后流程"，停止等待

步骤 3：用户确认后再执行
```

## HTTP 对照

| 方法 | 路径 |
|------|------|
| GET | `/aftersales` |
| GET | `/aftersales/{id}` |
| POST | `/aftersales` |
| PUT | `/aftersales/{id}/handle` |
| PUT | `/aftersales/{id}/close` |
