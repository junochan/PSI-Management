---
name: psi-smart-ims-warehouses
description: >-
  智链进销存仓库：通过 psims warehouses 管理仓库档案、轻量下拉 options、全部列表 all、批量删除；对应 HTTP /warehouses。
  当用户配置多仓、库存/调拨选择仓库或脚本初始化仓库时使用。
---

# 仓库

## 功能与作用

仓库定义 **库存所在物理/逻辑仓位**。**`options`** 为轻量下拉（id/编码/名称，无统计），**`all`** 为带统计信息的全部仓库列表。

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

## 写操作执行规范（必须）

涉及仓库数据变更的操作（`create`、`update`、`delete`、`batch-delete`）必须遵循：

1. 先确认并补齐必填字段（如仓库名称、编码、地址、状态等接口要求字段）。
2. 信息不完整时先引导用户补齐，禁止直接执行写命令。
3. 执行前给出变更摘要（目标仓库、关键字段、影响范围）。
4. 仅在用户明确确认后再执行。
5. 删除/批量删除前需再次提示对库存与业务关联的影响并请求确认。

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
