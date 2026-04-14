package com.smartims.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 销售订单实体类
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Data
@TableName("sales_order")
public class SalesOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long customerId;

    private String customerName;

    private String customerType;

    private Long productId;

    private String productName;

    private String sku;

    private BigDecimal unitPrice;

    private Integer quantity;

    private Integer shippedQuantity;

    private Integer pendingQuantity;

    private BigDecimal amount;

    private String status;

    private String payStatus;

    private String payMethod;

    private String invoiceType;

    private String invoiceNo;

    private String receiverName;

    private String receiverPhone;

    private String receiverAddress;

    private String logisticsCompany;

    private String logisticsNo;

    private Long warehouseId;

    private String warehouseName;

    private LocalDateTime shipTime;

    private LocalDate expectArriveDate;

    private Long operatorId;

    private String operatorName;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

}