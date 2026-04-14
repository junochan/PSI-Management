package com.smartims.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
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

    /** 创建时在服务层校验非空 */
    private String name;

    private String code;

    private String type;

    /** 前端常用字段名 contact */
    @JsonAlias({ "contact" })
    private String contactPerson;

    /** 前端常用字段名 phone */
    @JsonAlias({ "phone" })
    private String contactPhone;

    private String email;

    private String address;

    private String vipLevel;

    private String remark;

}