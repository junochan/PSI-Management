package com.smartims.embedding;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToLongFunction;

@Component
public class ImageVectorSearchHelper {

    private final LocalImageEmbeddingStore store;
    private final ProductImageEmbeddingIndexer indexer;
    private final String embeddingModel;
    private final double defaultThreshold;

    public ImageVectorSearchHelper(
            LocalImageEmbeddingStore store,
            ProductImageEmbeddingIndexer indexer,
            @Value("${app.image-search.embedding-model:tongyi-embedding-vision-flash-2026-03-06}") String embeddingModel,
            @Value("${app.image-search.similarity-threshold:0.7}") double defaultThreshold) {
        this.store = store;
        this.indexer = indexer;
        this.embeddingModel = embeddingModel;
        this.defaultThreshold = Math.min(1.0, Math.max(0.0, defaultThreshold));
    }

    public <T> List<T> sortByVectorSimilarity(
            List<T> list,
            float[] queryVector,
            String namespace,
            ToLongFunction<T> idGetter,
            Function<T, String> imagesGetter,
            Double requestThreshold) {
        double t = requestThreshold != null
                ? Math.min(1.0, Math.max(0.0, requestThreshold))
                : defaultThreshold;
        List<Scored<T>> scored = new ArrayList<>();
        for (T item : list) {
            long id = idGetter.applyAsLong(item);
            String imgs = imagesGetter.apply(item);
            if (!StringUtils.hasText(imgs)) {
                continue;
            }
            indexer.ensureIndexed(namespace, id, imgs);
            String sig = LocalImageEmbeddingStore.sha256Hex(imgs);
            List<float[]> vecs = store.loadVectors(namespace, id, embeddingModel, sig);
            double best = maxCosine(queryVector, vecs);
            if (best >= t) {
                scored.add(new Scored<>(item, best));
            }
        }
        scored.sort(Comparator.comparingDouble((Scored<T> s) -> s.score).reversed());
        List<T> out = new ArrayList<>(scored.size());
        for (Scored<T> s : scored) {
            out.add(s.item);
        }
        return out;
    }

    private static double maxCosine(float[] query, List<float[]> candidates) {
        if (query == null || query.length == 0 || candidates == null || candidates.isEmpty()) {
            return -1.0;
        }
        double best = -1.0;
        for (float[] v : candidates) {
            if (v == null || v.length != query.length) {
                continue;
            }
            double c = cosine(query, v);
            if (c > best) {
                best = c;
            }
        }
        return best;
    }

    private static double cosine(float[] a, float[] b) {
        double dot = 0.0;
        double na = 0.0;
        double nb = 0.0;
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            dot += x * y;
            na += x * x;
            nb += y * y;
        }
        if (na <= 0.0 || nb <= 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private static final class Scored<T> {
        final T item;
        final double score;

        Scored(T item, double score) {
            this.item = item;
            this.score = score;
        }
    }
}
