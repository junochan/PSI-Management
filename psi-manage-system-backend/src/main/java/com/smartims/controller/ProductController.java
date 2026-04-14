package com.smartims.controller;

import com.smartims.annotation.OperationLog;
import com.smartims.common.PageResult;
import com.smartims.common.Result;
import com.smartims.dto.PageQuery;
import com.smartims.dto.ProductDTO;
import com.smartims.dto.ProductImageSearchRequest;
import com.smartims.entity.Product;
import com.smartims.exception.BusinessException;
import com.smartims.service.ProductImageStorageService;
import com.smartims.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 商品控制器
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Tag(name = "商品管理", description = "商品相关接口")
@Slf4j
@RestController
@RequestMapping("/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductImageStorageService productImageStorageService;

    @Operation(summary = "上传商品图片（本地存储，返回可写入商品的 URL）")
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, String>> uploadProductImage(@RequestParam("file") MultipartFile file) {
        try {
            String url = productImageStorageService.saveProductImage(file);
            return Result.success(Map.of("url", url));
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("保存商品图片失败", e);
            throw new BusinessException("图片保存失败");
        }
    }

    @Operation(summary = "查询商品列表")
    @GetMapping
    public Result<PageResult<Product>> getProductList(PageQuery pageQuery) {
        PageResult<Product> result = productService.getProductList(pageQuery);
        return Result.success(result);
    }

    @Operation(summary = "以图搜图（按商品主图相似度排序）")
    @PostMapping("/search-by-image")
    public Result<PageResult<Product>> searchByImage(@Valid @RequestBody ProductImageSearchRequest request) {
        PageResult<Product> result = productService.searchByImage(request);
        return Result.success(result);
    }

    @Operation(summary = "查询商品详情")
    @GetMapping("/{id}")
    public Result<Product> getProductById(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        return Result.success(product);
    }

    @Operation(summary = "添加商品")
    @OperationLog(module = "商品管理", action = "新增", description = "新增商品信息")
    @PostMapping
    public Result<Void> addProduct(@Valid @RequestBody ProductDTO dto) {
        productService.addProduct(dto);
        return Result.success("商品添加成功");
    }

    @Operation(summary = "更新商品")
    @OperationLog(module = "商品管理", action = "修改", description = "修改商品信息")
    @PutMapping("/{id}")
    public Result<Void> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductDTO dto) {
        productService.updateProduct(id, dto);
        return Result.success("商品更新成功");
    }

    @Operation(summary = "删除商品")
    @OperationLog(module = "商品管理", action = "删除", description = "删除商品")
    @DeleteMapping("/{id}")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return Result.success("商品删除成功");
    }

    @Operation(summary = "批量删除商品")
    @OperationLog(module = "商品管理", action = "删除", description = "批量删除商品")
    @DeleteMapping("/batch")
    public Result<Void> batchDeleteProduct(@RequestBody List<Long> ids) {
        productService.batchDeleteProduct(ids);
        return Result.success("批量删除成功");
    }

}