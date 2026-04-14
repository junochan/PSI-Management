package com.smartims.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 发货DTO
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Data
public class ShippingDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "发货仓库不能为空")
    private Long warehouseId;

    @NotNull(message = "发货数量不能为空")
    private Integer quantity;

    private String logisticsCompany;

    private String logisticsNo;

    private String remark;

}