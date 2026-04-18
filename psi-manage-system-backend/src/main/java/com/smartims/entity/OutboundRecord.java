package com.smartims.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 出库记录实体类
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Data
@TableName("outbound_record")
public class OutboundRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long salesOrderId;

    private String salesOrderNo;

    private Long customerId;

    private String customerName;

    private Long productId;

    private String productName;

    /**
     * 商品主图 URL（与 Product.image 一致）；非表字段，列表接口按 productId 批量补全。
     */
    @TableField(exist = false)
    private String productImage;

    /**
     * 商品分类名称（无图时表格可展示分类图标）；非表字段。
     */
    @TableField(exist = false)
    private String category;

    private String sku;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal amount;

    private Long warehouseId;

    private String warehouseName;

    private Long operatorId;

    private String operatorName;

    private String remark;

    private String outboundType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;

}