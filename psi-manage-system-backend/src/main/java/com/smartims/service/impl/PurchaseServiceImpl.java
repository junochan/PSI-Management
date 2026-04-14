package com.smartims.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private Map<String, Object> aggregatePurchaseFinancials(PageQuery pageQuery) {
        LambdaQueryWrapper<PurchaseOrder> w = buildPurchaseOrderFilter(pageQuery);
        w.select(PurchaseOrder::getAmount, PurchaseOrder::getPayStatus);
        List<PurchaseOrder> rows = purchaseOrderMapper.selectList(w);
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal unpaid = BigDecimal.ZERO;
        BigDecimal paid = BigDecimal.ZERO;
        BigDecimal refunded = BigDecimal.ZERO;
        for (PurchaseOrder o : rows) {
            BigDecimal amt = o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO;
            total = total.add(amt);
            String ps = o.getPayStatus();
            if ("unpaid".equals(ps)) {
                unpaid = unpaid.add(amt);
            } else if ("paid".equals(ps)) {
                paid = paid.add(amt);
            } else if ("refunded".equals(ps)) {
                refunded = refunded.add(amt);
            }
        }
        Map<String, Object> m = new HashMap<>();
        m.put("totalAmount", total);
        m.put("unpaidAmount", unpaid);
        m.put("paidAmount", paid);
        m.put("refundedAmount", refunded);
        return m;
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
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPurchaseOrder(PurchaseOrderDTO dto) {
        // 验证供应商
        Supplier supplier = supplierMapper.selectById(dto.getSupplierId());
        if (supplier == null || supplier.getDeleted() == 1) {
            throw new BusinessException("供应商不存在");
        }

        // 验证商品
        Product product = productMapper.selectById(dto.getProductId());
        if (product == null || product.getDeleted() == 1) {
            throw new BusinessException("商品不存在");
        }

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
        order.setWarehouseId(dto.getWarehouseId());
        order.setRemark(dto.getRemark());

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

        // 验证商品
        Product product = productMapper.selectById(dto.getProductId());
        if (product == null || product.getDeleted() == 1) {
            throw new BusinessException("商品不存在");
        }

        order.setSupplierId(dto.getSupplierId());
        order.setSupplierName(supplier.getName());
        order.setProductId(dto.getProductId());
        order.setProductName(product.getName());
        order.setSku(product.getCode());
        order.setUnitPrice(dto.getUnitPrice());
        order.setTotalQuantity(dto.getQuantity());
        order.setPendingQuantity(dto.getQuantity());
        order.setAmount(dto.getUnitPrice().multiply(new BigDecimal(dto.getQuantity())));
        order.setPayMethod(dto.getPayMethod());
        order.setExpectDate(dto.getExpectDate());
        order.setWarehouseId(dto.getWarehouseId());
        order.setRemark(dto.getRemark());

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

        // 创建入库记录
        InboundRecord record = new InboundRecord();
        record.setOrderNo(CodeGenerator.generateInboundOrderNo());
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

        inboundRecordMapper.insert(record);

        // 更新库存
        LambdaQueryWrapper<Inventory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Inventory::getProductId, order.getProductId());
        queryWrapper.eq(Inventory::getWarehouseId, dto.getWarehouseId());
        queryWrapper.eq(Inventory::getDeleted, 0);
        Inventory inventory = inventoryMapper.selectOne(queryWrapper);

        if (inventory == null) {
            // 新增库存记录
            inventory = new Inventory();
            inventory.setProductId(order.getProductId());
            inventory.setSku(order.getSku());
            inventory.setProductName(order.getProductName());
            inventory.setWarehouseId(dto.getWarehouseId());
            inventory.setWarehouseName(warehouse.getName());
            inventory.setStock(dto.getQuantity());
            inventory.setSafeStock(10);
            inventory.setStatus("normal");
            inventory.setLastInboundTime(LocalDateTime.now());
            inventoryMapper.insert(inventory);
            inventoryEmbeddingSyncService.indexInventoryRow(inventory);
        } else {
            // 更新库存数量
            inventory.setStock(inventory.getStock() + dto.getQuantity());
            inventory.setLastInboundTime(LocalDateTime.now());
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
        purchaseOrderMapper.updateById(order);

        log.info("确认入库成功：orderId={}, quantity={}", orderId, dto.getQuantity());
    }

    @Override
    public PageResult<InboundRecord> getInboundRecordList(PageQuery pageQuery) {
        LambdaQueryWrapper<InboundRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InboundRecord::getDeleted, 0);

        if (pageQuery.getWarehouseId() != null) {
            queryWrapper.eq(InboundRecord::getWarehouseId, pageQuery.getWarehouseId());
        }
        if (StringUtils.hasText(pageQuery.getOperatorName())) {
            queryWrapper.eq(InboundRecord::getOperatorName, pageQuery.getOperatorName());
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
            queryWrapper.and(wrapper -> wrapper
                    .like(InboundRecord::getOrderNo, pageQuery.getKeyword())
                    .or()
                    .like(InboundRecord::getProductName, pageQuery.getKeyword())
                    .or()
                    .like(InboundRecord::getWarehouseName, pageQuery.getKeyword())
                    .or()
                    .like(InboundRecord::getPurchaseOrderNo, pageQuery.getKeyword())
            );
        }

        queryWrapper.orderByDesc(InboundRecord::getCreateTime);

        Page<InboundRecord> page = new Page<>(pageQuery.getPage(), pageQuery.getSize());
        Page<InboundRecord> result = inboundRecordMapper.selectPage(page, queryWrapper);

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
        return record;
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