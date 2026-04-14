package com.smartims.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 入库DTO
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Data
public class InboundDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "入库仓库不能为空")
    private Long warehouseId;

    @NotNull(message = "入库数量不能为空")
    @Min(value = 1, message = "入库数量最小值为1")
    private Integer quantity;

    private String batchNo;

    private String remark;

}