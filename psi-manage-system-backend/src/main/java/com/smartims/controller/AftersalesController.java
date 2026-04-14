package com.smartims.controller;

import com.smartims.common.PageResult;
import com.smartims.common.Result;
import com.smartims.dto.AftersalesDTO;
import com.smartims.dto.AftersalesHandleDTO;
import com.smartims.dto.PageQuery;
import com.smartims.entity.AftersalesOrder;
import com.smartims.service.AftersalesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 售后管理控制器
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Tag(name = "售后管理", description = "售后工单相关接口")
@RestController
@RequestMapping("/v1/aftersales")
@RequiredArgsConstructor
public class AftersalesController {

    private final AftersalesService aftersalesService;

    @Operation(summary = "查询售后工单列表")
    @GetMapping
    public Result<PageResult<AftersalesOrder>> getAftersalesList(PageQuery pageQuery) {
        PageResult<AftersalesOrder> result = aftersalesService.getAftersalesList(pageQuery);
        return Result.success(result);
    }

    @Operation(summary = "查询售后工单详情")
    @GetMapping("/{id}")
    public Result<AftersalesOrder> getAftersalesById(@PathVariable Long id) {
        AftersalesOrder order = aftersalesService.getAftersalesById(id);
        return Result.success(order);
    }

    @Operation(summary = "创建售后工单")
    @PostMapping
    public Result<Void> createAftersales(@Valid @RequestBody AftersalesDTO dto) {
        aftersalesService.createAftersales(dto);
        return Result.success("售后工单创建成功");
    }

    @Operation(summary = "处理售后工单")
    @PutMapping("/{id}/handle")
    public Result<Void> handleAftersales(@PathVariable Long id, @Valid @RequestBody AftersalesHandleDTO dto) {
        aftersalesService.handleAftersales(id, dto);
        return Result.success("售后工单处理成功");
    }

    @Operation(summary = "关闭售后工单")
    @PutMapping("/{id}/close")
    public Result<Void> closeAftersales(@PathVariable Long id) {
        aftersalesService.closeAftersales(id);
        return Result.success("售后工单已关闭");
    }

}