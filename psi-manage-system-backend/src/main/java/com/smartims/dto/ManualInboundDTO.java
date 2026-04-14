package com.smartims.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 手动入库DTO
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Data
public class ManualInboundDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 商品ID
     */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /**
     * 仓库ID
     */
    @NotNull(message = "仓库ID不能为空")
    private Long warehouseId;

    /**
     * 入库数量
     */
    @NotNull(message = "入库数量不能为空")
    private Integer quantity;

    /**
     * 备注
     */
    private String remark;

}