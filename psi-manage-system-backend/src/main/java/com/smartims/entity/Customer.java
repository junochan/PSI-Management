package com.smartims.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户实体类
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Data
@TableName("customer")
public class Customer implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String name;

    private String code;

    private String type;

    private String address;

    private String phone;

    private String email;

    private String contact;

    private String vipLevel;

    private BigDecimal creditAmount;

    private Integer totalOrders;

    private BigDecimal totalAmount;

    /** 最近有效销售单下单时间（不含已取消订单），与 total_orders/total_amount 一并维护 */
    private LocalDateTime lastOrderTime;

    private Integer status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

}