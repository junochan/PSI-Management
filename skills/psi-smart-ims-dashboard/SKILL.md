---
name: psi-smart-ims-dashboard
description: >-
  智链进销存仪表盘：通过 psims dashboard overview 拉取聚合经营数据；对应 HTTP GET /dashboard/overview 与前端 dashboardApi。
  当用户看首页 KPI、销售趋势、分类/商品/客户排行、库存预警与呆滞、最近订单或脚本导出仪表盘 JSON 时使用。
---

# 仪表盘（聚合数据）

## 功能与作用

后端 **`DashboardOverviewVO`** 在选定时间窗口内汇总：

- **summary**：本月销售额、本月成交订单数、在售商品种类、客户总数（字段名以后端为准）。
- **salesTrend**：按日销售趋势（区间内每日一条，无单为 0）。
- **categorySalesTop5**：分类销售 Top5（含占比等）。
- **productSalesTop5** / **customerSalesTop5**：商品与客户销售排名。
- **recentOrders**：最近订单列表。
- **inventoryWarningTop10** / **inventoryStagnantTop10**：库存预警与呆滞 Top 列表。

查询参数 **`days`** 仅允许 **7、30、90**（其它值会被后端纠正为默认 **7**）。

对应前端：`dashboardApi.overview(days)`，首页/仪表盘视图（路由由动态导航配置，常见为 `/dashboard`）。

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

终端输出为解包后的 **`data`**（与前端 axios 拦截器一致），便于 `jq` 等工具处理。

## HTTP 对照

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/dashboard/overview` | Query：`days`（7 / 30 / 90，默认 7）；需 Bearer |

## 权限

需登录且具备后端为 `/v1/dashboard/**` 配置的权限（如 `dashboard`），否则可能 **403**；与 Web 端一致。
