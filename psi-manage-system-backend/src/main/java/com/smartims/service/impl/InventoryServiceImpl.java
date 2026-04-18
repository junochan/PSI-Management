package com.smartims.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartims.common.PageResult;
import com.smartims.dto.InventoryImageSearchRequest;
import com.smartims.dto.InventoryTransferDTO;
import com.smartims.dto.ManualInboundDTO;
import com.smartims.dto.PageQuery;
import com.smartims.embedding.DashScopeMultimodalEmbeddingService;
import com.smartims.embedding.ImagePayloadUtil;
import com.smartims.embedding.ImageVectorSearchHelper;
import com.smartims.embedding.ProductImageEmbeddingIndexer;
import com.smartims.entity.*;
import com.smartims.exception.BusinessException;
import com.smartims.mapper.*;
import com.smartims.security.UserContext;
import com.smartims.service.InventoryEmbeddingSyncService;
import com.smartims.service.InventoryService;
import com.smartims.service.SystemConfigService;
import com.smartims.util.CodeGenerator;
import com.smartims.util.InventoryStatusUtil;
import com.smartims.util.StatusNameResolver;
import com.smartims.vo.InventoryStatsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 库存服务实现类
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private static final int IMAGE_SEARCH_MAX_ROWS = 5000;
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter ORDER_NO_DATE = DateTimeFormatter.ofPattern("yyMMdd");
    private static final int MAX_ORDER_NO_ALLOC_ATTEMPTS = 5;

    private final InventoryMapper inventoryMapper;
    private final ProductMapper productMapper;
    private final WarehouseMapper warehouseMapper;
    private final InventoryTransferMapper inventoryTransferMapper;
    private final InventoryWarningMapper inventoryWarningMapper;
    private final InboundRecordMapper inboundRecordMapper;
    private final OutboundRecordMapper outboundRecordMapper;
    private final SysUserMapper sysUserMapper;
    private final DashScopeMultimodalEmbeddingService dashScopeMultimodalEmbeddingService;
    private final ImageVectorSearchHelper imageVectorSearchHelper;
    private final InventoryEmbeddingSyncService inventoryEmbeddingSyncService;
    private final SystemConfigService systemConfigService;

    /**
     * 与仪表盘呆滞 Top10 一致：库存行未单独设置 {@code stagnant_days} 时，使用系统「呆滞商品天数」。
     */
    private int resolveDefaultStagnantThreshold() {
        try {
            Integer cfg = systemConfigService.getSystemConfig().getStaleDays();
            if (cfg != null && cfg >= 1) {
                return cfg;
            }
        } catch (Exception e) {
            log.debug("读取系统呆滞商品天数失败，使用默认 90", e);
        }
        return 90;
    }

    @Override
    public PageResult<Inventory> getInventoryList(PageQuery pageQuery) {
        LambdaQueryWrapper<Inventory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Inventory::getDeleted, 0);

        if (pageQuery.getProductId() != null) {
            queryWrapper.eq(Inventory::getProductId, pageQuery.getProductId());
        }
        if (pageQuery.getWarehouseId() != null) {
            queryWrapper.eq(Inventory::getWarehouseId, pageQuery.getWarehouseId());
        }

        if (StringUtils.hasText(pageQuery.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper
                    .like(Inventory::getProductName, pageQuery.getKeyword())
                    .or()
                    .like(Inventory::getSku, pageQuery.getKeyword())
                    .or()
                    .like(Inventory::getWarehouseName, pageQuery.getKeyword())
            );
        }

        applyDayRange(queryWrapper, pageQuery.getLastOutboundStart(), pageQuery.getLastOutboundEnd(), Inventory::getLastOutboundTime);
        applyDayRange(queryWrapper, pageQuery.getLastInboundStart(), pageQuery.getLastInboundEnd(), Inventory::getLastInboundTime);

        if (StringUtils.hasText(pageQuery.getStagnantStatus())) {
            int defStale = resolveDefaultStagnantThreshold();
            String stagnantDayExpr =
                    "TIMESTAMPDIFF(DAY, COALESCE(last_outbound_time, last_inbound_time, create_time), NOW())";
            String stagnantThreshExpr = "COALESCE(stagnant_days, " + defStale + ")";
            if ("stagnant".equalsIgnoreCase(pageQuery.getStagnantStatus())) {
                queryWrapper.apply(stagnantDayExpr + " >= " + stagnantThreshExpr);
            } else if ("normal".equalsIgnoreCase(pageQuery.getStagnantStatus())) {
                queryWrapper.apply(stagnantDayExpr + " < " + stagnantThreshExpr);
            }
        }

        if (StringUtils.hasText(pageQuery.getSort())) {
            if ("asc".equalsIgnoreCase(pageQuery.getOrder())) {
                queryWrapper.orderByAsc(Inventory::getUpdateTime);
            } else {
                queryWrapper.orderByDesc(Inventory::getUpdateTime);
            }
        } else {
            queryWrapper.orderByDesc(Inventory::getUpdateTime);
        }

        Page<Inventory> page = new Page<>(pageQuery.getPage(), pageQuery.getSize());
        Page<Inventory> result = inventoryMapper.selectPage(page, queryWrapper);
        enrichInventoryDisplayFields(result.getRecords());

        return PageResult.build(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public Inventory getInventoryById(Long id) {
        Inventory inventory = inventoryMapper.selectById(id);
        if (inventory == null || inventory.getDeleted() == 1) {
            throw new BusinessException("库存记录不存在");
        }
        enrichInventoryDisplayFields(List.of(inventory));
        return inventory;
    }

    @Override
    public List<Inventory> getInventoryByProductId(Long productId) {
        LambdaQueryWrapper<Inventory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Inventory::getProductId, productId);
        queryWrapper.eq(Inventory::getDeleted, 0);
        List<Inventory> list = inventoryMapper.selectList(queryWrapper);
        enrichInventoryDisplayFields(list);
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createTransfer(InventoryTransferDTO dto) {
        // 验证商品
        Product product = productMapper.selectById(dto.getProductId());
        if (product == null || product.getDeleted() == 1) {
            throw new BusinessException("商品不存在");
        }

        // 验证源仓库
        Warehouse fromWarehouse = warehouseMapper.selectById(dto.getFromWarehouseId());
        if (fromWarehouse == null || fromWarehouse.getDeleted() == 1) {
            throw new BusinessException("源仓库不存在");
        }

        // 验证目标仓库
        Warehouse toWarehouse = warehouseMapper.selectById(dto.getToWarehouseId());
        if (toWarehouse == null || toWarehouse.getDeleted() == 1) {
            throw new BusinessException("目标仓库不存在");
        }

        // 验证源仓库库存
        LambdaQueryWrapper<Inventory> fromQuery = new LambdaQueryWrapper<>();
        fromQuery.eq(Inventory::getProductId, dto.getProductId());
        fromQuery.eq(Inventory::getWarehouseId, dto.getFromWarehouseId());
        fromQuery.eq(Inventory::getDeleted, 0);
        Inventory fromInventory = inventoryMapper.selectOne(fromQuery);

        if (fromInventory == null || fromInventory.getStock() < dto.getQuantity()) {
            throw new BusinessException("源仓库库存不足");
        }

        // 创建调拨单（单号按库内当日最大序号递增，避免进程重启后与历史单号冲突）
        InventoryTransfer transfer = new InventoryTransfer();
        transfer.setProductId(dto.getProductId());
        transfer.setProductName(product.getName());
        transfer.setSku(product.getCode());
        transfer.setFromWarehouseId(dto.getFromWarehouseId());
        transfer.setFromWarehouseName(fromWarehouse.getName());
        transfer.setToWarehouseId(dto.getToWarehouseId());
        transfer.setToWarehouseName(toWarehouse.getName());
        transfer.setQuantity(dto.getQuantity());
        transfer.setStatus("pending"); // 待确认
        transfer.setRemark(dto.getRemark());

        Long operatorId = UserContext.getCurrentUserId();
        if (operatorId != null) {
            transfer.setOperatorId(operatorId);
            SysUser user = sysUserMapper.selectById(operatorId);
            if (user != null) {
                transfer.setOperatorName(resolveOperatorName(user));
            }
        }

        for (int attempt = 0; attempt < MAX_ORDER_NO_ALLOC_ATTEMPTS; attempt++) {
            transfer.setOrderNo(allocateTransferOrderNo());
            try {
                inventoryTransferMapper.insert(transfer);
                log.info("创建调拨单成功：orderNo={}", transfer.getOrderNo());
                return;
            } catch (DuplicateKeyException e) {
                log.warn("调拨单号冲突，重新分配 attempt={} orderNo={}", attempt + 1, transfer.getOrderNo());
                if (attempt == MAX_ORDER_NO_ALLOC_ATTEMPTS - 1) {
                    throw new BusinessException("调拨单号生成失败，请稍后重试");
                }
            }
        }
    }

    /**
     * 生成当日唯一调拨单号，与 {@link CodeGenerator#generateTransferOrderNo()} 格式一致。
     */
    private String allocateTransferOrderNo() {
        String dateStr = LocalDate.now().format(ORDER_NO_DATE);
        String prefix = CodeGenerator.TRANSFER_PREFIX + dateStr;
        String maxNo = inventoryTransferMapper.selectMaxOrderNoByDatePrefix(prefix);
        int nextSeq = 1;
        if (maxNo != null && maxNo.length() >= prefix.length() + 4) {
            try {
                String tail = maxNo.substring(maxNo.length() - 4);
                nextSeq = Integer.parseInt(tail) + 1;
            } catch (NumberFormatException ignored) {
                nextSeq = 1;
            }
        }
        if (nextSeq < 1 || nextSeq > 9999) {
            throw new BusinessException("当日调拨单序号已用尽，请联系管理员");
        }
        return prefix + String.format("%04d", nextSeq);
    }

    /**
     * 生成当日唯一入库单号，与 {@link CodeGenerator#generateInboundOrderNo()} 格式一致。
     */
    private String allocateInboundRecordOrderNo() {
        String dateStr = LocalDate.now().format(ORDER_NO_DATE);
        String prefix = CodeGenerator.INBOUND_PREFIX + dateStr;
        String maxNo = inboundRecordMapper.selectMaxOrderNoByDatePrefix(prefix);
        int nextSeq = 1;
        if (maxNo != null && maxNo.length() >= prefix.length() + 4) {
            try {
                String tail = maxNo.substring(maxNo.length() - 4);
                nextSeq = Integer.parseInt(tail) + 1;
            } catch (NumberFormatException ignored) {
                nextSeq = 1;
            }
        }
        if (nextSeq < 1 || nextSeq > 9999) {
            throw new BusinessException("当日入库单序号已用尽，请联系管理员");
        }
        return prefix + String.format("%04d", nextSeq);
    }

    /**
     * 生成当日唯一出库单号，与 {@link CodeGenerator#generateOutboundOrderNo()} 格式一致。
     */
    private String allocateOutboundRecordOrderNo() {
        String dateStr = LocalDate.now().format(ORDER_NO_DATE);
        String prefix = CodeGenerator.OUTBOUND_PREFIX + dateStr;
        String maxNo = outboundRecordMapper.selectMaxOrderNoByDatePrefix(prefix);
        int nextSeq = 1;
        if (maxNo != null && maxNo.length() >= prefix.length() + 4) {
            try {
                String tail = maxNo.substring(maxNo.length() - 4);
                nextSeq = Integer.parseInt(tail) + 1;
            } catch (NumberFormatException ignored) {
                nextSeq = 1;
            }
        }
        if (nextSeq < 1 || nextSeq > 9999) {
            throw new BusinessException("当日出库单序号已用尽，请联系管理员");
        }
        return prefix + String.format("%04d", nextSeq);
    }

    private void insertInboundRecordWithAllocatedOrderNo(InboundRecord record) {
        for (int attempt = 0; attempt < MAX_ORDER_NO_ALLOC_ATTEMPTS; attempt++) {
            record.setOrderNo(allocateInboundRecordOrderNo());
            try {
                inboundRecordMapper.insert(record);
                return;
            } catch (DuplicateKeyException e) {
                log.warn("入库单号冲突，重新分配 attempt={} orderNo={}", attempt + 1, record.getOrderNo());
                if (attempt == MAX_ORDER_NO_ALLOC_ATTEMPTS - 1) {
                    throw new BusinessException("入库单号生成失败，请稍后重试");
                }
            }
        }
    }

    private void insertOutboundRecordWithAllocatedOrderNo(OutboundRecord record) {
        for (int attempt = 0; attempt < MAX_ORDER_NO_ALLOC_ATTEMPTS; attempt++) {
            record.setOrderNo(allocateOutboundRecordOrderNo());
            try {
                outboundRecordMapper.insert(record);
                return;
            } catch (DuplicateKeyException e) {
                log.warn("出库单号冲突，重新分配 attempt={} orderNo={}", attempt + 1, record.getOrderNo());
                if (attempt == MAX_ORDER_NO_ALLOC_ATTEMPTS - 1) {
                    throw new BusinessException("出库单号生成失败，请稍后重试");
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmTransfer(Long transferId) {
        InventoryTransfer transfer = getTransferById(transferId);

        if (!"pending".equals(transfer.getStatus())) {
            throw new BusinessException("只有待确认状态的调拨单可以确认");
        }

        // 减少源仓库库存
        LambdaQueryWrapper<Inventory> fromQuery = new LambdaQueryWrapper<>();
        fromQuery.eq(Inventory::getProductId, transfer.getProductId());
        fromQuery.eq(Inventory::getWarehouseId, transfer.getFromWarehouseId());
        fromQuery.eq(Inventory::getDeleted, 0);
        Inventory fromInventory = inventoryMapper.selectOne(fromQuery);

        if (fromInventory == null || fromInventory.getStock() < transfer.getQuantity()) {
            throw new BusinessException("源仓库库存不足");
        }

        fromInventory.setStock(fromInventory.getStock() - transfer.getQuantity());
        updateStockStatus(fromInventory);
        inventoryMapper.updateById(fromInventory);

        // 增加目标仓库库存
        LambdaQueryWrapper<Inventory> toQuery = new LambdaQueryWrapper<>();
        toQuery.eq(Inventory::getProductId, transfer.getProductId());
        toQuery.eq(Inventory::getWarehouseId, transfer.getToWarehouseId());
        toQuery.eq(Inventory::getDeleted, 0);
        Inventory toInventory = inventoryMapper.selectOne(toQuery);

        if (toInventory == null) {
            Warehouse toWarehouse = warehouseMapper.selectById(transfer.getToWarehouseId());
            Product product = productMapper.selectById(transfer.getProductId());
            toInventory = new Inventory();
            toInventory.setProductId(transfer.getProductId());
            // 与采购入库/手动入库一致：sku 使用商品编码；唯一性由库表 uk_sku_warehouse(sku, warehouse_id) 保证
            toInventory.setSku(StringUtils.hasText(transfer.getSku()) ? transfer.getSku() : product.getCode());
            toInventory.setProductName(product.getName());
            toInventory.setSpec(product.getSpec());
            toInventory.setCategory(product.getCategoryName());
            toInventory.setWarehouseId(transfer.getToWarehouseId());
            toInventory.setWarehouseName(toWarehouse.getName());
            toInventory.setStock(transfer.getQuantity());
            toInventory.setSafeStock(10);
            toInventory.setCostPrice(product.getCostPrice());
            if (product.getCostPrice() != null) {
                toInventory.setStockValue(product.getCostPrice().multiply(new BigDecimal(transfer.getQuantity())));
            }
            toInventory.setStatus("normal");
            toInventory.setLastInboundTime(LocalDateTime.now());
            updateStockStatus(toInventory);
            inventoryMapper.insert(toInventory);
            inventoryEmbeddingSyncService.indexInventoryRow(toInventory);
        } else {
            toInventory.setStock(toInventory.getStock() + transfer.getQuantity());
            toInventory.setLastInboundTime(LocalDateTime.now());
            updateStockStatus(toInventory);
            inventoryMapper.updateById(toInventory);
        }

        // 更新调拨状态
        transfer.setStatus("completed");
        transfer.setCompleteTime(LocalDateTime.now());
        inventoryTransferMapper.updateById(transfer);

        log.info("确认调拨成功：transferId={}", transferId);
    }

    @Override
    public PageResult<InventoryTransfer> getTransferList(PageQuery pageQuery) {
        LambdaQueryWrapper<InventoryTransfer> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InventoryTransfer::getDeleted, 0);

        if (StringUtils.hasText(pageQuery.getTransferStatus())) {
            queryWrapper.eq(InventoryTransfer::getStatus, pageQuery.getTransferStatus().trim());
        }
        if (pageQuery.getWarehouseId() != null) {
            Long wid = pageQuery.getWarehouseId();
            queryWrapper.and(w -> w.eq(InventoryTransfer::getFromWarehouseId, wid)
                    .or()
                    .eq(InventoryTransfer::getToWarehouseId, wid));
        }
        applyTransferCreateTimeRange(queryWrapper, pageQuery.getCreateTimeStart(), pageQuery.getCreateTimeEnd());

        if (StringUtils.hasText(pageQuery.getKeyword())) {
            String kw = pageQuery.getKeyword().trim();
            queryWrapper.and(wrapper -> wrapper
                    .like(InventoryTransfer::getOrderNo, kw)
                    .or()
                    .like(InventoryTransfer::getProductName, kw)
                    .or()
                    .like(InventoryTransfer::getSku, kw)
            );
        }

        queryWrapper.orderByDesc(InventoryTransfer::getCreateTime);

        Page<InventoryTransfer> page = new Page<>(pageQuery.getPage(), pageQuery.getSize());
        Page<InventoryTransfer> result = inventoryTransferMapper.selectPage(page, queryWrapper);
        enrichTransferOperators(result.getRecords());
        enrichTransferProductFields(result.getRecords());

        return PageResult.build(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public InventoryTransfer getTransferById(Long id) {
        InventoryTransfer transfer = inventoryTransferMapper.selectById(id);
        if (transfer == null || transfer.getDeleted() == 1) {
            throw new BusinessException("调拨记录不存在");
        }
        enrichTransferOperators(List.of(transfer));
        enrichTransferProductFields(List.of(transfer));
        return transfer;
    }

    /**
     * 调拨记录操作人兜底补全：
     * 1) 兼容历史数据 operator_name 为空；
     * 2) 兼容用户姓名为空时回退 username。
     */
    private void enrichTransferOperators(List<InventoryTransfer> transfers) {
        if (transfers == null || transfers.isEmpty()) {
            return;
        }
        Set<Long> operatorIds = new HashSet<>();
        for (InventoryTransfer transfer : transfers) {
            if (transfer == null) {
                continue;
            }
            if (!StringUtils.hasText(transfer.getOperatorName()) && transfer.getOperatorId() != null) {
                operatorIds.add(transfer.getOperatorId());
            }
        }
        if (operatorIds.isEmpty()) {
            return;
        }

        LambdaQueryWrapper<SysUser> userQuery = new LambdaQueryWrapper<>();
        userQuery.in(SysUser::getId, operatorIds);
        userQuery.eq(SysUser::getDeleted, 0);
        List<SysUser> users = sysUserMapper.selectList(userQuery);
        if (users.isEmpty()) {
            return;
        }

        Map<Long, String> operatorNameMap = new HashMap<>();
        for (SysUser user : users) {
            if (user != null && user.getId() != null) {
                operatorNameMap.put(user.getId(), resolveOperatorName(user));
            }
        }
        for (InventoryTransfer transfer : transfers) {
            if (transfer == null || StringUtils.hasText(transfer.getOperatorName()) || transfer.getOperatorId() == null) {
                continue;
            }
            transfer.setOperatorName(operatorNameMap.get(transfer.getOperatorId()));
        }
    }

    /**
     * 调拨列表/详情：按 productId 批量补商品主图与分类名，供前端表格展示缩略图。
     */
    private void enrichTransferProductFields(List<InventoryTransfer> transfers) {
        if (transfers == null || transfers.isEmpty()) {
            return;
        }
        Set<Long> productIds = new HashSet<>();
        for (InventoryTransfer transfer : transfers) {
            if (transfer != null && transfer.getProductId() != null) {
                productIds.add(transfer.getProductId());
            }
        }
        if (productIds.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<Product> productQuery = new LambdaQueryWrapper<>();
        productQuery.in(Product::getId, productIds);
        productQuery.eq(Product::getDeleted, 0);
        List<Product> products = productMapper.selectList(productQuery);
        Map<Long, Product> productMap = new HashMap<>();
        for (Product product : products) {
            if (product != null && product.getId() != null) {
                productMap.put(product.getId(), product);
            }
        }
        for (InventoryTransfer transfer : transfers) {
            if (transfer == null || transfer.getProductId() == null) {
                continue;
            }
            Product product = productMap.get(transfer.getProductId());
            if (product != null) {
                transfer.setProductImage(product.getImage());
                transfer.setCategory(product.getCategoryName());
            }
        }
    }

    /**
     * 为库存列表补充前端展示字段，避免前端再调用商品/仓库分页接口拼装数据。
     */
    private void enrichInventoryDisplayFields(List<Inventory> inventories) {
        if (inventories == null || inventories.isEmpty()) {
            return;
        }
        Set<Long> productIds = new HashSet<>();
        Set<Long> warehouseIds = new HashSet<>();
        for (Inventory inventory : inventories) {
            if (inventory == null) {
                continue;
            }
            if (inventory.getProductId() != null) {
                productIds.add(inventory.getProductId());
            }
            if (inventory.getWarehouseId() != null) {
                warehouseIds.add(inventory.getWarehouseId());
            }
        }

        Map<Long, Product> productMap = new HashMap<>();
        if (!productIds.isEmpty()) {
            LambdaQueryWrapper<Product> productQuery = new LambdaQueryWrapper<>();
            productQuery.in(Product::getId, productIds);
            productQuery.eq(Product::getDeleted, 0);
            List<Product> products = productMapper.selectList(productQuery);
            for (Product product : products) {
                if (product != null && product.getId() != null) {
                    productMap.put(product.getId(), product);
                }
            }
        }

        Map<Long, Warehouse> warehouseMap = new HashMap<>();
        if (!warehouseIds.isEmpty()) {
            LambdaQueryWrapper<Warehouse> warehouseQuery = new LambdaQueryWrapper<>();
            warehouseQuery.in(Warehouse::getId, warehouseIds);
            warehouseQuery.eq(Warehouse::getDeleted, 0);
            List<Warehouse> warehouses = warehouseMapper.selectList(warehouseQuery);
            for (Warehouse warehouse : warehouses) {
                if (warehouse != null && warehouse.getId() != null) {
                    warehouseMap.put(warehouse.getId(), warehouse);
                }
            }
        }

        for (Inventory inventory : inventories) {
            if (inventory == null) {
                continue;
            }
            Product product = inventory.getProductId() == null ? null : productMap.get(inventory.getProductId());
            if (product != null) {
                inventory.setProductImage(product.getImage());
                inventory.setProductStatus(product.getStatus());
                if (!StringUtils.hasText(inventory.getProductName())) {
                    inventory.setProductName(product.getName());
                }
                if (!StringUtils.hasText(inventory.getSpec())) {
                    inventory.setSpec(product.getSpec());
                }
                if (!StringUtils.hasText(inventory.getCategory())) {
                    inventory.setCategory(product.getCategoryName());
                }
                if (!StringUtils.hasText(inventory.getSku())) {
                    inventory.setSku(product.getCode());
                }
            }

            Warehouse warehouse = inventory.getWarehouseId() == null ? null : warehouseMap.get(inventory.getWarehouseId());
            if (warehouse != null) {
                inventory.setWarehouseCode(warehouse.getCode());
                inventory.setWarehouseStatus(warehouse.getStatus());
                if (!StringUtils.hasText(inventory.getWarehouseName())) {
                    inventory.setWarehouseName(warehouse.getName());
                }
            }
        }
    }

    private String resolveOperatorName(SysUser user) {
        if (user == null) {
            return null;
        }
        if (StringUtils.hasText(user.getName())) {
            return user.getName();
        }
        return user.getUsername();
    }

    @Override
    public PageResult<InventoryWarning> getWarningList(PageQuery pageQuery) {
        LambdaQueryWrapper<InventoryWarning> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InventoryWarning::getDeleted, 0);

        if (StringUtils.hasText(pageQuery.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper
                    .like(InventoryWarning::getProductName, pageQuery.getKeyword())
                    .or()
                    .like(InventoryWarning::getWarehouseName, pageQuery.getKeyword())
            );
        }

        queryWrapper.orderByDesc(InventoryWarning::getCreateTime);

        Page<InventoryWarning> page = new Page<>(pageQuery.getPage(), pageQuery.getSize());
        Page<InventoryWarning> result = inventoryWarningMapper.selectPage(page, queryWrapper);

        return PageResult.build(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleWarning(Long warningId, String handleRemark) {
        InventoryWarning warning = inventoryWarningMapper.selectById(warningId);
        if (warning == null || warning.getDeleted() == 1) {
            throw new BusinessException("库存预警不存在");
        }

        warning.setStatus(1); // handled - use Integer
        inventoryWarningMapper.updateById(warning);

        log.info("处理库存预警成功：warningId={}", warningId);
    }

    @Override
    public InventoryStatsVO getInventoryStats() {
        InventoryStatsVO stats = new InventoryStatsVO();

        YearMonth currentMonth = YearMonth.now();
        YearMonth lastMonth = currentMonth.minusMonths(1);

        LocalDateTime currentMonthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime currentMonthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59);
        LocalDateTime lastMonthStart = lastMonth.atDay(1).atStartOfDay();
        LocalDateTime lastMonthEnd = lastMonth.atEndOfMonth().atTime(23, 59, 59);

        // 商品种类数量（按productId统计不重复的商品）
        LambdaQueryWrapper<Inventory> currentInventoryQuery = new LambdaQueryWrapper<>();
        currentInventoryQuery.eq(Inventory::getDeleted, 0)
                .ge(Inventory::getUpdateTime, currentMonthStart)
                .le(Inventory::getUpdateTime, currentMonthEnd);

        Set<Long> currentProductIds = new HashSet<>();
        inventoryMapper.selectList(currentInventoryQuery)
                .forEach(inv -> currentProductIds.add(inv.getProductId()));
        stats.setCategoryCount(currentProductIds.size());

        // 上月商品种类数量
        LambdaQueryWrapper<Inventory> lastInventoryQuery = new LambdaQueryWrapper<>();
        lastInventoryQuery.eq(Inventory::getDeleted, 0)
                .ge(Inventory::getUpdateTime, lastMonthStart)
                .le(Inventory::getUpdateTime, lastMonthEnd);

        Set<Long> lastProductIds = new HashSet<>();
        inventoryMapper.selectList(lastInventoryQuery)
                .forEach(inv -> lastProductIds.add(inv.getProductId()));
        stats.setLastMonthCategoryCount(lastProductIds.size());

        // 商品种类变化
        stats.setCategoryChange(currentProductIds.size() - lastProductIds.size());

        // 库存总额
        LambdaQueryWrapper<Inventory> allInventoryQuery = new LambdaQueryWrapper<>();
        allInventoryQuery.eq(Inventory::getDeleted, 0);

        BigDecimal totalValue = inventoryMapper.selectList(allInventoryQuery)
                .stream()
                .map(inv -> inv.getStockValue() != null ? inv.getStockValue() :
                        (inv.getCostPrice() != null && inv.getStock() != null ?
                                inv.getCostPrice().multiply(new BigDecimal(inv.getStock())) : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalValue(totalValue);

        // 计算上月库存总额（使用上月末的库存记录）
        BigDecimal lastMonthTotalValue = BigDecimal.ZERO;
        // 这里简化处理，实际应该有历史库存数据，暂时用当前库存作为基准
        stats.setLastMonthTotalValue(lastMonthTotalValue);

        // 库存总额变化百分比（上月为0时，如果有库存则100%）
        if (lastMonthTotalValue.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal change = totalValue.subtract(lastMonthTotalValue)
                    .divide(lastMonthTotalValue, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100));
            stats.setValueChangePercent(change);
        } else {
            stats.setValueChangePercent(totalValue.compareTo(BigDecimal.ZERO) > 0 ? new BigDecimal(100) : BigDecimal.ZERO);
        }

        // 库存预警数量（未处理的预警）
        LambdaQueryWrapper<InventoryWarning> warningQuery = new LambdaQueryWrapper<>();
        warningQuery.eq(InventoryWarning::getDeleted, 0)
                .eq(InventoryWarning::getStatus, 0); // 未处理状态

        Long warningCount = inventoryWarningMapper.selectCount(warningQuery);
        stats.setWarningCount(warningCount.intValue());

        // 调拨中数量（本月pending状态的调拨单）
        LambdaQueryWrapper<InventoryTransfer> currentTransferQuery = new LambdaQueryWrapper<>();
        currentTransferQuery.eq(InventoryTransfer::getDeleted, 0)
                .eq(InventoryTransfer::getStatus, "pending")
                .ge(InventoryTransfer::getCreateTime, currentMonthStart)
                .le(InventoryTransfer::getCreateTime, currentMonthEnd);

        Long transferringCount = inventoryTransferMapper.selectCount(currentTransferQuery);
        stats.setTransferringCount(transferringCount.intValue());

        // 上月调拨中数量
        LambdaQueryWrapper<InventoryTransfer> lastTransferQuery = new LambdaQueryWrapper<>();
        lastTransferQuery.eq(InventoryTransfer::getDeleted, 0)
                .eq(InventoryTransfer::getStatus, "pending")
                .ge(InventoryTransfer::getCreateTime, lastMonthStart)
                .le(InventoryTransfer::getCreateTime, lastMonthEnd);

        Long lastMonthTransferringCount = inventoryTransferMapper.selectCount(lastTransferQuery);
        stats.setLastMonthTransferringCount(lastMonthTransferringCount.intValue());

        // 调拨中变化
        stats.setTransferringChange(transferringCount.intValue() - lastMonthTransferringCount.intValue());

        return stats;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void manualInbound(Long inventoryId, Integer quantity, String remark) {
        Inventory inventory = getInventoryById(inventoryId);

        // 创建入库记录
        InboundRecord record = new InboundRecord();
        record.setProductId(inventory.getProductId());
        record.setProductName(inventory.getProductName());
        record.setSku(inventory.getSku());
        record.setWarehouseId(inventory.getWarehouseId());
        record.setWarehouseName(inventory.getWarehouseName());
        record.setQuantity(quantity);
        record.setUnitPrice(inventory.getCostPrice());
        if (inventory.getCostPrice() != null) {
            record.setAmount(inventory.getCostPrice().multiply(new BigDecimal(quantity)));
        }
        record.setRemark(remark != null ? remark : "其他入库");

        // 设置操作人信息
        Long operatorId = UserContext.getCurrentUserId();
        if (operatorId != null) {
            record.setOperatorId(operatorId);
            SysUser user = sysUserMapper.selectById(operatorId);
            if (user != null) {
                record.setOperatorName(user.getName());
            }
        }

        insertInboundRecordWithAllocatedOrderNo(record);

        // 增加库存
        int currentStock = inventory.getStock() != null ? inventory.getStock() : 0;
        inventory.setStock(currentStock + quantity);
        inventory.setLastInboundTime(LocalDateTime.now());

        // 更新库存价值
        if (inventory.getCostPrice() != null) {
            inventory.setStockValue(inventory.getCostPrice().multiply(new BigDecimal(inventory.getStock())));
        }

        // 更新状态
        updateStockStatus(inventory);

        inventoryMapper.updateById(inventory);
        log.info("手动入库成功：inventoryId={}, quantity={}, newStock={}", inventoryId, quantity, inventory.getStock());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void manualOutbound(Long inventoryId, Integer quantity, String remark) {
        Inventory inventory = getInventoryById(inventoryId);

        // 验证库存数量
        int currentStock = inventory.getStock() != null ? inventory.getStock() : 0;
        if (currentStock < quantity) {
            throw new BusinessException("库存不足，当前库存：" + currentStock + "，出库数量：" + quantity);
        }

        // 创建出库记录
        OutboundRecord record = new OutboundRecord();
        record.setProductId(inventory.getProductId());
        record.setProductName(inventory.getProductName());
        record.setSku(inventory.getSku());
        record.setWarehouseId(inventory.getWarehouseId());
        record.setWarehouseName(inventory.getWarehouseName());
        record.setQuantity(quantity);
        record.setUnitPrice(inventory.getCostPrice());
        if (inventory.getCostPrice() != null) {
            record.setAmount(inventory.getCostPrice().multiply(new BigDecimal(quantity)));
        }
        record.setRemark(remark != null ? remark : "其他出库");
        record.setOutboundType("other");

        // 设置操作人信息
        Long operatorId = UserContext.getCurrentUserId();
        if (operatorId != null) {
            record.setOperatorId(operatorId);
            SysUser user = sysUserMapper.selectById(operatorId);
            if (user != null) {
                record.setOperatorName(user.getName());
            }
        }

        insertOutboundRecordWithAllocatedOrderNo(record);

        // 减少库存
        inventory.setStock(currentStock - quantity);
        inventory.setLastOutboundTime(LocalDateTime.now());

        // 更新库存价值
        if (inventory.getCostPrice() != null) {
            inventory.setStockValue(inventory.getCostPrice().multiply(new BigDecimal(inventory.getStock())));
        }

        // 更新状态
        updateStockStatus(inventory);

        inventoryMapper.updateById(inventory);
        log.info("手动出库成功：inventoryId={}, quantity={}, newStock={}", inventoryId, quantity, inventory.getStock());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void manualInboundNew(ManualInboundDTO dto) {
        // 查询商品
        Product product = productMapper.selectById(dto.getProductId());
        if (product == null || product.getDeleted() == 1) {
            throw new BusinessException("商品不存在");
        }
        if (StatusNameResolver.isProductOffSale(product.getStatus())) {
            throw new BusinessException("该商品已停售，无法通过其他入库增加库存");
        }

        // 查询仓库
        Warehouse warehouse = warehouseMapper.selectById(dto.getWarehouseId());
        if (warehouse == null || warehouse.getDeleted() == 1) {
            throw new BusinessException("仓库不存在");
        }

        // 创建入库记录
        InboundRecord record = new InboundRecord();
        record.setProductId(product.getId());
        record.setProductName(product.getName());
        record.setSku(product.getCode());
        record.setWarehouseId(warehouse.getId());
        record.setWarehouseName(warehouse.getName());
        record.setQuantity(dto.getQuantity());
        record.setUnitPrice(product.getCostPrice());
        if (product.getCostPrice() != null) {
            record.setAmount(product.getCostPrice().multiply(new BigDecimal(dto.getQuantity())));
        }
        record.setRemark(dto.getRemark() != null ? dto.getRemark() : "其他入库");

        // 设置操作人信息
        Long operatorId = UserContext.getCurrentUserId();
        if (operatorId != null) {
            record.setOperatorId(operatorId);
            SysUser user = sysUserMapper.selectById(operatorId);
            if (user != null) {
                record.setOperatorName(user.getName());
            }
        }

        insertInboundRecordWithAllocatedOrderNo(record);

        // 查询是否已存在该商品在该仓库的库存记录
        LambdaQueryWrapper<Inventory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Inventory::getProductId, dto.getProductId());
        queryWrapper.eq(Inventory::getWarehouseId, dto.getWarehouseId());
        queryWrapper.eq(Inventory::getDeleted, 0);
        Inventory existingInventory = inventoryMapper.selectOne(queryWrapper);

        if (existingInventory != null) {
            // 已存在，增加库存
            int currentStock = existingInventory.getStock() != null ? existingInventory.getStock() : 0;
            existingInventory.setStock(currentStock + dto.getQuantity());
            existingInventory.setLastInboundTime(LocalDateTime.now());
            if (existingInventory.getCostPrice() != null) {
                existingInventory.setStockValue(existingInventory.getCostPrice().multiply(new BigDecimal(existingInventory.getStock())));
            }
            updateStockStatus(existingInventory);
            inventoryMapper.updateById(existingInventory);
            log.info("手动入库成功（已有库存）：productId={}, warehouseId={}, quantity={}, newStock={}",
                    dto.getProductId(), dto.getWarehouseId(), dto.getQuantity(), existingInventory.getStock());
        } else {
            // 不存在，创建新的库存记录
            Inventory inventory = new Inventory();
            inventory.setSku(product.getCode());
            inventory.setProductId(product.getId());
            inventory.setProductName(product.getName());
            inventory.setSpec(product.getSpec());
            inventory.setCategory(product.getCategoryName());
            inventory.setWarehouseId(warehouse.getId());
            inventory.setWarehouseName(warehouse.getName());
            inventory.setStock(dto.getQuantity());
            inventory.setSafeStock(10);
            inventory.setCostPrice(product.getCostPrice());
            inventory.setStockValue(product.getCostPrice() != null ?
                    product.getCostPrice().multiply(new BigDecimal(dto.getQuantity())) : BigDecimal.ZERO);
            inventory.setLastInboundTime(LocalDateTime.now());
            updateStockStatus(inventory);
            inventoryMapper.insert(inventory);
            inventoryEmbeddingSyncService.indexInventoryRow(inventory);
            log.info("手动入库成功（新建库存）：productId={}, warehouseId={}, quantity={}",
                    dto.getProductId(), dto.getWarehouseId(), dto.getQuantity());
        }
    }

    /**
     * 更新库存状态
     */
    private void updateStockStatus(Inventory inventory) {
        InventoryStatusUtil.applyStatus(inventory);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSafeStock(Long inventoryId, Integer safeStock) {
        Inventory inventory = getInventoryById(inventoryId);

        if (safeStock == null || safeStock < 0) {
            throw new BusinessException("库存预警值必须大于等于0");
        }

        inventory.setSafeStock(safeStock);
        updateStockStatus(inventory);
        inventoryMapper.updateById(inventory);
        log.info("更新库存预警值成功：inventoryId={}, safeStock={}", inventoryId, safeStock);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLocation(Long inventoryId, String location) {
        Inventory inventory = getInventoryById(inventoryId);

        inventory.setLocation(location);
        inventoryMapper.updateById(inventory);
        log.info("更新库位成功：inventoryId={}, location={}", inventoryId, location);
    }

    @Override
    public PageResult<OutboundRecord> getOutboundRecordList(PageQuery pageQuery) {
        Long effectiveProductId = pageQuery.getProductId();
        Long effectiveWarehouseId = pageQuery.getWarehouseId();
        if (pageQuery.getInventoryId() != null) {
            Inventory inv = inventoryMapper.selectById(pageQuery.getInventoryId());
            if (inv == null) {
                return PageResult.build(0L, pageQuery.getPage(), pageQuery.getSize(), Collections.emptyList());
            }
            effectiveProductId = inv.getProductId();
            effectiveWarehouseId = inv.getWarehouseId();
        }

        LambdaQueryWrapper<OutboundRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OutboundRecord::getDeleted, 0);

        if (effectiveWarehouseId != null) {
            queryWrapper.eq(OutboundRecord::getWarehouseId, effectiveWarehouseId);
        }
        if (effectiveProductId != null) {
            queryWrapper.eq(OutboundRecord::getProductId, effectiveProductId);
        }

        LocalDateTime outboundStart = parseQueryDayStart(pageQuery.getCreateTimeStart());
        LocalDateTime outboundEnd = parseQueryDayEnd(pageQuery.getCreateTimeEnd());
        if (outboundStart != null) {
            queryWrapper.ge(OutboundRecord::getCreateTime, outboundStart);
        }
        if (outboundEnd != null) {
            queryWrapper.le(OutboundRecord::getCreateTime, outboundEnd);
        }
        if (StringUtils.hasText(pageQuery.getOperatorName())) {
            queryWrapper.like(OutboundRecord::getOperatorName, pageQuery.getOperatorName().trim());
        }

        if (StringUtils.hasText(pageQuery.getKeyword())) {
            String kw = pageQuery.getKeyword().trim();
            queryWrapper.and(wrapper -> wrapper
                    .like(OutboundRecord::getOrderNo, kw)
                    .or()
                    .like(OutboundRecord::getProductName, kw)
                    .or()
                    .like(OutboundRecord::getWarehouseName, kw)
                    .or()
                    .like(OutboundRecord::getCustomerName, kw)
                    .or()
                    .like(OutboundRecord::getSalesOrderNo, kw)
            );
        }

        queryWrapper.orderByDesc(OutboundRecord::getCreateTime);

        Page<OutboundRecord> page = new Page<>(pageQuery.getPage(), pageQuery.getSize());
        Page<OutboundRecord> result = outboundRecordMapper.selectPage(page, queryWrapper);
        enrichOutboundProductFields(result.getRecords());

        return PageResult.build(result.getTotal(), pageQuery.getPage(), pageQuery.getSize(), result.getRecords());
    }

    /**
     * 出库流水列表：按 productId 批量补商品主图与分类名。
     */
    private void enrichOutboundProductFields(List<OutboundRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Set<Long> productIds = new HashSet<>();
        for (OutboundRecord record : records) {
            if (record != null && record.getProductId() != null) {
                productIds.add(record.getProductId());
            }
        }
        if (productIds.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<Product> productQuery = new LambdaQueryWrapper<>();
        productQuery.in(Product::getId, productIds);
        productQuery.eq(Product::getDeleted, 0);
        List<Product> products = productMapper.selectList(productQuery);
        Map<Long, Product> productMap = new HashMap<>();
        for (Product product : products) {
            if (product != null && product.getId() != null) {
                productMap.put(product.getId(), product);
            }
        }
        for (OutboundRecord record : records) {
            if (record == null || record.getProductId() == null) {
                continue;
            }
            Product product = productMap.get(record.getProductId());
            if (product != null) {
                record.setProductImage(product.getImage());
                record.setCategory(product.getCategoryName());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStagnantDays(Long inventoryId, Integer stagnantDays) {
        Inventory inventory = getInventoryById(inventoryId);

        if (stagnantDays == null || stagnantDays < 1) {
            throw new BusinessException("呆滞预警天数必须大于等于1");
        }

        inventory.setStagnantDays(stagnantDays);
        inventoryMapper.updateById(inventory);
        log.info("更新呆滞预警天数成功：inventoryId={}, stagnantDays={}", inventoryId, stagnantDays);
    }

    @Override
    public PageResult<Inventory> searchByImage(InventoryImageSearchRequest req) {
        long page = req.getPage() != null && req.getPage() >= 1 ? req.getPage() : 1;
        long size = req.getSize() != null && req.getSize() >= 1 ? Math.min(req.getSize(), IMAGE_SEARCH_MAX_ROWS) : 10;

        String qPayload;
        try {
            qPayload = ImagePayloadUtil.toPngDataUrl(req.getImageBase64().trim());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("无法解析查询图片，请上传有效的图片文件");
        }
        var qVec = dashScopeMultimodalEmbeddingService.embedImagePayload(qPayload);
        if (qVec.isEmpty()) {
            log.warn("查询图向量化失败，请配置环境变量 DASHSCOPE_API_KEY 或 application.yml 中 dashscope.api-key");
            return PageResult.build(0L, page, size, new ArrayList<>());
        }
        float[] queryVector = qVec.get();

        LambdaQueryWrapper<Inventory> wrapper = buildImageSearchQueryWrapper(req);
        List<Inventory> list = inventoryMapper.selectList(wrapper);
        list.removeIf(i -> !matchesStagnantFilter(i, req.getStagnantStatus()));

        if (list.size() > IMAGE_SEARCH_MAX_ROWS) {
            list = new ArrayList<>(list.subList(0, IMAGE_SEARCH_MAX_ROWS));
        }

        Set<Long> productIds = new HashSet<>();
        for (Inventory inv : list) {
            if (inv.getProductId() != null) {
                productIds.add(inv.getProductId());
            }
        }
        Map<Long, Product> productMap = new HashMap<>();
        if (!productIds.isEmpty()) {
            LambdaQueryWrapper<Product> pq = new LambdaQueryWrapper<>();
            pq.in(Product::getId, productIds);
            List<Product> products = productMapper.selectList(pq);
            for (Product p : products) {
                if (p != null) {
                    productMap.put(p.getId(), p);
                }
            }
        }

        List<Inventory> sorted = imageVectorSearchHelper.sortByVectorSimilarity(
                list,
                queryVector,
                ProductImageEmbeddingIndexer.NS_INVENTORY,
                Inventory::getId,
                inv -> {
                    Product p = inv.getProductId() == null ? null : productMap.get(inv.getProductId());
                    return p != null ? p.getImage() : null;
                },
                req.getSimilarityThreshold());
        long total = sorted.size();
        int from = (int) ((page - 1) * size);
        List<Inventory> pageRecords = new ArrayList<>();
        if (from < total) {
            int to = (int) Math.min(from + size, total);
            pageRecords = new ArrayList<>(sorted.subList(from, to));
        }
        return PageResult.build(total, page, size, pageRecords);
    }

    private LambdaQueryWrapper<Inventory> buildImageSearchQueryWrapper(InventoryImageSearchRequest req) {
        LambdaQueryWrapper<Inventory> q = new LambdaQueryWrapper<>();
        q.eq(Inventory::getDeleted, 0);
        if (StringUtils.hasText(req.getKeyword())) {
            String k = req.getKeyword().trim();
            q.and(w -> w.like(Inventory::getProductName, k)
                    .or().like(Inventory::getSku, k)
                    .or().like(Inventory::getWarehouseName, k));
        }
        if (req.getProductId() != null) {
            q.eq(Inventory::getProductId, req.getProductId());
        }
        if (req.getWarehouseId() != null) {
            q.eq(Inventory::getWarehouseId, req.getWarehouseId());
        }
        applyDayRange(q, req.getLastOutboundStart(), req.getLastOutboundEnd(), Inventory::getLastOutboundTime);
        applyDayRange(q, req.getLastInboundStart(), req.getLastInboundEnd(), Inventory::getLastInboundTime);
        return q;
    }

    private void applyDayRange(LambdaQueryWrapper<Inventory> q, String start, String end,
                               SFunction<Inventory, LocalDateTime> field) {
        try {
            if (StringUtils.hasText(start)) {
                LocalDateTime t = LocalDate.parse(start.trim(), ISO_DATE).atStartOfDay();
                q.ge(field, t);
            }
            if (StringUtils.hasText(end)) {
                LocalDateTime t = LocalDate.parse(end.trim(), ISO_DATE).atTime(23, 59, 59);
                q.le(field, t);
            }
        } catch (Exception e) {
            log.debug("日期范围解析跳过: {} - {}", start, end);
        }
    }

    private void applyTransferCreateTimeRange(LambdaQueryWrapper<InventoryTransfer> q, String start, String end) {
        LocalDateTime t0 = parseQueryDayStart(start);
        LocalDateTime t1 = parseQueryDayEnd(end);
        if (t0 != null) {
            q.ge(InventoryTransfer::getCreateTime, t0);
        }
        if (t1 != null) {
            q.le(InventoryTransfer::getCreateTime, t1);
        }
    }

    private static LocalDateTime parseQueryDayStart(String day) {
        if (!StringUtils.hasText(day)) {
            return null;
        }
        try {
            return LocalDate.parse(day.trim(), ISO_DATE).atStartOfDay();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static LocalDateTime parseQueryDayEnd(String day) {
        if (!StringUtils.hasText(day)) {
            return null;
        }
        try {
            return LocalDateTime.of(LocalDate.parse(day.trim(), ISO_DATE), LocalTime.of(23, 59, 59));
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean matchesStagnantFilter(Inventory i, String stagnantStatus) {
        if (!StringUtils.hasText(stagnantStatus)) {
            return true;
        }
        long sd = computeStagnantDays(i);
        int warning = i.getStagnantDays() != null ? i.getStagnantDays() : resolveDefaultStagnantThreshold();
        if ("stagnant".equalsIgnoreCase(stagnantStatus)) {
            return sd >= warning;
        }
        if ("normal".equalsIgnoreCase(stagnantStatus)) {
            return sd < warning;
        }
        return true;
    }

    private long computeStagnantDays(Inventory i) {
        LocalDateTime ref = null;
        if (i.getLastOutboundTime() != null) {
            ref = i.getLastOutboundTime();
        } else if (i.getLastInboundTime() != null) {
            ref = i.getLastInboundTime();
        } else if (i.getCreateTime() != null) {
            ref = i.getCreateTime();
        }
        if (ref == null) {
            return 0L;
        }
        return ChronoUnit.DAYS.between(ref, LocalDateTime.now());
    }

}