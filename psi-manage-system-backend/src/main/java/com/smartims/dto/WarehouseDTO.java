package com.smartims.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    @Size(max = 100, message = "仓库名称长度不能超过100个字符")
    private String name;

    @Size(max = 20, message = "仓库编码长度不能超过20个字符")
    private String code;

    @Size(max = 200, message = "仓库地址长度不能超过200个字符")
    private String address;

    @Size(max = 50, message = "负责人姓名长度不能超过50个字符")
    private String managerName;

    private String managerPhone;

    private Integer capacity;

    private BigDecimal capacityUsed;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

}