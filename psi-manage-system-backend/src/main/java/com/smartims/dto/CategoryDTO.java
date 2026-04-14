package com.smartims.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 商品分类DTO
 *
 * @author Smart IMS Team
 * @since 2026-04-13
 */
@Data
public class CategoryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 分类编码
     */
    private String code;

    /**
     * 父分类ID
     */
    private Long parentId;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态
     */
    private Integer status;

}