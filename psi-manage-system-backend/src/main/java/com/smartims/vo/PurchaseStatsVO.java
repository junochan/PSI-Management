package com.smartims.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 采购统计VO
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Data
public class PurchaseStatsVO {

    /**
     * 本月采购金额
     */
    private BigDecimal monthAmount;

    /**
     * 待入库订单数量
     */
    private Integer pendingInboundCount;

    /**
     * 合作供应商数量
     */
    private Integer supplierCount;

    /**
     * 待付款订单数量
     */
    private Integer unpaidCount;

    /**
     * 待付款总金额
     */
    private BigDecimal unpaidAmount;

}