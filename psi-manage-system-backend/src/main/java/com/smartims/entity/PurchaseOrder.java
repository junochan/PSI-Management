package com.smartims.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.smartims.util.StatusNameResolver;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 采购订单实体类
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Data
@TableName("purchase_order")
public class PurchaseOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long supplierId;

    private String supplierName;

    private Long productId;

    private String productName;

    private String sku;

    private BigDecimal unitPrice;

    private Integer totalQuantity;

    private Integer pendingQuantity;

    private Integer inboundQuantity;

    private BigDecimal amount;

    private String inboundStatus;

    private String payStatus;

    private String payMethod;

    private LocalDate expectDate;

    private Long warehouseId;

    private String warehouseName;

    private Long operatorId;

    private String operatorName;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    public String getInboundStatusName() {
        return StatusNameResolver.resolvePurchaseInboundStatusName(inboundStatus);
    }

    public String getPayStatusName() {
        return StatusNameResolver.resolvePayStatusName(payStatus);
    }

}