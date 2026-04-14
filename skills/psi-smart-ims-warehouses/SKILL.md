---
name: psi-smart-ims-warehouses
description: >-
  智链进销存仓库：通过 psims warehouses 管理仓库档案、获取全部仓库下拉列表、批量删除；对应 HTTP /warehouses。
  当用户配置多仓、库存/调拨选择仓库或脚本初始化仓库时使用。
---

# 仓库

## 功能与作用

仓库定义 **库存所在物理/逻辑仓位**。前端在库存相关仓库详情页展示；`all` 接口常用于下拉框一次性拉取全部仓库（不分页）。

## CLI 调用

```bash
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
| `list` | 分页列表 |
| `all` | 全部仓库（无分页） |
| `get <id>` | 详情 |
| `create` / `update <id>` / `delete <id>` | CRUD |
| `batch-delete` | id 数组 |

## HTTP 对照

| 方法 | 路径 |
|------|------|
| GET | `/warehouses` |
| GET | `/warehouses/all` |
| GET | `/warehouses/{id}` |
| POST | `/warehouses` |
| PUT | `/warehouses/{id}` |
| DELETE | `/warehouses/{id}` |
| DELETE | `/warehouses/batch` |
