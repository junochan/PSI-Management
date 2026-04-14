package com.smartims.embedding;

import com.smartims.util.ImageHashUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public final class ImagePayloadUtil {

    private ImagePayloadUtil() {}

    public static String toPngDataUrl(String imageBase64OrDataUrl) throws IOException {
        if (imageBase64OrDataUrl == null || imageBase64OrDataUrl.isBlank()) {
            throw new IllegalArgumentException("empty image");
        }
        BufferedImage bi = ImageHashUtil.decodeDataUrlOrBase64(imageBase64OrDataUrl.trim());
        if (bi == null) {
            throw new IllegalArgumentException("invalid image");
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bi, "png", baos);
        String b64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        return "data:image/png;base64," + b64;
    }
}
