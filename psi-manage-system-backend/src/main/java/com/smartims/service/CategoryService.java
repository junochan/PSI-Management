package com.smartims.service;

import com.smartims.entity.SysCategory;
import com.smartims.dto.CategoryDTO;

import java.util.List;

/**
 * 商品分类服务接口
 *
 * @author Smart IMS Team
 * @since 2026-04-13
 */
public interface CategoryService {

    /**
     * 获取所有分类列表
     */
    List<SysCategory> getAllCategories();

    /**
     * 获取分类详情
     */
    SysCategory getCategoryById(Long id);

    /**
     * 创建分类
     */
    void createCategory(CategoryDTO dto);

    /**
     * 更新分类
     */
    void updateCategory(Long id, CategoryDTO dto);

    /**
     * 删除分类
     */
    void deleteCategory(Long id);

}