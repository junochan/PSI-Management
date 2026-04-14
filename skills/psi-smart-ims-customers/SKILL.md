---
name: psi-smart-ims-customers
description: >-
  智链进销存客户主数据：通过 psims customers 完成列表、详情、增删改、批量删除；对应 HTTP /customers。
  当用户维护销售客户资料、销售单选择客户或批量清理测试数据时使用。
---

# 客户

## 功能与作用

客户是 **销售与售后** 的基础档案。前端在销售模块的客户详情/编辑中维护；列表支持分页与筛选。

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

## HTTP 对照

| 方法 | 路径 |
|------|------|
| GET | `/customers` |
| GET | `/customers/{id}` |
| POST | `/customers` |
| PUT | `/customers/{id}` |
| DELETE | `/customers/{id}` |
| DELETE | `/customers/batch` |
