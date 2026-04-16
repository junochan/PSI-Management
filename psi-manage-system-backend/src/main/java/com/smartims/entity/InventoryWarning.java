package com.smartims.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.smartims.util.StatusNameResolver;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 库存预警实体类
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Data
@TableName("inventory_warning")
public class InventoryWarning implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String type;

    private String sku;

    private String productName;

    private String content;

    private Long warehouseId;

    private String warehouseName;

    private Integer currentStock;

    private Integer safeStock;

    private LocalDateTime expireDate;

    private Integer stockAge;

    private Integer status;

    private LocalDateTime handleTime;

    private Long handlerId;

    private String handlerName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;

    public String getStatusName() {
        return StatusNameResolver.resolveWarningStatusName(status);
    }

}