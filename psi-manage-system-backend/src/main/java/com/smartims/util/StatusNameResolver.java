package com.smartims.util;

/**
 * 状态名称解析工具。
 */
public final class StatusNameResolver {

    private StatusNameResolver() {
    }

    public static String resolveSalesOrderStatusName(String status) {
        return switch (safe(status)) {
            case "pending", "待发货" -> "待发货";
            case "shipped", "已发货" -> "已发货";
            case "completed", "已完成" -> "已完成";
            case "cancelled", "已取消" -> "已取消";
            default -> status;
        };
    }

    public static String resolvePayStatusName(String status) {
        return switch (safe(status)) {
            case "unpaid", "待付款" -> "待付款";
            case "paid", "已付款" -> "已付款";
            case "refunded", "已退款" -> "已退款";
            default -> status;
        };
    }

    public static String resolvePurchaseInboundStatusName(String status) {
        return switch (safe(status)) {
            case "pending", "待入库" -> "待入库";
            case "partial", "部分入库" -> "部分入库";
            case "completed", "已完成" -> "已完成";
            case "cancelled", "已取消" -> "已取消";
            default -> status;
        };
    }

    public static String resolveAftersalesStatusName(String status) {
        return switch (safe(status)) {
            case "pending", "待处理" -> "待处理";
            case "processing", "处理中" -> "处理中";
            case "completed", "已完成" -> "已完成";
            case "closed", "已关闭" -> "已关闭";
            default -> status;
        };
    }

    public static String resolveProductStatusName(String status) {
        return switch (safe(status)) {
            case "on_sale", "onsale", "active", "在售" -> "在售";
            case "off_sale", "offsale", "inactive", "停售" -> "停售";
            default -> status;
        };
    }

    /**
     * 是否停售。空值视为非停售（与新建商品默认「在售」及旧数据兼容）。
     */
    public static boolean isProductOffSale(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        return "停售".equals(resolveProductStatusName(status.trim()));
    }

    public static String resolveInventoryStatusName(String status) {
        return switch (safe(status)) {
            case "normal", "正常" -> "正常";
            case "warning", "偏低" -> "偏低";
            case "critical", "紧急补货" -> "紧急补货";
            default -> status;
        };
    }

    public static String resolveTransferStatusName(String status) {
        return switch (safe(status)) {
            case "transferring", "调拨中" -> "调拨中";
            case "completed", "已完成" -> "已完成";
            case "cancelled", "已取消" -> "已取消";
            default -> status;
        };
    }

    public static String resolveEnableDisableStatusName(Integer status) {
        if (status == null) {
            return null;
        }
        return status == 1 ? "启用" : status == 0 ? "禁用" : String.valueOf(status);
    }

    public static String resolveNormalDisableStatusName(Integer status) {
        if (status == null) {
            return null;
        }
        return status == 1 ? "正常" : status == 0 ? "禁用" : String.valueOf(status);
    }

    public static String resolveSupplierStatusName(Integer status) {
        if (status == null) {
            return null;
        }
        return status == 1 ? "合作中" : status == 0 ? "已停用" : String.valueOf(status);
    }

    public static String resolveWarehouseStatusName(Integer status) {
        if (status == null) {
            return null;
        }
        return status == 1 ? "正常" : status == 0 ? "停用" : String.valueOf(status);
    }

    public static String resolveWarningStatusName(Integer status) {
        if (status == null) {
            return null;
        }
        return status == 0 ? "待处理" : status == 1 ? "已处理" : String.valueOf(status);
    }

    public static String resolveOperationStatusName(Integer status) {
        if (status == null) {
            return null;
        }
        return status == 1 ? "成功" : status == 0 ? "失败" : String.valueOf(status);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
