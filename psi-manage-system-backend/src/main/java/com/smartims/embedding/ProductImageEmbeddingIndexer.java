package com.smartims.embedding;

import com.smartims.service.ProductImageLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
public class ProductImageEmbeddingIndexer {

    private static final Logger log = LoggerFactory.getLogger(ProductImageEmbeddingIndexer.class);

    public static final String NS_INVENTORY = "inventory";

    /** 商品列表以图搜图：按商品 ID 缓存主图向量 */
    public static final String NS_PRODUCT = "product";

    private final ProductImageLoader productImageLoader;
    private final LocalImageEmbeddingStore store;
    private final DashScopeMultimodalEmbeddingService dashScope;
    private final String embeddingModel;

    public ProductImageEmbeddingIndexer(
            ProductImageLoader productImageLoader,
            LocalImageEmbeddingStore store,
            DashScopeMultimodalEmbeddingService dashScope,
            @Value("${app.image-search.embedding-model:tongyi-embedding-vision-flash-2026-03-06}") String embeddingModel) {
        this.productImageLoader = productImageLoader;
        this.store = store;
        this.dashScope = dashScope;
        this.embeddingModel = embeddingModel;
    }

    public void delete(String namespace, Long id) {
        if (id == null) {
            return;
        }
        store.delete(namespace, id);
    }

    public void indexRow(String namespace, Long id, String productImages) {
        if (id == null) {
            return;
        }
        if (!StringUtils.hasText(productImages)) {
            store.delete(namespace, id);
            return;
        }
        String sig = LocalImageEmbeddingStore.sha256Hex(productImages);
        List<String> refs = productImageLoader.allImageStrings(productImages);
        List<float[]> vectors = new ArrayList<>();
        for (String ref : refs) {
            Optional<String> payload = refToImagePayload(ref.trim());
            if (payload.isEmpty()) {
                continue;
            }
            Optional<float[]> vec = dashScope.embedImagePayload(payload.get());
            vec.ifPresent(vectors::add);
        }
        if (vectors.isEmpty()) {
            store.delete(namespace, id);
            return;
        }
        try {
            LocalImageEmbeddingRecord rec = new LocalImageEmbeddingRecord();
            rec.setModel(embeddingModel);
            rec.setImagesSignature(sig);
            rec.setEmbeddings(LocalImageEmbeddingStore.toJsonFloatRows(vectors));
            store.save(namespace, id, rec);
        } catch (IOException e) {
            log.warn("写入本地向量文件失败 namespace={} id={}", namespace, id, e);
        }
    }

    public void ensureIndexed(String namespace, Long id, String productImages) {
        if (id == null || !StringUtils.hasText(productImages)) {
            return;
        }
        String sig = LocalImageEmbeddingStore.sha256Hex(productImages);
        List<float[]> cached = store.loadVectors(namespace, id, embeddingModel, sig);
        if (!cached.isEmpty()) {
            return;
        }
        indexRow(namespace, id, productImages);
    }

    private Optional<String> refToImagePayload(String ref) {
        if (!StringUtils.hasText(ref)) {
            return Optional.empty();
        }
        if (ref.startsWith("data:image")) {
            return Optional.of(ref);
        }
        if (ref.startsWith("http://") || ref.startsWith("https://")) {
            return Optional.of(ref);
        }
        BufferedImage bi = productImageLoader.loadImage(ref);
        if (bi == null) {
            return Optional.empty();
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bi, "png", baos);
            String b64 = Base64.getEncoder().encodeToString(baos.toByteArray());
            return Optional.of("data:image/png;base64," + b64);
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
