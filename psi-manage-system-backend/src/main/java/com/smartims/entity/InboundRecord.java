package com.smartims.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 入库记录实体类
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Data
@TableName("inbound_record")
public class InboundRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long purchaseOrderId;

    private String purchaseOrderNo;

    private Long supplierId;

    private String supplierName;

    private Long productId;

    private String productName;

    private String sku;

    /**
     * 商品图片（聚合字段，非表字段）
     */
    @TableField(exist = false)
    private String productImage;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal amount;

    private Long warehouseId;

    private String warehouseName;

    private String batchNo;

    private LocalDate expireDate;

    private Long operatorId;

    private String operatorName;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;

}