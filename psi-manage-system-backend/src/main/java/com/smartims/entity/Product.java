package com.smartims.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.smartims.util.StatusNameResolver;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品实体类
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Data
@TableName("product")
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 商品主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 商品编码
     */
    private String code;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 品牌
     */
    private String brand;

    /**
     * 规格
     */
    private String spec;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 成本价
     */
    private java.math.BigDecimal costPrice;

    /**
     * 销售价
     */
    private java.math.BigDecimal salePrice;

    /**
     * 状态：在售/停售
     */
    private String status;

    /**
     * 商品图片：单张 URL，或多张 JSON 数组 / 逗号分隔 URL（至多 10 张，与 ProductImageLoader 一致）
     */
    private String image;

    /**
     * 商品描述
     */
    private String description;

    /**
     * 库存数量
     */
    private Integer stock;

    /**
     * 安全库存预警值
     */
    private Integer safeStock;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    private Integer deleted;

    public String getStatusName() {
        return StatusNameResolver.resolveProductStatusName(status);
    }

}