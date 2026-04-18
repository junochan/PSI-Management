---
name: psi-smart-ims-suppliers
description: >-
  智链进销存供应商主数据：通过 psims suppliers 完成列表、详情、增删改、批量删除；另有 GET /supplier-industries 行业字典（下拉）。
  当用户维护采购往来单位、采购单选择供应商、表单选行业或脚本同步供应商时使用。
---

# 供应商

## ⚠️ 写操作前确认（最高优先级，不可跳过）

**本技能中所有会改动系统数据的操作，必须先暂停 → 向用户确认 → 得到明确同意 → 才可执行。**

### 什么是写操作（必须确认）

| 类别 | 示例 |
|------|------|
| 新增 | `POST /suppliers`（创建供应商） |
| 修改 | `PUT /suppliers/{id}`（修改供应商信息） |
| 删除 | `DELETE /suppliers/{id}`、`DELETE /suppliers/batch`（删除/批量删除供应商） |

**可不经确认的只读操作**：`GET /suppliers`（列表）、`GET /suppliers/{id}`（详情）、`GET /supplier-industries`（行业字典）等纯查询。

### 确认流程（每次写操作前必须完整执行）

```
1. 收集必要参数（供应商名称等必填字段）
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

跳过确认直接执行写操作是严重失误，会导致供应商数据被错误创建/修改/删除。每次执行前问一句，远胜于事后补救。

## 功能与作用

供应商是 **采购业务** 的基础主数据：名称、联系方式、结算方式等。列表接口支持分页与筛选。

**行业字典**：`GET /supplier-industries` 返回启用中的行业列表，供供应商表单「所属行业」下拉。CLI：`psims supplier-industries`。

**GET 含中文的 `keyword` 等**：须 URL 编码，否则 Tomcat 报 `Invalid character found in the request target`；`psims suppliers list -q` 由 axios 编码。详见 **`psi-smart-ims-overview`** →「GET 查询串编码（Tomcat / Agent 手写 URL）」。

## CLI 调用

```bash
psims suppliers list -q "{\"current\":1,\"size\":10}"
psims suppliers get 1
psims suppliers create -d "{\"supplierName\":\"某厂\"}"
psims suppliers update 1 -f ./supplier.json
psims suppliers delete 1
psims suppliers batch-delete -d "[1,2,3]"
psims supplier-industries
```

| 子命令 | 说明 |
|--------|------|
| `list` | `-q` 查询 JSON |
| `get <id>` | 详情 |
| `create` | `-d` / `-f` Body |
| `update <id>` | 同上 |
| `delete <id>` | 单删 |
| `batch-delete` | Body 为 id 数组 |

顶层命令 **`supplier-industries`**（与 `suppliers` 并列）：`GET /supplier-industries`，无子命令。

## 接口参数清单（按技能内接口）

| 接口 | 路径参数 | Query 参数 | Body 参数 | 文件参数 |
|------|----------|------------|-----------|----------|
| `GET /suppliers` | 无 | `page`,`size`,`sort`,`order`,`keyword`,`productId`,`warehouseId`,`customerId`,`supplierId`,`categoryName`,`productStatus`,`stagnantStatus`,`inboundStatus`,`payStatus`,`salesOrderStatus`,`aftersalesStatus`,`lastOutboundStart`,`lastOutboundEnd`,`lastInboundStart`,`lastInboundEnd`,`expectDateStart`,`expectDateEnd`,`createTimeStart`,`createTimeEnd`,`operatorName`（均可选） | 无 | 无 |
| `GET /suppliers/{id}` | `id`(必填) | 无 | 无 | 无 |
| `POST /suppliers` | 无 | 无 | `name`(必填),`code`(可选),`industryIds`(可选, `number[]`),`contactPerson`(可选),`contactPhone`(可选),`email`(可选),`address`(可选),`bankName`(可选),`bankAccount`(可选),`taxNo`(可选),`remark`(可选) | 无 |
| `PUT /suppliers/{id}` | `id`(必填) | 无 | 同 `POST /suppliers` 字段 | 无 |
| `DELETE /suppliers/{id}` | `id`(必填) | 无 | 无 | 无 |
| `DELETE /suppliers/batch` | 无 | 无 | `ids`(必填, `number[]`) | 无 |
| `GET /supplier-industries` | 无 | 无 | 无 | 无 |

CLI 参数对应：`list` 用 `-q`；`create/update/batch-delete` 用 `-d` 或 `-f`。

字段级完整参数查询（CLI）：

- `psims spec show suppliers list`
- `psims spec show suppliers create`
- `psims spec show suppliers update`
- `psims spec show suppliers batch-delete`

## 各写操作执行步骤

### 新增供应商（POST /suppliers）

```
步骤 1：收集必填字段
  必须字段：name（供应商名称）
  可选字段：code、industryIds、contactPerson、contactPhone、email、address、bankName、bankAccount、taxNo、remark

步骤 2：⛔ 输出确认卡片，停止等待

步骤 3：用户确认后再执行 POST /suppliers
```

### 修改供应商（PUT /suppliers/{id}）

```
步骤 1：确认要修改的供应商 id 及变更字段

步骤 2：⛔ 输出确认卡片（含 id、变更前后值），停止等待

步骤 3：用户确认后再执行 PUT /suppliers/{id}
```

### 删除供应商（DELETE /suppliers/{id}）

```
步骤 1：确认要删除的供应商 id 及名称

步骤 2：⛔ 输出确认卡片 + ⚠️ 不可逆提示，停止等待

步骤 3：用户确认后再执行
```

### 批量删除（DELETE /suppliers/batch）

```
步骤 1：确认要删除的供应商 id 列表及数量

步骤 2：⛔ 输出确认卡片 + ⚠️ 不可逆 + 影响数量提示，停止等待

步骤 3：用户确认后再执行
```

## HTTP 对照

| 方法 | 路径 |
|------|------|
| GET | `/suppliers` |
| GET | `/suppliers/{id}` |
| POST | `/suppliers` |
| PUT | `/suppliers/{id}` |
| DELETE | `/suppliers/{id}` |
| DELETE | `/suppliers/batch` |
| GET | `/supplier-industries` | 行业字典（下拉） |
