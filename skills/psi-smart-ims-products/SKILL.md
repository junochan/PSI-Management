---
name: psi-smart-ims-products
description: >-
  智链进销存商品与分类：通过 psims 管理商品 CRUD、批量删除、Excel 异步导入与进度查询、图片上传、以图搜图及商品分类 CRUD；对应 HTTP /products 与 /categories。
  当用户维护商品主数据、批量 Excel 导入、或用图片检索商品时使用。
---

# 商品与商品分类

## ⚠️ 写操作前确认（最高优先级，不可跳过）

**本技能中所有会改动系统数据的操作，必须先暂停 → 向用户确认 → 得到明确同意 → 才可执行。**

### 什么是写操作（必须确认）

| 类别 | 示例 |
|------|------|
| 新增 | `POST /products`（创建商品）、`POST /categories`（创建分类） |
| 修改 | `PUT /products/{id}`、`PUT /categories/{id}` |
| 删除 | `DELETE /products/{id}`、`DELETE /products/batch`、`DELETE /categories/{id}` |
| 导入 | `POST /products/import`（Excel 异步导入） |
| 上传 | `POST /products/image`（图片上传，会新增文件） |

**可不经确认的只读操作**：`GET /products`（列表）、`GET /products/{id}`（详情）、`POST /products/search-by-image`（以图搜图）、`GET /products/import/{jobId}`（查导入进度）、`GET /categories` 等纯查询。

### 确认流程（每次写操作前必须完整执行）

```
1. 收集必要参数（商品名称、价格、分类等必填字段）
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

跳过确认直接执行写操作是严重失误，会导致数据被错误创建/修改/删除。每次执行前问一句，远胜于事后补救。

---

## 功能与作用

- **商品**：维护 SKU、价格、库存相关主数据；支持 **主图上传**（multipart）、**Excel 异步批量导入**（`POST /products/import` 返回 `jobId`，`GET /products/import/{jobId}` 查进度）与 **以图搜图**（向量相似度，较慢）。
- **分类**：树/列表维度的分类维护，供商品选择与筛选。
- **批量删除**：一次提交多个商品 id，对应 `DELETE /products/batch`。

适用于商品列表、商品新增/编辑/详情、分类维护与批量导入场景。

**GET 含中文的 `keyword`**：`GET /products` 的查询串须 UTF-8 百分号编码；未编码时 Tomcat 报 `Invalid character found in the request target`。`psims products list -q '{"keyword":"红"}'` 由 axios 自动编码；手写 URL/curl 须 `encodeURIComponent`。完整说明见技能 **`psi-smart-ims-overview`** →「GET 查询串编码（Tomcat / Agent 手写 URL）」。

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
| `POST /products` | 无 | 无 | `code`(可选),`name`(必填),`brand`(可选),`spec`(可选),`categoryId`(可选),`categoryName`(可选),`costPrice`(必填),`salePrice`(必填),`status`(可选),`image`(可选，商品图片地址如果有需要先调图片上传接口),`description`(可选),`safeStock`(可选),`initialStock`(可选),`warehouseId`(可选) | 无 |
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

## 各写操作执行步骤

### 新增商品（POST /products）

```
步骤 1：收集必填字段
  必须字段：name（商品名称）、costPrice（成本价）、salePrice（售价）
  可选字段：brand、spec、categoryId、categoryName、status、image、description、safeStock 等

步骤 2：若用户提供图片但无 image URL
  先执行 POST /products/image 上传图片 → 获取 URL → 放入商品 Body

步骤 3：⛔ 输出确认卡片，停止等待

步骤 4：用户确认后再执行 POST /products
```

### 修改商品（PUT /products/{id}）

```
步骤 1：确认要修改的商品 id 及变更字段

步骤 2：⛔ 输出确认卡片（含 id、变更前后值），停止等待

步骤 3：用户确认后再执行 PUT /products/{id}
```

### 删除商品（DELETE /products/{id}）

```
步骤 1：确认要删除的商品 id 及名称

步骤 2：⛔ 输出确认卡片 + ⚠️ 不可逆提示，停止等待

步骤 3：用户确认后再执行 DELETE /products/{id}
```

### 批量删除（DELETE /products/batch）

```
步骤 1：确认要删除的商品 id 列表及数量

步骤 2：⛔ 输出确认卡片 + ⚠️ 不可逆 + 影响数量提示，停止等待

步骤 3：用户确认后再执行
```

## HTTP 对照

- 商品：`/products`、`/products/{id}`、`DELETE /products/batch`、`POST /products/import`、`GET /products/import/{jobId}`、`POST /products/image`、`POST /products/search-by-image`
- 分类：`/categories` 及 `/{id}`

静态资源 URL 前缀以服务端实际返回或部署配置为准（常见如 `/api/uploads/products/`）。
