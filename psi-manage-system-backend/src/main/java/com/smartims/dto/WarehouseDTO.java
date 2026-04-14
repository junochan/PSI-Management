package com.smartims.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.io.Serializable;

/**
 * 仓库DTO
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Data
public class WarehouseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "仓库名称不能为空")
    private String name;

    private String code;

    private String address;

    private String managerName;

    private String managerPhone;

    private Integer capacity;

    private BigDecimal capacityUsed;

    private String remark;

}