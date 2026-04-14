package com.smartims.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 销售统计VO
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Data
public class SalesStatsVO {

    /**
     * 本月销售额
     */
    private BigDecimal monthAmount;

    /**
     * 上月销售额
     */
    private BigDecimal lastMonthAmount;

    /**
     * 销售额同比变化百分比
     */
    private BigDecimal amountChangePercent;

    /**
     * 本月成交订单数量
     */
    private Integer monthOrderCount;

    /**
     * 上月成交订单数量
     */
    private Integer lastMonthOrderCount;

    /**
     * 订单同比变化百分比
     */
    private BigDecimal orderChangePercent;

    /**
     * 本月活跃客户数量
     */
    private Integer activeCustomerCount;

    /**
     * 上月活跃客户数量
     */
    private Integer lastMonthActiveCustomerCount;

    /**
     * 客户同比变化
     */
    private Integer customerChange;

    /**
     * 待处理售后数量
     */
    private Integer pendingAftersalesCount;

}