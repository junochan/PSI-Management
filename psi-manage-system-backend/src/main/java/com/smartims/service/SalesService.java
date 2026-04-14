package com.smartims.service;

import com.smartims.common.PageResult;
import com.smartims.dto.PageQuery;
import com.smartims.dto.SalesOrderDTO;
import com.smartims.dto.ShippingDTO;
import com.smartims.entity.SalesOrder;
import com.smartims.vo.SalesStatsVO;

/**
 * 销售服务接口
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
public interface SalesService {

    /**
     * 分页查询销售订单列表
     */
    PageResult<SalesOrder> getSalesOrderList(PageQuery pageQuery);

    /**
     * 根据ID查询销售订单详情
     */
    SalesOrder getSalesOrderById(Long id);

    /**
     * 创建销售订单
     */
    void createSalesOrder(SalesOrderDTO dto);

    /**
     * 更新销售订单
     */
    void updateSalesOrder(Long id, SalesOrderDTO dto);

    /**
     * 确认付款
     */
    void confirmPayment(Long orderId);

    /**
     * 确认发货
     */
    void confirmShipping(Long orderId, ShippingDTO dto);

    /**
     * 确认收货
     */
    void confirmReceived(Long orderId);

    /**
     * 取消订单
     */
    void cancelOrder(Long orderId);

    /**
     * 获取销售统计数据
     */
    SalesStatsVO getSalesStats();

}