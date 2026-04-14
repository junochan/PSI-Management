package com.smartims.controller;

import com.smartims.annotation.OperationLog;
import com.smartims.common.PageResult;
import com.smartims.common.Result;
import com.smartims.dto.InboundDTO;
import com.smartims.dto.PageQuery;
import com.smartims.dto.PurchaseOrderDTO;
import com.smartims.entity.InboundRecord;
import com.smartims.entity.PurchaseOrder;
import com.smartims.service.PurchaseService;
import com.smartims.vo.PurchaseStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 采购管理控制器
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Tag(name = "采购管理", description = "采购订单及入库相关接口")
@RestController
@RequestMapping("/v1/purchase")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    @Operation(summary = "查询采购订单列表")
    @GetMapping("/orders")
    public Result<PageResult<PurchaseOrder>> getPurchaseOrderList(PageQuery pageQuery) {
        PageResult<PurchaseOrder> result = purchaseService.getPurchaseOrderList(pageQuery);
        return Result.success(result);
    }

    @Operation(summary = "查询采购订单详情")
    @GetMapping("/orders/{id}")
    public Result<PurchaseOrder> getPurchaseOrderById(@PathVariable Long id) {
        PurchaseOrder order = purchaseService.getPurchaseOrderById(id);
        return Result.success(order);
    }

    @Operation(summary = "创建采购订单")
    @OperationLog(module = "采购管理", action = "新增", description = "创建采购订单")
    @PostMapping("/orders")
    public Result<Void> createPurchaseOrder(@Valid @RequestBody PurchaseOrderDTO dto) {
        purchaseService.createPurchaseOrder(dto);
        return Result.success("采购订单创建成功");
    }

    @Operation(summary = "更新采购订单")
    @OperationLog(module = "采购管理", action = "修改", description = "修改采购订单")
    @PutMapping("/orders/{id}")
    public Result<Void> updatePurchaseOrder(@PathVariable Long id, @Valid @RequestBody PurchaseOrderDTO dto) {
        purchaseService.updatePurchaseOrder(id, dto);
        return Result.success("采购订单更新成功");
    }

    @Operation(summary = "确认入库")
    @OperationLog(module = "采购管理", action = "入库", description = "确认采购入库")
    @PostMapping("/orders/{id}/inbound")
    public Result<Void> confirmInbound(@PathVariable Long id, @Valid @RequestBody InboundDTO dto) {
        purchaseService.confirmInbound(id, dto);
        return Result.success("入库确认成功");
    }

    @Operation(summary = "取消采购订单")
    @OperationLog(module = "采购管理", action = "取消", description = "取消采购订单")
    @PutMapping("/orders/{id}/cancel")
    public Result<Void> cancelPurchaseOrder(@PathVariable Long id) {
        purchaseService.cancelPurchaseOrder(id);
        return Result.success("订单已取消");
    }

    @Operation(summary = "删除采购订单")
    @OperationLog(module = "采购管理", action = "删除", description = "删除采购订单")
    @DeleteMapping("/orders/{id}")
    public Result<Void> deletePurchaseOrder(@PathVariable Long id) {
        purchaseService.deletePurchaseOrder(id);
        return Result.success("采购订单已删除");
    }

    @Operation(summary = "查询入库记录列表")
    @GetMapping("/inbounds")
    public Result<PageResult<InboundRecord>> getInboundRecordList(PageQuery pageQuery) {
        PageResult<InboundRecord> result = purchaseService.getInboundRecordList(pageQuery);
        return Result.success(result);
    }

    @Operation(summary = "查询入库记录详情")
    @GetMapping("/inbounds/{id}")
    public Result<InboundRecord> getInboundRecordById(@PathVariable Long id) {
        InboundRecord record = purchaseService.getInboundRecordById(id);
        return Result.success(record);
    }

    @Operation(summary = "获取采购统计数据")
    @GetMapping("/stats")
    public Result<PurchaseStatsVO> getPurchaseStats() {
        PurchaseStatsVO stats = purchaseService.getPurchaseStats();
        return Result.success(stats);
    }

}