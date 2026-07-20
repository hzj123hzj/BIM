package com.bmi.util;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

import java.awt.image.BufferedImage;

/**
 * 图像工具类。
 *
 * 提供 {@link BufferedImage} 到 JavaFX {@link Image} 的转换，
 * 自行基于像素数组实现，避免依赖 {@code javafx.swing} 模块（该模块在本项目 lib 中未提供）。
 */
public final class ImageUtil {
    private ImageUtil() {
    }

    /**
     * 将 AWT {@link BufferedImage} 转换为 JavaFX {@link Image}。
     * 采用预乘 (premultiplied) BGRA 像素格式，适配 {@link PixelFormat#getByteBgraPreInstance()}。
     *
     * @param src 源图像，可为 {@code null}
     * @return 对应的 JavaFX 图像；src 为 {@code null} 时返回 {@code null}
     */
    public static Image toFXImage(BufferedImage src) {
        if (src == null) {
            return null;
        }
        int w = src.getWidth();
        int h = src.getHeight();
        int[] argb = src.getRGB(0, 0, w, h, null, 0, w);
        byte[] buf = new byte[w * h * 4];
        for (int i = 0; i < argb.length; i++) {
            int p = argb[i];
            int a = (p >> 24) & 0xFF;
            int r = (p >> 16) & 0xFF;
            int g = (p >> 8) & 0xFF;
            int b = p & 0xFF;
            // 预乘 alpha：value = channel * a / 255
            buf[i * 4]     = (byte) ((b * a) / 255); // B
            buf[i * 4 + 1] = (byte) ((g * a) / 255); // G
            buf[i * 4 + 2] = (byte) ((r * a) / 255); // R
            buf[i * 4 + 3] = (byte) a;               // A
        }
        WritableImage img = new WritableImage(w, h);
        img.getPixelWriter().setPixels(0, 0, w, h,
                PixelFormat.getByteBgraPreInstance(), buf, 0, w * 4);
        return img;
    }
}
