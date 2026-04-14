package com.smartims.controller;

import com.smartims.common.PageResult;
import com.smartims.common.Result;
import com.smartims.dto.PageQuery;
import com.smartims.dto.SupplierDTO;
import com.smartims.entity.Supplier;
import com.smartims.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 供应商管理控制器
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Tag(name = "供应商管理", description = "供应商相关接口")
@RestController
@RequestMapping("/v1/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @Operation(summary = "查询供应商列表")
    @GetMapping
    public Result<PageResult<Supplier>> getSupplierList(PageQuery pageQuery) {
        PageResult<Supplier> result = supplierService.getSupplierList(pageQuery);
        return Result.success(result);
    }

    @Operation(summary = "查询供应商详情")
    @GetMapping("/{id}")
    public Result<Supplier> getSupplierById(@PathVariable Long id) {
        Supplier supplier = supplierService.getSupplierById(id);
        return Result.success(supplier);
    }

    @Operation(summary = "创建供应商")
    @PostMapping
    public Result<Void> createSupplier(@Valid @RequestBody SupplierDTO dto) {
        supplierService.createSupplier(dto);
        return Result.success("供应商创建成功");
    }

    @Operation(summary = "更新供应商")
    @PutMapping("/{id}")
    public Result<Void> updateSupplier(@PathVariable Long id, @Valid @RequestBody SupplierDTO dto) {
        supplierService.updateSupplier(id, dto);
        return Result.success("供应商更新成功");
    }

    @Operation(summary = "删除供应商")
    @DeleteMapping("/{id}")
    public Result<Void> deleteSupplier(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
        return Result.success("供应商删除成功");
    }

    @Operation(summary = "批量删除供应商")
    @DeleteMapping("/batch")
    public Result<Void> batchDeleteSupplier(@RequestBody List<Long> ids) {
        supplierService.batchDeleteSupplier(ids);
        return Result.success("批量删除成功");
    }

}