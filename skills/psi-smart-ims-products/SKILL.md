---
name: psi-smart-ims-products
description: >-
  智链进销存商品与分类：通过 psims 管理商品 CRUD、批量删除、Excel 异步导入与进度查询、图片上传、以图搜图及商品分类 CRUD；对应 HTTP /products 与 /categories。
  当用户维护商品主数据、批量 Excel 导入、或用图片检索商品时使用。
---

# 商品与商品分类

## 功能与作用

- **商品**：维护 SKU、价格、库存相关主数据；支持 **主图上传**（multipart）、**Excel 异步批量导入**（`POST /products/import` 返回 `jobId`，`GET /products/import/{jobId}` 查进度）与 **以图搜图**（向量相似度，较慢）。
- **分类**：树/列表维度的分类维护，供商品选择与筛选。
- **批量删除**：一次提交多个商品 id，对应 `DELETE /products/batch`。

对应前端：`/products`、商品新增/编辑/详情、`productApi.importProducts` / `getImportTask`；分类在商品表单与设置中消费。

## CLI 调用

**前置**：已 `psims auth login` 或设置 `PSI_API_BASE` + `--token` / `PSI_TOKEN`。

### 商品 `psims products <子命令>`

| 子命令 | 作用 |
|--------|------|
| `list` | 分页/筛选列表，`-q` 传查询 JSON |
| `get <id>` | 详情 |
| `create` | 新建，`-d` 或 `-f` 传 ProductDTO |
| `update <id>` | 更新 |
| `delete <id>` | 删除 |
| `batch-delete` | Body 为 id 数组 JSON |
| `import-excel` | **必须** `--file <本地 Excel>`，multipart 字段 `file`，返回含 `jobId` |
| `import-task <jobId>` | 查询异步导入进度（与 `getImportTask` 一致） |
| `upload-image` | **必须** `--file <本地路径>`，对应 multipart `file` |
| `search-image` | Body 含 `imageBase64` 等（见后端 `ProductImageSearchRequest`） |

示例：

```bash
psims products list -q "{\"current\":1,\"size\":10}"
psims products get 1
psims products create -d "{\"productName\":\"测试\",\"sku\":\"T1\"}"
psims products upload-image --file ./a.jpg
psims products import-excel --file ./products.xlsx
psims products import-task "<jobId>"
psims products search-image -f ./search-body.json
```

### 分类 `psims categories <子命令>`

| 子命令 | 作用 |
|--------|------|
| `list` | 全部分类 |
| `get <id>` | 详情 |
| `create` / `update <id>` / `delete <id>` | 增删改，Body 用 `-d` 或 `-f` |

```bash
psims categories list
psims categories create -d "{\"name\":\"数码\"}"
```

## HTTP 对照

- 商品：`/products`、`/products/{id}`、`DELETE /products/batch`、`POST /products/import`、`GET /products/import/{jobId}`、`POST /products/image`、`POST /products/search-by-image`
- 分类：`/categories` 及 `/{id}`

静态资源 URL 前缀见后端配置（如 `/api/uploads/products/`）。
