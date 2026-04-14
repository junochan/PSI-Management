package com.smartims.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存实体类
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Data
@TableName("inventory")
public class Inventory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String sku;

    private Long productId;

    private String productName;

    private String spec;

    private String category;

    private Long warehouseId;

    private String warehouseName;

    private String location;

    private Integer stock;

    private Integer safeStock;

    private BigDecimal costPrice;

    private BigDecimal stockValue;

    private String status;

    private Integer stagnantDays; // 呆滞预警天数（可独立设置，默认取系统配置）

    private LocalDateTime lastInboundTime;

    private LocalDateTime lastOutboundTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

}