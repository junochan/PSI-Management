package com.smartims.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;

/**
 * 图像解码与简易哈希工具（解码供 DashScope 查询图规范化使用）。
 */
public final class ImageHashUtil {

    private ImageHashUtil() {}

    public static BufferedImage decodeDataUrlOrBase64(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        String s = raw.trim();
        try {
            if (s.startsWith("data:image")) {
                int comma = s.indexOf(',');
                if (comma < 0) {
                    return null;
                }
                String b64 = s.substring(comma + 1);
                byte[] bytes = Base64.getDecoder().decode(b64);
                return ImageIO.read(new ByteArrayInputStream(bytes));
            }
            byte[] bytes = Base64.getDecoder().decode(s);
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            return null;
        }
    }
}
