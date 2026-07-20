package com.bmi.util;

import javafx.scene.paint.Color;
import javafx.scene.Scene;

public class Theme {
    public static final Color PRIMARY   = Color.rgb(45, 140, 160);
    public static final Color PRIMARY_L = Color.rgb(100, 190, 210);
    public static final Color PRIMARY_D = Color.rgb(30, 100, 120);
    public static final Color ACCENT    = Color.rgb(255, 150, 70);
    public static final Color BG        = Color.rgb(240, 246, 249);
    public static final Color CARD_BG   = Color.rgb(255, 255, 255);
    public static final Color HEADER_BG = Color.rgb(225, 240, 245);
    public static final Color TEXT_DARK = Color.rgb(40, 50, 60);
    public static final Color TEXT_GRAY = Color.rgb(100, 110, 120);
    public static final Color BORDER    = Color.rgb(210, 220, 228);
    public static final Color SUCCESS   = Color.rgb(60, 180, 120);
    public static final Color WARNING   = Color.rgb(255, 180, 60);
    public static final Color DANGER    = Color.rgb(230, 90, 90);

    public static String hex(Color c) {
        return String.format("#%02X%02X%02X",
                (int) (c.getRed() * 255), (int) (c.getGreen() * 255), (int) (c.getBlue() * 255));
    }

    public static void styleScene(Scene scene) {
        // 从 classpath 加载样式表（构建时需将 style.css 复制到输出目录）
        String url = Theme.class.getResource("/style.css").toExternalForm();
        if (!scene.getStylesheets().contains(url)) scene.getStylesheets().add(url);
    }
}
