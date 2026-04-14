package com.smartims.embedding;

import com.alibaba.dashscope.embeddings.MultiModalEmbedding;
import com.alibaba.dashscope.embeddings.MultiModalEmbeddingItemBase;
import com.alibaba.dashscope.embeddings.MultiModalEmbeddingItemImage;
import com.alibaba.dashscope.embeddings.MultiModalEmbeddingOutput;
import com.alibaba.dashscope.embeddings.MultiModalEmbeddingParam;
import com.alibaba.dashscope.embeddings.MultiModalEmbeddingResult;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.dashscope.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class DashScopeMultimodalEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(DashScopeMultimodalEmbeddingService.class);

    private static final Object DASHSCOPE_API_KEY_LOCK = new Object();

    private final String embeddingModel;
    private final String apiKey;

    public DashScopeMultimodalEmbeddingService(
            @Value("${app.image-search.embedding-model:tongyi-embedding-vision-flash-2026-03-06}") String embeddingModel,
            @Value("${dashscope.api-key:}") String apiKey) {
        this.embeddingModel = embeddingModel;
        this.apiKey = apiKey;
    }

    private String resolveApiKey() {
        if (StringUtils.hasText(apiKey)) {
            return apiKey.trim();
        }
        String env = System.getenv("DASHSCOPE_API_KEY");
        return StringUtils.hasText(env) ? env.trim() : null;
    }

    public Optional<float[]> embedImagePayload(String imagePayload) {
        String key = resolveApiKey();
        if (!StringUtils.hasText(key)) {
            log.warn("未配置 DASHSCOPE_API_KEY 或 dashscope.api-key，无法生成图像向量");
            return Optional.empty();
        }
        if (!StringUtils.hasText(imagePayload)) {
            return Optional.empty();
        }
        try {
            List<MultiModalEmbeddingItemBase> contents =
                    Collections.singletonList(new MultiModalEmbeddingItemImage(imagePayload.trim()));
            MultiModalEmbeddingParam param = MultiModalEmbeddingParam.builder()
                    .model(embeddingModel)
                    .contents(contents)
                    .build();
            MultiModalEmbedding client = new MultiModalEmbedding();
            MultiModalEmbeddingResult result;
            synchronized (DASHSCOPE_API_KEY_LOCK) {
                String prev = Constants.apiKey;
                try {
                    Constants.apiKey = key;
                    result = client.call(param);
                } finally {
                    Constants.apiKey = prev;
                }
            }
            return extractFirstVector(result);
        } catch (NoApiKeyException | UploadFileException e) {
            log.error("DashScope 多模态向量调用失败", e);
            return Optional.empty();
        } catch (Exception e) {
            log.error("DashScope 多模态向量调用异常", e);
            return Optional.empty();
        }
    }

    private static Optional<float[]> extractFirstVector(MultiModalEmbeddingResult result) {
        if (result == null) {
            return Optional.empty();
        }
        MultiModalEmbeddingOutput output = result.getOutput();
        if (output == null || output.getEmbeddings() == null || output.getEmbeddings().isEmpty()) {
            return Optional.empty();
        }
        Object first = output.getEmbeddings().get(0);
        if (first == null) {
            return Optional.empty();
        }
        try {
            java.lang.reflect.Method m = first.getClass().getMethod("getEmbedding");
            Object emb = m.invoke(first);
            if (emb instanceof List<?> list && !list.isEmpty()) {
                float[] arr = new float[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    Object o = list.get(i);
                    arr[i] = o instanceof Number n ? n.floatValue() : 0f;
                }
                return Optional.of(arr);
            }
        } catch (ReflectiveOperationException ignored) {
            return Optional.empty();
        }
        return Optional.empty();
    }
}
