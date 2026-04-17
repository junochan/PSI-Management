package com.smartims.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 售后处理DTO
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Data
public class AftersalesHandleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "处理结果不能为空")
    @Size(max = 500, message = "处理结果不能超过500个字符")
    private String handleResult;

    private BigDecimal refundAmount;

    @Size(max = 500, message = "处理备注不能超过500个字符")
    private String remark;

}