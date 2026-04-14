package com.smartims.common;

import lombok.Getter;

/**
 * 响应状态码枚举
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Getter
public enum ResultCode {

    /**
     * 成功
     */
    SUCCESS(200, "操作成功"),

    /**
     * 失败
     */
    FAIL(400, "操作失败"),

    /**
     * 参数错误
     */
    PARAM_ERROR(400, "请求参数错误"),

    /**
     * 未登录
     */
    UNAUTHORIZED(401, "未登录或登录已过期"),

    /**
     * 无权限
     */
    FORBIDDEN(403, "无权限访问"),

    /**
     * 资源不存在
     */
    NOT_FOUND(404, "资源不存在"),

    /**
     * 请求方法不支持
     */
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),

    /**
     * 服务器内部错误
     */
    INTERNAL_ERROR(500, "服务器内部错误"),

    /**
     * 数据已存在
     */
    DATA_EXIST(500, "数据已存在"),

    /**
     * 数据不存在
     */
    DATA_NOT_EXIST(500, "数据不存在"),

    /**
     * 库存不足
     */
    STOCK_NOT_ENOUGH(500, "库存不足"),

    /**
     * 订单状态不允许操作
     */
    ORDER_STATUS_ERROR(500, "订单状态不允许此操作");

    /**
     * 状态码
     */
    private final Integer code;

    /**
     * 提示信息
     */
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

}