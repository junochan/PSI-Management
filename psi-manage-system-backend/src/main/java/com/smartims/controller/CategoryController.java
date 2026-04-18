package com.smartims.controller;

import com.smartims.annotation.OperationLog;
import com.smartims.common.PageResult;
import com.smartims.common.Result;
import com.smartims.dto.PageQuery;
import com.smartims.dto.CategoryDTO;
import com.smartims.entity.SysCategory;
import com.smartims.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品分类管理控制器
 *
 * @author Smart IMS Team
 * @since 2026-04-13
 */
@Tag(name = "商品分类管理", description = "商品分类相关接口")
@RestController
@RequestMapping("/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "获取分类列表",
            description = "status：不传不按状态过滤；1 仅启用；0 仅禁用。name、code 可选，分别对分类名称、编码模糊查询（同时传为 AND）")
    @GetMapping
    public Result<List<SysCategory>> listCategories(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code) {
        List<SysCategory> categories = categoryService.listCategories(status, name, code);
        return Result.success(categories);
    }

    @Operation(summary = "分页获取分类列表", description = "keyword：名称或编码模糊；status：1 启用 0 禁用，不传则不过滤状态")
    @GetMapping("/page")
    public Result<PageResult<SysCategory>> pageCategories(
            PageQuery pageQuery,
            @RequestParam(required = false) Integer status) {
        return Result.success(categoryService.pageCategories(pageQuery, status));
    }

    @Operation(summary = "获取分类详情")
    @GetMapping("/{id}")
    public Result<SysCategory> getCategoryById(@PathVariable Long id) {
        SysCategory category = categoryService.getCategoryById(id);
        return Result.success(category);
    }

    @Operation(summary = "创建分类")
    @OperationLog(module = "分类管理", action = "新增", description = "创建商品分类")
    @PostMapping
    public Result<Void> createCategory(@RequestBody CategoryDTO dto) {
        categoryService.createCategory(dto);
        return Result.success("分类创建成功");
    }

    @Operation(summary = "更新分类")
    @OperationLog(module = "分类管理", action = "修改", description = "修改商品分类")
    @PutMapping("/{id}")
    public Result<Void> updateCategory(@PathVariable Long id, @RequestBody CategoryDTO dto) {
        categoryService.updateCategory(id, dto);
        return Result.success("分类更新成功");
    }

    @Operation(summary = "删除分类")
    @OperationLog(module = "分类管理", action = "删除", description = "删除商品分类")
    @DeleteMapping("/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success("分类删除成功");
    }

}