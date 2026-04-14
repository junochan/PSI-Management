package com.smartims.service;

import com.smartims.common.PageResult;
import com.smartims.dto.InventoryImageSearchRequest;
import com.smartims.dto.InventoryTransferDTO;
import com.smartims.dto.ManualInboundDTO;
import com.smartims.dto.PageQuery;
import com.smartims.entity.Inventory;
import com.smartims.entity.InventoryTransfer;
import com.smartims.entity.InventoryWarning;
import com.smartims.entity.OutboundRecord;
import com.smartims.vo.InventoryStatsVO;

import java.util.List;

/**
 * 库存服务接口
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
public interface InventoryService {

    /**
     * 分页查询库存列表
     */
    PageResult<Inventory> getInventoryList(PageQuery pageQuery);

    /**
     * 以图搜图：在筛选条件内按商品主图与查询图的向量相似度排序并分页
     */
    PageResult<Inventory> searchByImage(InventoryImageSearchRequest request);

    /**
     * 根据ID查询库存详情
     */
    Inventory getInventoryById(Long id);

    /**
     * 查询商品在各仓库的库存
     */
    List<Inventory> getInventoryByProductId(Long productId);

    /**
     * 创建调拨单
     */
    void createTransfer(InventoryTransferDTO dto);

    /**
     * 确认调拨
     */
    void confirmTransfer(Long transferId);

    /**
     * 分页查询调拨记录
     */
    PageResult<InventoryTransfer> getTransferList(PageQuery pageQuery);

    /**
     * 根据ID查询调拨详情
     */
    InventoryTransfer getTransferById(Long id);

    /**
     * 分页查询库存预警列表
     */
    PageResult<InventoryWarning> getWarningList(PageQuery pageQuery);

    /**
     * 处理库存预警
     */
    void handleWarning(Long warningId, String handleRemark);

    /**
     * 获取库存统计数据
     */
    InventoryStatsVO getInventoryStats();

    /**
     * 手动入库
     */
    void manualInbound(Long inventoryId, Integer quantity, String remark);

    /**
     * 手动出库
     */
    void manualOutbound(Long inventoryId, Integer quantity, String remark);

    /**
     * 手动入库（初始化商品库存）
     */
    void manualInboundNew(ManualInboundDTO dto);

    /**
     * 更新库存预警值
     */
    void updateSafeStock(Long inventoryId, Integer safeStock);

    /**
     * 更新库位
     */
    void updateLocation(Long inventoryId, String location);

    /**
     * 分页查询出库记录列表
     */
    PageResult<OutboundRecord> getOutboundRecordList(PageQuery pageQuery);

    /**
     * 更新呆滞预警天数
     */
    void updateStagnantDays(Long inventoryId, Integer stagnantDays);

}