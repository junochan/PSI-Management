package com.smartims.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 供应商DTO
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Data
public class SupplierDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "供应商名称不能为空")
    private String name;

    private String code;

    /** 所属行业 ID 列表（多选），对应 supplier_industry.id */
    private List<Long> industryIds;

    private String contactPerson;

    private String contactPhone;

    private String email;

    private String address;

    private String bankName;

    private String bankAccount;

    private String taxNo;

    private String remark;

}