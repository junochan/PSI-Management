package com.smartims.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartims.common.PageResult;
import com.smartims.dto.PageQuery;
import com.smartims.dto.ProductDTO;
import com.smartims.dto.ProductImageSearchRequest;
import com.smartims.embedding.DashScopeMultimodalEmbeddingService;
import com.smartims.embedding.ImagePayloadUtil;
import com.smartims.embedding.ImageVectorSearchHelper;
import com.smartims.embedding.ProductImageEmbeddingIndexer;
import com.smartims.entity.Inventory;
import com.smartims.entity.Product;
import com.smartims.entity.Warehouse;
import com.smartims.exception.BusinessException;
import com.smartims.mapper.InventoryMapper;
import com.smartims.mapper.ProductMapper;
import com.smartims.mapper.WarehouseMapper;
import com.smartims.service.InventoryEmbeddingSyncService;
import com.smartims.service.ProductImageStorageService;
import com.smartims.service.ProductService;
import com.smartims.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 商品服务实现类
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final int IMAGE_SEARCH_MAX_ROWS = 5000;

    private static final DateTimeFormatter PRODUCT_CODE_DATE = DateTimeFormatter.ofPattern("yyMMdd");

    private final ProductMapper productMapper;
    private final InventoryMapper inventoryMapper;
    private final WarehouseMapper warehouseMapper;
    private final InventoryEmbeddingSyncService inventoryEmbeddingSyncService;
    private final ProductImageStorageService productImageStorageService;
    private final DashScopeMultimodalEmbeddingService dashScopeMultimodalEmbeddingService;
    private final ImageVectorSearchHelper imageVectorSearchHelper;

    private static void validateProductImageField(String image) {
        if (!StringUtils.hasText(image)) {
            return;
        }
        if (image.trim().startsWith("data:")) {
            throw new BusinessException("请先上传图片，数据库仅保存图片访问地址");
        }
    }

    @Override
    public PageResult<Product> getProductList(PageQuery pageQuery) {
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getDeleted, 0);

        if (StringUtils.hasText(pageQuery.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper
                    .like(Product::getName, pageQuery.getKeyword())
                    .or()
                    .like(Product::getCode, pageQuery.getKeyword())
                    .or()
                    .like(Product::getCategoryName, pageQuery.getKeyword())
            );
        }

        if (StringUtils.hasText(pageQuery.getCategoryName())) {
            queryWrapper.eq(Product::getCategoryName, pageQuery.getCategoryName());
        }
        if (StringUtils.hasText(pageQuery.getProductStatus())) {
            queryWrapper.eq(Product::getStatus, pageQuery.getProductStatus());
        }

        if (StringUtils.hasText(pageQuery.getSort())) {
            if ("asc".equalsIgnoreCase(pageQuery.getOrder())) {
                queryWrapper.orderByAsc(Product::getCreateTime);
            } else {
                queryWrapper.orderByDesc(Product::getCreateTime);
            }
        } else {
            queryWrapper.orderByDesc(Product::getCreateTime);
        }

        Page<Product> page = new Page<>(pageQuery.getPage(), pageQuery.getSize());
        Page<Product> result = productMapper.selectPage(page, queryWrapper);

        return PageResult.build(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public PageResult<Product> searchByImage(ProductImageSearchRequest req) {
        long page = req.getPage() != null && req.getPage() >= 1 ? req.getPage() : 1;
        long size = req.getSize() != null && req.getSize() >= 1 ? Math.min(req.getSize(), 100) : 10;

        String qPayload;
        try {
            qPayload = ImagePayloadUtil.toPngDataUrl(req.getImageBase64().trim());
        } catch (Exception e) {
            throw new BusinessException("无法解析查询图片，请上传有效的图片文件");
        }
        var qVec = dashScopeMultimodalEmbeddingService.embedImagePayload(qPayload);
        if (qVec.isEmpty()) {
            log.warn("查询图向量化失败，请配置环境变量 DASHSCOPE_API_KEY 或 application.yml 中 dashscope.api-key");
            return PageResult.build(0L, page, size, new ArrayList<>());
        }
        float[] queryVector = qVec.get();

        LambdaQueryWrapper<Product> wrapper = buildProductImageSearchQueryWrapper(req);
        List<Product> list = productMapper.selectList(wrapper);

        if (list.size() > IMAGE_SEARCH_MAX_ROWS) {
            list = new ArrayList<>(list.subList(0, IMAGE_SEARCH_MAX_ROWS));
        }

        List<Product> sorted = imageVectorSearchHelper.sortByVectorSimilarity(
                list,
                queryVector,
                ProductImageEmbeddingIndexer.NS_PRODUCT,
                Product::getId,
                Product::getImage,
                req.getSimilarityThreshold());
        long total = sorted.size();
        int from = (int) ((page - 1) * size);
        List<Product> pageRecords = new ArrayList<>();
        if (from < total) {
            int to = (int) Math.min(from + size, total);
            pageRecords = new ArrayList<>(sorted.subList(from, to));
        }
        return PageResult.build(total, page, size, pageRecords);
    }

    private LambdaQueryWrapper<Product> buildProductImageSearchQueryWrapper(ProductImageSearchRequest req) {
        LambdaQueryWrapper<Product> q = new LambdaQueryWrapper<>();
        q.eq(Product::getDeleted, 0);
        if (StringUtils.hasText(req.getKeyword())) {
            String k = req.getKeyword().trim();
            q.and(w -> w.like(Product::getName, k)
                    .or().like(Product::getCode, k)
                    .or().like(Product::getCategoryName, k));
        }
        if (StringUtils.hasText(req.getCategoryName())) {
            q.eq(Product::getCategoryName, req.getCategoryName().trim());
        }
        if (StringUtils.hasText(req.getStatus())) {
            q.eq(Product::getStatus, req.getStatus().trim());
        }
        return q;
    }

    @Override
    public Product getProductById(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null || product.getDeleted() == 1) {
            throw new BusinessException("商品不存在");
        }
        return product;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addProduct(ProductDTO dto) {
        validateProductImageField(dto.getImage());

        Product product = new Product();
        product.setName(dto.getName());
        product.setBrand(dto.getBrand());
        product.setSpec(dto.getSpec());
        product.setCategoryId(dto.getCategoryId() != null ? dto.getCategoryId() : 1L);
        product.setCategoryName(dto.getCategoryName());
        product.setCostPrice(dto.getCostPrice());
        product.setSalePrice(dto.getSalePrice());
        product.setStatus(dto.getStatus() != null ? dto.getStatus() : "在售");
        product.setImage(dto.getImage());
        product.setDescription(dto.getDescription());
        product.setStock(dto.getInitialStock() != null ? dto.getInitialStock() : 0);
        product.setSafeStock(dto.getSafeStock() != null ? dto.getSafeStock() : 10);

        insertProductWithAllocatedCode(product);
        log.info("添加商品成功：id={}, name={}", product.getId(), product.getName());

        // 同步创建库存记录（如果指定了初始库存和仓库）
        if (dto.getInitialStock() != null && dto.getInitialStock() > 0 && dto.getWarehouseId() != null) {
            Warehouse warehouse = warehouseMapper.selectById(dto.getWarehouseId());
            if (warehouse == null || warehouse.getDeleted() == 1) {
                throw new BusinessException("仓库不存在");
            }

            Inventory inventory = new Inventory();
            inventory.setSku(product.getCode());
            inventory.setProductId(product.getId());
            inventory.setProductName(product.getName());
            inventory.setSpec(product.getSpec());
            inventory.setCategory(product.getCategoryName());
            inventory.setWarehouseId(dto.getWarehouseId());
            inventory.setWarehouseName(warehouse.getName());
            inventory.setStock(dto.getInitialStock());
            inventory.setSafeStock(dto.getSafeStock() != null ? dto.getSafeStock() : 10);
            inventory.setCostPrice(product.getCostPrice());
            inventory.setStockValue(product.getCostPrice() != null ?
                product.getCostPrice().multiply(new BigDecimal(dto.getInitialStock())) : BigDecimal.ZERO);
            inventory.setStatus(dto.getInitialStock() >= (dto.getSafeStock() != null ? dto.getSafeStock() : 10) ? "normal" : "warning");
            inventory.setLastInboundTime(LocalDateTime.now());

            inventoryMapper.insert(inventory);
            inventoryEmbeddingSyncService.indexInventoryRow(inventory);
            log.info("同步创建库存记录：productId={}, warehouseId={}, stock={}", product.getId(), dto.getWarehouseId(), dto.getInitialStock());
        }
    }

    /**
     * 写入商品：按「PROD + 当日日期 + 序号」分配编码（取库中当日最大值+1），并发冲突时重试。
     */
    private void insertProductWithAllocatedCode(Product product) {
        final int maxAttempts = 5;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            product.setCode(allocateProductCode());
            try {
                productMapper.insert(product);
                return;
            } catch (DuplicateKeyException e) {
                log.warn("商品编码冲突，重新分配 attempt={} code={}", attempt + 1, product.getCode());
                if (attempt == maxAttempts - 1) {
                    log.error("商品编码多次冲突，放弃重试", e);
                    throw new BusinessException("商品编码生成失败，请稍后重试");
                }
            }
        }
    }

    /**
     * 生成当日唯一商品编码，与 {@link CodeGenerator#generateProductCode()} 格式一致。
     */
    private String allocateProductCode() {
        String dateStr = LocalDate.now().format(PRODUCT_CODE_DATE);
        String prefix = CodeGenerator.PRODUCT_PREFIX + dateStr;
        String maxCode = productMapper.selectMaxCodeByDatePrefix(prefix);
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
            throw new BusinessException("当日商品编码序号已用尽，请联系管理员");
        }
        return prefix + String.format("%04d", nextSeq);
    }

    @Override
    public void updateProduct(Long id, ProductDTO dto) {
        validateProductImageField(dto.getImage());

        Product existing = getProductById(id);
        String previousImage = existing.getImage();

        // 检查编码是否重复（排除自身）
        String newCode = dto.getCode() != null ? dto.getCode() : existing.getCode();
        if (!existing.getCode().equals(newCode)) {
            LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Product::getCode, newCode);
            queryWrapper.eq(Product::getDeleted, 0);
            queryWrapper.ne(Product::getId, id);
            if (productMapper.selectCount(queryWrapper) > 0) {
                throw new BusinessException("商品编码已存在");
            }
        }

        existing.setCode(dto.getCode() != null ? dto.getCode() : existing.getCode());
        existing.setName(dto.getName() != null ? dto.getName() : existing.getName());
        existing.setBrand(dto.getBrand());
        existing.setSpec(dto.getSpec());
        existing.setCategoryId(dto.getCategoryId() != null ? dto.getCategoryId() : existing.getCategoryId());
        existing.setCategoryName(dto.getCategoryName() != null ? dto.getCategoryName() : existing.getCategoryName());
        existing.setCostPrice(dto.getCostPrice() != null ? dto.getCostPrice() : existing.getCostPrice());
        existing.setSalePrice(dto.getSalePrice() != null ? dto.getSalePrice() : existing.getSalePrice());
        existing.setStatus(dto.getStatus() != null ? dto.getStatus() : existing.getStatus());
        existing.setImage(dto.getImage());
        existing.setDescription(dto.getDescription());
        // 注意：编辑商品时不允许修改库存，库存只能通过入库/出库操作变更

        productMapper.updateById(existing);
        log.info("更新商品成功：id={}, name={}", id, existing.getName());

        // 同步更新库存表中的商品信息
        LambdaQueryWrapper<Inventory> invQuery = new LambdaQueryWrapper<>();
        invQuery.eq(Inventory::getProductId, id);
        invQuery.eq(Inventory::getDeleted, 0);
        List<Inventory> inventories = inventoryMapper.selectList(invQuery);
        for (Inventory inv : inventories) {
            inv.setSku(existing.getCode());
            inv.setProductName(existing.getName());
            inv.setSpec(existing.getSpec());
            inv.setCategory(existing.getCategoryName());
            inv.setCostPrice(existing.getCostPrice());
            if (existing.getCostPrice() != null && inv.getStock() != null) {
                inv.setStockValue(existing.getCostPrice().multiply(new BigDecimal(inv.getStock())));
            }
            inventoryMapper.updateById(inv);
        }
        log.info("同步更新库存表商品信息：productId={}, updatedCount={}", id, inventories.size());

        if (!Objects.equals(previousImage, existing.getImage())) {
            productImageStorageService.deleteManagedImageIfPresent(previousImage);
            inventoryEmbeddingSyncService.reindexAllByProductId(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long id) {
        Product product = getProductById(id);

        // 检查是否存在库存记录
        LambdaQueryWrapper<Inventory> invQuery = new LambdaQueryWrapper<>();
        invQuery.eq(Inventory::getProductId, id);
        invQuery.eq(Inventory::getDeleted, 0);
        long inventoryCount = inventoryMapper.selectCount(invQuery);
        if (inventoryCount > 0) {
            throw new BusinessException("该商品存在库存记录，无法删除。请先清理库存后再删除商品。");
        }

        productImageStorageService.deleteManagedImageIfPresent(product.getImage());
        productMapper.deleteById(id);
        log.info("删除商品成功：id={}, name={}", id, product.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteProduct(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要删除的商品");
        }

        // 检查每个商品是否存在库存记录
        for (Long id : ids) {
            LambdaQueryWrapper<Inventory> invQuery = new LambdaQueryWrapper<>();
            invQuery.eq(Inventory::getProductId, id);
            invQuery.eq(Inventory::getDeleted, 0);
            long inventoryCount = inventoryMapper.selectCount(invQuery);
            if (inventoryCount > 0) {
                Product product = productMapper.selectById(id);
                throw new BusinessException("商品「" + (product != null ? product.getName() : id) + "」存在库存记录，无法删除");
            }
        }

        for (Long id : ids) {
            Product p = productMapper.selectById(id);
            if (p != null) {
                productImageStorageService.deleteManagedImageIfPresent(p.getImage());
            }
        }
        productMapper.deleteBatchIds(ids);
        log.info("批量删除商品成功：ids={}", ids);
    }

}