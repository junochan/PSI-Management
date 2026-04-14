package com.smartims.vo;

import com.smartims.entity.SalesOrder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 仪表盘聚合数据
 *
 * @author Smart IMS Team
 * @since 2026-04-14
 */
@Data
public class DashboardOverviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Summary summary;
    /** 销售趋势（按日，含区间内每一天，无单则为 0） */
    private List<SalesTrendPoint> salesTrend;
    /** 分类销售 Top5（与趋势相同统计口径与区间） */
    private List<CategorySale> categorySalesTop5;
    private List<ProductRank> productSalesTop5;
    private List<CustomerRank> customerSalesTop5;
    private List<SalesOrder> recentOrders;
    private List<InventoryWarningRow> inventoryWarningTop10;
    private List<InventoryStagnantRow> inventoryStagnantTop10;

    @Data
    public static class Summary implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 本月销售额（与 /sales/stats 一致） */
        private BigDecimal monthAmount;
        /** 本月成交订单数 */
        private Integer monthOrderCount;
        /** 在售商品种类（未删除商品数） */
        private Integer productCount;
        /** 客户总数 */
        private Integer customerCount;
    }

    @Data
    public static class SalesTrendPoint implements Serializable {
        private static final long serialVersionUID = 1L;
        /** yyyy-MM-dd */
        private String date;
        private BigDecimal amount;
    }

    @Data
    public static class CategorySale implements Serializable {
        private static final long serialVersionUID = 1L;
        private String categoryName;
        private BigDecimal amount;
        private Integer percent;
    }

    @Data
    public static class ProductRank implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long productId;
        private String productName;
        private Integer orderCount;
        private BigDecimal totalAmount;
    }

    @Data
    public static class CustomerRank implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long customerId;
        private String customerName;
        private Integer orderCount;
        private BigDecimal totalAmount;
    }

    @Data
    public static class InventoryWarningRow implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
        private String productName;
        private String warehouseName;
        private Integer stock;
        private Integer safeStock;
    }

    @Data
    public static class InventoryStagnantRow implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
        private String productName;
        private String warehouseName;
        private Long stagnantDays;
    }
}
