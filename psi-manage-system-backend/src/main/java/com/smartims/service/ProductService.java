package com.smartims.service;

import com.smartims.common.PageResult;
import com.smartims.dto.PageQuery;
import com.smartims.dto.ProductDTO;
import com.smartims.dto.ProductImageSearchRequest;
import com.smartims.entity.Product;

import java.util.List;

/**
 * 商品服务接口
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
public interface ProductService {

    /**
     * 分页查询商品列表
     */
    PageResult<Product> getProductList(PageQuery pageQuery);

    /**
     * 以图搜图：在筛选条件内按商品主图与查询图的向量相似度排序并分页
     */
    PageResult<Product> searchByImage(ProductImageSearchRequest request);

    /**
     * 根据ID查询商品详情
     */
    Product getProductById(Long id);

    /**
     * 添加商品
     */
    void addProduct(ProductDTO dto);

    /**
     * 更新商品
     */
    void updateProduct(Long id, ProductDTO dto);

    /**
     * 删除商品
     */
    void deleteProduct(Long id);

    /**
     * 批量删除商品
     */
    void batchDeleteProduct(List<Long> ids);

}