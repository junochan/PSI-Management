package com.smartims.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.smartims.util.StatusNameResolver;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 供应商所属行业字典表
 */
@Data
@TableName("supplier_industry")
public class SupplierIndustry implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 行业名称 */
    private String name;

    /** 编码（可选） */
    private String code;

    /** 排序，越小越靠前 */
    private Integer sort;

    /** 状态：1-启用，0-停用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    public String getStatusName() {
        return StatusNameResolver.resolveEnableDisableStatusName(status);
    }

}
