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
     * 分类列表
     *
     * @param status 为 null 时不按状态过滤；为 1 仅启用，为 0 仅禁用（与实体 status 一致）
     * @param name    非空时对分类名称模糊匹配（LIKE）
     * @param code    非空时对分类编码模糊匹配（LIKE）；可与 name 同时传入，条件为 AND
     */
    List<SysCategory> listCategories(Integer status, String name, String code);

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