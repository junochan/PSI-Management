package com.smartims.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartims.common.PageResult;
import com.smartims.dto.InboundDTO;
import com.smartims.dto.PageQuery;
import com.smartims.dto.PurchaseOrderDTO;
import com.smartims.entity.*;
import com.smartims.exception.BusinessException;
import com.smartims.mapper.*;
import com.smartims.service.InventoryEmbeddingSyncService;
import com.smartims.service.PurchaseService;
import com.smartims.security.UserContext;
import com.smartims.util.CodeGenerator;
import com.smartims.util.InventoryStatusUtil;
import com.smartims.util.StatusNameResolver;
import com.smartims.vo.PurchaseStatsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 采购服务实现类
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseServiceImpl implements PurchaseService {

    private static final DateTimeFormatter PURCHASE_ORDER_DATE = DateTimeFormatter.ofPattern("yyMMdd");
    private static final int MAX_PURCHASE_ORDER_NO_ATTEMPTS = 5;
    private static final int MAX_INBOUND_RECORD_ORDER_NO_ATTEMPTS = 5;

    private final PurchaseOrderMapper purchaseOrderMapper;
    private final SupplierMapper supplierMapper;
    private final ProductMapper productMapper;
    private final WarehouseMapper warehouseMapper;
    private final InventoryMapper inventoryMapper;
    private final InboundRecordMapper inboundRecordMapper;
    private final SysUserMapper sysUserMapper;
    private final InventoryEmbeddingSyncService inventoryEmbeddingSyncService;

    @Override
    public PageResult<PurchaseOrder> getPurchaseOrderList(PageQuery pageQuery) {
        LambdaQueryWrapper<PurchaseOrder> queryWrapper = buildPurchaseOrderFilter(pageQuery);

        if (StringUtils.hasText(pageQuery.getSort())) {
            if ("asc".equalsIgnoreCase(pageQuery.getOrder())) {
                queryWrapper.orderByAsc(PurchaseOrder::getCreateTime);
            } else {
                queryWrapper.orderByDesc(PurchaseOrder::getCreateTime);
            }
        } else {
            queryWrapper.orderByDesc(PurchaseOrder::getCreateTime);
        }

        Page<PurchaseOrder> page = new Page<>(pageQuery.getPage(), pageQuery.getSize());
        Page<PurchaseOrder> result = purchaseOrderMapper.selectPage(page, queryWrapper);
        enrichPurchaseOrderProductImages(result.getRecords());

        Map<String, Object> summary = aggregatePurchaseFinancials(pageQuery);
        return PageResult.build(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords(), summary);
    }

    private LambdaQueryWrapper<PurchaseOrder> buildPurchaseOrderFilter(PageQuery pageQuery) {
        LambdaQueryWrapper<PurchaseOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PurchaseOrder::getDeleted, 0);

        if (pageQuery.getSupplierId() != null) {
            queryWrapper.eq(PurchaseOrder::getSupplierId, pageQuery.getSupplierId());
        }
        if (pageQuery.getProductId() != null) {
            queryWrapper.eq(PurchaseOrder::getProductId, pageQuery.getProductId());
        }
        if (StringUtils.hasText(pageQuery.getInboundStatus())) {
            queryWrapper.eq(PurchaseOrder::getInboundStatus, pageQuery.getInboundStatus());
        }
        if (StringUtils.hasText(pageQuery.getPayStatus())) {
            queryWrapper.eq(PurchaseOrder::getPayStatus, pageQuery.getPayStatus());
        }
        LocalDate expectStart = parseLocalDate(pageQuery.getExpectDateStart());
        LocalDate expectEnd = parseLocalDate(pageQuery.getExpectDateEnd());
        if (expectStart != null) {
            queryWrapper.ge(PurchaseOrder::getExpectDate, expectStart);
        }
        if (expectEnd != null) {
            queryWrapper.le(PurchaseOrder::getExpectDate, expectEnd);
        }

        if (StringUtils.hasText(pageQuery.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper
                    .like(PurchaseOrder::getOrderNo, pageQuery.getKeyword())
                    .or()
                    .like(PurchaseOrder::getProductName, pageQuery.getKeyword())
                    .or()
                    .like(PurchaseOrder::getSupplierName, pageQuery.getKeyword())
            );
        }
        return queryWrapper;
    }

    /**
     * 与 {@link #buildPurchaseOrderFilter} 条件一致，使用 {@link QueryWrapper} 以便书写 SUM/CASE 聚合字段
     * （{@link LambdaQueryWrapper#select} 仅接受列方法引用，不接受原始 SQL 片段）。
     */
    private QueryWrapper<PurchaseOrder> buildPurchaseOrderFilterQuery(PageQuery pageQuery) {
        QueryWrapper<PurchaseOrder> q = new QueryWrapper<>();
        q.eq("deleted", 0);
        if (pageQuery.getSupplierId() != null) {
            q.eq("supplier_id", pageQuery.getSupplierId());
        }
        if (pageQuery.getProductId() != null) {
            q.eq("product_id", pageQuery.getProductId());
        }
        if (StringUtils.hasText(pageQuery.getInboundStatus())) {
            q.eq("inbound_status", pageQuery.getInboundStatus());
        }
        if (StringUtils.hasText(pageQuery.getPayStatus())) {
            q.eq("pay_status", pageQuery.getPayStatus());
        }
        LocalDate expectStart = parseLocalDate(pageQuery.getExpectDateStart());
        LocalDate expectEnd = parseLocalDate(pageQuery.getExpectDateEnd());
        if (expectStart != null) {
            q.ge("expect_date", expectStart);
        }
        if (expectEnd != null) {
            q.le("expect_date", expectEnd);
        }
        if (StringUtils.hasText(pageQuery.getKeyword())) {
            String kw = pageQuery.getKeyword();
            q.and(w -> w.like("order_no", kw)
                    .or()
                    .like("product_name", kw)
                    .or()
                    .like("supplier_name", kw));
        }
        return q;
    }

    /**
     * 在数据库侧聚合金额，避免对全量匹配行做 selectList（供应商详情等场景下订单多时可达数秒）。
     */
    private Map<String, Object> aggregatePurchaseFinancials(PageQuery pageQuery) {
        QueryWrapper<PurchaseOrder> w = buildPurchaseOrderFilterQuery(pageQuery);
        w.select(
                "COALESCE(SUM(amount),0) AS totalAmount",
                "COALESCE(SUM(CASE WHEN pay_status = 'unpaid' THEN COALESCE(amount,0) ELSE 0 END),0) AS unpaidAmount",
                "COALESCE(SUM(CASE WHEN pay_status = 'paid' THEN COALESCE(amount,0) ELSE 0 END),0) AS paidAmount",
                "COALESCE(SUM(CASE WHEN pay_status = 'refunded' THEN COALESCE(amount,0) ELSE 0 END),0) AS refundedAmount"
        );
        List<Map<String, Object>> maps = purchaseOrderMapper.selectMaps(w);
        Map<String, Object> row = maps == null || maps.isEmpty() ? Collections.emptyMap() : maps.get(0);
        Map<String, Object> m = new HashMap<>();
        m.put("totalAmount", mapDecimal(row, "totalAmount"));
        m.put("unpaidAmount", mapDecimal(row, "unpaidAmount"));
        m.put("paidAmount", mapDecimal(row, "paidAmount"));
        m.put("refundedAmount", mapDecimal(row, "refundedAmount"));
        return m;
    }

    private static BigDecimal mapDecimal(Map<String, Object> row, String key) {
        if (row == null || key == null) {
            return BigDecimal.ZERO;
        }
        Object v = row.get(key);
        if (v == null) {
            for (Map.Entry<String, Object> e : row.entrySet()) {
                if (e.getKey() != null && e.getKey().equalsIgnoreCase(key)) {
                    v = e.getValue();
                    break;
                }
            }
        }
        if (v == null) {
            return BigDecimal.ZERO;
        }
        if (v instanceof BigDecimal bd) {
            return bd;
        }
        if (v instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        try {
            return new BigDecimal(v.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private static LocalDate parseLocalDate(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        try {
            return LocalDate.parse(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public PurchaseOrder getPurchaseOrderById(Long id) {
        PurchaseOrder order = purchaseOrderMapper.selectById(id);
        if (order == null || order.getDeleted() == 1) {
            throw new BusinessException("采购订单不存在");
        }
        hydratePurchaseOrderDetailFields(order);
        return order;
    }

    private void hydratePurchaseOrderDetailFields(PurchaseOrder order) {
        if (order == null) {
            return;
        }
        Product product = null;
        if (order.getProductId() != null) {
            product = productMapper.selectById(order.getProductId());
            if (product != null && product.getDeleted() != 1) {
                if (!StringUtils.hasText(order.getProductName())) {
                    order.setProductName(product.getName());
                }
                order.setProductImage(product.getImage());
                order.setCategoryId(product.getCategoryId());
                order.setCategoryName(product.getCategoryName());
            }
        }
        if (order.getSupplierId() != null && !StringUtils.hasText(order.getSupplierName())) {
            Supplier supplier = supplierMapper.selectById(order.getSupplierId());
            if (supplier != null && supplier.getDeleted() != 1) {
                order.setSupplierName(supplier.getName());
            }
        }
        if (order.getWarehouseId() != null && !StringUtils.hasText(order.getWarehouseName())) {
            Warehouse warehouse = warehouseMapper.selectById(order.getWarehouseId());
            if (warehouse != null && warehouse.getDeleted() != 1) {
                order.setWarehouseName(warehouse.getName());
            }
        }
        if (order.getOperatorId() != null && !StringUtils.hasText(order.getOperatorName())) {
            SysUser user = sysUserMapper.selectById(order.getOperatorId());
            if (user != null) {
                order.setOperatorName(user.getName());
            }
        }
        // 详情展示：入库仓库以实际入库流水为准（表内字段可能未维护或与真实入库不一致）
        reconcilePurchaseOrderWarehouseFromInboundRecords(order);
    }

    /**
     * 按本单关联的 {@link InboundRecord} 聚合仓库：多仓时名称用「、」拼接，仅单仓时写入 warehouseId。
     * 无入库记录时不修改（沿用表内约定仓或空）。用于详情读接口与 {@link #confirmInbound} 落库同步。
     */
    private void reconcilePurchaseOrderWarehouseFromInboundRecords(PurchaseOrder order) {
        if (order == null || order.getId() == null) {
            return;
        }
        LambdaQueryWrapper<InboundRecord> q = new LambdaQueryWrapper<>();
        q.eq(InboundRecord::getPurchaseOrderId, order.getId());
        q.eq(InboundRecord::getDeleted, 0);
        q.orderByAsc(InboundRecord::getCreateTime);
        List<InboundRecord> rows = inboundRecordMapper.selectList(q);
        if (rows == null || rows.isEmpty()) {
            return;
        }
        LinkedHashSet<String> names = new LinkedHashSet<>();
        LinkedHashSet<Long> distinctWarehouseIds = new LinkedHashSet<>();
        for (InboundRecord r : rows) {
            String nm = null;
            if (StringUtils.hasText(r.getWarehouseName())) {
                nm = r.getWarehouseName().trim();
            } else if (r.getWarehouseId() != null) {
                Warehouse w = warehouseMapper.selectById(r.getWarehouseId());
                if (w != null && w.getDeleted() != 1 && StringUtils.hasText(w.getName())) {
                    nm = w.getName().trim();
                }
            }
            if (StringUtils.hasText(nm)) {
                names.add(nm);
            }
            if (r.getWarehouseId() != null) {
                distinctWarehouseIds.add(r.getWarehouseId());
            }
        }
        if (names.isEmpty()) {
            return;
        }
        order.setWarehouseName(String.join("、", names));
        if (distinctWarehouseIds.size() == 1) {
            order.setWarehouseId(distinctWarehouseIds.iterator().next());
        } else {
            order.setWarehouseId(null);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPurchaseOrder(PurchaseOrderDTO dto) {
        // 验证供应商
        Supplier supplier = supplierMapper.selectById(dto.getSupplierId());
        if (supplier == null || supplier.getDeleted() == 1) {
            throw new BusinessException("供应商不存在");
        }

        // 验证商品（停售不可新建采购单）
        Product product = productMapper.selectById(dto.getProductId());
        assertProductOrderableForPurchase(product, null);

        // 创建采购订单（单号按数据库当日最大值递增，避免与内存计数器冲突；并发冲突时重试）
        PurchaseOrder order = new PurchaseOrder();
        order.setSupplierId(dto.getSupplierId());
        order.setSupplierName(supplier.getName());
        order.setProductId(dto.getProductId());
        order.setProductName(product.getName());
        order.setSku(product.getCode());
        order.setUnitPrice(dto.getUnitPrice());
        order.setTotalQuantity(dto.getQuantity());
        order.setPendingQuantity(dto.getQuantity());
        order.setInboundQuantity(0);
        order.setAmount(dto.getUnitPrice().multiply(new BigDecimal(dto.getQuantity())));
        order.setInboundStatus("pending"); // 待入库
        order.setPayStatus("unpaid"); // 未付款
        order.setPayMethod(dto.getPayMethod());
        order.setExpectDate(dto.getExpectDate());
        order.setRemark(dto.getRemark());
        Long operatorId = UserContext.getCurrentUserId();
        if (operatorId != null) {
            order.setOperatorId(operatorId);
            SysUser user = sysUserMapper.selectById(operatorId);
            if (user != null) {
                order.setOperatorName(user.getName());
            }
        }

        for (int attempt = 0; attempt < MAX_PURCHASE_ORDER_NO_ATTEMPTS; attempt++) {
            order.setOrderNo(allocatePurchaseOrderNo());
            try {
                purchaseOrderMapper.insert(order);
                log.info("创建采购订单成功：orderNo={}", order.getOrderNo());
                return;
            } catch (DuplicateKeyException e) {
                log.warn("采购单号冲突，重新分配 attempt={} orderNo={}", attempt + 1, order.getOrderNo());
                if (attempt == MAX_PURCHASE_ORDER_NO_ATTEMPTS - 1) {
                    throw new BusinessException("采购单号生成失败，请稍后重试");
                }
            }
        }
    }

    private void assertProductOrderableForPurchase(Product product, Long previousOrderProductId) {
        if (product == null || product.getDeleted() == 1) {
            throw new BusinessException("商品不存在");
        }
        if (!StatusNameResolver.isProductOffSale(product.getStatus())) {
            return;
        }
        if (previousOrderProductId != null && previousOrderProductId.equals(product.getId())) {
            return;
        }
        throw new BusinessException("该商品已停售，无法用于新建采购单或更换为该商品");
    }

    /**
     * 生成当日唯一采购单号，与 {@link CodeGenerator#generatePurchaseOrderNo()} 格式一致。
     */
    private String allocatePurchaseOrderNo() {
        String dateStr = LocalDate.now().format(PURCHASE_ORDER_DATE);
        String prefix = CodeGenerator.PURCHASE_PREFIX + dateStr;
        String maxNo = purchaseOrderMapper.selectMaxOrderNoByDatePrefix(prefix);
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
            throw new BusinessException("当日采购单序号已用尽，请联系管理员");
        }
        return prefix + String.format("%04d", nextSeq);
    }

    /**
     * 生成当日唯一入库单号，与 {@link CodeGenerator#generateInboundOrderNo()} 格式一致。
     */
    private String allocateInboundRecordOrderNo() {
        String dateStr = LocalDate.now().format(PURCHASE_ORDER_DATE);
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

    private void insertInboundRecordWithAllocatedOrderNo(InboundRecord record) {
        for (int attempt = 0; attempt < MAX_INBOUND_RECORD_ORDER_NO_ATTEMPTS; attempt++) {
            record.setOrderNo(allocateInboundRecordOrderNo());
            try {
                inboundRecordMapper.insert(record);
                return;
            } catch (DuplicateKeyException e) {
                log.warn("入库单号冲突，重新分配 attempt={} orderNo={}", attempt + 1, record.getOrderNo());
                if (attempt == MAX_INBOUND_RECORD_ORDER_NO_ATTEMPTS - 1) {
                    throw new BusinessException("入库单号生成失败，请稍后重试");
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePurchaseOrder(Long id, PurchaseOrderDTO dto) {
        PurchaseOrder order = getPurchaseOrderById(id);

        // 只有待入库状态可以修改
        if (!"pending".equals(order.getInboundStatus())) {
            throw new BusinessException("只有待入库状态的订单可以修改");
        }

        // 验证供应商
        Supplier supplier = supplierMapper.selectById(dto.getSupplierId());
        if (supplier == null || supplier.getDeleted() == 1) {
            throw new BusinessException("供应商不存在");
        }

        // 验证商品（停售仅允许订单未更换商品时保留）
        Product product = productMapper.selectById(dto.getProductId());
        assertProductOrderableForPurchase(product, order.getProductId());

        order.setSupplierId(dto.getSupplierId());
        order.setSupplierName(supplier.getName());
        order.setProductId(dto.getProductId());
        order.setProductName(product.getName());
        order.setSku(product.getCode());
        order.setUnitPrice(dto.getUnitPrice());
        order.setTotalQuantity(dto.getQuantity());
        order.setPendingQuantity(dto.getQuantity());
        order.setAmount(dto.getUnitPrice().multiply(new BigDecimal(dto.getQuantity())));
        if (dto.getPayMethod() != null) {
            order.setPayMethod(dto.getPayMethod());
        }
        if (StringUtils.hasText(dto.getPayStatus())) {
            String payStatus = dto.getPayStatus().trim();
            if (!"unpaid".equals(payStatus) && !"paid".equals(payStatus) && !"refunded".equals(payStatus)) {
                throw new BusinessException("付款状态非法");
            }
            order.setPayStatus(payStatus);
        }
        if (dto.getExpectDate() != null) {
            order.setExpectDate(dto.getExpectDate());
        }
        if (dto.getRemark() != null) {
            order.setRemark(dto.getRemark());
        }
        Long operatorId = UserContext.getCurrentUserId();
        if (operatorId != null) {
            order.setOperatorId(operatorId);
            SysUser user = sysUserMapper.selectById(operatorId);
            if (user != null) {
                order.setOperatorName(user.getName());
            }
        }

        purchaseOrderMapper.updateById(order);
        log.info("更新采购订单成功：id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmInbound(Long orderId, InboundDTO dto) {
        PurchaseOrder order = getPurchaseOrderById(orderId);

        // 验证订单状态
        if ("completed".equals(order.getInboundStatus())) {
            throw new BusinessException("订单已完成入库");
        }
        if ("cancelled".equals(order.getInboundStatus())) {
            throw new BusinessException("订单已取消");
        }

        // 验证仓库
        Warehouse warehouse = warehouseMapper.selectById(dto.getWarehouseId());
        if (warehouse == null || warehouse.getDeleted() == 1) {
            throw new BusinessException("仓库不存在");
        }

        // 验证入库数量
        if (dto.getQuantity() > order.getPendingQuantity()) {
            throw new BusinessException("入库数量不能超过待入库数量");
        }

        Product product = productMapper.selectById(order.getProductId());
        if (product == null || product.getDeleted() == 1) {
            throw new BusinessException("商品不存在");
        }

        // 创建入库记录
        InboundRecord record = new InboundRecord();
        record.setPurchaseOrderId(orderId);
        record.setPurchaseOrderNo(order.getOrderNo());
        record.setSupplierId(order.getSupplierId());
        record.setSupplierName(order.getSupplierName());
        record.setProductId(order.getProductId());
        record.setProductName(order.getProductName());
        record.setSku(order.getSku());
        record.setWarehouseId(dto.getWarehouseId());
        record.setWarehouseName(warehouse.getName());
        record.setQuantity(dto.getQuantity());
        record.setUnitPrice(order.getUnitPrice());
        record.setAmount(order.getUnitPrice().multiply(new BigDecimal(dto.getQuantity())));
        record.setBatchNo(dto.getBatchNo());
        record.setRemark(dto.getRemark());

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

        // 更新库存
        LambdaQueryWrapper<Inventory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Inventory::getProductId, order.getProductId());
        queryWrapper.eq(Inventory::getWarehouseId, dto.getWarehouseId());
        queryWrapper.eq(Inventory::getDeleted, 0);
        Inventory inventory = inventoryMapper.selectOne(queryWrapper);

        if (inventory == null) {
            // 新增库存记录（与手动入库等路径一致：冗余商品规格/分类，供库存详情展示）
            inventory = new Inventory();
            inventory.setProductId(order.getProductId());
            inventory.setSku(order.getSku());
            inventory.setProductName(order.getProductName());
            inventory.setSpec(product.getSpec());
            inventory.setCategory(product.getCategoryName());
            inventory.setWarehouseId(dto.getWarehouseId());
            inventory.setWarehouseName(warehouse.getName());
            inventory.setStock(dto.getQuantity());
            inventory.setSafeStock(10);
            BigDecimal unit = order.getUnitPrice() != null ? order.getUnitPrice() : product.getCostPrice();
            inventory.setCostPrice(unit);
            if (unit != null) {
                inventory.setStockValue(unit.multiply(new BigDecimal(dto.getQuantity())));
            }
            InventoryStatusUtil.applyStatus(inventory);
            inventory.setLastInboundTime(LocalDateTime.now());
            inventoryMapper.insert(inventory);
            inventoryEmbeddingSyncService.indexInventoryRow(inventory);
        } else {
            // 更新库存数量；历史采购入库可能未写入 spec，在此补全
            int currentStock = inventory.getStock() != null ? inventory.getStock() : 0;
            int newStock = currentStock + dto.getQuantity();
            inventory.setStock(newStock);
            inventory.setLastInboundTime(LocalDateTime.now());
            if (!StringUtils.hasText(inventory.getSpec())) {
                inventory.setSpec(product.getSpec());
            }
            if (!StringUtils.hasText(inventory.getCategory())) {
                inventory.setCategory(product.getCategoryName());
            }
            BigDecimal effectiveCostPrice = inventory.getCostPrice() != null
                    ? inventory.getCostPrice()
                    : (order.getUnitPrice() != null ? order.getUnitPrice() : product.getCostPrice());
            if (inventory.getCostPrice() == null && effectiveCostPrice != null) {
                inventory.setCostPrice(effectiveCostPrice);
            }
            if (effectiveCostPrice != null) {
                inventory.setStockValue(effectiveCostPrice.multiply(new BigDecimal(newStock)));
            }
            InventoryStatusUtil.applyStatus(inventory);
            inventoryMapper.updateById(inventory);
        }

        // 更新采购订单入库状态
        order.setInboundQuantity(order.getInboundQuantity() + dto.getQuantity());
        order.setPendingQuantity(order.getPendingQuantity() - dto.getQuantity());

        if (order.getInboundQuantity() >= order.getTotalQuantity()) {
            order.setInboundStatus("completed"); // 已完成
        } else {
            order.setInboundStatus("partial"); // 部分入库
        }
        // 采购单入库仓库与流水一致（库存页/采购页选采购单入库均走本方法）
        reconcilePurchaseOrderWarehouseFromInboundRecords(order);
        purchaseOrderMapper.updateById(order);

        log.info("确认入库成功：orderId={}, quantity={}", orderId, dto.getQuantity());
    }

    @Override
    public PageResult<InboundRecord> getInboundRecordList(PageQuery pageQuery) {
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

        LambdaQueryWrapper<InboundRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InboundRecord::getDeleted, 0);

        if (effectiveWarehouseId != null) {
            queryWrapper.eq(InboundRecord::getWarehouseId, effectiveWarehouseId);
        }
        if (effectiveProductId != null) {
            queryWrapper.eq(InboundRecord::getProductId, effectiveProductId);
        }
        if (StringUtils.hasText(pageQuery.getOperatorName())) {
            queryWrapper.like(InboundRecord::getOperatorName, pageQuery.getOperatorName().trim());
        }
        LocalDateTime inboundStart = parseDayStart(pageQuery.getCreateTimeStart());
        LocalDateTime inboundEnd = parseDayEnd(pageQuery.getCreateTimeEnd());
        if (inboundStart != null) {
            queryWrapper.ge(InboundRecord::getCreateTime, inboundStart);
        }
        if (inboundEnd != null) {
            queryWrapper.le(InboundRecord::getCreateTime, inboundEnd);
        }

        if (StringUtils.hasText(pageQuery.getKeyword())) {
            String kw = pageQuery.getKeyword().trim();
            queryWrapper.and(wrapper -> wrapper
                    .like(InboundRecord::getOrderNo, kw)
                    .or()
                    .like(InboundRecord::getProductName, kw)
                    .or()
                    .like(InboundRecord::getWarehouseName, kw)
                    .or()
                    .like(InboundRecord::getPurchaseOrderNo, kw)
                    .or()
                    .like(InboundRecord::getSupplierName, kw)
            );
        }

        queryWrapper.orderByDesc(InboundRecord::getCreateTime);

        Page<InboundRecord> page = new Page<>(pageQuery.getPage(), pageQuery.getSize());
        Page<InboundRecord> result = inboundRecordMapper.selectPage(page, queryWrapper);
        enrichInboundRecordProductImages(result.getRecords());

        return PageResult.build(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    private static LocalDateTime parseDayStart(String day) {
        LocalDate d = parseLocalDate(day);
        return d == null ? null : d.atStartOfDay();
    }

    private static LocalDateTime parseDayEnd(String day) {
        LocalDate d = parseLocalDate(day);
        return d == null ? null : LocalDateTime.of(d, LocalTime.of(23, 59, 59));
    }

    @Override
    public InboundRecord getInboundRecordById(Long id) {
        InboundRecord record = inboundRecordMapper.selectById(id);
        if (record == null || record.getDeleted() == 1) {
            throw new BusinessException("入库记录不存在");
        }
        enrichInboundRecordProductImages(List.of(record));
        return record;
    }

    private void enrichPurchaseOrderProductImages(List<PurchaseOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        Map<Long, String> imageMap = queryProductImageMap(orders.stream()
                .map(PurchaseOrder::getProductId)
                .toList());
        for (PurchaseOrder order : orders) {
            if (order == null || order.getProductId() == null) {
                continue;
            }
            order.setProductImage(imageMap.get(order.getProductId()));
        }
    }

    private void enrichInboundRecordProductImages(List<InboundRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Map<Long, String> imageMap = queryProductImageMap(records.stream()
                .map(InboundRecord::getProductId)
                .toList());
        for (InboundRecord record : records) {
            if (record == null || record.getProductId() == null) {
                continue;
            }
            record.setProductImage(imageMap.get(record.getProductId()));
        }
    }

    private Map<Long, String> queryProductImageMap(List<Long> productIds) {
        Set<Long> validProductIds = new HashSet<>();
        if (productIds != null) {
            for (Long productId : productIds) {
                if (productId != null) {
                    validProductIds.add(productId);
                }
            }
        }
        if (validProductIds.isEmpty()) {
            return new HashMap<>();
        }

        LambdaQueryWrapper<Product> productQuery = new LambdaQueryWrapper<>();
        productQuery.select(Product::getId, Product::getImage);
        productQuery.in(Product::getId, validProductIds);
        productQuery.eq(Product::getDeleted, 0);
        List<Product> products = productMapper.selectList(productQuery);

        Map<Long, String> imageMap = new HashMap<>();
        for (Product product : products) {
            if (product != null && product.getId() != null) {
                imageMap.put(product.getId(), product.getImage());
            }
        }
        return imageMap;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelPurchaseOrder(Long id) {
        PurchaseOrder order = getPurchaseOrderById(id);
        if (!"pending".equals(order.getInboundStatus())) {
            throw new BusinessException("只有待入库状态的订单可以取消");
        }
        order.setInboundStatus("cancelled");
        purchaseOrderMapper.updateById(order);
        log.info("取消采购订单成功：id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePurchaseOrder(Long id) {
        PurchaseOrder order = getPurchaseOrderById(id);
        if (!"pending".equals(order.getInboundStatus())) {
            throw new BusinessException("只有待入库状态的订单可以删除");
        }
        purchaseOrderMapper.deleteById(id);
        log.info("删除采购订单成功：id={}", id);
    }

    @Override
    public PurchaseStatsVO getPurchaseStats() {
        PurchaseStatsVO stats = new PurchaseStatsVO();

        // 本月采购金额
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        LambdaQueryWrapper<PurchaseOrder> monthQuery = new LambdaQueryWrapper<>();
        monthQuery.eq(PurchaseOrder::getDeleted, 0)
                .ge(PurchaseOrder::getCreateTime, monthStart)
                .le(PurchaseOrder::getCreateTime, monthEnd);

        BigDecimal monthAmount = purchaseOrderMapper.selectList(monthQuery)
                .stream()
                .map(PurchaseOrder::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setMonthAmount(monthAmount);

        // 待入库订单数量
        LambdaQueryWrapper<PurchaseOrder> pendingQuery = new LambdaQueryWrapper<>();
        pendingQuery.eq(PurchaseOrder::getDeleted, 0)
                .eq(PurchaseOrder::getInboundStatus, "pending");
        Long pendingCount = purchaseOrderMapper.selectCount(pendingQuery);
        stats.setPendingInboundCount(pendingCount.intValue());

        // 合作供应商数量
        LambdaQueryWrapper<Supplier> supplierQuery = new LambdaQueryWrapper<>();
        supplierQuery.eq(Supplier::getDeleted, 0);
        Long supplierCount = supplierMapper.selectCount(supplierQuery);
        stats.setSupplierCount(supplierCount.intValue());

        // 待付款订单数量和金额
        LambdaQueryWrapper<PurchaseOrder> unpaidQuery = new LambdaQueryWrapper<>();
        unpaidQuery.eq(PurchaseOrder::getDeleted, 0)
                .eq(PurchaseOrder::getPayStatus, "unpaid");
        Long unpaidCount = purchaseOrderMapper.selectCount(unpaidQuery);
        stats.setUnpaidCount(unpaidCount.intValue());

        BigDecimal unpaidAmount = purchaseOrderMapper.selectList(unpaidQuery)
                .stream()
                .map(PurchaseOrder::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setUnpaidAmount(unpaidAmount);

        return stats;
    }

}