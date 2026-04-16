package com.smartims.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 系统配置DTO
 */
@Data
public class SystemConfigDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String companyName;

    private String phone;

    private String address;

    private String stockWarning;

    private Integer staleDays;
}
