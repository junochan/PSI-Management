package com.smartims.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.smartims.util.StatusNameResolver;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志实体类
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Data
@TableName("sys_operation_log")
public class SysOperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String userName;

    private String roleName;

    private String action;

    private String content;

    private String module;

    private String ip;

    private Integer status;

    private String errorMsg;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;

    public String getStatusName() {
        return StatusNameResolver.resolveOperationStatusName(status);
    }

}