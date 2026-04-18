---
name: psi-smart-ims-customers
description: >-
  智链进销存客户主数据：通过 psims customers 完成列表、详情、增删改、批量删除；对应 HTTP /customers。
  当用户维护销售客户资料、销售单选择客户或批量清理测试数据时使用。
---

# 客户

## ⚠️ 写操作前确认（最高优先级，不可跳过）

**本技能中所有会改动系统数据的操作，必须先暂停 → 向用户确认 → 得到明确同意 → 才可执行。**

### 什么是写操作（必须确认）

| 类别 | 示例 |
|------|------|
| 新增 | `POST /customers`（创建客户） |
| 修改 | `PUT /customers/{id}`（修改客户信息） |
| 删除 | `DELETE /customers/{id}`、`DELETE /customers/batch`（删除/批量删除客户） |

**可不经确认的只读操作**：`GET /customers`（列表）、`GET /customers/{id}`（详情）等纯查询。

### 确认流程（每次写操作前必须完整执行）

```
1. 收集必要参数（客户名称等必填字段）
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

跳过确认直接执行写操作是严重失误，会导致客户数据被错误创建/修改/删除。每次执行前问一句，远胜于事后补救。

## 功能与作用

客户是 **销售与售后** 的基础档案，列表支持分页与筛选。

**GET 含中文的 `keyword` 等**：须 URL 编码，否则 Tomcat 报 `Invalid character found in the request target`；`psims customers list -q` 由 axios 编码。详见 **`psi-smart-ims-overview`** →「GET 查询串编码（Tomcat / Agent 手写 URL）」。

## CLI 调用

```bash
psims customers list -q "{\"current\":1,\"size\":10}"
psims customers get 1
psims customers create -d "{\"customerName\":\"某客户\"}"
psims customers update 1 -f ./customer.json
psims customers delete 1
psims customers batch-delete -d "[1,2,3]"
```

| 子命令 | 说明 |
|--------|------|
| `list` | `-q` 查询 JSON |
| `get <id>` | 详情 |
| `create` / `update <id>` / `delete <id>` | 标准 CRUD |
| `batch-delete` | id 数组 |

## 接口参数清单（按技能内接口）

| 接口 | 路径参数 | Query 参数 | Body 参数 | 文件参数 |
|------|----------|------------|-----------|----------|
| `GET /customers` | 无 | `page`,`size`,`sort`,`order`,`keyword`,`productId`,`warehouseId`,`customerId`,`supplierId`,`categoryName`,`productStatus`,`stagnantStatus`,`inboundStatus`,`payStatus`,`salesOrderStatus`,`aftersalesStatus`,`lastOutboundStart`,`lastOutboundEnd`,`lastInboundStart`,`lastInboundEnd`,`expectDateStart`,`expectDateEnd`,`createTimeStart`,`createTimeEnd`,`operatorName`（均可选） | 无 | 无 |
| `GET /customers/{id}` | `id`(必填) | 无 | 无 | 无 |
| `POST /customers` | 无 | 无 | `name`(必填),`code`(可选),`type`(可选),`contactPerson`(可选, 兼容 `contact`),`contactPhone`(可选, 兼容 `phone`),`email`(可选),`address`(可选),`vipLevel`(可选),`remark`(可选) | 无 |
| `PUT /customers/{id}` | `id`(必填) | 无 | 同 `POST /customers` 字段 | 无 |
| `DELETE /customers/{id}` | `id`(必填) | 无 | 无 | 无 |
| `DELETE /customers/batch` | 无 | 无 | `ids`(必填, `number[]`) | 无 |

CLI 参数对应：`list` 用 `-q`；`create/update/batch-delete` 用 `-d` 或 `-f`。

字段级完整参数查询（CLI）：

- `psims spec show customers list`
- `psims spec show customers create`
- `psims spec show customers update`
- `psims spec show customers batch-delete`

## 各写操作执行步骤

### 新增客户（POST /customers）

```
步骤 1：收集必填字段
  必须字段：name（客户名称）
  可选字段：code、type、contactPerson、contactPhone、email、address、vipLevel、remark

步骤 2：⛔ 输出确认卡片，停止等待

步骤 3：用户确认后再执行 POST /customers
```

### 修改客户（PUT /customers/{id}）

```
步骤 1：确认要修改的客户 id 及变更字段

步骤 2：⛔ 输出确认卡片（含 id、变更前后值），停止等待

步骤 3：用户确认后再执行 PUT /customers/{id}
```

### 删除客户（DELETE /customers/{id}）

```
步骤 1：确认要删除的客户 id 及名称

步骤 2：⛔ 输出确认卡片 + ⚠️ 不可逆提示，停止等待

步骤 3：用户确认后再执行
```

### 批量删除（DELETE /customers/batch）

```
步骤 1：确认要删除的客户 id 列表及数量

步骤 2：⛔ 输出确认卡片 + ⚠️ 不可逆 + 影响数量提示，停止等待

步骤 3：用户确认后再执行
```

## HTTP 对照

| 方法 | 路径 |
|------|------|
| GET | `/customers` |
| GET | `/customers/{id}` |
| POST | `/customers` |
| PUT | `/customers/{id}` |
| DELETE | `/customers/{id}` |
| DELETE | `/customers/batch` |
