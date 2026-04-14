---
name: psi-smart-ims-aftersales
description: >-
  智链进销存售后：通过 psims aftersales 查询、创建、处理、关闭售后单；对应 HTTP /aftersales。
  当用户跟进退换修、客服闭环或按条件筛选售后记录时使用。
---

# 售后

## 功能与作用

售后单关联销售订单与客户诉求，支持 **处理**（填写处理结果）与 **关闭**。列表接口支持分页与筛选。

对应前端：`/sales/aftersales/:id`。

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
| `handle <id>` | 处理（Body 由后端 DTO 决定） |
| `close <id>` | 关闭 |

## HTTP 对照

| 方法 | 路径 |
|------|------|
| GET | `/aftersales` |
| GET | `/aftersales/{id}` |
| POST | `/aftersales` |
| PUT | `/aftersales/{id}/handle` |
| PUT | `/aftersales/{id}/close` |
