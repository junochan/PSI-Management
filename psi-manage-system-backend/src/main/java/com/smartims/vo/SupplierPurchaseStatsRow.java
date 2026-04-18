package com.smartims.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 按供应商聚合的采购统计（列表接口填充用，非独立接口返回体）
 */
@Data
public class SupplierPurchaseStatsRow implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long supplierId;

    private Long purchaseOrderCount;

    private BigDecimal totalPurchaseAmount;
}
