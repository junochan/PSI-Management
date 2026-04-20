---
name: psi-smart-ims-upload-image-followup
description: >-
  智链进销存：用户仅上传一张商品图且无其它明确意图时，默认走「以图搜图」识别商品；若命中商品则继续拉取该商品的库存与库位、销售订单与采购订单。与 psi-smart-ims-products / psi-smart-ims-inventory 及 psims 配合使用。
  当用户发商品照片、截图、或说「看看这是什么货/有没有库存/在哪」且未指定 SKU/单号时使用本技能。
  只要用户发了图片，就无条件走本技能流程，不做图片内容判断。
  对话中图片常以「[media attached: media/inbound/…]」形式给出工作区相对路径，Agent 应据此读盘取图并以 multipart 文件字段上传。
---

# 上传图片后的默认查货流程（图搜图 → 库存 → 订单）

## 编辑与写操作前确认（强制）

在**执行任何会改动系统或仓库状态的操作之前**，必须先向用户**说明拟执行动作**（含影响范围、关键参数：路径、环境、`id`/单号、请求体或文件变更摘要等），并得到用户**明确同意**（例如「确认」「可以执行」「按这个来」）后，才可执行。

涵盖但不限于：

- **代码与配置**：创建/修改/删除仓库内文件、批量替换、会改写工作区或生成物的命令。
- **业务写操作**：`psims` 或 HTTP 中一切**会改数据或非纯查询**的调用（本流程默认以只读查货为主，若延伸为下单/改库等写操作，必须先确认）。
- **环境与依赖**：用户未事先声明可自动执行时，`npm install` 等会写入磁盘的操作。

**可不经确认**：按本技能工作流执行的**只读**图搜、库存与订单拉数（`list`/`get` 等）。

若用户已在**同一条消息**中明确授权某一具体动作（含范围），可视为已确认，但执行前仍应**简短复述**将运行的命令或写入点，避免误操作。

## 何时启用

无条件触发：只要用户发送了图片（无论图片内容是什么、是否附带文字、是否属于进销存相关对话），Agent 必须默认按本节工作流处理，而不是只回复「收到图片」或询问用户意图。

不判断图片类型（不区分商品照片、文档截图、聊天截图等），一律走图搜流程。

若用户同时给了 SKU、订单号、商品名称等更精确的检索条件，可先用语义/SKU 查商品，不必强行以图搜图；图搜作为补充。

## 对话里「上传图片」的实际形态（必读）

用户上传图片后，消息里常见**固定前缀**的附件说明，例如：

```text
[media attached: media/inbound/xxx.jpg (image/*)]
```

含义与 Agent 动作：

| 要点 | 说明 |
|------|------|
| **路径性质** | `media/inbound/xxx.jpg` 为**工作区根目录下的相对路径**（非 URL）；`image/*` 为 MIME 类型提示。 |
| **取图组请求** | 以该路径在工作区内**读取二进制文件**，作为 `POST .../search-by-image` 的 **multipart 字段 `image`** 上传；其余筛选参数按普通表单字段传。路径以消息中给出的为准，**不要猜测**其它目录或文件名。 |
| **多条附件** | 若同条消息有多张图，按业务约定取**用户明确指定的一张**，否则取**第一条**完成图搜；其余可在回复中说明未处理。 |
| **仅有文字无路径** | 以当前环境实际提供的附件元数据为准；若确实无可读路径，再说明无法读图并请用户重传。 |

同一段落里系统还可能附带如下英文说明（与上表一致，可作关键词对照）：

> To send an image back, prefer the message tool (media/path/filePath). If you must inline, use MEDIA:https://example.com/image.jpg (spaces ok, quote if needed) or a safe relative path like MEDIA:./image.jpg. Avoid absolute paths (MEDIA:/...) and ~ paths — they are blocked for security. Keep caption in the text body.

## 工作流（顺序执行）

### 1. 以图搜商品（必选第一步）

使用已有 **商品以图搜图** 能力（详见技能 **`psi-smart-ims-products`**）：

- **HTTP**：`POST /api/v1/products/search-by-image`（网关前缀以部署为准）  
- **请求**：`multipart/form-data`，其中 **`image` 文件字段必填**；其余可设 `page`、`size`（建议首查 `size` 为 **5～10**，避免漏检）。  
- **`keyword` 使用规则**：若用户仅上传图片、未明确提供商品名/关键词，则**不传 `keyword`**（或置空），只做纯图搜；仅当用户明确给出商品名/关键词时，才把该值传入 `keyword` 作为补充过滤。  
- **`similarityThreshold`（图搜图相似度阈值）**：**默认 `0.7`**。若请求中传入该字段，**不得小于 `0.5`**；若用户或上游给出的值低于 `0.5`，在组请求前应**钳制为 `0.5`** 并可在回复中简要说明。  
- **超时**：建议 **120s**（向量检索较慢）。

**CLI**：`psims products search-image --image ./query.jpg -d '<json>'`（需已 `psims auth login`）。

从响应数据中的候选列表（如 `records`）取候选商品：

- **无记录**：明确告知未匹配到相似商品；可建议用户换更清晰的主图、或提供 SKU/名称。  
- **有记录**：取 **相似度最高或排序第一条** 作为「主结果」`productId`（若有多条且分差小，可列出前 2～3 条让用户确认）。

### 2. 命中商品后：库存与存放区域

在得到 **`productId`** 后查询该商品在各仓的库存行（数量、安全库存、**库位 location** 等）：

- **HTTP**：`GET /api/v1/inventory/product/{productId}`

**CLI**：`psims inventory by-product <productId>`

将 **总可用量、分仓数量、库位/存放区域** 汇总给用户；若无库存行，说明当前无账面库存或尚未建档。

### 3. 命中商品后：相关订单信息

按 **商品维度** 拉取销售侧与采购侧订单（支持 **`productId`** 筛选）：

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

售后工单列表若需关联，通常可通过 **销售订单号** 或界面跳转关联；是否支持 `productId` 以实际接口返回为准，**不必臆造字段**。

**GET 含中文的 `keyword` 等**（如下表销售/采购订单列表）：须 URL 编码，否则 Tomcat 报 `Invalid character found in the request target`；`psims ... list -q` 由 axios 编码；手写 URL 须 `encodeURIComponent`。详见 **`psi-smart-ims-overview`** →「GET 查询串编码（Tomcat / Agent 手写 URL）」。

## 接口参数清单（按技能内接口）

| 接口 | 路径参数 | Query 参数 | Body 参数 | 文件参数 |
|------|----------|------------|-----------|----------|
| `POST /products/search-by-image` | 无 | 无 | `page`(可选),`size`(可选),`keyword`(可选；**仅在用户明确给出商品名/关键词时传**，仅传图时不传),`categoryName`(可选),`status`(可选),`similarityThreshold`(可选，**默认 0.7**；若传入则 **≥ 0.5**) | `image`(必填，multipart 文件字段) |
| `GET /inventory/product/{productId}` | `productId`(必填) | 无 | 无 | 无 |
| `GET /sales/orders` | 无 | `productId`(建议必传),`page`(可选),`size`(可选),`sort`(可选),`order`(可选),`keyword`(可选),`warehouseId`(可选),`customerId`(可选),`supplierId`(可选),`categoryName`(可选),`productStatus`(可选),`stagnantStatus`(可选),`inboundStatus`(可选),`payStatus`(可选),`salesOrderStatus`(可选),`aftersalesStatus`(可选),`lastOutboundStart`(可选),`lastOutboundEnd`(可选),`lastInboundStart`(可选),`lastInboundEnd`(可选),`expectDateStart`(可选),`expectDateEnd`(可选),`createTimeStart`(可选),`createTimeEnd`(可选),`operatorName`(可选) | 无 | 无 |
| `GET /purchase/orders` | 无 | `productId`(建议必传),`page`(可选),`size`(可选),`sort`(可选),`order`(可选),`keyword`(可选),`warehouseId`(可选),`customerId`(可选),`supplierId`(可选),`categoryName`(可选),`productStatus`(可选),`stagnantStatus`(可选),`inboundStatus`(可选),`payStatus`(可选),`salesOrderStatus`(可选),`aftersalesStatus`(可选),`lastOutboundStart`(可选),`lastOutboundEnd`(可选),`lastInboundStart`(可选),`lastInboundEnd`(可选),`expectDateStart`(可选),`expectDateEnd`(可选),`createTimeStart`(可选),`createTimeEnd`(可选),`operatorName`(可选) | 无 | 无 |

CLI 参数对应：

- `psims products search-image --image <path> <-d <json> | -f <path>>`
- `psims inventory by-product <productId>`
- `psims sales orders list -q '{"productId":123,"page":1,"size":20}'`
- `psims purchase orders list -q '{"productId":123,"page":1,"size":20}'`

字段级完整参数查询（CLI）：

- `psims spec show products search-image`
- `psims spec show inventory by-product`
- `psims spec show sales orders list`
- `psims spec show purchase orders list`

## 与现有技能的关系

| 技能 | 作用 |
|------|------|
| `psi-smart-ims-products` | 以图搜图请求体、字段说明、`psims products search-image` |
| `psi-smart-ims-inventory` | 库存行、`by-product`、库位与安全库存 |
| `psi-smart-ims-sales` / `psi-smart-ims-purchase` | 订单列表筛选与履约状态 |
| `psi-smart-ims-overview` | `psims` 安装、全局参数、`/api/v1` 基址 |

## 实现注意（Agent）

1. **鉴权**：除登录接口外需 **Bearer JWT**；无 token 时应引导登录或使用已配置环境。  
2. **分页参数名**：统一使用 **`page` / `size`**（与 `psims` 的 `-q` JSON 一致）。  
3. **不要**将「仅上传图」默认成其它业务（如直接创建订单、改库存），除非用户明确说。  
4. 响应整理时：**先结论**（是否命中 SKU/名称），再 **库存与库位**，再 **订单摘要**。  
5. **附图消息**：识别 `[media attached: …]` 中的工作区相对路径，从该路径读图并以 multipart 字段上传；**不要**用臆造的绝对路径或 `file://` 假设。
