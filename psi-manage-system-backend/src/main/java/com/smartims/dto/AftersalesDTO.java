package com.smartims.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 售后工单DTO
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Data
public class AftersalesDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "销售订单ID不能为空")
    private Long salesOrderId;

    @NotBlank(message = "售后类型不能为空")
    private String type;

    @NotBlank(message = "售后内容不能为空")
    private String content;

    private String expectHandle;

    private BigDecimal refundAmount;

    private String remark;

}