package com.smartims.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartims.embedding.ProductImageEmbeddingIndexer;
import com.smartims.entity.Inventory;
import com.smartims.entity.Product;
import com.smartims.mapper.InventoryMapper;
import com.smartims.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 库存与商品主图向量缓存同步（对齐参考项目：库存新增/更新时写入 NS_INVENTORY 向量）。
 * 本系统商品图在 {@link Product#getImage()}，按库存主键落盘至 {@code data/image-embeddings/inventory/{id}.json}。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryEmbeddingSyncService {

    private final ProductMapper productMapper;
    private final InventoryMapper inventoryMapper;
    private final ProductImageEmbeddingIndexer productImageEmbeddingIndexer;

    /**
     * 按当前商品主图重建该库存行的向量缓存。
     */
    public void indexInventoryRow(Inventory inventory) {
        if (inventory == null || inventory.getId() == null) {
            return;
        }
        if (inventory.getProductId() == null) {
            productImageEmbeddingIndexer.delete(ProductImageEmbeddingIndexer.NS_INVENTORY, inventory.getId());
            return;
        }
        Product p = productMapper.selectById(inventory.getProductId());
        String img = p != null ? p.getImage() : null;
        try {
            productImageEmbeddingIndexer.indexRow(ProductImageEmbeddingIndexer.NS_INVENTORY, inventory.getId(), img);
        } catch (Exception e) {
            log.warn("库存向量索引失败 inventoryId={}, productId={}", inventory.getId(), inventory.getProductId(), e);
        }
    }

    public void deleteInventoryEmbedding(Long inventoryId) {
        if (inventoryId == null) {
            return;
        }
        productImageEmbeddingIndexer.delete(ProductImageEmbeddingIndexer.NS_INVENTORY, inventoryId);
    }

    /**
     * 商品主图变更时，刷新该商品下全部库存行的向量。
     */
    public void reindexAllByProductId(Long productId) {
        if (productId == null) {
            return;
        }
        LambdaQueryWrapper<Inventory> q = new LambdaQueryWrapper<>();
        q.eq(Inventory::getProductId, productId);
        q.eq(Inventory::getDeleted, 0);
        List<Inventory> list = inventoryMapper.selectList(q);
        for (Inventory inv : list) {
            indexInventoryRow(inv);
        }
    }
}
