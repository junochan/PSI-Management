package com.smartims.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 仓库实体类
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Data
@TableName("warehouse")
public class Warehouse implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String name;

    private String code;

    private String address;

    private Long managerId;

    private String managerName;

    private Integer totalCategories;

    private Integer totalStock;

    private BigDecimal totalValue;

    private Integer capacity;

    private BigDecimal capacityUsed;

    private Integer status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

}