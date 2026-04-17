package com.smartims.embedding;

import com.smartims.exception.BusinessException;
import com.smartims.util.ImageHashUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public final class ImagePayloadUtil {

    /** 以图搜图等场景：原始图片经 Base64 / DataURL 传输时的解码后体积上限 */
    public static final long MAX_QUERY_IMAGE_BYTES = 2L * 1024 * 1024;

    private ImagePayloadUtil() {}

    public static String toPngDataUrl(String imageBase64OrDataUrl) throws IOException {
        if (imageBase64OrDataUrl == null || imageBase64OrDataUrl.isBlank()) {
            throw new IllegalArgumentException("empty image");
        }
        String trimmed = imageBase64OrDataUrl.trim();
        assertDecodedImagePayloadWithin(trimmed, MAX_QUERY_IMAGE_BYTES);
        BufferedImage bi = ImageHashUtil.decodeDataUrlOrBase64(trimmed);
        if (bi == null) {
            throw new IllegalArgumentException("invalid image");
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bi, "png", baos);
        String b64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        return "data:image/png;base64," + b64;
    }

    /**
     * 按 Base64 解码后的字节数估算上限（不整段解码，避免超大恶意请求占内存）。
     */
    static void assertDecodedImagePayloadWithin(String dataUrlOrBase64, long maxDecodedBytes) {
        String payload = extractBase64Payload(dataUrlOrBase64);
        if (payload.isEmpty()) {
            return;
        }
        long decodedLen = base64DecodedByteLength(payload);
        if (decodedLen > maxDecodedBytes) {
            throw new BusinessException("图片大小不能超过 2MB");
        }
    }

    private static String extractBase64Payload(String s) {
        int idx = s.indexOf("base64,");
        if (idx >= 0) {
            return s.substring(idx + "base64,".length()).replaceAll("\\s", "");
        }
        return s.replaceAll("\\s", "");
    }

    private static long base64DecodedByteLength(String b64) {
        int len = b64.length();
        if (len == 0) {
            return 0;
        }
        int padding = 0;
        if (b64.charAt(len - 1) == '=') {
            padding++;
        }
        if (len >= 2 && b64.charAt(len - 2) == '=') {
            padding++;
        }
        return (len * 3L) / 4L - padding;
    }
}
