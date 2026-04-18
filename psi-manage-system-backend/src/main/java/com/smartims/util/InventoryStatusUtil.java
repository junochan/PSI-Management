package com.smartims.util;

import com.smartims.entity.Inventory;

/**
 * 库存状态计算工具。
 */
public final class InventoryStatusUtil {

    private static final int DEFAULT_SAFE_STOCK = 10;

    private InventoryStatusUtil() {
    }

    public static String resolveStatus(Integer stock, Integer safeStock) {
        int currentStock = stock != null ? stock : 0;
        int threshold = safeStock != null ? safeStock : DEFAULT_SAFE_STOCK;

        if (currentStock <= 0 || currentStock < threshold / 2) {
            return "critical";
        }
        if (currentStock < threshold) {
            return "warning";
        }
        return "normal";
    }

    public static void applyStatus(Inventory inventory) {
        if (inventory == null) {
            return;
        }
        inventory.setStatus(resolveStatus(inventory.getStock(), inventory.getSafeStock()));
    }
}
