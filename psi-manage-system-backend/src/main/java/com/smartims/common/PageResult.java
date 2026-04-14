package com.smartims.common;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 分页响应结果类
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页码
     */
    private Long page;

    /**
     * 每页数量
     */
    private Long size;

    /**
     * 数据列表
     */
    private List<T> list;

    /**
     * 可选汇总（如订单金额汇总），按接口约定填充
     */
    private Map<String, Object> summary;

    public PageResult() {
    }

    public PageResult(Long total, Long page, Long size, List<T> list) {
        this.total = total;
        this.page = page;
        this.size = size;
        this.list = list;
    }

    /**
     * 构建分页结果
     */
    public static <T> PageResult<T> build(Long total, Long page, Long size, List<T> list) {
        return new PageResult<>(total, page, size, list);
    }

    public static <T> PageResult<T> build(Long total, Long page, Long size, List<T> list, Map<String, Object> summary) {
        PageResult<T> r = new PageResult<>(total, page, size, list);
        r.setSummary(summary);
        return r;
    }

}