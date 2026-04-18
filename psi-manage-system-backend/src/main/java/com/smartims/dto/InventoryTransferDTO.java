package com.smartims.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 调拨DTO
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Data
public class InventoryTransferDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "源仓库ID不能为空")
    private Long fromWarehouseId;

    @NotNull(message = "目标仓库ID不能为空")
    private Long toWarehouseId;

    @NotNull(message = "调拨数量不能为空")
    @Min(value = 1, message = "调拨数量最小值为1")
    private Integer quantity;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;

}