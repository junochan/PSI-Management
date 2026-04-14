package com.smartims.embedding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class LocalImageEmbeddingStore {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final Path baseDir;

    public LocalImageEmbeddingStore(
            @Value("${app.image-search.embedding-store-dir:}") String baseDirConfig) {
        String raw = StringUtils.hasText(baseDirConfig)
                ? baseDirConfig.trim()
                : "data/image-embeddings";
        this.baseDir = Paths.get(raw).toAbsolutePath().normalize();
    }

    public Path getBaseDir() {
        return baseDir;
    }

    private Path filePath(String namespace, long id) {
        String ns = namespace.replaceAll("[^a-zA-Z0-9_-]", "_");
        return baseDir.resolve(ns).resolve(id + ".json");
    }

    public Optional<LocalImageEmbeddingRecord> load(String namespace, long id) {
        Path f = filePath(namespace, id);
        if (!Files.isReadable(f)) {
            return Optional.empty();
        }
        try {
            byte[] bytes = Files.readAllBytes(f);
            if (bytes.length == 0) {
                return Optional.empty();
            }
            LocalImageEmbeddingRecord r = MAPPER.readValue(bytes, LocalImageEmbeddingRecord.class);
            return Optional.ofNullable(r);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public List<float[]> loadVectors(String namespace, long id, String expectedModel, String imagesSignature) {
        Optional<LocalImageEmbeddingRecord> opt = load(namespace, id);
        if (opt.isEmpty()) {
            return Collections.emptyList();
        }
        LocalImageEmbeddingRecord r = opt.get();
        if (!StringUtils.hasText(r.getImagesSignature())
                || !r.getImagesSignature().equals(imagesSignature)) {
            return Collections.emptyList();
        }
        if (expectedModel != null && StringUtils.hasText(r.getModel()) && !expectedModel.equals(r.getModel())) {
            return Collections.emptyList();
        }
        if (r.getEmbeddings() == null || r.getEmbeddings().isEmpty()) {
            return Collections.emptyList();
        }
        List<float[]> out = new ArrayList<>();
        for (List<Float> row : r.getEmbeddings()) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            float[] a = new float[row.size()];
            for (int i = 0; i < row.size(); i++) {
                a[i] = row.get(i) != null ? row.get(i) : 0f;
            }
            out.add(a);
        }
        return out;
    }

    public void save(String namespace, long id, LocalImageEmbeddingRecord record) throws IOException {
        Path f = filePath(namespace, id);
        Files.createDirectories(f.getParent());
        byte[] json = MAPPER.writeValueAsBytes(record);
        Files.write(f, json);
    }

    public void delete(String namespace, long id) {
        try {
            Files.deleteIfExists(filePath(namespace, id));
        } catch (IOException ignored) {
            // best-effort
        }
    }

    public static List<List<Float>> toJsonFloatRows(List<float[]> vectors) {
        List<List<Float>> rows = new ArrayList<>();
        for (float[] v : vectors) {
            List<Float> row = new ArrayList<>(v.length);
            for (float x : v) {
                row.add(x);
            }
            rows.add(row);
        }
        return rows;
    }

    public static String sha256Hex(String s) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(s != null ? s.hashCode() : 0);
        }
    }
}
