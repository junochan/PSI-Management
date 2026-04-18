package com.smartims.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.smartims.util.StatusNameResolver;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 供应商实体类
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Data
@TableName("supplier")
public class Supplier implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String name;

    private String code;

    private String address;

    private String phone;

    private String email;

    private String contact;

    /** 所属行业 ID 列表（非表字段，来自 supplier_industry_link） */
    @TableField(exist = false)
    private List<Long> industryIds;

    /** 行业名称展示（顿号拼接，非表字段） */
    @TableField(exist = false)
    private String industryNames;

    /** 合作采购订单数（非表字段，来自 purchase_order 聚合） */
    @TableField(exist = false)
    private Long purchaseOrderCount;

    /** 总采购额（非表字段，来自 purchase_order 聚合） */
    @TableField(exist = false)
    private BigDecimal totalPurchaseAmount;

    private Integer deliveryDays;

    private String bankName;

    private String bankAccount;

    private Integer status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    public String getStatusName() {
        return StatusNameResolver.resolveSupplierStatusName(status);
    }

}