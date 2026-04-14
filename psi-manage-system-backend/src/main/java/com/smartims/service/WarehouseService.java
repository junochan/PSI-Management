package com.smartims.service;

import com.smartims.common.PageResult;
import com.smartims.dto.PageQuery;
import com.smartims.dto.WarehouseDTO;
import com.smartims.entity.Warehouse;

import java.util.List;

/**
 * 仓库服务接口
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
public interface WarehouseService {

    /**
     * 分页查询仓库列表
     */
    PageResult<Warehouse> getWarehouseList(PageQuery pageQuery);

    /**
     * 根据ID查询仓库详情
     */
    Warehouse getWarehouseById(Long id);

    /**
     * 创建仓库
     */
    void createWarehouse(WarehouseDTO dto);

    /**
     * 更新仓库
     */
    void updateWarehouse(Long id, WarehouseDTO dto);

    /**
     * 删除仓库
     */
    void deleteWarehouse(Long id);

    /**
     * 批量删除仓库
     */
    void batchDeleteWarehouse(List<Long> ids);

    /**
     * 获取所有仓库列表（不分页）
     */
    List<Warehouse> getAllWarehouses();

    /**
     * 下拉筛选用：仅 id、编码、名称（无统计字段，供仅有「库存查看」等账号拉取）
     */
    List<Warehouse> listOptions();

}