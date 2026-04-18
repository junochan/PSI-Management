package com.smartims.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartims.common.PageResult;
import com.smartims.dto.CategoryDTO;
import com.smartims.dto.PageQuery;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

    private static final DateTimeFormatter CATEGORY_CODE_DATE = DateTimeFormatter.ofPattern("yyMMdd");

    private final SysCategoryMapper sysCategoryMapper;
    private final ProductMapper productMapper;

    @Override
    public List<SysCategory> listCategories(Integer status, String name, String code) {
        LambdaQueryWrapper<SysCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysCategory::getDeleted, 0);
        if (status != null) {
            queryWrapper.eq(SysCategory::getStatus, status);
        }
        if (StringUtils.hasText(name)) {
            queryWrapper.like(SysCategory::getName, name.trim());
        }
        if (StringUtils.hasText(code)) {
            queryWrapper.like(SysCategory::getCode, code.trim());
        }
        queryWrapper.orderByAsc(SysCategory::getSort);
        return sysCategoryMapper.selectList(queryWrapper);
    }

    @Override
    public PageResult<SysCategory> pageCategories(PageQuery pageQuery, Integer status) {
        LambdaQueryWrapper<SysCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysCategory::getDeleted, 0);
        if (status != null) {
            queryWrapper.eq(SysCategory::getStatus, status);
        }
        if (StringUtils.hasText(pageQuery.getKeyword())) {
            String kw = pageQuery.getKeyword().trim();
            queryWrapper.and(w -> w.like(SysCategory::getName, kw).or().like(SysCategory::getCode, kw));
        }
        queryWrapper.orderByAsc(SysCategory::getSort);
        Page<SysCategory> page = new Page<>(pageQuery.getPage(), pageQuery.getSize());
        Page<SysCategory> result = sysCategoryMapper.selectPage(page, queryWrapper);
        return PageResult.build(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
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
        String categoryName = normalizeCategoryName(dto.getName());
        if (categoryNameExists(categoryName, null)) {
            throw new BusinessException("分类名称已存在");
        }

        SysCategory category = new SysCategory();
        category.setName(categoryName);
        category.setParentId(dto.getParentId() != null ? dto.getParentId() : 0L);
        category.setSort(dto.getSort() != null ? dto.getSort() : 0);
        category.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);

        if (!StringUtils.hasText(dto.getCode())) {
            category.setCode(allocateCategoryCode());
            sysCategoryMapper.insert(category);
            log.info("创建分类成功：id={}, name={}, code={}", category.getId(), category.getName(), category.getCode());
        } else {
            String code = dto.getCode().trim();
            if (categoryCodeExists(code, null)) {
                throw new BusinessException("分类编码已存在");
            }
            category.setCode(code);
            sysCategoryMapper.insert(category);
            log.info("创建分类成功：id={}, name={}, code={}", category.getId(), category.getName(), category.getCode());
        }
    }

    /**
     * 生成当日唯一分类编码，与 {@link CodeGenerator#generateCategoryCode()} 格式一致。
     */
    private String allocateCategoryCode() {
        String dateStr = LocalDate.now().format(CATEGORY_CODE_DATE);
        String prefix = CodeGenerator.CATEGORY_PREFIX + dateStr;
        String maxCode = sysCategoryMapper.selectMaxCodeByDatePrefix(prefix);
        int nextSeq = 1;
        if (maxCode != null && maxCode.length() >= prefix.length() + 4) {
            try {
                String tail = maxCode.substring(maxCode.length() - 4);
                nextSeq = Integer.parseInt(tail) + 1;
            } catch (NumberFormatException ignored) {
                nextSeq = 1;
            }
        }
        if (nextSeq < 1 || nextSeq > 9999) {
            throw new BusinessException("当日分类编码序号已用尽，请联系管理员");
        }
        return prefix + String.format("%04d", nextSeq);
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

    private boolean categoryNameExists(String name, Long excludeId) {
        LambdaQueryWrapper<SysCategory> w = new LambdaQueryWrapper<>();
        w.eq(SysCategory::getName, name);
        w.eq(SysCategory::getDeleted, 0);
        if (excludeId != null) {
            w.ne(SysCategory::getId, excludeId);
        }
        return sysCategoryMapper.selectCount(w) > 0;
    }

    private String normalizeCategoryName(String name) {
        String normalizedName = name == null ? null : name.trim();
        if (!StringUtils.hasText(normalizedName)) {
            throw new BusinessException("分类名称不能为空");
        }
        return normalizedName;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(Long id, CategoryDTO dto) {
        SysCategory category = getCategoryById(id);

        if (dto.getName() != null) {
            String categoryName = normalizeCategoryName(dto.getName());
            if (categoryNameExists(categoryName, id)) {
                throw new BusinessException("分类名称已存在");
            }
            category.setName(categoryName);
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