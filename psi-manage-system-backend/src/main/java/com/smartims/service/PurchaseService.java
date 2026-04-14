package com.smartims.service;

import com.smartims.common.PageResult;
import com.smartims.dto.InboundDTO;
import com.smartims.dto.PageQuery;
import com.smartims.dto.PurchaseOrderDTO;
import com.smartims.entity.InboundRecord;
import com.smartims.entity.PurchaseOrder;
import com.smartims.vo.PurchaseStatsVO;

import java.util.List;

/**
 * 采购服务接口
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
public interface PurchaseService {

    /**
     * 分页查询采购订单列表
     */
    PageResult<PurchaseOrder> getPurchaseOrderList(PageQuery pageQuery);

    /**
     * 根据ID查询采购订单详情
     */
    PurchaseOrder getPurchaseOrderById(Long id);

    /**
     * 创建采购订单
     */
    void createPurchaseOrder(PurchaseOrderDTO dto);

    /**
     * 更新采购订单
     */
    void updatePurchaseOrder(Long id, PurchaseOrderDTO dto);

    /**
     * 确认入库
     */
    void confirmInbound(Long orderId, InboundDTO dto);

    /**
     * 分页查询入库记录列表
     */
    PageResult<InboundRecord> getInboundRecordList(PageQuery pageQuery);

    /**
     * 根据ID查询入库记录详情
     */
    InboundRecord getInboundRecordById(Long id);

    /**
     * 取消采购订单
     */
    void cancelPurchaseOrder(Long id);

    /**
     * 删除采购订单
     */
    void deletePurchaseOrder(Long id);

    /**
     * 获取采购统计数据
     */
    PurchaseStatsVO getPurchaseStats();

}