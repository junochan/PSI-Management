package com.smartims.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品DTO
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Data
public class ProductDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 商品编码（系统自动生成，无需填写）
     */
    private String code;

    @NotBlank(message = "商品名称不能为空")
    private String name;

    private String brand;

    private String spec;

    private Long categoryId;

    private String categoryName;

    @NotNull(message = "成本价不能为空")
    private BigDecimal costPrice;

    @NotNull(message = "销售价不能为空")
    private BigDecimal salePrice;

    private String status;

    private String image;

    private String description;

    /**
     * 安全库存预警值
     */
    private Integer safeStock;

    /**
     * 初始库存（仅新建商品时使用）
     */
    private Integer initialStock;

    /**
     * 入库仓库ID（仅新建商品时使用）
     */
    private Long warehouseId;

}