package com.smartims.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartims.util.ImageHashUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 解析商品图字段（data URL / 逗号分隔 / JSON 数组 / 附件文件名）。
 */
@Service
public class ProductImageLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Value("${app.attachment-dir:data}")
    private String attachmentDir;

    public BufferedImage loadImage(String imageRef) {
        if (!StringUtils.hasText(imageRef)) {
            return null;
        }
        String s = imageRef.trim();
        if (s.startsWith("data:image")) {
            return ImageHashUtil.decodeDataUrlOrBase64(s);
        }
        BufferedImage fromUploadUrl = loadFromProductUploadUrl(s);
        if (fromUploadUrl != null) {
            return fromUploadUrl;
        }
        if (s.startsWith("[")) {
            String first = firstImageString(s);
            return loadImage(first);
        }
        BufferedImage fromFile = loadFromAttachmentFile(s);
        if (fromFile != null) {
            return fromFile;
        }
        return ImageHashUtil.decodeDataUrlOrBase64(s);
    }

    /**
     * 本地上传商品图 URL，如 /api/uploads/products/xxx.jpg（与 {@link ProductImageStorageService} 一致）
     */
    private BufferedImage loadFromProductUploadUrl(String ref) {
        if (!StringUtils.hasText(attachmentDir)) {
            return null;
        }
        String marker = "/uploads/products/";
        int idx = ref.indexOf(marker);
        if (idx < 0) {
            marker = "uploads/products/";
            idx = ref.indexOf(marker);
        }
        if (idx < 0) {
            return null;
        }
        String filename = ref.substring(idx + marker.length());
        if (filename.isEmpty() || filename.contains("..") || filename.indexOf('/') >= 0 || filename.indexOf('\\') >= 0) {
            return null;
        }
        try {
            Path base = Paths.get(attachmentDir, "uploads", "products").toAbsolutePath().normalize();
            Path file = base.resolve(filename).normalize();
            if (!file.startsWith(base) || !Files.isReadable(file)) {
                return null;
            }
            try (InputStream in = Files.newInputStream(file)) {
                return ImageIO.read(in);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private BufferedImage loadFromAttachmentFile(String filename) {
        if (!StringUtils.hasText(attachmentDir)) {
            return null;
        }
        if (filename.contains("..") || filename.indexOf('/') >= 0 || filename.indexOf('\\') >= 0) {
            return null;
        }
        try {
            Path base = Paths.get(attachmentDir).toAbsolutePath().normalize();
            Path file = base.resolve(filename).normalize();
            if (!file.startsWith(base) || !Files.isReadable(file)) {
                return null;
            }
            try (InputStream in = Files.newInputStream(file)) {
                return ImageIO.read(in);
            }
        } catch (Exception e) {
            return null;
        }
    }

    public String firstImageString(String productImages) {
        if (!StringUtils.hasText(productImages)) {
            return null;
        }
        String t = productImages.trim();
        if (t.startsWith("[")) {
            try {
                List<String> list = MAPPER.readValue(t, new TypeReference<List<String>>() {});
                if (list != null && !list.isEmpty()) {
                    return list.get(0);
                }
            } catch (Exception ignored) {
                // fallthrough
            }
        }
        if (t.contains(",")) {
            String[] parts = t.split(",", 2);
            return parts[0].trim();
        }
        return t;
    }

    public List<String> allImageStrings(String productImages) {
        List<String> out = new ArrayList<>();
        if (!StringUtils.hasText(productImages)) {
            return out;
        }
        String t = productImages.trim();
        if (t.startsWith("[")) {
            try {
                List<String> list = MAPPER.readValue(t, new TypeReference<List<String>>() {});
                if (list != null) {
                    out.addAll(list);
                }
                return out;
            } catch (Exception ignored) {
                // fallthrough
            }
        }
        if (t.contains(",")) {
            for (String p : t.split(",")) {
                if (StringUtils.hasText(p.trim())) {
                    out.add(p.trim());
                }
            }
            return out;
        }
        out.add(t);
        return out;
    }
}
