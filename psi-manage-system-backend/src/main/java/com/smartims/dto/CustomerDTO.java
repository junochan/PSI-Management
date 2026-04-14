package com.smartims.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 客户DTO
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Data
public class CustomerDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "客户名称不能为空")
    private String name;

    private String code;

    private String type;

    private String contactPerson;

    private String contactPhone;

    private String email;

    private String address;

    private String vipLevel;

    private String remark;

}