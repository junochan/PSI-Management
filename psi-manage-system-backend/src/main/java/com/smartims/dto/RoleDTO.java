package com.smartims.dto;

import lombok.Data;

/**
 * 角色数据传输对象
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Data
public class RoleDTO {

    /**
     * 角色名称
     */
    private String name;

    /**
     * 角色编码
     */
    private String code;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 状态
     */
    private Integer status;

}