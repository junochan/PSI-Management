package com.smartims.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.smartims.util.StatusNameResolver;
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

    /**
     * 非表字段：列表/仪表盘等场景由服务根据商品回填首图（与 product.image 一致）
     */
    @TableField(exist = false)
    private String productImage;

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

    /**
     * 确认付款时间
     */
    private LocalDateTime payTime;

    /**
     * 确认收货/订单完成时间
     */
    private LocalDateTime completeTime;

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

    public String getStatusName() {
        return StatusNameResolver.resolveSalesOrderStatusName(status);
    }

    public String getPayStatusName() {
        return StatusNameResolver.resolvePayStatusName(payStatus);
    }

}