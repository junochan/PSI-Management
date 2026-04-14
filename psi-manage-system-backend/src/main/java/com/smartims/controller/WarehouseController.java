package com.smartims.controller;

import com.smartims.annotation.OperationLog;
import com.smartims.common.PageResult;
import com.smartims.common.Result;
import com.smartims.dto.PageQuery;
import com.smartims.dto.WarehouseDTO;
import com.smartims.entity.Warehouse;
import com.smartims.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 仓库管理控制器
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Tag(name = "仓库管理", description = "仓库相关接口")
@RestController
@RequestMapping("/v1/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @Operation(summary = "查询仓库列表")
    @GetMapping
    public Result<PageResult<Warehouse>> getWarehouseList(PageQuery pageQuery) {
        PageResult<Warehouse> result = warehouseService.getWarehouseList(pageQuery);
        return Result.success(result);
    }

    @Operation(summary = "查询仓库详情")
    @GetMapping("/{id}")
    public Result<Warehouse> getWarehouseById(@PathVariable Long id) {
        Warehouse warehouse = warehouseService.getWarehouseById(id);
        return Result.success(warehouse);
    }

    @Operation(summary = "获取所有仓库（不分页）")
    @GetMapping("/all")
    public Result<List<Warehouse>> getAllWarehouses() {
        List<Warehouse> list = warehouseService.getAllWarehouses();
        return Result.success(list);
    }

    @Operation(summary = "创建仓库")
    @OperationLog(module = "仓库管理", action = "新增", description = "创建新仓库")
    @PostMapping
    public Result<Void> createWarehouse(@Valid @RequestBody WarehouseDTO dto) {
        warehouseService.createWarehouse(dto);
        return Result.success("仓库创建成功");
    }

    @Operation(summary = "更新仓库")
    @OperationLog(module = "仓库管理", action = "修改", description = "修改仓库信息")
    @PutMapping("/{id}")
    public Result<Void> updateWarehouse(@PathVariable Long id, @Valid @RequestBody WarehouseDTO dto) {
        warehouseService.updateWarehouse(id, dto);
        return Result.success("仓库更新成功");
    }

    @Operation(summary = "删除仓库")
    @OperationLog(module = "仓库管理", action = "删除", description = "删除仓库")
    @DeleteMapping("/{id}")
    public Result<Void> deleteWarehouse(@PathVariable Long id) {
        warehouseService.deleteWarehouse(id);
        return Result.success("仓库删除成功");
    }

    @Operation(summary = "批量删除仓库")
    @OperationLog(module = "仓库管理", action = "删除", description = "批量删除仓库")
    @DeleteMapping("/batch")
    public Result<Void> batchDeleteWarehouse(@RequestBody List<Long> ids) {
        warehouseService.batchDeleteWarehouse(ids);
        return Result.success("批量删除成功");
    }

}