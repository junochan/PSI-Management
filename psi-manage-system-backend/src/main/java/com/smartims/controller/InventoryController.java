package com.smartims.controller;

import com.smartims.annotation.OperationLog;
import com.smartims.common.PageResult;
import com.smartims.common.Result;
import com.smartims.dto.InventoryImageSearchRequest;
import com.smartims.dto.InventoryTransferDTO;
import com.smartims.dto.ManualInboundDTO;
import com.smartims.dto.PageQuery;
import com.smartims.entity.Inventory;
import com.smartims.entity.InventoryTransfer;
import com.smartims.entity.InventoryWarning;
import com.smartims.entity.OutboundRecord;
import com.smartims.service.InventoryService;
import com.smartims.vo.InventoryStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 库存管理控制器
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Tag(name = "库存管理", description = "库存、调拨、预警相关接口")
@RestController
@RequestMapping("/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @Operation(summary = "查询库存列表")
    @GetMapping
    public Result<PageResult<Inventory>> getInventoryList(PageQuery pageQuery) {
        PageResult<Inventory> result = inventoryService.getInventoryList(pageQuery);
        return Result.success(result);
    }

    @Operation(summary = "以图搜图（按商品主图相似度排序）")
    @PostMapping(value = "/search-by-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<PageResult<Inventory>> searchByImage(
            @RequestParam("image") MultipartFile image,
            @Valid @ModelAttribute InventoryImageSearchRequest request) {
        PageResult<Inventory> result = inventoryService.searchByImage(request, image);
        return Result.success(result);
    }

    @Operation(summary = "分页查询调拨记录")
    @GetMapping("/transfers")
    public Result<PageResult<InventoryTransfer>> getTransferList(PageQuery pageQuery) {
        PageResult<InventoryTransfer> result = inventoryService.getTransferList(pageQuery);
        return Result.success(result);
    }

    @Operation(summary = "查询调拨单详情")
    @GetMapping("/transfers/{id}")
    public Result<InventoryTransfer> getTransferById(@PathVariable Long id) {
        InventoryTransfer transfer = inventoryService.getTransferById(id);
        return Result.success(transfer);
    }

    @Operation(summary = "获取库存统计数据")
    @GetMapping("/stats")
    public Result<InventoryStatsVO> getInventoryStats() {
        InventoryStatsVO stats = inventoryService.getInventoryStats();
        return Result.success(stats);
    }

    @Operation(summary = "分页查询库存预警列表")
    @GetMapping("/warnings")
    public Result<PageResult<InventoryWarning>> getWarningList(PageQuery pageQuery) {
        PageResult<InventoryWarning> result = inventoryService.getWarningList(pageQuery);
        return Result.success(result);
    }

    @Operation(summary = "查询库存详情")
    @GetMapping("/{id}")
    public Result<Inventory> getInventoryById(@PathVariable Long id) {
        Inventory inventory = inventoryService.getInventoryById(id);
        return Result.success(inventory);
    }

    @Operation(summary = "查询商品库存分布")
    @GetMapping("/product/{productId}")
    public Result<List<Inventory>> getInventoryByProductId(@PathVariable Long productId) {
        List<Inventory> list = inventoryService.getInventoryByProductId(productId);
        return Result.success(list);
    }

    @Operation(summary = "创建调拨单")
    @PostMapping("/transfers")
    @OperationLog(module = "库存管理", action = "调拨", description = "创建库存调拨单")
    public Result<Void> createTransfer(@Valid @RequestBody InventoryTransferDTO dto) {
        inventoryService.createTransfer(dto);
        return Result.success("调拨单创建成功");
    }

    @Operation(summary = "确认调拨")
    @PutMapping("/transfers/{id}/confirm")
    @OperationLog(module = "库存管理", action = "调拨", description = "确认库存调拨")
    public Result<Void> confirmTransfer(@PathVariable Long id) {
        inventoryService.confirmTransfer(id);
        return Result.success("调拨确认成功");
    }

    @Operation(summary = "处理库存预警")
    @PutMapping("/warnings/{id}/handle")
    @OperationLog(module = "库存管理", action = "预警", description = "处理库存预警")
    public Result<Void> handleWarning(@PathVariable Long id, @RequestParam String handleRemark) {
        inventoryService.handleWarning(id, handleRemark);
        return Result.success("预警处理成功");
    }

    @Operation(summary = "手动入库")
    @PostMapping("/inbound")
    @OperationLog(module = "库存管理", action = "入库", description = "手动入库操作")
    public Result<Void> manualInbound(@RequestParam Long inventoryId, @RequestParam Integer quantity, @RequestParam(required = false) String remark) {
        inventoryService.manualInbound(inventoryId, quantity, remark);
        return Result.success("入库成功");
    }

    @Operation(summary = "手动出库")
    @PostMapping("/outbound")
    @OperationLog(module = "库存管理", action = "出库", description = "手动出库操作")
    public Result<Void> manualOutbound(@RequestParam Long inventoryId, @RequestParam Integer quantity, @RequestParam(required = false) String remark) {
        inventoryService.manualOutbound(inventoryId, quantity, remark);
        return Result.success("出库成功");
    }

    @Operation(summary = "手动入库（初始化商品库存）")
    @PostMapping("/inbound/new")
    @OperationLog(module = "库存管理", action = "入库", description = "初始化商品库存入库")
    public Result<Void> manualInboundNew(@Valid @RequestBody ManualInboundDTO dto) {
        inventoryService.manualInboundNew(dto);
        return Result.success("入库成功");
    }

    @Operation(summary = "更新库存预警值")
    @PutMapping("/{id}/safe-stock")
    public Result<Void> updateSafeStock(@PathVariable Long id, @RequestParam Integer safeStock) {
        inventoryService.updateSafeStock(id, safeStock);
        return Result.success("库存预警值更新成功");
    }

    @Operation(summary = "更新库位")
    @PutMapping("/{id}/location")
    public Result<Void> updateLocation(@PathVariable Long id, @RequestParam String location) {
        inventoryService.updateLocation(id, location);
        return Result.success("库位更新成功");
    }

    @Operation(summary = "查询出库记录列表")
    @GetMapping("/outbounds")
    public Result<PageResult<OutboundRecord>> getOutboundRecordList(PageQuery pageQuery) {
        PageResult<OutboundRecord> result = inventoryService.getOutboundRecordList(pageQuery);
        return Result.success(result);
    }

    @Operation(summary = "更新呆滞预警天数")
    @PutMapping("/{id}/stagnant-days")
    public Result<Void> updateStagnantDays(@PathVariable Long id, @RequestParam Integer stagnantDays) {
        inventoryService.updateStagnantDays(id, stagnantDays);
        return Result.success("呆滞预警天数更新成功");
    }

}