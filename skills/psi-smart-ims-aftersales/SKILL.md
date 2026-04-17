---
name: psi-smart-ims-aftersales
description: >-
  智链进销存售后：通过 psims aftersales 查询、创建、处理、关闭售后单；对应 HTTP /aftersales。
  当用户跟进退换修、客服闭环或按条件筛选售后记录时使用。
---

# 售后

## 编辑与写操作前确认（强制）

在**执行任何会改动系统或仓库状态的操作之前**，必须先向用户**说明拟执行动作**（含影响范围、关键参数：路径、环境、`id`/单号、请求体或文件变更摘要等），并得到用户**明确同意**（例如「确认」「可以执行」「按这个来」）后，才可执行。

涵盖但不限于：

- **代码与配置**：创建/修改/删除仓库内文件、批量替换、会改写工作区或生成物的命令。
- **业务写操作**：`psims` 或 HTTP 中带 `create`、`handle`、`close` 等**会改数据或非纯查询**的调用。
- **环境与依赖**：用户未事先声明可自动执行时，`npm install` 等会写入磁盘的操作；直接改库、清缓存等。

**可不经确认**：只读排查（读文件、`list`/`get` 等纯读接口）、纯口头方案及文档说明。

若用户已在**同一条消息**中明确授权某一具体动作（含范围），可视为已确认，但执行前仍应**简短复述**将运行的命令或写入点，避免误操作。

## 功能与作用

售后单关联销售订单与客户诉求，支持 **处理**（填写处理结果）与 **关闭**。列表接口支持分页与筛选。

适用于售后列表、售后详情与售后处理闭环场景。

**GET 含中文的 `keyword` 等**：须 URL 编码，否则 Tomcat 报 `Invalid character found in the request target`；`psims aftersales list -q` 由 axios 编码。详见 **`psi-smart-ims-overview`** →「GET 查询串编码（Tomcat / Agent 手写 URL）」。

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
| `handle <id>` | 处理（Body 为处理请求 JSON） |
| `close <id>` | 关闭 |

## 接口参数清单（按技能内接口）

| 接口 | 路径参数 | Query 参数 | Body 参数 | 文件参数 |
|------|----------|------------|-----------|----------|
| `GET /aftersales` | 无 | `page`,`size`,`sort`,`order`,`keyword`,`productId`,`warehouseId`,`customerId`,`supplierId`,`categoryName`,`productStatus`,`stagnantStatus`,`inboundStatus`,`payStatus`,`salesOrderStatus`,`aftersalesStatus`,`lastOutboundStart`,`lastOutboundEnd`,`lastInboundStart`,`lastInboundEnd`,`expectDateStart`,`expectDateEnd`,`createTimeStart`,`createTimeEnd`,`operatorName`（均可选） | 无 | 无 |
| `GET /aftersales/{id}` | `id`(必填) | 无 | 无 | 无 |
| `POST /aftersales` | 无 | 无 | `salesOrderId`(必填),`type`(必填),`content`(必填),`expectHandle`(可选),`refundAmount`(可选),`remark`(可选) | 无 |
| `PUT /aftersales/{id}/handle` | `id`(必填) | 无 | `handleResult`(必填),`refundAmount`(可选),`remark`(可选) | 无 |
| `PUT /aftersales/{id}/close` | `id`(必填) | 无 | 无 | 无 |

CLI 参数对应：`list` 用 `-q`；`create/handle` 用 `-d` 或 `-f`。

字段级完整参数查询（CLI）：

- `psims spec show aftersales list`
- `psims spec show aftersales create`
- `psims spec show aftersales handle`

## 写操作执行规范（必须）

涉及售后数据变更的操作（`create`、`handle`、`close`）必须遵循：

1. 先确认并补齐必填字段（如关联订单、客户、问题描述、处理结果等）。
2. 信息不完整时先引导用户补齐，禁止直接执行写命令。
3. 执行前输出变更摘要（售后单、关键字段、状态变化）。
4. 仅在用户明确确认后再执行。
5. `close` 前需再次确认“本次将结束该售后流程”。

## HTTP 对照

| 方法 | 路径 |
|------|------|
| GET | `/aftersales` |
| GET | `/aftersales/{id}` |
| POST | `/aftersales` |
| PUT | `/aftersales/{id}/handle` |
| PUT | `/aftersales/{id}/close` |
