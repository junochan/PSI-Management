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

适用于商品列表、商品新增/编辑/详情、分类维护与批量导入场景。

## CLI 调用

**前置**：已 `psims auth login` 或设置 `PSI_API_BASE` + `--token` / `PSI_TOKEN`。

### 商品 `psims products <子命令>`

| 子命令 | 作用 |
|--------|------|
| `list` | 分页/筛选列表，`-q` 传查询 JSON |
| `get <id>` | 详情 |
| `create` | 新建，`-d` 或 `-f` 传商品 JSON |
| `update <id>` | 更新 |
| `delete <id>` | 删除 |
| `batch-delete` | Body 为 id 数组 JSON |
| `import-excel` | **必须** `--file <本地 Excel>`，multipart 字段 `file`，返回含 `jobId` |
| `import-task <jobId>` | 查询异步导入进度（与 `getImportTask` 一致） |
| `upload-image` | **必须** `--file <本地路径>`，对应 multipart `file` |
| `search-image` | Body 含 `imageBase64` 等图片检索字段 |

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

## 接口参数清单（按技能内接口）

### 商品接口

| 接口 | 路径参数 | Query 参数 | Body 参数 | 文件参数 |
|------|----------|------------|-----------|----------|
| `GET /products` | 无 | `page`,`size`,`sort`,`order`,`keyword`,`productId`,`warehouseId`,`customerId`,`supplierId`,`categoryName`,`productStatus`,`stagnantStatus`,`inboundStatus`,`payStatus`,`salesOrderStatus`,`aftersalesStatus`,`lastOutboundStart`,`lastOutboundEnd`,`lastInboundStart`,`lastInboundEnd`,`expectDateStart`,`expectDateEnd`,`createTimeStart`,`createTimeEnd`,`operatorName`（均可选） | 无 | 无 |
| `GET /products/{id}` | `id`(必填) | 无 | 无 | 无 |
| `POST /products` | 无 | 无 | `code`(可选),`name`(必填),`brand`(可选),`spec`(可选),`categoryId`(可选),`categoryName`(可选),`costPrice`(必填),`salePrice`(必填),`status`(可选),`image`(可选),`description`(可选),`safeStock`(可选),`initialStock`(可选),`warehouseId`(可选) | 无 |
| `PUT /products/{id}` | `id`(必填) | 无 | 同 `POST /products` 字段 | 无 |
| `DELETE /products/{id}` | `id`(必填) | 无 | 无 | 无 |
| `DELETE /products/batch` | 无 | 无 | `ids`(必填, `number[]`) | 无 |
| `POST /products/import` | 无 | 无 | 无 | `file`(必填, Excel) |
| `GET /products/import/{jobId}` | `jobId`(必填) | 无 | 无 | 无 |
| `POST /products/image` | 无 | 无 | 无 | `file`(必填, 图片) |
| `POST /products/search-by-image` | 无 | 无 | `page`(可选),`size`(可选),`keyword`(可选),`categoryName`(可选),`status`(可选),`imageBase64`(必填),`similarityThreshold`(可选) | 无 |

### 分类接口

| 接口 | 路径参数 | Query 参数 | Body 参数 | 文件参数 |
|------|----------|------------|-----------|----------|
| `GET /categories` | 无 | 无 | 无 | 无 |
| `GET /categories/{id}` | `id`(必填) | 无 | 无 | 无 |
| `POST /categories` | 无 | 无 | `name`(可选),`code`(可选),`parentId`(可选),`sort`(可选),`status`(可选) | 无 |
| `PUT /categories/{id}` | `id`(必填) | 无 | 同 `POST /categories` 字段 | 无 |
| `DELETE /categories/{id}` | `id`(必填) | 无 | 无 | 无 |

CLI 参数对应：

- 列表：`-q|--query <json>`
- 写入 Body：`-d|--data <json>` 或 `-f|--file <path>`
- 上传文件：`--file <path>`

字段级完整参数查询（CLI）：

- `psims spec show products create`
- `psims spec show products update`
- `psims spec show products search-image`
- `psims spec show categories create`

## 写操作执行规范（必须）

涉及商品/分类数据变更的操作（`create`、`update`、`delete`、`batch-delete`、`import-excel`）必须按以下顺序执行：

1. 先向用户确认并补齐必填字段（如商品名称、SKU、分类、价格、库存相关字段，或分类名称等）。
2. 若用户信息不全，先引导补齐，不直接执行写操作。
3. **新增商品（`products create`）时，若当前会话中已提供该商品图片，必须先执行 `psims products upload-image --file <该图片路径>` 上传图片；拿到上传成功返回的图片 URL 后，再将该 URL 作为商品图片字段放入 `products create` 的请求体一起创建；不可省略。**
4. 执行前给出变更摘要（目标对象、关键字段、影响范围）。
5. 仅在用户明确回复“确认执行”后再调用写命令。
6. 删除/批量删除属于高风险操作，需再次提示不可逆影响后再执行。

## HTTP 对照

- 商品：`/products`、`/products/{id}`、`DELETE /products/batch`、`POST /products/import`、`GET /products/import/{jobId}`、`POST /products/image`、`POST /products/search-by-image`
- 分类：`/categories` 及 `/{id}`

静态资源 URL 前缀以服务端实际返回或部署配置为准（常见如 `/api/uploads/products/`）。
