package com.smartims.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

/**
 * 分页查询请求参数
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Data
public class PageQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页码
     */
    @Min(value = 1, message = "页码最小值为1")
    private Long page = 1L;

    /**
     * 每页数量
     */
    @Min(value = 1, message = "每页数量最小值为1")
    @Max(value = 100, message = "每页数量最大值为100")
    private Long size = 10L;

    /**
     * 排序字段
     */
    private String sort;

    /**
     * 排序方式（asc/desc）
     */
    private String order = "desc";

    /**
     * 搜索关键词
     */
    private String keyword;

    // ----- 可选筛选（各接口按需使用，未使用的字段忽略） -----

    private Long productId;
    private Long warehouseId;
    private Long customerId;
    private Long supplierId;

    /** 商品分类名称（商品列表） */
    private String categoryName;

    /** 商品状态：在售/停售 */
    private String productStatus;

    /** 库存呆滞：stagnant / normal */
    private String stagnantStatus;

    /** 采购入库状态 */
    private String inboundStatus;

    /** 付款状态 */
    private String payStatus;

    /** 销售订单状态：pending / shipped / completed / cancelled */
    private String salesOrderStatus;

    /** 售后工单状态（与前端展示文案一致） */
    private String aftersalesStatus;

    /** yyyy-MM-dd — 库存最后出库时间范围 */
    private String lastOutboundStart;
    private String lastOutboundEnd;
    /** yyyy-MM-dd — 库存最后入库时间范围 */
    private String lastInboundStart;
    private String lastInboundEnd;

    /** yyyy-MM-dd — 采购期望交货日期 */
    private String expectDateStart;
    private String expectDateEnd;

    /** yyyy-MM-dd — 销售下单时间 */
    private String createTimeStart;
    private String createTimeEnd;

    /** 入库记录：操作人精确匹配 */
    private String operatorName;

    /**
     * 获取起始位置
     */
    public long getOffset() {
        return (page - 1) * size;
    }

}