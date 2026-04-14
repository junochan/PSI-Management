package com.smartims.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartims.common.PageResult;
import com.smartims.dto.PageQuery;
import com.smartims.dto.SalesOrderDTO;
import com.smartims.dto.ShippingDTO;
import com.smartims.entity.*;
import com.smartims.exception.BusinessException;
import com.smartims.mapper.*;
import com.smartims.security.UserContext;
import com.smartims.service.SalesService;
import com.smartims.util.CodeGenerator;
import com.smartims.vo.SalesStatsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 销售服务实现类
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalesServiceImpl implements SalesService {

    private final SalesOrderMapper salesOrderMapper;
    private final CustomerMapper customerMapper;
    private final ProductMapper productMapper;
    private final WarehouseMapper warehouseMapper;
    private final InventoryMapper inventoryMapper;
    private final AftersalesOrderMapper aftersalesOrderMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public PageResult<SalesOrder> getSalesOrderList(PageQuery pageQuery) {
        LambdaQueryWrapper<SalesOrder> queryWrapper = buildSalesOrderFilter(pageQuery);

        if (StringUtils.hasText(pageQuery.getSort())) {
            if ("asc".equalsIgnoreCase(pageQuery.getOrder())) {
                queryWrapper.orderByAsc(SalesOrder::getCreateTime);
            } else {
                queryWrapper.orderByDesc(SalesOrder::getCreateTime);
            }
        } else {
            queryWrapper.orderByDesc(SalesOrder::getCreateTime);
        }

        Page<SalesOrder> page = new Page<>(pageQuery.getPage(), pageQuery.getSize());
        Page<SalesOrder> result = salesOrderMapper.selectPage(page, queryWrapper);

        Map<String, Object> summary = aggregateSalesFinancials(pageQuery);
        return PageResult.build(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords(), summary);
    }

    private LambdaQueryWrapper<SalesOrder> buildSalesOrderFilter(PageQuery pageQuery) {
        LambdaQueryWrapper<SalesOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SalesOrder::getDeleted, 0);

        if (pageQuery.getCustomerId() != null) {
            queryWrapper.eq(SalesOrder::getCustomerId, pageQuery.getCustomerId());
        }
        if (pageQuery.getProductId() != null) {
            queryWrapper.eq(SalesOrder::getProductId, pageQuery.getProductId());
        }
        if (StringUtils.hasText(pageQuery.getPayStatus())) {
            queryWrapper.eq(SalesOrder::getPayStatus, pageQuery.getPayStatus());
        }
        if (StringUtils.hasText(pageQuery.getSalesOrderStatus())) {
            queryWrapper.eq(SalesOrder::getStatus, pageQuery.getSalesOrderStatus());
        }
        LocalDateTime rangeStart = parseDayStart(pageQuery.getCreateTimeStart());
        LocalDateTime rangeEnd = parseDayEnd(pageQuery.getCreateTimeEnd());
        if (rangeStart != null) {
            queryWrapper.ge(SalesOrder::getCreateTime, rangeStart);
        }
        if (rangeEnd != null) {
            queryWrapper.le(SalesOrder::getCreateTime, rangeEnd);
        }

        if (StringUtils.hasText(pageQuery.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper
                    .like(SalesOrder::getOrderNo, pageQuery.getKeyword())
                    .or()
                    .like(SalesOrder::getProductName, pageQuery.getKeyword())
                    .or()
                    .like(SalesOrder::getCustomerName, pageQuery.getKeyword())
            );
        }
        return queryWrapper;
    }

    private Map<String, Object> aggregateSalesFinancials(PageQuery pageQuery) {
        LambdaQueryWrapper<SalesOrder> w = buildSalesOrderFilter(pageQuery);
        w.select(SalesOrder::getAmount, SalesOrder::getPayStatus);
        List<SalesOrder> rows = salesOrderMapper.selectList(w);
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal unpaid = BigDecimal.ZERO;
        BigDecimal paid = BigDecimal.ZERO;
        for (SalesOrder o : rows) {
            BigDecimal amt = o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO;
            total = total.add(amt);
            String ps = o.getPayStatus();
            if ("unpaid".equals(ps)) {
                unpaid = unpaid.add(amt);
            } else if ("paid".equals(ps)) {
                paid = paid.add(amt);
            }
        }
        Map<String, Object> m = new HashMap<>();
        m.put("totalAmount", total);
        m.put("unpaidAmount", unpaid);
        m.put("paidAmount", paid);
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

    private static LocalDateTime parseDayStart(String day) {
        LocalDate d = parseLocalDate(day);
        return d == null ? null : d.atStartOfDay();
    }

    private static LocalDateTime parseDayEnd(String day) {
        LocalDate d = parseLocalDate(day);
        return d == null ? null : LocalDateTime.of(d, LocalTime.of(23, 59, 59));
    }

    @Override
    public SalesOrder getSalesOrderById(Long id) {
        SalesOrder order = salesOrderMapper.selectById(id);
        if (order == null || order.getDeleted() == 1) {
            throw new BusinessException("销售订单不存在");
        }
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createSalesOrder(SalesOrderDTO dto) {
        // 验证客户
        Customer customer = customerMapper.selectById(dto.getCustomerId());
        if (customer == null || customer.getDeleted() == 1) {
            throw new BusinessException("客户不存在");
        }

        // 验证商品
        Product product = productMapper.selectById(dto.getProductId());
        if (product == null || product.getDeleted() == 1) {
            throw new BusinessException("商品不存在");
        }

        // 验证库存
        if (dto.getWarehouseId() != null) {
            LambdaQueryWrapper<Inventory> invQuery = new LambdaQueryWrapper<>();
            invQuery.eq(Inventory::getProductId, dto.getProductId());
            invQuery.eq(Inventory::getWarehouseId, dto.getWarehouseId());
            invQuery.eq(Inventory::getDeleted, 0);
            Inventory inventory = inventoryMapper.selectOne(invQuery);
            if (inventory == null || inventory.getStock() < dto.getQuantity()) {
                throw new BusinessException("库存不足");
            }
        }

        // 创建销售订单
        SalesOrder order = new SalesOrder();
        order.setOrderNo(CodeGenerator.generateSalesOrderNo());
        order.setCustomerId(dto.getCustomerId());
        order.setCustomerName(customer.getName());
        order.setCustomerType(customer.getType());
        order.setProductId(dto.getProductId());
        order.setProductName(product.getName());
        order.setSku(product.getCode());
        order.setUnitPrice(dto.getUnitPrice());
        order.setQuantity(dto.getQuantity());
        order.setShippedQuantity(0); // 初始发货数量为0
        order.setPendingQuantity(dto.getQuantity()); // 待发货数量等于订单数量
        order.setAmount(dto.getUnitPrice().multiply(new BigDecimal(dto.getQuantity())));
        order.setStatus("pending"); // 待发货
        order.setPayStatus("unpaid"); // 未付款
        order.setPayMethod(dto.getPayMethod());
        order.setWarehouseId(dto.getWarehouseId());
        order.setReceiverName(dto.getReceiverName());
        order.setReceiverPhone(dto.getReceiverPhone());
        order.setReceiverAddress(dto.getReceiverAddress());
        order.setRemark(dto.getRemark());

        // 设置操作人信息
        Long operatorId = UserContext.getCurrentUserId();
        if (operatorId != null) {
            order.setOperatorId(operatorId);
            SysUser user = sysUserMapper.selectById(operatorId);
            if (user != null) {
                order.setOperatorName(user.getName());
            }
        }

        salesOrderMapper.insert(order);
        log.info("创建销售订单成功：orderNo={}", order.getOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSalesOrder(Long id, SalesOrderDTO dto) {
        SalesOrder order = getSalesOrderById(id);

        // 只有待发货状态可以修改
        if (!"pending".equals(order.getStatus())) {
            throw new BusinessException("只有待发货状态的订单可以修改");
        }

        // 验证客户
        Customer customer = customerMapper.selectById(dto.getCustomerId());
        if (customer == null || customer.getDeleted() == 1) {
            throw new BusinessException("客户不存在");
        }

        // 验证商品
        Product product = productMapper.selectById(dto.getProductId());
        if (product == null || product.getDeleted() == 1) {
            throw new BusinessException("商品不存在");
        }

        order.setCustomerId(dto.getCustomerId());
        order.setCustomerName(customer.getName());
        order.setCustomerType(customer.getType());
        order.setProductId(dto.getProductId());
        order.setProductName(product.getName());
        order.setSku(product.getCode());
        order.setUnitPrice(dto.getUnitPrice());
        order.setQuantity(dto.getQuantity());
        // 更新待发货数量 = 总数量 - 已发货数量
        int shippedQty = order.getShippedQuantity() != null ? order.getShippedQuantity() : 0;
        order.setPendingQuantity(dto.getQuantity() - shippedQty);
        order.setAmount(dto.getUnitPrice().multiply(new BigDecimal(dto.getQuantity())));
        order.setPayMethod(dto.getPayMethod());
        order.setWarehouseId(dto.getWarehouseId());
        order.setReceiverName(dto.getReceiverName());
        order.setReceiverPhone(dto.getReceiverPhone());
        order.setReceiverAddress(dto.getReceiverAddress());
        order.setRemark(dto.getRemark());

        salesOrderMapper.updateById(order);
        log.info("更新销售订单成功：id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmPayment(Long orderId) {
        SalesOrder order = getSalesOrderById(orderId);

        // 验证订单状态
        if (!"pending".equals(order.getStatus())) {
            throw new BusinessException("只有待发货状态的订单可以确认付款");
        }

        // 验证付款状态
        if ("paid".equals(order.getPayStatus())) {
            throw new BusinessException("订单已付款，无需重复确认");
        }

        order.setPayStatus("paid"); // 已付款
        salesOrderMapper.updateById(order);
        log.info("确认付款成功：orderId={}", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmShipping(Long orderId, ShippingDTO dto) {
        SalesOrder order = getSalesOrderById(orderId);

        // 验证订单状态
        if (!"pending".equals(order.getStatus())) {
            throw new BusinessException("只有待发货状态的订单可以发货");
        }

        // 验证付款状态 - 只有已付款的订单才能发货
        if (!"paid".equals(order.getPayStatus())) {
            throw new BusinessException("只有已付款的订单才能发货，请先确认付款");
        }

        // 验证仓库
        Warehouse warehouse = warehouseMapper.selectById(dto.getWarehouseId());
        if (warehouse == null || warehouse.getDeleted() == 1) {
            throw new BusinessException("仓库不存在");
        }

        // 发货数量 - 使用DTO中的数量或订单数量
        int shipQuantity = dto.getQuantity() != null ? dto.getQuantity() : order.getQuantity();

        // 计算待发货数量：订单总数 - 已发货数量
        int shippedQty = order.getShippedQuantity() != null ? order.getShippedQuantity() : 0;
        int pendingQty = order.getQuantity() - shippedQty;

        // 如果订单有 pendingQuantity 且大于计算值，使用它（兼容旧数据）
        if (order.getPendingQuantity() != null && order.getPendingQuantity() > pendingQty) {
            pendingQty = order.getPendingQuantity();
        }

        if (pendingQty <= 0) {
            throw new BusinessException("订单已全部发货，无需再次发货");
        }

        if (shipQuantity > pendingQty) {
            throw new BusinessException("发货数量超过待发货数量，待发货数量：" + pendingQty);
        }

        // 验证库存
        LambdaQueryWrapper<Inventory> invQuery = new LambdaQueryWrapper<>();
        invQuery.eq(Inventory::getProductId, order.getProductId());
        invQuery.eq(Inventory::getWarehouseId, dto.getWarehouseId());
        invQuery.eq(Inventory::getDeleted, 0);
        Inventory inventory = inventoryMapper.selectOne(invQuery);

        if (inventory == null || inventory.getStock() < shipQuantity) {
            throw new BusinessException("库存不足，当前库存：" + (inventory != null ? inventory.getStock() : 0) + "，发货数量：" + shipQuantity);
        }

        // 更新库存
        inventory.setStock(inventory.getStock() - shipQuantity);
        inventory.setLastOutboundTime(LocalDateTime.now());
        inventoryMapper.updateById(inventory);

        // 更新订单发货信息
        int newShippedQty = shippedQty + shipQuantity;
        int newPendingQty = order.getQuantity() - newShippedQty;

        order.setShippedQuantity(newShippedQty);
        order.setPendingQuantity(newPendingQty);
        order.setWarehouseId(dto.getWarehouseId());
        order.setWarehouseName(warehouse.getName());
        order.setLogisticsCompany(dto.getLogisticsCompany());
        order.setLogisticsNo(dto.getLogisticsNo());
        order.setShipTime(LocalDateTime.now());
        if (dto.getRemark() != null) {
            order.setRemark(dto.getRemark());
        }

        // 如果全部发货完成，更新状态为已发货
        if (newPendingQty <= 0) {
            order.setStatus("shipped");
        }

        salesOrderMapper.updateById(order);
        log.info("确认发货成功：orderId={}, shipQuantity={}, shippedQuantity={}, pendingQuantity={}", orderId, shipQuantity, newShippedQty, newPendingQty);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceived(Long orderId) {
        SalesOrder order = getSalesOrderById(orderId);

        if (!"shipped".equals(order.getStatus())) {
            throw new BusinessException("只有已发货状态的订单可以确认收货");
        }

        order.setStatus("completed"); // 已完成
        salesOrderMapper.updateById(order);
        log.info("确认收货成功：orderId={}", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId) {
        SalesOrder order = getSalesOrderById(orderId);

        if (!"pending".equals(order.getStatus())) {
            throw new BusinessException("只有待发货状态的订单可以取消");
        }

        order.setStatus("cancelled"); // 已取消
        salesOrderMapper.updateById(order);
        log.info("取消订单成功：orderId={}", orderId);
    }

    @Override
    public SalesStatsVO getSalesStats() {
        SalesStatsVO stats = new SalesStatsVO();

        YearMonth currentMonth = YearMonth.now();
        YearMonth lastMonth = currentMonth.minusMonths(1);

        LocalDateTime currentMonthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime currentMonthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59);
        LocalDateTime lastMonthStart = lastMonth.atDay(1).atStartOfDay();
        LocalDateTime lastMonthEnd = lastMonth.atEndOfMonth().atTime(23, 59, 59);

        // 本月销售额（已完成和已发货的订单）- 支持中英文状态
        LambdaQueryWrapper<SalesOrder> currentMonthQuery = new LambdaQueryWrapper<>();
        currentMonthQuery.eq(SalesOrder::getDeleted, 0)
                .and(wrapper -> wrapper
                        .in(SalesOrder::getStatus, "completed", "shipped")
                        .or()
                        .in(SalesOrder::getStatus, "已完成", "已发货", "处理中")
                )
                .ge(SalesOrder::getCreateTime, currentMonthStart)
                .le(SalesOrder::getCreateTime, currentMonthEnd);

        BigDecimal monthAmount = salesOrderMapper.selectList(currentMonthQuery)
                .stream()
                .map(SalesOrder::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setMonthAmount(monthAmount);

        // 上月销售额 - 支持中英文状态
        LambdaQueryWrapper<SalesOrder> lastMonthQuery = new LambdaQueryWrapper<>();
        lastMonthQuery.eq(SalesOrder::getDeleted, 0)
                .and(wrapper -> wrapper
                        .in(SalesOrder::getStatus, "completed", "shipped")
                        .or()
                        .in(SalesOrder::getStatus, "已完成", "已发货", "处理中")
                )
                .ge(SalesOrder::getCreateTime, lastMonthStart)
                .le(SalesOrder::getCreateTime, lastMonthEnd);

        BigDecimal lastMonthAmount = salesOrderMapper.selectList(lastMonthQuery)
                .stream()
                .map(SalesOrder::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setLastMonthAmount(lastMonthAmount);

        // 计算销售额同比变化百分比
        if (lastMonthAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal change = monthAmount.subtract(lastMonthAmount)
                    .divide(lastMonthAmount, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100));
            stats.setAmountChangePercent(change);
        } else {
            stats.setAmountChangePercent(monthAmount.compareTo(BigDecimal.ZERO) > 0 ? new BigDecimal(100) : BigDecimal.ZERO);
        }

        // 本月成交订单数量
        Long monthOrderCount = salesOrderMapper.selectCount(currentMonthQuery);
        stats.setMonthOrderCount(monthOrderCount.intValue());

        // 上月成交订单数量
        Long lastMonthOrderCount = salesOrderMapper.selectCount(lastMonthQuery);
        stats.setLastMonthOrderCount(lastMonthOrderCount.intValue());

        // 计算订单同比变化百分比
        if (lastMonthOrderCount > 0) {
            BigDecimal change = new BigDecimal(monthOrderCount - lastMonthOrderCount)
                    .divide(new BigDecimal(lastMonthOrderCount), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100));
            stats.setOrderChangePercent(change);
        } else {
            stats.setOrderChangePercent(monthOrderCount > 0 ? new BigDecimal(100) : BigDecimal.ZERO);
        }

        // 本月活跃客户（本月有订单的客户数量）- 包含所有状态的订单
        LambdaQueryWrapper<SalesOrder> currentMonthAllQuery = new LambdaQueryWrapper<>();
        currentMonthAllQuery.eq(SalesOrder::getDeleted, 0)
                .ge(SalesOrder::getCreateTime, currentMonthStart)
                .le(SalesOrder::getCreateTime, currentMonthEnd);

        Set<Long> currentMonthCustomers = new HashSet<>();
        salesOrderMapper.selectList(currentMonthAllQuery)
                .forEach(order -> currentMonthCustomers.add(order.getCustomerId()));
        stats.setActiveCustomerCount(currentMonthCustomers.size());

        // 上月活跃客户 - 包含所有状态的订单
        LambdaQueryWrapper<SalesOrder> lastMonthAllQuery = new LambdaQueryWrapper<>();
        lastMonthAllQuery.eq(SalesOrder::getDeleted, 0)
                .ge(SalesOrder::getCreateTime, lastMonthStart)
                .le(SalesOrder::getCreateTime, lastMonthEnd);

        Set<Long> lastMonthCustomers = new HashSet<>();
        salesOrderMapper.selectList(lastMonthAllQuery)
                .forEach(order -> lastMonthCustomers.add(order.getCustomerId()));
        stats.setLastMonthActiveCustomerCount(lastMonthCustomers.size());

        // 客户同比变化
        stats.setCustomerChange(currentMonthCustomers.size() - lastMonthCustomers.size());

        // 待处理售后数量
        LambdaQueryWrapper<AftersalesOrder> aftersalesQuery = new LambdaQueryWrapper<>();
        aftersalesQuery.eq(AftersalesOrder::getDeleted, 0)
                .eq(AftersalesOrder::getStatus, "pending")
                .or()
                .eq(AftersalesOrder::getStatus, "processing");
        Long pendingCount = aftersalesOrderMapper.selectCount(aftersalesQuery);
        stats.setPendingAftersalesCount(pendingCount.intValue());

        return stats;
    }

}