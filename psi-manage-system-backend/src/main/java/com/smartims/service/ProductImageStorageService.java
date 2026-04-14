package com.smartims.service;

import com.smartims.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 商品图片本地存储：磁盘路径与对外 URL 约定见 {@link com.smartims.config.WebMvcConfig}
 */
@Service
public class ProductImageStorageService {

    private static final String UPLOAD_SUB = "uploads/products";
    private static final Set<String> ALLOWED_CT = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    @Value("${app.attachment-dir:data}")
    private String attachmentDir;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    public String saveProductImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择图片文件");
        }
        String ct = file.getContentType();
        if (ct == null || !ALLOWED_CT.contains(ct.toLowerCase(Locale.ROOT))) {
            throw new BusinessException("仅支持 JPG、PNG、GIF、WEBP 图片");
        }

        Path dir = Paths.get(attachmentDir, UPLOAD_SUB).toAbsolutePath().normalize();
        Files.createDirectories(dir);

        String ext = extensionForContentType(ct);
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        Path target = dir.resolve(filename);
        file.transferTo(target);

        String cp = (contextPath == null || contextPath.isEmpty()) ? "" : contextPath;
        return cp + "/uploads/products/" + filename;
    }

    /**
     * 删除本地上传的图片（仅处理 URL 指向 {@code /uploads/products/} 下的文件）
     */
    public void deleteManagedImageIfPresent(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return;
        }
        String pathPart = stripToPath(imageUrl.trim());
        String marker = "/uploads/products/";
        int idx = pathPart.indexOf(marker);
        if (idx < 0) {
            return;
        }
        String filename = pathPart.substring(idx + marker.length());
        if (filename.isEmpty() || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return;
        }
        Path base = Paths.get(attachmentDir, UPLOAD_SUB).toAbsolutePath().normalize();
        Path target = base.resolve(filename).normalize();
        if (!target.startsWith(base)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // 不因清理失败阻断业务
        }
    }

    private static String stripToPath(String imageUrl) {
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            try {
                return new URL(imageUrl).getPath();
            } catch (Exception e) {
                return imageUrl;
            }
        }
        return imageUrl;
    }

    private static String extensionForContentType(String contentType) {
        String ct = contentType.toLowerCase(Locale.ROOT);
        if (ct.contains("png")) {
            return ".png";
        }
        if (ct.contains("gif")) {
            return ".gif";
        }
        if (ct.contains("webp")) {
            return ".webp";
        }
        return ".jpg";
    }
}
