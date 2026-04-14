package com.smartims.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartims.dto.CategoryDTO;
import com.smartims.entity.Product;
import com.smartims.entity.SysCategory;
import com.smartims.exception.BusinessException;
import com.smartims.mapper.ProductMapper;
import com.smartims.mapper.SysCategoryMapper;
import com.smartims.service.CategoryService;
import com.smartims.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 商品分类服务实现类
 *
 * @author Smart IMS Team
 * @since 2026-04-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final SysCategoryMapper sysCategoryMapper;
    private final ProductMapper productMapper;

    @Override
    public List<SysCategory> getAllCategories() {
        LambdaQueryWrapper<SysCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysCategory::getDeleted, 0);
        queryWrapper.eq(SysCategory::getStatus, 1);
        queryWrapper.orderByAsc(SysCategory::getSort);
        return sysCategoryMapper.selectList(queryWrapper);
    }

    @Override
    public SysCategory getCategoryById(Long id) {
        SysCategory category = sysCategoryMapper.selectById(id);
        if (category == null || category.getDeleted() == 1) {
            throw new BusinessException("分类不存在");
        }
        return category;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createCategory(CategoryDTO dto) {
        String code;
        if (!StringUtils.hasText(dto.getCode())) {
            code = CodeGenerator.generateCategoryCode();
        } else {
            code = dto.getCode().trim();
            if (categoryCodeExists(code, null)) {
                throw new BusinessException("分类编码已存在");
            }
        }

        SysCategory category = new SysCategory();
        category.setName(dto.getName());
        category.setCode(code);
        category.setParentId(dto.getParentId() != null ? dto.getParentId() : 0L);
        category.setSort(dto.getSort() != null ? dto.getSort() : 0);
        category.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);

        sysCategoryMapper.insert(category);
        log.info("创建分类成功：id={}, name={}, code={}", category.getId(), category.getName(), category.getCode());
    }

    private boolean categoryCodeExists(String code, Long excludeId) {
        LambdaQueryWrapper<SysCategory> w = new LambdaQueryWrapper<>();
        w.eq(SysCategory::getCode, code);
        w.eq(SysCategory::getDeleted, 0);
        if (excludeId != null) {
            w.ne(SysCategory::getId, excludeId);
        }
        return sysCategoryMapper.selectCount(w) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(Long id, CategoryDTO dto) {
        SysCategory category = getCategoryById(id);

        if (dto.getName() != null) {
            category.setName(dto.getName());
        }
        if (dto.getCode() != null) {
            String newCode = dto.getCode().trim();
            if (!StringUtils.hasText(newCode)) {
                throw new BusinessException("分类编码不能为空");
            }
            if (categoryCodeExists(newCode, id)) {
                throw new BusinessException("分类编码已存在");
            }
            category.setCode(newCode);
        }
        if (dto.getParentId() != null) {
            category.setParentId(dto.getParentId());
        }
        if (dto.getSort() != null) {
            category.setSort(dto.getSort());
        }
        if (dto.getStatus() != null) {
            category.setStatus(dto.getStatus());
        }

        sysCategoryMapper.updateById(category);
        log.info("更新分类成功：id={}, name={}", id, category.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        SysCategory category = getCategoryById(id);

        // 检查是否有子分类
        LambdaQueryWrapper<SysCategory> childQuery = new LambdaQueryWrapper<>();
        childQuery.eq(SysCategory::getParentId, id);
        childQuery.eq(SysCategory::getDeleted, 0);
        if (sysCategoryMapper.selectCount(childQuery) > 0) {
            throw new BusinessException("该分类存在子分类，无法删除");
        }

        // 检查是否有商品使用该分类
        LambdaQueryWrapper<Product> productQuery = new LambdaQueryWrapper<>();
        productQuery.eq(Product::getCategoryId, id);
        productQuery.eq(Product::getDeleted, 0);
        long productCount = productMapper.selectCount(productQuery);
        if (productCount > 0) {
            throw new BusinessException("该分类已被" + productCount + "个商品使用，无法删除");
        }

        sysCategoryMapper.deleteById(id);
        log.info("删除分类成功：id={}, name={}", id, category.getName());
    }

}