---
name: psi-smart-ims-suppliers
description: >-
  智链进销存供应商主数据：通过 psims suppliers 完成列表、详情、增删改、批量删除；对应 HTTP /suppliers。
  当用户维护采购往来单位、采购单选择供应商或脚本同步供应商时使用。
---

# 供应商

## 功能与作用

供应商是 **采购业务** 的基础主数据：名称、联系方式、结算方式等。前端在采购模块的供应商详情/编辑页维护；列表接口支持分页与筛选（查询参数与后端 `PageQuery` 及自定义字段一致）。

## CLI 调用

```bash
psims suppliers list -q "{\"current\":1,\"size\":10}"
psims suppliers get 1
psims suppliers create -d "{\"supplierName\":\"某厂\"}"
psims suppliers update 1 -f ./supplier.json
psims suppliers delete 1
psims suppliers batch-delete -d "[1,2,3]"
```

| 子命令 | 说明 |
|--------|------|
| `list` | `-q` 查询 JSON |
| `get <id>` | 详情 |
| `create` | `-d` / `-f` Body |
| `update <id>` | 同上 |
| `delete <id>` | 单删 |
| `batch-delete` | Body 为 id 数组 |

## HTTP 对照

| 方法 | 路径 |
|------|------|
| GET | `/suppliers` |
| GET | `/suppliers/{id}` |
| POST | `/suppliers` |
| PUT | `/suppliers/{id}` |
| DELETE | `/suppliers/{id}` |
| DELETE | `/suppliers/batch` |
