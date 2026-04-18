package com.smartims.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Size;
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

    /** 创建时在服务层校验非空；与 customer.name varchar(100) 一致 */
    @Size(max = 100, message = "客户名称长度不能超过100")
    private String name;

    @Size(max = 20, message = "客户编码长度不能超过20")
    private String code;

    @Size(max = 20, message = "客户类型长度不能超过20")
    private String type;

    /** 前端常用字段名 contact；与 customer.contact varchar(50) 一致 */
    @JsonAlias({ "contact" })
    @Size(max = 50, message = "联系人长度不能超过50")
    private String contactPerson;

    /** 前端常用字段名 phone；与 customer.phone varchar(20) 一致 */
    @JsonAlias({ "phone" })
    @Size(max = 20, message = "联系电话长度不能超过20")
    private String contactPhone;

    @Size(max = 100, message = "邮箱长度不能超过100")
    private String email;

    @Size(max = 200, message = "地址长度不能超过200")
    private String address;

    @Size(max = 20, message = "VIP等级长度不能超过20")
    private String vipLevel;

    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;

}