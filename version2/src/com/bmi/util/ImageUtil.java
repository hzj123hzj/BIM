package com.bmi.util;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * 图像工具类。
 *
 * 提供 {@link BufferedImage} 到 JavaFX {@link Image} 的转换，
 * 自行基于像素数组实现，避免依赖 {@code javafx.swing} 模块（该模块在本项目 lib 中未提供）。
 *
 * 同时提供食物图片存储所需的字节互转与感知哈希（aHash）工具，
 * 用于 AI 识图时与本地食物图片做相似度消歧。
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

    /** 将字节数组包装为 JavaFX {@link Image}（用于展示库中存储的食物图片）。 */
    public static Image byteArrayToImage(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return new Image(new ByteArrayInputStream(bytes));
    }

    /** 将 AWT {@link BufferedImage} 编码为指定格式的字节数组（如 "png"/"jpg"）。 */
    public static byte[] bufferedImageToBytes(BufferedImage img, String fmt) {
        if (img == null) {
            return null;
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, fmt, baos);
            return baos.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    /** 将图片字节解码回 AWT {@link BufferedImage}（供 pHash 计算）。 */
    public static BufferedImage bufferedImageFromBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 感知哈希（average hash，aHash）：将图像缩到 8x8 灰度，
     * 与整体均值比较生成 64 位指纹。无需第三方库，适合本地快速粗匹配。
     * 同图/近似图指纹汉明距离小；差异大则距离大。
     */
    public static long perceptualHash(BufferedImage img) {
        if (img == null) {
            return 0L;
        }
        BufferedImage small = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        Graphics g = small.getGraphics();
        g.drawImage(img.getScaledInstance(8, 8, java.awt.Image.SCALE_AREA_AVERAGING), 0, 0, null);
        g.dispose();

        long[] vals = new long[64];
        long sum = 0;
        for (int i = 0; i < 64; i++) {
            int x = i % 8, y = i / 8;
            int rgb = small.getRGB(x, y);
            int r = (rgb >> 16) & 0xFF;
            int gg = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            long gray = (r * 77 + gg * 150 + b * 29) >> 8; // 近似亮度
            vals[i] = gray;
            sum += gray;
        }
        long avg = sum / 64;
        long hash = 0L;
        for (int i = 0; i < 64; i++) {
            if (vals[i] > avg) {
                hash |= (1L << i);
            }
        }
        return hash;
    }

    /** 两个 64 位指纹的汉明距离（不同位数）。 */
    public static int hamming(long a, long b) {
        return Long.bitCount(a ^ b);
    }
}
