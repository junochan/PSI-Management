---
name: psi-smart-ims-purchase
description: >-
  智链进销存采购管理：通过 psims purchase 管理采购订单、入库单查询与采购统计；对应 HTTP /purchase/*。
  涉及采购订单相关的时候使用。
---

# 采购（订单与入库）

## 编辑与写操作前确认（强制）

在**执行任何会改动系统或仓库状态的操作之前**，必须先向用户**说明拟执行动作**（含影响范围、关键参数：路径、环境、`id`/单号、请求体或文件变更摘要等），并得到用户**明确同意**（例如「确认」「可以执行」「按这个来」）后，才可执行。

涵盖但不限于：

- **代码与配置**：创建/修改/删除仓库内文件、批量替换、会改写工作区或生成物的命令。
- **业务写操作**：`psims` 或 HTTP 中带 `create`、`update`、`delete`、`cancel`、`confirm`、采购入库等**会改数据或非纯查询**的调用。
- **环境与依赖**：用户未事先声明可自动执行时，`npm install` 等会写入磁盘的操作；直接改库、清缓存等。

**可不经确认**：只读排查（读文件、`list`/`get`/统计等纯读接口）、纯口头方案及文档说明。

若用户已在**同一条消息**中明确授权某一具体动作（含范围），可视为已确认，但执行前仍应**简短复述**将运行的命令或写入点，避免误操作。

## 功能与作用

- **采购订单**：向供应商下单，可后续 **取消**、**删除**。
- **查询入库单**：查询由采购产生的入库记录及明细。
- **统计**：采购侧汇总数据（金额、笔数等，以接口返回字段为准）。

适用于采购单详情/编辑、入库单查询与采购统计场景。

**GET 含中文的 `keyword` 等**：须 URL 编码，否则 Tomcat 报 `Invalid character found in the request target`；`psims purchase orders list -q` / `psims purchase inbounds list -q` 等由 axios 编码。详见 **`psi-smart-ims-overview`** →「GET 查询串编码（Tomcat / Agent 手写 URL）」。

## CLI 调用

### 采购订单 `psims purchase orders <子命令>`

```bash
psims purchase orders list -q "{\"current\":1,\"size\":10}"
psims purchase orders get 1
psims purchase orders create -f ./po.json
psims purchase orders update 1 -d "{...}"
psims purchase orders cancel 1
psims purchase orders delete 1
psims purchase orders inbound 1 -f ./inbound.json
```

| 子命令 | 作用 |
|--------|------|
| `list` | 订单分页列表 |
| `get <id>` | 订单详情 |
| `create` | 新建订单 |
| `update <id>` | 修改订单 |
| `cancel <id>` | 取消 |
| `delete <id>` | 删除 |
| `inbound <id>` | 对该订单执行采购入库，Body 为入库请求 JSON |

### 入库单 `psims purchase inbounds <子命令>`

```bash
psims purchase inbounds list -q "{}"
psims purchase inbounds get 1
```

### 统计

```bash
psims purchase stats
```

## 接口参数清单（按技能内接口）

### 采购订单

| 接口 | 路径参数 | Query 参数 | Body 参数 | 文件参数 |
|------|----------|------------|-----------|----------|
| `GET /purchase/orders` | 无 | `page`,`size`,`sort`,`order`,`keyword`,`productId`,`warehouseId`,`customerId`,`supplierId`,`categoryName`,`productStatus`,`stagnantStatus`,`inboundStatus`,`payStatus`,`salesOrderStatus`,`aftersalesStatus`,`lastOutboundStart`,`lastOutboundEnd`,`lastInboundStart`,`lastInboundEnd`,`expectDateStart`,`expectDateEnd`,`createTimeStart`,`createTimeEnd`,`operatorName`（均可选） | 无 | 无 |
| `GET /purchase/orders/{id}` | `id`(必填) | 无 | 无 | 无 |
| `POST /purchase/orders` | 无 | 无 | `supplierId`(必填),`productId`(必填),`quantity`(必填),`unitPrice`(必填),`expectDate`(可选),`payMethod`(可选),`remark`(可选) | 无 |
| `PUT /purchase/orders/{id}` | `id`(必填) | 无 | 同 `POST /purchase/orders` 字段 | 无 |
| `PUT /purchase/orders/{id}/cancel` | `id`(必填) | 无 | 无 | 无 |
| `DELETE /purchase/orders/{id}` | `id`(必填) | 无 | 无 | 无 |
| `POST /purchase/orders/{id}/inbound` | `id`(必填) | 无 | `warehouseId`(必填),`quantity`(必填),`batchNo`(可选),`remark`(可选) | 无 |

### 入库单与统计

| 接口 | 路径参数 | Query 参数 | Body 参数 | 文件参数 |
|------|----------|------------|-----------|----------|
| `GET /purchase/inbounds` | 无 | `page`,`size`,`sort`,`order`,`keyword`,`productId`,`warehouseId`,`customerId`,`supplierId`,`categoryName`,`productStatus`,`stagnantStatus`,`inboundStatus`,`payStatus`,`salesOrderStatus`,`aftersalesStatus`,`lastOutboundStart`,`lastOutboundEnd`,`lastInboundStart`,`lastInboundEnd`,`expectDateStart`,`expectDateEnd`,`createTimeStart`,`createTimeEnd`,`operatorName`（均可选） | 无 | 无 |
| `GET /purchase/inbounds/{id}` | `id`(必填) | 无 | 无 | 无 |
| `GET /purchase/stats` | 无 | 无 | 无 | 无 |

CLI 参数对应：`list` 用 `-q`；`create/update/inbound` 用 `-d` 或 `-f`。

字段级完整参数查询（CLI）：

- `psims spec show purchase orders list`
- `psims spec show purchase orders create`
- `psims spec show purchase orders update`
- `psims spec show purchase orders inbound`

## 写操作执行规范（必须）

涉及采购数据变更的操作（`create`、`update`、`cancel`、`delete`、`inbound`）必须遵循：

1. 先确认并补齐必填字段（如供应商、商品明细、数量、价格相关字段等）。
2. 信息不完整时先引导用户补齐，禁止直接执行写命令。
3. 执行前输出变更摘要（单据编号或目标对象、关键字段、预期库存影响）。
4. 仅在用户明确确认后再执行。
5. `cancel`、`delete`、`inbound` 属于高影响操作，需再次提示影响范围后再执行。

## HTTP 对照

- 订单：`/purchase/orders`、 `/purchase/orders/{id}`、 `cancel`、`DELETE`、`POST .../inbound`
- 入库：`/purchase/inbounds`、`/purchase/inbounds/{id}`
- 统计：`GET /purchase/stats`
