package com.smartims.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.smartims.util.StatusNameResolver;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 调拨记录实体类
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Data
@TableName("inventory_transfer")
public class InventoryTransfer implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long productId;

    private String productName;

    /**
     * 商品主图 URL（多图时与 Product.image 一致）；非表字段，列表接口按 productId 批量补全。
     */
    @TableField(exist = false)
    private String productImage;

    /**
     * 商品分类名称（用于前端无图时展示分类图标）；非表字段。
     */
    @TableField(exist = false)
    private String category;

    private String sku;

    private Long fromWarehouseId;

    private String fromWarehouseName;

    private Long toWarehouseId;

    private String toWarehouseName;

    private Integer quantity;

    private String status;

    private Long operatorId;

    private String operatorName;

    private String remark;

    private LocalDateTime completeTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    public String getStatusName() {
        return StatusNameResolver.resolveTransferStatusName(status);
    }

}