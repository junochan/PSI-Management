package com.smartims.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 采购订单DTO
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Data
public class PurchaseOrderDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "供应商ID不能为空")
    private Long supplierId;

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "采购数量不能为空")
    @Min(value = 1, message = "采购数量最小值为1")
    private Integer quantity;

    @NotNull(message = "单价不能为空")
    private BigDecimal unitPrice;

    private LocalDate expectDate;

    private Long warehouseId;

    private String payMethod;

    private String remark;

}