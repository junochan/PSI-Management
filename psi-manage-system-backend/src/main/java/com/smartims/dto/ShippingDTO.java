package com.smartims.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @NotBlank(message = "物流公司不能为空")
    @Size(max = 50, message = "物流公司长度不能超过50")
    private String logisticsCompany;

    @NotBlank(message = "物流单号不能为空")
    @Size(max = 50, message = "物流单号长度不能超过50")
    private String logisticsNo;

    /** 收货人（发货时落库到销售单，供详情展示） */
    @NotBlank(message = "收货人姓名不能为空")
    @Size(max = 50, message = "收货人姓名长度不能超过50")
    private String receiverName;

    @NotBlank(message = "收货人电话不能为空")
    @Size(max = 100, message = "收货人电话长度不能超过100")
    private String receiverPhone;

    @NotBlank(message = "收货地址不能为空")
    @Size(max = 200, message = "收货地址长度不能超过200")
    private String receiverAddress;

    private String remark;

}