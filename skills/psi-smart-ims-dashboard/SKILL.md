---
name: psi-smart-ims-dashboard
description: >-
  智链进销存仪表盘：通过 psims dashboard overview 拉取聚合经营数据；对应 HTTP GET /dashboard/overview。
  当用户看首页 KPI、销售趋势、分类/商品/客户排行、库存预警与呆滞、最近订单或脚本导出仪表盘 JSON 时使用。
---

# 仪表盘（聚合数据）

## 功能与作用

`GET /dashboard/overview` 在选定时间窗口内汇总：

- **summary**：本月销售额、本月成交订单数、在售商品种类、客户总数（字段名以接口返回为准）。
- **salesTrend**：按日销售趋势（区间内每日一条，无单为 0）。
- **categorySalesTop5**：分类销售 Top5（含占比等）。
- **productSalesTop5** / **customerSalesTop5**：商品与客户销售排名。
- **recentOrders**：最近订单列表。
- **inventoryWarningTop10** / **inventoryStagnantTop10**：库存预警与呆滞 Top 列表。

查询参数 **`days`** 仅允许 **7、30、90**（其它值会被服务端纠正为默认 **7**）。

可用于首页或仪表盘视图的数据拉取与脚本化导出。

## CLI 调用

**前置**：`psims` 位于 **`psi-smart-ims-overview/psi-cli/`**（见总览技能）；已 `npm install`，并已 **`psims auth login`** 或设置 `PSI_TOKEN` / `--token`。

```bash
psims dashboard overview
psims dashboard overview --days 30
psims dashboard overview --days 90
```

| 子命令 | 说明 |
|--------|------|
| `overview` | `GET /dashboard/overview`，`-d / --days` 为 **7**（默认）、**30** 或 **90** |

终端输出为解包后的 **`data`**，便于 `jq` 等工具处理。

## 接口参数清单（按技能内接口）

| 接口 | 路径参数 | Query 参数 | Body 参数 | 文件参数 |
|------|----------|------------|-----------|----------|
| `GET /dashboard/overview` | 无 | `days`(可选, 仅 `7`/`30`/`90`, 默认 `7`) | 无 | 无 |

CLI 参数对应：`psims dashboard overview [-d|--days <7|30|90>]`

字段级完整参数查询（CLI）：`psims spec show dashboard overview`

## HTTP 对照

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/dashboard/overview` | Query：`days`（7 / 30 / 90，默认 7）；需 Bearer |

## 权限

需登录且具备服务端配置的仪表盘权限（如 `dashboard`），否则可能 **403**。
