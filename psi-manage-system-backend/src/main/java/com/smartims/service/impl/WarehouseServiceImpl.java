package com.smartims.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartims.common.PageResult;
import com.smartims.dto.PageQuery;
import com.smartims.dto.WarehouseDTO;
import com.smartims.entity.Inventory;
import com.smartims.entity.Warehouse;
import com.smartims.exception.BusinessException;
import com.smartims.mapper.InventoryMapper;
import com.smartims.mapper.WarehouseMapper;
import com.smartims.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * 仓库服务实现类
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseMapper warehouseMapper;
    private final InventoryMapper inventoryMapper;

    @Override
    public PageResult<Warehouse> getWarehouseList(PageQuery pageQuery) {
        LambdaQueryWrapper<Warehouse> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Warehouse::getDeleted, 0);

        if (StringUtils.hasText(pageQuery.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper
                    .like(Warehouse::getName, pageQuery.getKeyword())
                    .or()
                    .like(Warehouse::getCode, pageQuery.getKeyword())
                    .or()
                    .like(Warehouse::getManagerName, pageQuery.getKeyword())
            );
        }

        if (StringUtils.hasText(pageQuery.getSort())) {
            if ("asc".equalsIgnoreCase(pageQuery.getOrder())) {
                queryWrapper.orderByAsc(Warehouse::getCreateTime);
            } else {
                queryWrapper.orderByDesc(Warehouse::getCreateTime);
            }
        } else {
            queryWrapper.orderByDesc(Warehouse::getCreateTime);
        }

        Page<Warehouse> page = new Page<>(pageQuery.getPage(), pageQuery.getSize());
        Page<Warehouse> result = warehouseMapper.selectPage(page, queryWrapper);

        // 动态计算每个仓库的统计数据
        for (Warehouse warehouse : result.getRecords()) {
            calculateWarehouseStats(warehouse);
        }

        return PageResult.build(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    /**
     * 计算仓库统计数据（从库存表动态计算）
     */
    private void calculateWarehouseStats(Warehouse warehouse) {
        LambdaQueryWrapper<Inventory> invQuery = new LambdaQueryWrapper<>();
        invQuery.eq(Inventory::getWarehouseId, warehouse.getId());
        invQuery.eq(Inventory::getDeleted, 0);

        List<Inventory> inventories = inventoryMapper.selectList(invQuery);

        // 商品种类数（不同的productId数量）
        long categoryCount = inventories.stream()
                .map(Inventory::getProductId)
                .distinct()
                .count();

        // 总库存数量
        int totalStock = inventories.stream()
                .mapToInt(inv -> inv.getStock() != null ? inv.getStock() : 0)
                .sum();

        // 库存总价值
        BigDecimal totalValue = inventories.stream()
                .map(inv -> inv.getStockValue() != null ? inv.getStockValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        warehouse.setTotalCategories((int) categoryCount);
        warehouse.setTotalStock(totalStock);
        warehouse.setTotalValue(totalValue);
    }

    @Override
    public Warehouse getWarehouseById(Long id) {
        Warehouse warehouse = warehouseMapper.selectById(id);
        if (warehouse == null || warehouse.getDeleted() == 1) {
            throw new BusinessException("仓库不存在");
        }
        // 动态计算统计数据
        calculateWarehouseStats(warehouse);
        return warehouse;
    }

    @Override
    public void createWarehouse(WarehouseDTO dto) {
        // 检查编码是否重复
        if (StringUtils.hasText(dto.getCode())) {
            LambdaQueryWrapper<Warehouse> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Warehouse::getCode, dto.getCode());
            queryWrapper.eq(Warehouse::getDeleted, 0);
            if (warehouseMapper.selectCount(queryWrapper) > 0) {
                throw new BusinessException("仓库编码已存在");
            }
        }

        Warehouse warehouse = new Warehouse();
        warehouse.setName(dto.getName());
        warehouse.setCode(dto.getCode());
        warehouse.setAddress(dto.getAddress());
        warehouse.setManagerName(dto.getManagerName());
        warehouse.setCapacity(dto.getCapacity());
        if (dto.getCapacityUsed() != null) {
            warehouse.setCapacityUsed(dto.getCapacityUsed());
        } else {
            warehouse.setCapacityUsed(new BigDecimal("50")); // 默认50%
        }
        warehouse.setRemark(dto.getRemark());
        warehouse.setStatus(1); // active

        warehouseMapper.insert(warehouse);
        log.info("创建仓库成功：id={}, name={}", warehouse.getId(), warehouse.getName());
    }

    @Override
    public void updateWarehouse(Long id, WarehouseDTO dto) {
        Warehouse existing = getWarehouseById(id);

        // 检查编码是否重复（排除自身）
        if (StringUtils.hasText(dto.getCode()) && !dto.getCode().equals(existing.getCode())) {
            LambdaQueryWrapper<Warehouse> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Warehouse::getCode, dto.getCode());
            queryWrapper.eq(Warehouse::getDeleted, 0);
            queryWrapper.ne(Warehouse::getId, id);
            if (warehouseMapper.selectCount(queryWrapper) > 0) {
                throw new BusinessException("仓库编码已存在");
            }
        }

        existing.setName(dto.getName());
        existing.setCode(dto.getCode());
        existing.setAddress(dto.getAddress());
        existing.setManagerName(dto.getManagerName());
        existing.setCapacity(dto.getCapacity());
        if (dto.getCapacityUsed() != null) {
            existing.setCapacityUsed(dto.getCapacityUsed());
        }
        existing.setRemark(dto.getRemark());

        warehouseMapper.updateById(existing);
        log.info("更新仓库成功：id={}, name={}", id, existing.getName());
    }

    @Override
    public void deleteWarehouse(Long id) {
        Warehouse warehouse = getWarehouseById(id);

        // 检查仓库是否有库存
        LambdaQueryWrapper<Inventory> inventoryQuery = new LambdaQueryWrapper<>();
        inventoryQuery.eq(Inventory::getWarehouseId, id);
        inventoryQuery.eq(Inventory::getDeleted, 0);
        long inventoryCount = inventoryMapper.selectCount(inventoryQuery);
        if (inventoryCount > 0) {
            throw new BusinessException("该仓库存在库存，无法删除。请先清空库存后再删除。");
        }

        warehouseMapper.deleteById(id);
        log.info("删除仓库成功：id={}, name={}", id, warehouse.getName());
    }

    @Override
    public void batchDeleteWarehouse(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要删除的仓库");
        }
        warehouseMapper.deleteBatchIds(ids);
        log.info("批量删除仓库成功：ids={}", ids);
    }

    @Override
    public List<Warehouse> getAllWarehouses() {
        LambdaQueryWrapper<Warehouse> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Warehouse::getDeleted, 0);
        queryWrapper.eq(Warehouse::getStatus, 1); // active
        queryWrapper.orderByAsc(Warehouse::getName);
        List<Warehouse> warehouses = warehouseMapper.selectList(queryWrapper);

        // 动态计算每个仓库的统计数据
        for (Warehouse warehouse : warehouses) {
            calculateWarehouseStats(warehouse);
        }

        return warehouses;
    }

    @Override
    public List<Warehouse> listOptions() {
        LambdaQueryWrapper<Warehouse> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Warehouse::getDeleted, 0);
        queryWrapper.eq(Warehouse::getStatus, 1);
        queryWrapper.orderByAsc(Warehouse::getName);
        queryWrapper.select(Warehouse::getId, Warehouse::getName, Warehouse::getCode);
        return warehouseMapper.selectList(queryWrapper);
    }

}