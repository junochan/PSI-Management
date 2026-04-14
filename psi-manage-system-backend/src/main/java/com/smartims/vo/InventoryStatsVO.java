package com.smartims.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 库存统计数据VO
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Data
public class InventoryStatsVO {

    /**
     * 商品种类数量
     */
    private Integer categoryCount;

    /**
     * 上月商品种类数量
     */
    private Integer lastMonthCategoryCount;

    /**
     * 商品种类变化（同比）
     */
    private Integer categoryChange;

    /**
     * 库存总额
     */
    private BigDecimal totalValue;

    /**
     * 上月库存总额
     */
    private BigDecimal lastMonthTotalValue;

    /**
     * 库存总额变化百分比
     */
    private BigDecimal valueChangePercent;

    /**
     * 库存预警数量
     */
    private Integer warningCount;

    /**
     * 调拨中数量
     */
    private Integer transferringCount;

    /**
     * 上月调拨中数量
     */
    private Integer lastMonthTransferringCount;

    /**
     * 调拨中变化（同比）
     */
    private Integer transferringChange;

}