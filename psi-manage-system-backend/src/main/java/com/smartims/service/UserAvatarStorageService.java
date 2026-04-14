package com.smartims.service;

import com.smartims.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 用户头像本地存储，URL 与 {@link com.smartims.config.WebMvcConfig} 下 {@code /uploads/**} 一致。
 */
@Service
public class UserAvatarStorageService {

    private static final String UPLOAD_SUB = "uploads/avatars";
    private static final Set<String> ALLOWED_CT = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    @Value("${app.attachment-dir:data}")
    private String attachmentDir;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    public String saveAvatar(MultipartFile file) throws IOException {
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
        return cp + "/uploads/avatars/" + filename;
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
