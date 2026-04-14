package com.smartims.controller;

import com.smartims.annotation.OperationLog;
import com.smartims.common.PageResult;
import com.smartims.common.Result;
import com.smartims.dto.PageQuery;
import com.smartims.dto.SalesOrderDTO;
import com.smartims.dto.ShippingDTO;
import com.smartims.entity.SalesOrder;
import com.smartims.service.SalesService;
import com.smartims.vo.SalesStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 销售管理控制器
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Tag(name = "销售管理", description = "销售订单相关接口")
@RestController
@RequestMapping("/v1/sales")
@RequiredArgsConstructor
public class SalesController {

    private final SalesService salesService;

    @Operation(summary = "查询销售订单列表")
    @GetMapping("/orders")
    public Result<PageResult<SalesOrder>> getSalesOrderList(PageQuery pageQuery) {
        PageResult<SalesOrder> result = salesService.getSalesOrderList(pageQuery);
        return Result.success(result);
    }

    @Operation(summary = "查询销售订单详情")
    @GetMapping("/orders/{id}")
    public Result<SalesOrder> getSalesOrderById(@PathVariable Long id) {
        SalesOrder order = salesService.getSalesOrderById(id);
        return Result.success(order);
    }

    @Operation(summary = "创建销售订单")
    @PostMapping("/orders")
    @OperationLog(module = "销售管理", action = "新增", description = "创建销售订单")
    public Result<Void> createSalesOrder(@Valid @RequestBody SalesOrderDTO dto) {
        salesService.createSalesOrder(dto);
        return Result.success("销售订单创建成功");
    }

    @Operation(summary = "更新销售订单")
    @PutMapping("/orders/{id}")
    @OperationLog(module = "销售管理", action = "修改", description = "修改销售订单")
    public Result<Void> updateSalesOrder(@PathVariable Long id, @Valid @RequestBody SalesOrderDTO dto) {
        salesService.updateSalesOrder(id, dto);
        return Result.success("销售订单更新成功");
    }

    @Operation(summary = "确认付款")
    @PutMapping("/orders/{id}/payment")
    @OperationLog(module = "销售管理", action = "付款", description = "确认订单付款")
    public Result<Void> confirmPayment(@PathVariable Long id) {
        salesService.confirmPayment(id);
        return Result.success("付款确认成功");
    }

    @Operation(summary = "确认发货")
    @PostMapping("/orders/{id}/shipping")
    @OperationLog(module = "销售管理", action = "发货", description = "确认订单发货")
    public Result<Void> confirmShipping(@PathVariable Long id, @Valid @RequestBody ShippingDTO dto) {
        salesService.confirmShipping(id, dto);
        return Result.success("发货确认成功");
    }

    @Operation(summary = "确认收货")
    @PutMapping("/orders/{id}/received")
    @OperationLog(module = "销售管理", action = "收货", description = "确认订单收货")
    public Result<Void> confirmReceived(@PathVariable Long id) {
        salesService.confirmReceived(id);
        return Result.success("收货确认成功");
    }

    @Operation(summary = "取消订单")
    @PutMapping("/orders/{id}/cancel")
    @OperationLog(module = "销售管理", action = "取消", description = "取消销售订单")
    public Result<Void> cancelOrder(@PathVariable Long id) {
        salesService.cancelOrder(id);

        return Result.success("订单已取消");
    }

    @Operation(summary = "获取销售统计数据")
    @GetMapping("/stats")
    public Result<SalesStatsVO> getSalesStats() {
        SalesStatsVO stats = salesService.getSalesStats();
        return Result.success(stats);
    }

}