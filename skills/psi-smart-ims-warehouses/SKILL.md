---
name: psi-smart-ims-warehouses
description: >-
  智链进销存仓库：通过 psims warehouses 管理仓库档案、轻量下拉 options、全部列表 all、批量删除；对应 HTTP /warehouses。
  当用户配置多仓、库存/调拨选择仓库或脚本初始化仓库时使用。
---

# 仓库

## ⚠️ 写操作前确认（最高优先级，不可跳过）

**本技能中所有会改动系统数据的操作，必须先暂停 → 向用户确认 → 得到明确同意 → 才可执行。**

### 什么是写操作（必须确认）

| 类别 | 示例 |
|------|------|
| 新增 | `POST /warehouses`（创建仓库） |
| 修改 | `PUT /warehouses/{id}`（修改仓库信息） |
| 删除 | `DELETE /warehouses/{id}`、`DELETE /warehouses/batch`（删除/批量删除仓库） |

**可不经确认的只读操作**：`GET /warehouses`（列表）、`GET /warehouses/{id}`（详情）、`GET /warehouses/options`（轻量下拉）、`GET /warehouses/all`（全部仓库）等纯查询。

### 确认流程（每次写操作前必须完整执行）

```
1. 收集必要参数（仓库名称等必填字段）
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

跳过确认直接执行写操作是严重失误，会导致仓库数据被错误创建/修改/删除，且删除会影响关联的库存与业务单据。每次执行前问一句，远胜于事后补救。

## 功能与作用

仓库定义 **库存所在物理/逻辑仓位**。**`options`** 为轻量下拉（id/编码/名称，无统计），**`all`** 为带统计信息的全部仓库列表。

**GET 含中文的 `keyword` 等**：须 URL 编码，否则 Tomcat 报 `Invalid character found in the request target`；`psims warehouses list -q` 由 axios 编码。详见 **`psi-smart-ims-overview`** →「GET 查询串编码（Tomcat / Agent 手写 URL）」。

## CLI 调用

```bash
psims warehouses options
psims warehouses list -q "{\"current\":1,\"size\":10}"
psims warehouses all
psims warehouses get 1
psims warehouses create -d "{\"warehouseName\":\"主仓\",\"address\":\"...\"}"
psims warehouses update 1 -f ./warehouse.json
psims warehouses delete 1
psims warehouses batch-delete -d "[1,2,3]"
```

| 子命令 | 说明 |
|--------|------|
| `options` | 轻量下拉 `GET /warehouses/options` |
| `list` | 分页列表 |
| `all` | 全部仓库（无分页） |
| `get <id>` | 详情 |
| `create` / `update <id>` / `delete <id>` | CRUD |
| `batch-delete` | id 数组 |

## 接口参数清单（按技能内接口）

| 接口 | 路径参数 | Query 参数 | Body 参数 | 文件参数 |
|------|----------|------------|-----------|----------|
| `GET /warehouses/options` | 无 | 无 | 无 | 无 |
| `GET /warehouses` | 无 | `page`,`size`,`sort`,`order`,`keyword`,`productId`,`warehouseId`,`customerId`,`supplierId`,`categoryName`,`productStatus`,`stagnantStatus`,`inboundStatus`,`payStatus`,`salesOrderStatus`,`aftersalesStatus`,`lastOutboundStart`,`lastOutboundEnd`,`lastInboundStart`,`lastInboundEnd`,`expectDateStart`,`expectDateEnd`,`createTimeStart`,`createTimeEnd`,`operatorName`（均可选） | 无 | 无 |
| `GET /warehouses/all` | 无 | 无 | 无 | 无 |
| `GET /warehouses/{id}` | `id`(必填) | 无 | 无 | 无 |
| `POST /warehouses` | 无 | 无 | `name`(必填),`code`(可选),`address`(可选),`managerName`(可选),`managerPhone`(可选),`capacity`(可选),`capacityUsed`(可选),`remark`(可选) | 无 |
| `PUT /warehouses/{id}` | `id`(必填) | 无 | 同 `POST /warehouses` 字段 | 无 |
| `DELETE /warehouses/{id}` | `id`(必填) | 无 | 无 | 无 |
| `DELETE /warehouses/batch` | 无 | 无 | `ids`(必填, `number[]`) | 无 |

CLI 参数对应：`list` 用 `-q`；`create/update/batch-delete` 用 `-d` 或 `-f`。

字段级完整参数查询（CLI）：

- `psims spec show warehouses list`
- `psims spec show warehouses create`
- `psims spec show warehouses update`
- `psims spec show warehouses batch-delete`

## 各写操作执行步骤

### 新增仓库（POST /warehouses）

```
步骤 1：收集必填字段
  必须字段：name（仓库名称）
  可选字段：code、address、managerName、managerPhone、capacity、capacityUsed、remark

步骤 2：⛔ 输出确认卡片，停止等待

步骤 3：用户确认后再执行 POST /warehouses
```

### 修改仓库（PUT /warehouses/{id}）

```
步骤 1：确认要修改的仓库 id 及变更字段

步骤 2：⛔ 输出确认卡片（含 id、变更前后值），停止等待

步骤 3：用户确认后再执行 PUT /warehouses/{id}
```

### 删除仓库（DELETE /warehouses/{id}）

```
步骤 1：确认要删除的仓库 id 及名称

步骤 2：⛔ 输出确认卡片 + ⚠️ 不可逆 + 关联库存/单据影响提示，停止等待

步骤 3：用户确认后再执行
```

### 批量删除（DELETE /warehouses/batch）

```
步骤 1：确认要删除的仓库 id 列表及数量

步骤 2：⛔ 输出确认卡片 + ⚠️ 不可逆 + 影响数量提示，停止等待

步骤 3：用户确认后再执行
```

## HTTP 对照

| 方法 | 路径 |
|------|------|
| GET | `/warehouses/options` |
| GET | `/warehouses` |
| GET | `/warehouses/all` |
| GET | `/warehouses/{id}` |
| POST | `/warehouses` |
| PUT | `/warehouses/{id}` |
| DELETE | `/warehouses/{id}` |
| DELETE | `/warehouses/batch` |
