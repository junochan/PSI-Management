package com.smartims.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 售后工单实体类
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Data
@TableName("aftersales_order")
public class AftersalesOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long salesOrderId;

    private String salesOrderNo;

    private Long customerId;

    private String customerName;

    private String type;

    private String content;

    private String expectHandle;

    private String status;

    private Long handlerId;

    private String handlerName;

    private String handleResult;

    private LocalDateTime handleTime;

    private BigDecimal refundAmount;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

}