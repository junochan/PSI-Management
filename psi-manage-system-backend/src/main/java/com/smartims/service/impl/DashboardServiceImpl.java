package com.smartims.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartims.entity.Customer;
import com.smartims.entity.Inventory;
import com.smartims.entity.Product;
import com.smartims.entity.SalesOrder;
import com.smartims.mapper.CustomerMapper;
import com.smartims.mapper.InventoryMapper;
import com.smartims.mapper.ProductMapper;
import com.smartims.mapper.SalesOrderMapper;
import com.smartims.service.DashboardService;
import com.smartims.service.SalesService;
import com.smartims.service.SystemConfigService;
import com.smartims.vo.DashboardOverviewVO;
import com.smartims.vo.SalesStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 仪表盘：聚合销售统计、趋势、排名与库存预警
 *
 * @author Smart IMS Team
 * @since 2026-04-14
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final SalesService salesService;
    private final SalesOrderMapper salesOrderMapper;
    private final ProductMapper productMapper;
    private final CustomerMapper customerMapper;
    private final InventoryMapper inventoryMapper;
    private final SystemConfigService systemConfigService;

    /** 与 {@link com.smartims.service.impl.SalesServiceImpl#getSalesStats()} 中「计入销售额」的口径一致 */
    private static boolean countsTowardRevenue(SalesOrder o) {
        String s = o.getStatus();
        if (s == null) {
            return false;
        }
        return "completed".equals(s) || "shipped".equals(s)
                || "已完成".equals(s) || "已发货".equals(s) || "处理中".equals(s);
    }

    @Override
    public DashboardOverviewVO getOverview(int days) {
        int d = (days == 7 || days == 30 || days == 90) ? days : 7;

        DashboardOverviewVO vo = new DashboardOverviewVO();

        SalesStatsVO salesStats = salesService.getSalesStats();
        DashboardOverviewVO.Summary summary = new DashboardOverviewVO.Summary();
        summary.setMonthAmount(salesStats.getMonthAmount());
        summary.setMonthOrderCount(salesStats.getMonthOrderCount());
        summary.setProductCount(Math.toIntExact(productMapper.selectCount(
                new LambdaQueryWrapper<Product>().eq(Product::getDeleted, 0))));
        summary.setCustomerCount(Math.toIntExact(customerMapper.selectCount(
                new LambdaQueryWrapper<Customer>().eq(Customer::getDeleted, 0))));
        vo.setSummary(summary);

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(d - 1);
        LocalDateTime rangeStart = start.atStartOfDay();
        LocalDateTime rangeEnd = end.atTime(23, 59, 59);

        LambdaQueryWrapper<SalesOrder> rangeQ = new LambdaQueryWrapper<>();
        rangeQ.eq(SalesOrder::getDeleted, 0)
                .ge(SalesOrder::getCreateTime, rangeStart)
                .le(SalesOrder::getCreateTime, rangeEnd);
        List<SalesOrder> inRange = salesOrderMapper.selectList(rangeQ);

        List<SalesOrder> revenueOrders = inRange.stream()
                .filter(DashboardServiceImpl::countsTowardRevenue)
                .collect(Collectors.toList());

        vo.setSalesTrend(buildTrend(start, end, revenueOrders));
        vo.setCategorySalesTop5(buildCategoryTop5(revenueOrders));
        vo.setProductSalesTop5(buildProductTop5(revenueOrders));
        vo.setCustomerSalesTop5(buildCustomerTop5(revenueOrders));

        LambdaQueryWrapper<SalesOrder> recentQ = new LambdaQueryWrapper<>();
        recentQ.eq(SalesOrder::getDeleted, 0)
                .orderByDesc(SalesOrder::getCreateTime)
                .last("LIMIT 10");
        List<SalesOrder> recentOrders = salesOrderMapper.selectList(recentQ);
        enrichRecentOrdersProductImage(recentOrders);
        vo.setRecentOrders(recentOrders);

        vo.setInventoryWarningTop10(buildWarningTop10());
        vo.setInventoryStagnantTop10(buildStagnantTop10());

        return vo;
    }

    /** 近期订单行展示商品缩略图：从商品表回填 {@link SalesOrder#getProductImage()} */
    private void enrichRecentOrdersProductImage(List<SalesOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        Set<Long> productIds = orders.stream()
                .map(SalesOrder::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (productIds.isEmpty()) {
            return;
        }
        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>().in(Product::getId, productIds));
        Map<Long, String> imageByProductId = new HashMap<>();
        for (Product p : products) {
            if (p.getId() != null && p.getImage() != null && !p.getImage().isBlank()) {
                imageByProductId.put(p.getId(), p.getImage());
            }
        }
        for (SalesOrder o : orders) {
            if (o.getProductId() == null) {
                continue;
            }
            String img = imageByProductId.get(o.getProductId());
            if (img != null) {
                o.setProductImage(img);
            }
        }
    }

    private List<DashboardOverviewVO.SalesTrendPoint> buildTrend(LocalDate start, LocalDate end, List<SalesOrder> orders) {
        Map<LocalDate, BigDecimal> daily = new HashMap<>();
        for (SalesOrder o : orders) {
            if (o.getCreateTime() == null) {
                continue;
            }
            LocalDate day = o.getCreateTime().toLocalDate();
            BigDecimal amt = o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO;
            daily.merge(day, amt, BigDecimal::add);
        }
        List<DashboardOverviewVO.SalesTrendPoint> list = new ArrayList<>();
        for (LocalDate cur = start; !cur.isAfter(end); cur = cur.plusDays(1)) {
            DashboardOverviewVO.SalesTrendPoint p = new DashboardOverviewVO.SalesTrendPoint();
            p.setDate(cur.toString());
            p.setAmount(daily.getOrDefault(cur, BigDecimal.ZERO));
            list.add(p);
        }
        return list;
    }

    private List<DashboardOverviewVO.CategorySale> buildCategoryTop5(List<SalesOrder> orders) {
        Set<Long> productIds = orders.stream()
                .map(SalesOrder::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Product> productMap = new HashMap<>();
        if (!productIds.isEmpty()) {
            List<Product> products = productMapper.selectList(
                    new LambdaQueryWrapper<Product>().in(Product::getId, productIds));
            for (Product p : products) {
                productMap.put(p.getId(), p);
            }
        }
        Map<String, BigDecimal> byCat = new HashMap<>();
        for (SalesOrder o : orders) {
            String cat = "其他";
            if (o.getProductId() != null) {
                Product pr = productMap.get(o.getProductId());
                if (pr != null && pr.getCategoryName() != null && !pr.getCategoryName().isBlank()) {
                    cat = pr.getCategoryName();
                }
            }
            BigDecimal amt = o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO;
            byCat.merge(cat, amt, BigDecimal::add);
        }
        BigDecimal total = byCat.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return byCat.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(5)
                .map(e -> {
                    DashboardOverviewVO.CategorySale c = new DashboardOverviewVO.CategorySale();
                    c.setCategoryName(e.getKey());
                    c.setAmount(e.getValue());
                    int pct = BigDecimal.ZERO.compareTo(total) == 0 ? 0
                            : e.getValue().multiply(BigDecimal.valueOf(100)).divide(total, 0, RoundingMode.HALF_UP).intValue();
                    c.setPercent(pct);
                    return c;
                })
                .collect(Collectors.toList());
    }

    private List<DashboardOverviewVO.ProductRank> buildProductTop5(List<SalesOrder> orders) {
        Map<Long, Agg> map = new HashMap<>();
        for (SalesOrder o : orders) {
            if (o.getProductId() == null) {
                continue;
            }
            Agg a = map.computeIfAbsent(o.getProductId(), id -> new Agg());
            a.orderCount++;
            a.totalAmount = a.totalAmount.add(o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO);
            if (a.name == null && o.getProductName() != null) {
                a.name = o.getProductName();
            }
        }
        Set<Long> ids = map.keySet();
        if (!ids.isEmpty()) {
            List<Product> plist = productMapper.selectList(
                    new LambdaQueryWrapper<Product>().in(Product::getId, ids));
            for (Product p : plist) {
                Agg a = map.get(p.getId());
                if (a != null && p.getName() != null) {
                    a.name = p.getName();
                }
            }
        }
        return map.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().totalAmount.compareTo(e1.getValue().totalAmount))
                .limit(5)
                .map(e -> {
                    DashboardOverviewVO.ProductRank r = new DashboardOverviewVO.ProductRank();
                    r.setProductId(e.getKey());
                    r.setProductName(e.getValue().name != null ? e.getValue().name : "-");
                    r.setOrderCount(e.getValue().orderCount);
                    r.setTotalAmount(e.getValue().totalAmount);
                    return r;
                })
                .collect(Collectors.toList());
    }

    private List<DashboardOverviewVO.CustomerRank> buildCustomerTop5(List<SalesOrder> orders) {
        Map<Long, Agg> map = new HashMap<>();
        for (SalesOrder o : orders) {
            if (o.getCustomerId() == null) {
                continue;
            }
            Agg a = map.computeIfAbsent(o.getCustomerId(), id -> new Agg());
            a.orderCount++;
            a.totalAmount = a.totalAmount.add(o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO);
            if (a.name == null && o.getCustomerName() != null) {
                a.name = o.getCustomerName();
            }
        }
        Set<Long> ids = map.keySet();
        if (!ids.isEmpty()) {
            List<Customer> clist = customerMapper.selectList(
                    new LambdaQueryWrapper<Customer>().in(Customer::getId, ids));
            for (Customer c : clist) {
                Agg a = map.get(c.getId());
                if (a != null && c.getName() != null) {
                    a.name = c.getName();
                }
            }
        }
        return map.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().totalAmount.compareTo(e1.getValue().totalAmount))
                .limit(5)
                .map(e -> {
                    DashboardOverviewVO.CustomerRank r = new DashboardOverviewVO.CustomerRank();
                    r.setCustomerId(e.getKey());
                    r.setCustomerName(e.getValue().name != null ? e.getValue().name : "-");
                    r.setOrderCount(e.getValue().orderCount);
                    r.setTotalAmount(e.getValue().totalAmount);
                    return r;
                })
                .collect(Collectors.toList());
    }

    private List<DashboardOverviewVO.InventoryWarningRow> buildWarningTop10() {
        LambdaQueryWrapper<Inventory> q = new LambdaQueryWrapper<>();
        q.eq(Inventory::getDeleted, 0)
                .apply("stock < safe_stock")
                .orderByAsc(Inventory::getStock)
                .last("LIMIT 10");
        List<Inventory> list = inventoryMapper.selectList(q);
        return list.stream().map(i -> {
            DashboardOverviewVO.InventoryWarningRow row = new DashboardOverviewVO.InventoryWarningRow();
            row.setId(i.getId());
            row.setProductName(i.getProductName());
            row.setWarehouseName(i.getWarehouseName());
            row.setStock(i.getStock());
            row.setSafeStock(i.getSafeStock());
            return row;
        }).collect(Collectors.toList());
    }

    private List<DashboardOverviewVO.InventoryStagnantRow> buildStagnantTop10() {
        LambdaQueryWrapper<Inventory> q = new LambdaQueryWrapper<>();
        q.eq(Inventory::getDeleted, 0);
        List<Inventory> all = inventoryMapper.selectList(q);
        List<DashboardOverviewVO.InventoryStagnantRow> rows = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        Integer cfgStale = systemConfigService.getSystemConfig().getStaleDays();
        int defaultStaleDays = (cfgStale != null && cfgStale >= 1) ? cfgStale : 90;
        for (Inventory i : all) {
            long sd = computeStagnantDays(i, now);
            int threshold = i.getStagnantDays() != null ? i.getStagnantDays() : defaultStaleDays;
            if (sd < threshold) {
                continue;
            }
            DashboardOverviewVO.InventoryStagnantRow row = new DashboardOverviewVO.InventoryStagnantRow();
            row.setId(i.getId());
            row.setProductName(i.getProductName());
            row.setWarehouseName(i.getWarehouseName());
            row.setStagnantDays(sd);
            rows.add(row);
        }
        rows.sort((a, b) -> {
            int c = Long.compare(b.getStagnantDays(), a.getStagnantDays());
            if (c != 0) {
                return c;
            }
            long idA = a.getId() != null ? a.getId() : 0L;
            long idB = b.getId() != null ? b.getId() : 0L;
            return Long.compare(idB, idA);
        });
        if (rows.size() > 10) {
            return rows.subList(0, 10);
        }
        return rows;
    }

    /**
     * 与库存列表呆滞筛选一致：{@code COALESCE(last_outbound_time, last_inbound_time, create_time)} 起算至今的天数。
     */
    private static long computeStagnantDays(Inventory i, LocalDateTime now) {
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
        return ChronoUnit.DAYS.between(ref, now);
    }

    private static class Agg {
        int orderCount;
        BigDecimal totalAmount = BigDecimal.ZERO;
        String name;
    }
}
