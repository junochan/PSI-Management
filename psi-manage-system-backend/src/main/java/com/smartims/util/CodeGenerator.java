package com.smartims.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 编码生成工具类
 * 统一规范系统中各类编码的生成规则
 *
 * 编码规则：
 * 1. 商品编码：PROD + yyMMdd + 4位序号，如 PROD2404120001
 * 2. 采购单号：PO + yyMMdd + 4位序号，如 PO2404120001
 * 3. 销售订单号：SO + yyMMdd + 4位序号，如 SO2404120001
 * 4. 入库单号：IN + yyMMdd + 4位序号，如 IN2404120001
 * 5. 出库单号：OUT + yyMMdd + 4位序号，如 OUT2404120001
 * 6. 调拨单号：TR + yyMMdd + 4位序号，如 TR2404120001
 * 7. 售后单号：AS + yyMMdd + 4位序号，如 AS2404120001
 * 8. 库存SKU：SKU + 商品ID(6位) + 仓库ID(3位)，如 SKU000001001
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
public class CodeGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");

    /**
     * 当前序号计数器（用于单号生成）
     * 实际生产环境应使用数据库序列或分布式ID生成器
     */
    private static int sequence = 0;

    /**
     * 商品编码前缀
     */
    public static final String PRODUCT_PREFIX = "PROD";

    /**
     * 采购单号前缀
     */
    public static final String PURCHASE_PREFIX = "PO";

    /**
     * 销售订单号前缀
     */
    public static final String SALES_PREFIX = "SO";

    /**
     * 入库单号前缀
     */
    public static final String INBOUND_PREFIX = "IN";

    /**
     * 出库单号前缀
     */
    public static final String OUTBOUND_PREFIX = "OUT";

    /**
     * 调拨单号前缀
     */
    public static final String TRANSFER_PREFIX = "TR";

    /**
     * 售后单号前缀
     */
    public static final String AFTERSALES_PREFIX = "AS";

    /**
     * 库存SKU前缀
     */
    public static final String SKU_PREFIX = "SKU";

    /**
     * 商品分类编码前缀
     */
    public static final String CATEGORY_PREFIX = "CAT";

    /**
     * 生成商品编码
     * 格式：PROD + yyMMdd + 4位序号
     * 示例：PROD2404120001
     *
     * @return 商品编码
     */
    public static String generateProductCode() {
        return generateCode(PRODUCT_PREFIX);
    }

    /**
     * 生成采购单号
     * 格式：PO + yyMMdd + 4位序号
     * 示例：PO2404120001
     *
     * @return 采购单号
     */
    public static String generatePurchaseOrderNo() {
        return generateCode(PURCHASE_PREFIX);
    }

    /**
     * 生成销售订单号
     * 格式：SO + yyMMdd + 4位序号
     * 示例：SO2404120001
     *
     * @return 销售订单号
     */
    public static String generateSalesOrderNo() {
        return generateCode(SALES_PREFIX);
    }

    /**
     * 生成入库单号
     * 格式：IN + yyMMdd + 4位序号
     * 示例：IN2404120001
     *
     * @return 入库单号
     */
    public static String generateInboundOrderNo() {
        return generateCode(INBOUND_PREFIX);
    }

    /**
     * 生成出库单号
     * 格式：OUT + yyMMdd + 4位序号
     * 示例：OUT2404120001
     *
     * @return 出库单号
     */
    public static String generateOutboundOrderNo() {
        return generateCode(OUTBOUND_PREFIX);
    }

    /**
     * 生成调拨单号
     * 格式：TR + yyMMdd + 4位序号
     * 示例：TR2404120001
     *
     * @return 调拨单号
     */
    public static String generateTransferOrderNo() {
        return generateCode(TRANSFER_PREFIX);
    }

    /**
     * 生成售后单号
     * 格式：AS + yyMMdd + 4位序号
     * 示例：AS2404120001
     *
     * @return 售后单号
     */
    public static String generateAftersalesOrderNo() {
        return generateCode(AFTERSALES_PREFIX);
    }

    /**
     * 生成库存SKU
     * 格式：SKU + 商品ID(6位) + 仓库ID(3位)
     * 示例：SKU000001001
     *
     * @param productId 商品ID
     * @param warehouseId 仓库ID
     * @return SKU编码
     */
    public static String generateSku(Long productId, Long warehouseId) {
        return SKU_PREFIX + String.format("%06d", productId) + String.format("%03d", warehouseId);
    }

    /**
     * 生成分类编码
     * 格式：CAT + yyMMdd + 4位序号
     * 示例：CAT2604140001
     *
     * @return 分类编码
     */
    public static String generateCategoryCode() {
        return generateCode(CATEGORY_PREFIX);
    }

    /**
     * 通用编码生成方法
     * 格式：前缀 + yyMMdd + 4位序号
     *
     * @param prefix 编码前缀
     * @return 编码
     */
    private static synchronized String generateCode(String prefix) {
        String dateStr = LocalDate.now().format(DATE_FORMATTER);
        sequence = (sequence + 1) % 10000;
        if (sequence == 0) {
            sequence = 1;
        }
        return prefix + dateStr + String.format("%04d", sequence);
    }

    /**
     * 获取编码说明
     *
     * @return 编码规则说明
     */
    public static String getCodeDescription() {
        return "编码规则说明:\n" +
                "1. 商品编码：PROD + yyMMdd + 4位序号 (例: PROD2404120001)\n" +
                "2. 采购单号：PO + yyMMdd + 4位序号 (例: PO2404120001)\n" +
                "3. 销售订单号：SO + yyMMdd + 4位序号 (例: SO2404120001)\n" +
                "4. 入库单号：IN + yyMMdd + 4位序号 (例: IN2404120001)\n" +
                "5. 出库单号：OUT + yyMMdd + 4位序号 (例: OUT2404120001)\n" +
                "6. 调拨单号：TR + yyMMdd + 4位序号 (例: TR2404120001)\n" +
                "7. 售后单号：AS + yyMMdd + 4位序号 (例: AS2404120001)\n" +
                "8. 库存SKU：SKU + 商品ID(6位) + 仓库ID(3位) (例: SKU000001001)";
    }
}