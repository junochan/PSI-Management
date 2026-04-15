---
name: psi-smart-ims-upload-image-followup
description: >-
  智链进销存：用户仅上传一张商品图且无其它明确意图时，默认走「以图搜图」识别商品；若命中商品则继续拉取该商品的库存与库位、销售订单与采购订单。与 psi-smart-ims-products / psi-smart-ims-inventory 及 psims 配合使用。
  当用户发商品照片、截图、或说「看看这是什么货/有没有库存/在哪」且未指定 SKU/单号时使用本技能。
---

# 上传图片后的默认查货流程（图搜图 → 库存 → 订单）

## 何时启用

在 **smart-ims / PSI 进销存** 相关对话中，若同时满足：

1. 用户 **上传了至少一张图片**（或提供了可当作查询图的本地路径 / base64），且  
2. **没有**更优先的意图（例如：不是改代码、不是部署、不是纯闲聊），且  
3. 未明确要求「不要用图搜」「只用 SKU 查」等，

则 Agent **默认**按本节工作流处理，而不是只回复「收到图片」。

若用户同时给了 SKU、订单号、商品名称等 **更精确的检索条件**，可先用语义/SKU 查商品，**不必**强行以图搜图；图搜作为补充。

## 工作流（顺序执行）

### 1. 以图搜商品（必选第一步）

使用已有 **商品以图搜图** 能力（详见技能 **`psi-smart-ims-products`**）：

- **HTTP**：`POST /api/v1/products/search-by-image`（网关前缀以部署为准，与前端 `productApi.searchByImage` 一致）  
- **Body**：`ProductImageSearchRequest`，其中 **`imageBase64` 必填**；可设 `page`、`size`（建议首查 `size` 为 **5～10**，避免漏检）。  
- **超时**：与前端一致建议 **120s**（向量检索较慢）。

**CLI**：`psims products search-image -d '<json>'` 或 `-f body.json`（需已 `psims auth login`）。

从响应 **`PageResult<Product>`** 的 `records` 中取候选商品：

- **无记录**：明确告知未匹配到相似商品；可建议用户换更清晰的主图、或提供 SKU/名称。  
- **有记录**：取 **相似度最高或排序第一条** 作为「主结果」`productId`（若有多条且分差小，可列出前 2～3 条让用户确认）。

### 2. 命中商品后：库存与存放区域

在得到 **`productId`** 后查询该商品在各仓的库存行（数量、安全库存、**库位 location** 等）：

- **HTTP**：`GET /api/v1/inventory/product/{productId}`（与 `inventoryApi.byProduct` 一致）

**CLI**：`psims inventory by-product <productId>`

将 **总可用量、分仓数量、库位/存放区域** 汇总给用户；若无库存行，说明当前无账面库存或尚未建档。

### 3. 命中商品后：相关订单信息

按 **商品维度** 拉取销售侧与采购侧订单（后端 `PageQuery` 支持 **`productId`** 筛选）：

| 业务 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 销售订单 | GET | `/sales/orders` | Query：`productId`、`page`、`size` 等 |
| 采购订单 | GET | `/purchase/orders` | Query：`productId`、`page`、`size` 等 |

**CLI**：

```bash
psims sales orders list -q "{\"productId\":123,\"page\":1,\"size\":20}"
psims purchase orders list -q "{\"productId\":123,\"page\":1,\"size\":20}"
```

向用户概括：**近期销售单/采购单笔数、订单号、状态、客户/供应商、数量与金额**（可按时间倒序取前若干条，避免刷屏）。

售后工单列表若需关联，通常可通过 **销售订单号** 或界面跳转关联；后端列表是否支持 `productId` 以实际接口为准，**不必臆造字段**。

## 与现有技能的关系

| 技能 | 作用 |
|------|------|
| `psi-smart-ims-products` | 以图搜图请求体、字段说明、`psims products search-image` |
| `psi-smart-ims-inventory` | 库存行、`by-product`、库位与安全库存 |
| `psi-smart-ims-sales` / `psi-smart-ims-purchase` | 订单列表筛选与履约状态 |
| `psi-smart-ims-overview` | `psims` 安装、全局参数、`/api/v1` 基址 |

## 实现注意（Agent）

1. **鉴权**：除登录接口外需 **Bearer JWT**；无 token 时应引导登录或使用已配置环境。  
2. **分页参数名**：后端 `PageQuery` 使用 **`page` / `size`**（与 `psims` 的 `-q` JSON 一致）。  
3. **不要**将「仅上传图」默认成其它业务（如直接创建订单、改库存），除非用户明确说。  
4. 响应整理时：**先结论**（是否命中 SKU/名称），再 **库存与库位**，再 **订单摘要**。
