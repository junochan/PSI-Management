package com.smartims.config.registry;

import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * 系统权限注册表：API 路径与权限码、前端路由与权限码的唯一来源，供过滤器与导航接口共用。
 * API 规则支持 HTTP 方法与「多权限满足其一」（OR）。
 */
@Component
public class ApplicationPermissionRegistry {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /** 商品/分类 GET：与库存、采购、销售只读联查共用 */
    private static final List<String> READ_PRODUCT_OR_CATEGORY = List.of(
            "products", "product:view", "purchase", "purchase:view", "sales", "sales:view",
            "inventory", "inventory:view", "inventory:transfer", "inventory:warehouse",
            "inventory:inbound", "inventory:outbound", "inventory:adjust", "inventory:records");

    /** 采购 GET：库存页联动采购单、入库流水 */
    private static final List<String> READ_PURCHASE = List.of(
            "purchase", "purchase:view", "inventory", "inventory:view", "inventory:transfer",
            "inventory:warehouse", "inventory:inbound", "inventory:outbound", "inventory:adjust",
            "inventory:records");

    /**
     * 采购入库流水 GET：不包含仅 inventory:view（与「出入库记录」inventory:records、入出库操作码对齐）
     */
    private static final List<String> READ_PURCHASE_INBOUNDS = List.of(
            "purchase", "purchase:view", "inventory", "inventory:transfer",
            "inventory:warehouse", "inventory:inbound", "inventory:outbound", "inventory:adjust",
            "inventory:records");

    /** 销售 GET：库存页联动销售出库等（不含仅 inventory:view，避免无授权即可拉订单拼出库记录） */
    private static final List<String> READ_SALES = List.of(
            "sales", "sales:view", "inventory", "inventory:transfer",
            "inventory:warehouse", "inventory:inbound", "inventory:outbound", "inventory:adjust",
            "inventory:records");

    private static final Set<String> HTTP_WRITE = Set.of("POST", "PUT", "PATCH", "DELETE");

    /**
     * 顺序：先匹配更具体的「方法 + 路径」规则，再匹配通配方法。
     */
    private final List<ApiPermissionRule> apiRules = new ArrayList<>();

    /**
     * 前端子路由（相对主布局），顺序影响 Vue Router 注册顺序
     */
    @Getter
    private final List<UiRouteDefinition> uiRoutes = new ArrayList<>();

    public ApplicationPermissionRegistry() {
        initApiRules();
        initUiRoutes();
    }

    private void initApiRules() {
        addRule("/v1/users/**", null, List.of("settings:user"));
        addRule("/v1/roles/**", null, List.of("settings:role"));
        addRule("/v1/logs/**", null, List.of("settings:user"));
        addRule("/v1/dashboard/**", null, List.of("dashboard"));

        // 商品/分类：无子路径的 GET（/v1/products、/v1/categories）单独注册，避免 /** 未匹配时落入仅菜单码 products 的兜底规则
        addRule("/v1/products", Set.of("GET", "HEAD"), READ_PRODUCT_OR_CATEGORY);
        addRule("/v1/categories", Set.of("GET", "HEAD"), READ_PRODUCT_OR_CATEGORY);

        // 商品 Excel 异步导入（须在泛化 /v1/products/** GET 之前，仅允许 product:add 查询任务进度）
        addRule("/v1/products/import", Set.of("POST"), List.of("product:add"));
        addRule("/v1/products/import/**", Set.of("GET", "HEAD"), List.of("product:add"));

        // 商品：读接口允许采购/销售/库存岗只读；写接口仅认操作码 product:*（不再用菜单码 products 放行写操作）
        addRule("/v1/products/**", Set.of("GET", "HEAD"), READ_PRODUCT_OR_CATEGORY);
        // 以图搜图：POST 但属查询，与读权限一致（须在泛化 POST 之前匹配）
        addRule("/v1/products/search-by-image", Set.of("POST"),
                List.of("products", "product:view", "purchase", "purchase:view", "sales", "sales:view",
                        "inventory", "inventory:view", "inventory:transfer", "inventory:warehouse",
                        "inventory:inbound", "inventory:outbound", "inventory:adjust", "inventory:records"));
        // 商品主图上传：新建或编辑表单均需
        addRule("/v1/products/image", Set.of("POST"), List.of("product:add", "product:edit"));
        addRule("/v1/products/batch", Set.of("DELETE"), List.of("product:delete"));
        addRule("/v1/products/**", Set.of("POST"), List.of("product:add"));
        addRule("/v1/products/**", Set.of("PUT", "PATCH"), List.of("product:edit"));
        addRule("/v1/products/**", Set.of("DELETE"), List.of("product:delete"));

        addRule("/v1/categories/**", Set.of("GET", "HEAD"), READ_PRODUCT_OR_CATEGORY);
        addRule("/v1/categories/**", Set.of("POST"), List.of("product:add"));
        addRule("/v1/categories/**", Set.of("PUT", "PATCH"), List.of("product:edit"));
        addRule("/v1/categories/**", Set.of("DELETE"), List.of("product:delete"));

        // 采购：读接口允许库存岗查看采购单/入库流水（与库存页联动）；写仍须 purchase
        addRule("/v1/purchase/orders", Set.of("GET", "HEAD"), READ_PURCHASE);
        addRule("/v1/purchase/inbounds", Set.of("GET", "HEAD"), READ_PURCHASE_INBOUNDS);
        addRule("/v1/purchase/inbounds/**", Set.of("GET", "HEAD"), READ_PURCHASE_INBOUNDS);
        addRule("/v1/purchase/stats", Set.of("GET", "HEAD"), READ_PURCHASE);
        addRule("/v1/purchase/**", Set.of("GET", "HEAD"),
                List.of("purchase", "purchase:view", "inventory", "inventory:view", "inventory:transfer",
                        "inventory:warehouse", "inventory:inbound", "inventory:outbound", "inventory:adjust",
                        "inventory:records"));
        addRule("/v1/purchase/**", HTTP_WRITE, List.of("purchase"));
        // 供应商：读放宽；写须 purchase:supplier / purchase:add|edit 或菜单 purchase
        addRule("/v1/suppliers/**", Set.of("GET", "HEAD"),
                List.of("purchase", "purchase:supplier", "inventory", "inventory:view", "inventory:transfer",
                        "inventory:warehouse", "inventory:inbound", "inventory:outbound", "inventory:adjust",
                        "inventory:records"));
        addRule("/v1/suppliers/**", Set.of("POST"), List.of("purchase:supplier", "purchase:add", "purchase"));
        addRule("/v1/suppliers/**", Set.of("PUT", "PATCH"), List.of("purchase:supplier", "purchase:edit", "purchase"));
        addRule("/v1/suppliers/**", Set.of("DELETE"), List.of("purchase:supplier", "purchase:edit", "purchase"));
        addRule("/v1/supplier-industries/**", Set.of("GET", "HEAD"),
                List.of("purchase", "inventory", "inventory:view", "inventory:transfer",
                        "inventory:warehouse", "inventory:inbound", "inventory:outbound", "inventory:adjust",
                        "inventory:records"));

        // 销售：订单付款/发货/收货单独拆权限；其余写仍须 sales
        addRule("/v1/sales/orders", Set.of("GET", "HEAD"), READ_SALES);
        addRule("/v1/sales/stats", Set.of("GET", "HEAD"), READ_SALES);
        addRule("/v1/sales/**", Set.of("GET", "HEAD"),
                List.of("sales", "sales:view", "inventory", "inventory:transfer",
                        "inventory:warehouse", "inventory:inbound", "inventory:outbound", "inventory:adjust",
                        "inventory:records"));
        addRule("/v1/sales/orders/*/payment", Set.of("PUT"), List.of("sales:payment", "sales:add", "sales"));
        addRule("/v1/sales/orders/*/shipping", Set.of("POST"), List.of("sales:ship", "sales"));
        addRule("/v1/sales/orders/*/received", Set.of("PUT"), List.of("sales:receive", "sales:ship", "sales"));
        addRule("/v1/sales/**", HTTP_WRITE, List.of("sales"));
        addRule("/v1/customers/**", Set.of("GET", "HEAD"), List.of("sales", "sales:view"));
        addRule("/v1/customers/**", HTTP_WRITE, List.of("sales"));
        addRule("/v1/aftersales/**", Set.of("GET", "HEAD"), List.of("sales", "sales:view"));
        addRule("/v1/aftersales/**", HTTP_WRITE, List.of("sales"));

        // 仓库：下拉 options 含 inventory:view；完整列表/详情不含仅 view（须菜单或子权限）
        addRule("/v1/warehouses/options", Set.of("GET", "HEAD"),
                List.of("inventory", "inventory:view", "inventory:transfer", "inventory:warehouse",
                        "inventory:inbound", "inventory:outbound", "inventory:adjust", "inventory:records",
                        "purchase", "purchase:view", "sales", "sales:view"));
        addRule("/v1/warehouses/**", Set.of("GET", "HEAD"),
                List.of("inventory", "inventory:transfer", "inventory:warehouse", "inventory:inbound", "inventory:outbound",
                        "inventory:records", "inventory:adjust", "purchase", "purchase:view", "sales", "sales:view"));
        addRule("/v1/warehouses/**", HTTP_WRITE, List.of("inventory:warehouse", "inventory"));

        // 库存：先匹配具体写接口，再读，最后兜底菜单码 inventory
        addRule("/v1/inventory", Set.of("GET", "HEAD"),
                List.of("inventory", "inventory:view", "inventory:transfer", "inventory:warehouse",
                        "inventory:inbound", "inventory:outbound", "inventory:adjust", "inventory:records", "sales", "sales:view"));
        addRule("/v1/inventory/search-by-image", Set.of("POST"),
                List.of("inventory", "inventory:view", "inventory:transfer", "inventory:warehouse",
                        "inventory:inbound", "inventory:outbound", "inventory:adjust", "inventory:records", "sales", "sales:view"));
        addRule("/v1/inventory/transfers", Set.of("POST"), List.of("inventory:transfer", "inventory"));
        addRule("/v1/inventory/transfers/*/confirm", Set.of("PUT"), List.of("inventory:transfer", "inventory"));
        addRule("/v1/inventory/inbound/new", Set.of("POST"), List.of("inventory:inbound", "inventory"));
        addRule("/v1/inventory/inbound", Set.of("POST"), List.of("inventory:inbound", "inventory"));
        addRule("/v1/inventory/outbound", Set.of("POST"), List.of("inventory:outbound", "inventory"));
        addRule("/v1/inventory/warnings/*/handle", Set.of("PUT"), List.of("inventory:adjust", "inventory"));
        addRule("/v1/inventory/*/safe-stock", Set.of("PUT"), List.of("inventory:adjust", "inventory"));
        addRule("/v1/inventory/*/location", Set.of("PUT"), List.of("inventory:adjust", "inventory"));
        addRule("/v1/inventory/*/stagnant-days", Set.of("PUT"), List.of("inventory:adjust", "inventory"));
        addRule("/v1/inventory/**", Set.of("GET", "HEAD"),
                List.of("inventory", "inventory:view", "inventory:transfer", "inventory:warehouse",
                        "inventory:inbound", "inventory:outbound", "inventory:adjust", "inventory:records", "sales", "sales:view"));
        addRule("/v1/inventory/**", HTTP_WRITE, List.of("inventory"));
    }

    private void addRule(String pattern, Set<String> methods, List<String> anyPermissions) {
        apiRules.add(new ApiPermissionRule(pattern, methods == null ? null : Collections.unmodifiableSet(methods),
                List.copyOf(anyPermissions)));
    }

    /**
     * 解析 REST API 所需权限码列表（满足其一即可）；无匹配规则则返回 empty。
     */
    public Optional<List<String>> resolveApiPermissions(String uriWithoutContext, String httpMethod) {
        String m = httpMethod == null ? "GET" : httpMethod.toUpperCase(Locale.ROOT);
        for (ApiPermissionRule rule : apiRules) {
            if (!pathMatcher.match(rule.getPattern(), uriWithoutContext)) {
                continue;
            }
            if (rule.getMethods() != null && !rule.getMethods().isEmpty()) {
                if (!rule.getMethods().contains(m)) {
                    continue;
                }
            }
            return Optional.of(rule.getAnyPermissions());
        }
        return Optional.empty();
    }

    @Getter
    public static class ApiPermissionRule {
        private final String pattern;
        /** null 或空表示任意 HTTP 方法均使用本规则（在路径匹配的前提下） */
        private final Set<String> methods;
        private final List<String> anyPermissions;

        public ApiPermissionRule(String pattern, Set<String> methods, List<String> anyPermissions) {
            this.pattern = pattern;
            this.methods = methods;
            this.anyPermissions = anyPermissions;
        }
    }

    private void initUiRoutes() {
        addUi("dashboard", "Dashboard", "仪表盘", "Dashboard", "dashboard", false, "NORMAL");
        addUi("products", "Products", "商品管理", "Products", "products", false, "NORMAL");
        addUi("products/view/:id", "ProductView", "商品详情", "ProductView", "products", true, "NORMAL");
        addUi("products/add", "ProductAdd", "添加商品", "ProductAdd", "products", true, "NORMAL");
        addUi("products/edit/:id", "ProductEdit", "编辑商品", "ProductEdit", "products", true, "NORMAL");
        addUi("purchase", "Purchase", "采购管理", "Purchase", "purchase", false, "NORMAL");
        addUi("purchase/order/:id", "PurchaseOrderDetail", "采购单详情", "PurchaseOrderDetail", "purchase", true, "NORMAL");
        addUi("purchase/edit/:id", "PurchaseOrderEdit", "编辑采购单", "PurchaseOrderEdit", "purchase", true, "NORMAL");
        addUi("purchase/supplier/:id", "SupplierDetail", "供应商详情", "SupplierDetail", "purchase:supplier", true, "NORMAL");
        addUi("purchase/supplier/edit/:id", "SupplierEdit", "编辑供应商", "SupplierEdit", "purchase:supplier", true, "NORMAL");
        addUi("purchase/inbound/:id", "InboundDetail", "入库详情", "InboundDetail", "purchase", true, "NORMAL");
        addUi("sales", "Sales", "销售管理", "Sales", "sales", false, "NORMAL");
        addUi("sales/order/:id", "SalesOrderDetail", "销售单详情", "SalesOrderDetail", "sales", true, "NORMAL");
        addUi("sales/customer/:id", "CustomerDetail", "客户详情", "CustomerDetail", "sales", true, "NORMAL");
        addUi("sales/customer/edit/:id", "CustomerEdit", "编辑客户", "CustomerEdit", "sales", true, "NORMAL");
        addUi("sales/aftersales/:id", "AftersalesDetail", "售后详情", "AftersalesDetail", "sales", true, "NORMAL");
        addUi("inventory", "Inventory", "库存管理", "Inventory", "inventory", false, "NORMAL");
        addUi("inventory/stock/:id", "InventoryDetail", "库存详情", "InventoryDetail", "inventory", true, "NORMAL");
        addUi("inventory/transfer/:id", "TransferDetail", "调拨详情", "TransferDetail", "inventory", true, "NORMAL");
        addUi("inventory/warehouse/:id", "WarehouseDetail", "仓库详情", "WarehouseDetail", "inventory", true, "NORMAL");
        addUi("settings", "Settings", "系统设置", "Settings", "settings", false, "SETTINGS_ANY");
        addUi("settings/permission/:id", "RolePermission", "权限管理", "RolePermission", "settings:role", true, "NORMAL");
    }

    private void addUi(String path, String routeName, String title, String viewKey, String permissionCode,
                       boolean hidden, String permissionMode) {
        uiRoutes.add(new UiRouteDefinition(path, routeName, title, viewKey, permissionCode, hidden, permissionMode));
    }

    @Getter
    public static class UiRouteDefinition {
        private final String path;
        private final String routeName;
        private final String title;
        private final String viewKey;
        private final String permissionCode;
        private final boolean hidden;
        private final String permissionMode;

        public UiRouteDefinition(String path, String routeName, String title, String viewKey, String permissionCode,
                                 boolean hidden, String permissionMode) {
            this.path = path;
            this.routeName = routeName;
            this.title = title;
            this.viewKey = viewKey;
            this.permissionCode = permissionCode;
            this.hidden = hidden;
            this.permissionMode = permissionMode;
        }
    }
}
