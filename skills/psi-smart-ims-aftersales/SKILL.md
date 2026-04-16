---
name: psi-smart-ims-aftersales
description: >-
  智链进销存售后：通过 psims aftersales 查询、创建、处理、关闭售后单；对应 HTTP /aftersales。
  当用户跟进退换修、客服闭环或按条件筛选售后记录时使用。
---

# 售后

## 功能与作用

售后单关联销售订单与客户诉求，支持 **处理**（填写处理结果）与 **关闭**。列表接口支持分页与筛选。

适用于售后列表、售后详情与售后处理闭环场景。

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
