import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.List;

/**
 * ============================================================
 * BMI 体质评估与预测系统 — 完整 Java Swing 实现
 * 技术栈: Java Swing + PostgreSQL + JDBC
 * 无第三方 GUI 库, 趋势图用 Graphics 手绘
 * ============================================================
 *
 * 【编译运行命令】
 * Windows:
 *   编译: javac -encoding UTF-8 -cp .;postgresql-42.7.3.jar HealthSystem.java
 *   运行: java -Dfile.encoding=UTF-8 -cp .;postgresql-42.7.3.jar HealthSystem
 * Linux/macOS:
 *   编译: javac -encoding UTF-8 -cp .:postgresql-42.7.3.jar HealthSystem.java
 *   运行: java -Dfile.encoding=UTF-8 -cp .:postgresql-42.7.3.jar HealthSystem
 *
 * 【数据库配置】
 *   数据库: health_db  用户: postgres  密码: yourdb  端口: 5432
 *   建表脚本: init_database.sql
 * ============================================================
 */
public class HealthSystem {

    // ==================== 数据库连接常量 ====================
    static final String DB_URL = "jdbc:postgresql://localhost:5432/health_db";
    static final String DB_USER = "postgres";
    static final String DB_PASS = "12345678";

    // 当前登录用户信息（全局共享）
    static String currentUsername;
    static String currentGender;
    static int currentAge;
    static double currentHeight;
    static String currentActivityLevel = "久坐";

    // 格式化工具
    static final DecimalFormat df2 = new DecimalFormat("0.00");
    static final DecimalFormat df1 = new DecimalFormat("0.0");
    static final DecimalFormat df0 = new DecimalFormat("0");

    // 程序入口
    public static void main(String[] args) {
        // 设置 JVM 默认文件编码为 UTF-8, 防止 Windows 下中文乱码
        System.setProperty("file.encoding", "UTF-8");

        // 设置界面外观为 Nimbus, 更现代、更柔和
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 强制设置 Swing 全局中文字体
        initGlobalFont();
        // 应用统一的 Nimbus 配色主题
        Theme.applyNimbusTheme();

        // 启动登录窗口
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }

    /**
     * 初始化全局中文字体
     * 通过 UIManager 和递归设置组件字体, 解决中文乱码问题
     */
    private static void initGlobalFont() {
        String[] fontNames = {"Microsoft YaHei UI", "Microsoft YaHei", "SimHei", "SimSun", "Noto Sans CJK SC", "Dialog"};
        Font font = null;
        for (String name : fontNames) {
            if (Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()).contains(name)) {
                font = new Font(name, Font.PLAIN, 13);
                break;
            }
        }
        if (font == null) {
            font = new Font("Dialog", Font.PLAIN, 13);
        }

        // 设置 UIManager 所有默认字体键
        String[] keys = {
            "Button.font", "ToggleButton.font", "RadioButton.font", "CheckBox.font",
            "ColorChooser.font", "ComboBox.font", "Label.font", "List.font",
            "MenuBar.font", "MenuItem.font", "RadioButtonMenuItem.font", "CheckBoxMenuItem.font",
            "Menu.font", "PopupMenu.font", "OptionPane.font", "Panel.font",
            "ProgressBar.font", "ScrollPane.font", "Viewport.font", "TabbedPane.font",
            "Table.font", "TableHeader.font", "TextField.font", "TextArea.font",
            "TextPane.font", "EditorPane.font", "TitledBorder.font", "ToolBar.font",
            "ToolTip.font", "Tree.font"
        };
        for (String key : keys) {
            UIManager.put(key, font);
        }
    }

    // ================================================================
    //                    UI 主题与辅助组件 (v2.0 现代设计系统)
    // ================================================================

    /**
     * v2.0 主题配置: 现代配色 + 字体系统 + 动画 + 组件样式
     * 设计理念: 深靛蓝主色 + 青绿点缀, 柔和阴影, 流畅动画
     */
    static class Theme {
        // === 主色板: 深靛蓝 (Indigo) + 青绿 (Teal) ===
        static final Color PRIMARY      = new Color(99,  102, 241);   // Indigo-500
        static final Color PRIMARY_L    = new Color(129, 140, 248);   // Indigo-400
        static final Color PRIMARY_D    = new Color(67,  56,  202);   // Indigo-700
        static final Color ACCENT       = new Color(20,  184, 166);   // Teal-500
        static final Color ACCENT_L     = new Color(45,  212, 191);   // Teal-400

        // === 背景色系 ===
        static final Color BG           = new Color(248, 250, 252);   // Slate-50
        static final Color CARD_BG      = new Color(255, 255, 255);
        static final Color HEADER_BG    = new Color(238, 242, 255);   // Indigo-50
        static final Color FOOTER_BG    = new Color(241, 245, 249);   // Slate-100

        // === 文字色系 ===
        static final Color TEXT_DARK    = new Color(30,  41,  59);    // Slate-800
        static final Color TEXT_BODY    = new Color(51,  65,  85);    // Slate-600
        static final Color TEXT_GRAY    = new Color(100, 116, 139);   // Slate-500
        static final Color TEXT_LIGHT   = new Color(148, 163, 184);   // Slate-400

        // === 边框与分割 ===
        static final Color BORDER       = new Color(226, 232, 240);   // Slate-200
        static final Color BORDER_L     = new Color(241, 245, 249);   // Slate-100

        // === 语义色 ===
        static final Color SUCCESS      = new Color(34,  197, 94);    // Green-500
        static final Color WARNING      = new Color(245, 158, 11);    // Amber-500
        static final Color DANGER       = new Color(239, 68,  68);    // Red-500
        static final Color INFO         = new Color(59,  130, 246);   // Blue-500

        // === 渐变色对 ===
        static final Color[] GRAD_PRIMARY  = { new Color(99,102,241), new Color(139,92,246) };  // Indigo→Purple
        static final Color[] GRAD_ACCENT   = { new Color(20,184,166), new Color(16,185,129) };  // Teal→Emerald
        static final Color[] GRAD_SUNSET   = { new Color(251,146,60), new Color(244,63,94)  };  // Orange→Rose
        static final Color[] GRAD_OCEAN    = { new Color(56,189,248), new Color(59,130,246) };  // Sky→Blue
        static final Color[] GRAD_HEADER   = { new Color(238,242,255), new Color(224,231,255) };

        // === 字体系统 ===
        static final String FONT_NAME = getAvailableFont();
        static final Font FONT_H1      = new Font(FONT_NAME, Font.BOLD, 22);
        static final Font FONT_H2      = new Font(FONT_NAME, Font.BOLD, 17);
        static final Font FONT_TITLE   = new Font(FONT_NAME, Font.BOLD, 18);
        static final Font FONT_HEADER  = new Font(FONT_NAME, Font.BOLD, 14);
        static final Font FONT_BODY    = new Font(FONT_NAME, Font.PLAIN, 13);
        static final Font FONT_BODY_B  = new Font(FONT_NAME, Font.BOLD, 13);
        static final Font FONT_SMALL   = new Font(FONT_NAME, Font.PLAIN, 12);
        static final Font FONT_TINY    = new Font(FONT_NAME, Font.PLAIN, 11);
        static final Font FONT_BIG_NUM = new Font(FONT_NAME, Font.BOLD, 28);

        /** 获取可用中文字体 */
        static String getAvailableFont() {
            String[] candidates = {"Microsoft YaHei UI", "Microsoft YaHei", "SimHei", "Noto Sans CJK SC", "PingFang SC", "Dialog"};
            String[] available = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            for (String c : candidates) {
                for (String a : available) {
                    if (a.equals(c)) return c;
                }
            }
            return "Dialog";
        }

        /** 调整 Nimbus 默认配色 */
        static void applyNimbusTheme() {
            UIManager.put("nimbusBase", PRIMARY);
            UIManager.put("nimbusBlueGrey", new Color(226, 232, 240));
            UIManager.put("control", BG);
            UIManager.put("nimbusSelectionBackground", PRIMARY_L);
            UIManager.put("nimbusSelection", PRIMARY_L);
            UIManager.put("nimbusFocus", new Color(129, 140, 248, 80));
            UIManager.put("nimbusLightBackground", CARD_BG);
            UIManager.put("text", TEXT_DARK);
            UIManager.put("nimbusBorder", BORDER);
            UIManager.put("Label.foreground", TEXT_DARK);
            UIManager.put("Panel.background", BG);
        }

        // === 按钮样式 ===

        /** 主色按钮 — 带悬停动画 */
        static void stylePrimaryButton(JButton btn) {
            btn.setFont(FONT_BODY_B);
            btn.setForeground(Color.WHITE);
            btn.setBackground(PRIMARY);
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setContentAreaFilled(false);
            btn.setOpaque(false);
            btn.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            addHoverEffect(btn, PRIMARY, PRIMARY_D);
        }

        /** 强调色按钮 — 带悬停动画 */
        static void styleAccentButton(JButton btn) {
            btn.setFont(FONT_BODY_B);
            btn.setForeground(Color.WHITE);
            btn.setBackground(ACCENT);
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setContentAreaFilled(false);
            btn.setOpaque(false);
            btn.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            addHoverEffect(btn, ACCENT, new Color(13, 148, 136));
        }

        /** 幽灵按钮 — 透明背景 */
        static void styleGhostButton(JButton btn) {
            btn.setFont(FONT_BODY);
            btn.setForeground(PRIMARY);
            btn.setBackground(new Color(238, 242, 255));
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setContentAreaFilled(false);
            btn.setOpaque(false);
            btn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            addHoverEffect(btn, new Color(238, 242, 255), new Color(224, 231, 255));
        }

        /** 危险按钮 */
        static void styleDangerButton(JButton btn) {
            btn.setFont(FONT_BODY_B);
            btn.setForeground(Color.WHITE);
            btn.setBackground(DANGER);
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setContentAreaFilled(false);
            btn.setOpaque(false);
            btn.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            addHoverEffect(btn, DANGER, new Color(220, 38, 38));
        }

        // === 输入框样式 ===

        /** 文本输入框 — 带焦点高亮 */
        static void styleTextField(JTextField field) {
            field.setFont(FONT_BODY);
            field.setForeground(TEXT_DARK);
            field.setBackground(CARD_BG);
            field.setCaretColor(PRIMARY);
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER, 1),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)));
            field.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    field.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(PRIMARY_L, 2),
                            BorderFactory.createEmptyBorder(7, 9, 7, 9)));
                }
                @Override
                public void focusLost(FocusEvent e) {
                    field.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(BORDER, 1),
                            BorderFactory.createEmptyBorder(8, 10, 8, 10)));
                }
            });
        }

        /** 密码框样式 */
        static void stylePasswordField(JPasswordField field) {
            field.setFont(FONT_BODY);
            field.setForeground(TEXT_DARK);
            field.setBackground(CARD_BG);
            field.setCaretColor(PRIMARY);
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER, 1),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)));
            field.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    field.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(PRIMARY_L, 2),
                            BorderFactory.createEmptyBorder(7, 9, 7, 9)));
                }
                @Override
                public void focusLost(FocusEvent e) {
                    field.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(BORDER, 1),
                            BorderFactory.createEmptyBorder(8, 10, 8, 10)));
                }
            });
        }

        /** 组合框统一样式 */
        static void styleComboBox(JComboBox<?> cb) {
            cb.setFont(FONT_BODY);
            cb.setForeground(TEXT_DARK);
            cb.setBackground(CARD_BG);
            cb.setBorder(BorderFactory.createLineBorder(BORDER, 1));
            cb.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        /** 微调器样式 */
        static void styleSpinner(JSpinner sp) {
            sp.setFont(FONT_BODY);
            sp.setForeground(TEXT_DARK);
            sp.setBackground(CARD_BG);
            ((JSpinner.DefaultEditor) sp.getEditor()).getTextField().setBorder(
                    BorderFactory.createEmptyBorder(5, 8, 5, 8));
        }

        // === 表格样式 ===

        /** 表格统一样式 — 现代斑马纹 */
        static void styleTable(JTable table) {
            table.setFont(FONT_BODY);
            table.setForeground(TEXT_DARK);
            table.setBackground(CARD_BG);
            table.setRowHeight(32);
            table.setSelectionBackground(new Color(238, 242, 255));
            table.setSelectionForeground(PRIMARY_D);
            table.setGridColor(BORDER_L);
            table.setShowVerticalLines(false);
            table.setShowHorizontalLines(true);
            table.setIntercellSpacing(new Dimension(0, 0));

            table.getTableHeader().setFont(FONT_HEADER);
            table.getTableHeader().setBackground(new Color(248, 250, 252));
            table.getTableHeader().setForeground(TEXT_BODY);
            table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, PRIMARY));
            table.getTableHeader().setReorderingAllowed(false);
        }

        // === 面板创建 ===

        /** 创建带标题和彩色装饰条的卡片面板 */
        static JPanel createCardPanel(String title, Color stripeColor) {
            JPanel card = new RoundedPanel(new BorderLayout(8, 8), 14);
            card.setBackground(CARD_BG);
            card.setBorder(BorderFactory.createEmptyBorder(14, 16, 16, 16));

            if (title != null && !title.isEmpty()) {
                JPanel headerPanel = new JPanel(new BorderLayout());
                headerPanel.setOpaque(false);
                // 装饰竖条
                JPanel stripe = new JPanel();
                stripe.setBackground(stripeColor);
                stripe.setPreferredSize(new Dimension(4, 18));
                stripe.setOpaque(false);
                stripe.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
                // 标题文字
                JLabel lblTitle = new JLabel(title);
                lblTitle.setFont(FONT_H2);
                lblTitle.setForeground(TEXT_DARK);
                headerPanel.add(stripe, BorderLayout.WEST);
                headerPanel.add(lblTitle, BorderLayout.CENTER);
                headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

                // 自定义 stripe 绘制
                JPanel stripeWrapper = new JPanel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(stripeColor);
                        g2.fillRoundRect(0, 0, 4, getHeight(), 2, 2);
                        g2.dispose();
                    }
                };
                stripeWrapper.setOpaque(false);
                stripeWrapper.setPreferredSize(new Dimension(4, 18));
                headerPanel.remove(stripe);
                headerPanel.add(stripeWrapper, BorderLayout.WEST);

                card.add(headerPanel, BorderLayout.NORTH);
            }
            return card;
        }

        /** 创建带渐变色的指标卡片 */
        static RoundedPanel createMetricCard(String label, String value, Color[] gradient) {
            RoundedPanel card = new RoundedPanel(new BorderLayout(6, 2), 14) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int w = Math.max(getWidth() - 6, 28);
                    int h = Math.max(getHeight() - 6, 28);
                    // 阴影
                    g2.setColor(new Color(0, 0, 0, 12));
                    g2.fillRoundRect(3, 3, w, h, 14, 14);
                    // 渐变背景
                    GradientPaint gp = new GradientPaint(0, 0, gradient[0], w, h, gradient[1]);
                    g2.setPaint(gp);
                    g2.fillRoundRect(0, 0, w, h, 14, 14);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            card.setOpaque(false);
            card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

            JLabel lblLabel = new JLabel(label);
            lblLabel.setFont(FONT_SMALL);
            lblLabel.setForeground(new Color(255, 255, 255, 200));
            card.add(lblLabel, BorderLayout.NORTH);

            JLabel lblValue = new JLabel(value);
            lblValue.setFont(FONT_BIG_NUM);
            lblValue.setForeground(Color.WHITE);
            card.add(lblValue, BorderLayout.CENTER);

            return card;
        }

        // === 按钮悬停动画 ===

        /** 为按钮添加悬停色变效果 */
        static void addHoverEffect(JButton btn, Color normalColor, Color hoverColor) {
            btn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    btn.putClientProperty("hoverColor", hoverColor);
                    btn.repaint();
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    btn.putClientProperty("hoverColor", null);
                    btn.repaint();
                }
            });
            btn.setBackground(normalColor);
        }
    }

    /** 渐变背景面板 (支持多色渐变) */
    static class GradientPanel extends JPanel {
        private final Color c1, c2;
        private boolean vertical = true;

        GradientPanel(LayoutManager layout, Color c1, Color c2) {
            super(layout);
            this.c1 = c1; this.c2 = c2;
            setOpaque(false);
        }
        GradientPanel(LayoutManager layout, Color[] colors) {
            this(layout, colors[0], colors[1]);
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            GradientPaint gp = new GradientPaint(0, 0, c1, vertical ? 0 : w, vertical ? h : 0, c2);
            g2.setPaint(gp);
            g2.fillRect(0, 0, w, h);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** 带柔和阴影和圆角的卡片面板 */
    static class RoundedPanel extends JPanel {
        private final int radius;
        private Color shadowColor = new Color(100, 116, 139, 25);
        private int shadowOffset = 4;

        RoundedPanel(LayoutManager layout, int radius) {
            super(layout);
            this.radius = radius;
            setOpaque(false);
            setBackground(Theme.CARD_BG);
        }
        RoundedPanel(LayoutManager layout, int radius, Color bg) {
            this(layout, radius);
            setBackground(bg);
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = Math.max(getWidth() - shadowOffset, radius * 2);
            int h = Math.max(getHeight() - shadowOffset, radius * 2);
            // 多层柔和阴影
            for (int i = 0; i < 3; i++) {
                int alpha = shadowColor.getAlpha() - i * 6;
                if (alpha < 5) continue;
                g2.setColor(new Color(shadowColor.getRed(), shadowColor.getGreen(), shadowColor.getBlue(), alpha));
                g2.fillRoundRect(i + 1, i + 1, w, h, radius, radius);
            }
            // 卡片背景
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, w, h, radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** 自定义圆角按钮绘制 */
    static class RoundButton extends JButton {
        private Color bgColor;
        private Color hoverColor;
        private int radius = 10;

        RoundButton(String text, Color bg, Color hover) {
            super(text);
            this.bgColor = bg;
            this.hoverColor = hover;
            setFont(Theme.FONT_BODY_B);
            setForeground(Color.WHITE);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) { repaint(); }
                @Override
                public void mouseExited(MouseEvent e) { repaint(); }
            });
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth() - 1, h = getHeight() - 1;
            Color c = getModel().isRollover() ? hoverColor : bgColor;
            // 阴影
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 40));
            g2.fillRoundRect(2, 2, w, h, radius, radius);
            // 主体
            g2.setColor(c);
            g2.fillRoundRect(0, 0, w, h, radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** 淡入动画工具 */
    static class FadeIn {
        static void apply(final JComponent comp, final int duration) {
            comp.setOpaque(false);
            final int steps = 20;
            final int delay = duration / steps;
            final javax.swing.Timer[] timer = new javax.swing.Timer[1];
            timer[0] = new javax.swing.Timer(delay, new ActionListener() {
                int alpha = 0;
                @Override
                public void actionPerformed(ActionEvent e) {
                    alpha += 255 / steps;
                    if (alpha >= 255) {
                        alpha = 255;
                        comp.putClientProperty("fadeInAlpha", null);
                        timer[0].stop();
                    } else {
                        comp.putClientProperty("fadeInAlpha", alpha);
                    }
                    comp.repaint();
                }
            });
            comp.putClientProperty("fadeInAlpha", 0);
            timer[0].start();
        }
    }

    /** 标签页样式增强器 */
    static void styleTabbedPane(JTabbedPane tabPane) {
        tabPane.setFont(Theme.FONT_BODY_B);
        tabPane.setForeground(Theme.TEXT_DARK);
        tabPane.setBackground(Theme.BG);
        tabPane.setBorder(null);
        tabPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        // 自定义标签页样式
        for (int i = 0; i < tabPane.getTabCount(); i++) {
            JLabel lbl = new JLabel(tabPane.getTitleAt(i)) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(getForeground());
                    g2.setFont(getFont());
                    g2.drawString(getText(), 0, getHeight() - 6);
                    g2.dispose();
                }
            };
        }
    }

    // ================================================================
    //                    第一部分: 密码工具类
    // ================================================================

    /**
     * 密码工具类 — SHA-256 加盐哈希
     * 使用 JDK 自带 MessageDigest, 不依赖第三方库
     */
    static class PasswordUtil {

        /** 生成随机盐值 (16字节 → 32位Hex字符串) */
        static String generateSalt() {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            return bytesToHex(salt);
        }

        /** SHA-256 加盐哈希 */
        static String hash(String password, String salt) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update((password + salt).getBytes("UTF-8"));
                return bytesToHex(md.digest());
            } catch (Exception e) {
                throw new RuntimeException("密码加密失败", e);
            }
        }

        /** 验证密码 */
        static boolean verify(String password, String salt, String hash) {
            return hash(password, salt).equals(hash);
        }

        /** 字节数组转 Hex 字符串 */
        private static String bytesToHex(byte[] bytes) {
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }
    }

    // ================================================================
    //                    第二部分: 数据库工具类
    // ================================================================

    /**
     * 数据库工具类 — 封装所有 JDBC 操作
     * 全部使用 PreparedStatement 防止 SQL 注入
     */
    static class DBUtil {

        /** 获取数据库连接 */
        static Connection getConnection() throws SQLException {
            try {
                Class.forName("org.postgresql.Driver");
            } catch (ClassNotFoundException e) {
                throw new SQLException("PostgreSQL JDBC 驱动未找到, 请确认 postgresql-42.7.3.jar 在 classpath 中");
            }
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
        }

        /** 测试数据库连接 */
        static boolean testConnection() {
            try (Connection conn = getConnection()) {
                return conn != null;
            } catch (SQLException e) {
                return false;
            }
        }

        // ==================== 用户相关操作 ====================

        /** 注册用户 */
        static boolean registerUser(String username, String password, String gender,
                                     int age, double height, String activityLevel) {
            String salt = PasswordUtil.generateSalt();
            String hash = PasswordUtil.hash(password, salt);
            String sql = "INSERT INTO users (username, password, salt, gender, age, height, activity_level) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, hash);
                ps.setString(3, salt);
                ps.setString(4, gender);
                ps.setInt(5, age);
                ps.setDouble(6, height);
                ps.setString(7, activityLevel);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                if (e.getSQLState().equals("23505")) {
                    JOptionPane.showMessageDialog(null, "用户名已存在, 请更换", "注册失败", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, "注册失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
                return false;
            }
        }

        /** 验证登录 */
        static boolean loginUser(String username, String password) {
            String sql = "SELECT password, salt, gender, age, height, activity_level FROM users WHERE username = ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String hash = rs.getString("password");
                    String salt = rs.getString("salt");
                    if (PasswordUtil.verify(password, salt, hash)) {
                        currentUsername = username;
                        currentGender = rs.getString("gender");
                        currentAge = rs.getInt("age");
                        currentHeight = rs.getDouble("height");
                        currentActivityLevel = rs.getString("activity_level");
                        if (currentActivityLevel == null) currentActivityLevel = "久坐";
                        return true;
                    }
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "数据库连接失败: " + e.getMessage(),
                        "错误", JOptionPane.ERROR_MESSAGE);
            }
            return false;
        }

        // ==================== 健康记录操作 ====================

        /** 保存健康记录 */
        static boolean saveHealthRecord(double weight, double bodyFat, double waterRate,
                double muscleRate, int visceralFat, double boneMuscle, double waist) {
            // 计算派生数据
            double bmi = HealthCalculator.calcBMI(weight, currentHeight);
            double bmr = HealthCalculator.calcAvgBMR(weight, currentHeight, currentAge, currentGender);
            double tdee = HealthCalculator.calcTDEE(bmr, currentActivityLevel);
            int bodyAge = HealthCalculator.calcBodyAge(currentAge, bodyFat, muscleRate, visceralFat, currentGender);
            String bodyType = HealthCalculator.classifyBodyType(bmi, bodyFat, currentGender);

            String sql = "INSERT INTO health_records (username, weight, body_fat, water_rate, muscle_rate, " +
                         "visceral_fat, bone_muscle, bmr, tdee, bmi, waist, body_age, body_type) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUsername);
                ps.setDouble(2, weight);
                ps.setDouble(3, bodyFat);
                ps.setDouble(4, waterRate);
                ps.setDouble(5, muscleRate);
                ps.setInt(6, visceralFat);
                ps.setDouble(7, boneMuscle);
                ps.setDouble(8, bmr);
                ps.setDouble(9, tdee);
                ps.setDouble(10, bmi);
                ps.setDouble(11, waist);
                ps.setInt(12, bodyAge);
                ps.setString(13, bodyType);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "保存健康记录失败: " + e.getMessage(),
                        "错误", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }

        /** 获取最新健康记录 */
        static Map<String, Object> getLatestHealthRecord() {
            String sql = "SELECT * FROM health_records WHERE username = ? ORDER BY record_date DESC, id DESC LIMIT 1";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUsername);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("weight", rs.getDouble("weight"));
                    map.put("body_fat", rs.getDouble("body_fat"));
                    map.put("water_rate", rs.getDouble("water_rate"));
                    map.put("muscle_rate", rs.getDouble("muscle_rate"));
                    map.put("visceral_fat", rs.getInt("visceral_fat"));
                    map.put("bone_muscle", rs.getDouble("bone_muscle"));
                    map.put("bmr", rs.getDouble("bmr"));
                    map.put("tdee", rs.getDouble("tdee"));
                    map.put("bmi", rs.getDouble("bmi"));
                    map.put("waist", rs.getDouble("waist"));
                    map.put("body_age", rs.getInt("body_age"));
                    try {
                        map.put("body_type", rs.getString("body_type"));
                    } catch (SQLException ex) {
                        map.put("body_type", "--");
                    }
                    map.put("record_date", rs.getDate("record_date"));
                    return map;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }

        /** 获取历史健康记录列表 */
        static List<Map<String, Object>> getHealthRecords(int limit) {
            List<Map<String, Object>> list = new ArrayList<>();
            String sql = "SELECT * FROM health_records WHERE username = ? ORDER BY record_date DESC, id DESC LIMIT ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUsername);
                ps.setInt(2, limit);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("record_date", rs.getDate("record_date"));
                    map.put("weight", rs.getDouble("weight"));
                    map.put("body_fat", rs.getDouble("body_fat"));
                    map.put("water_rate", rs.getDouble("water_rate"));
                    map.put("muscle_rate", rs.getDouble("muscle_rate"));
                    map.put("visceral_fat", rs.getInt("visceral_fat"));
                    map.put("bone_muscle", rs.getDouble("bone_muscle"));
                    map.put("bmr", rs.getDouble("bmr"));
                    map.put("tdee", rs.getDouble("tdee"));
                    map.put("bmi", rs.getDouble("bmi"));
                    map.put("waist", rs.getDouble("waist"));
                    map.put("body_age", rs.getInt("body_age"));
                    try {
                        map.put("body_type", rs.getString("body_type"));
                    } catch (SQLException ex) {
                        map.put("body_type", "--");
                    }
                    list.add(map);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }

        /** 检查今天是否已打卡（有健康记录） */
        static boolean isCheckedToday() {
            String sql = "SELECT 1 FROM health_records WHERE username = ? AND record_date = CURRENT_DATE";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUsername);
                return ps.executeQuery().next();
            } catch (SQLException e) {
                return false;
            }
        }

        // ==================== 运动记录操作 ====================

        /** 保存运动记录 */
        static boolean saveExerciseRecord(String type, int duration, String intensity, int calories) {
            String sql = "INSERT INTO exercise_records (username, exercise_type, duration, intensity, calories_burned) " +
                         "VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUsername);
                ps.setString(2, type);
                ps.setInt(3, duration);
                ps.setString(4, intensity);
                ps.setInt(5, calories);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        /** 获取今日运动消耗总热量 */
        static int getTodayExerciseCalories() {
            String sql = "SELECT COALESCE(SUM(calories_burned), 0) FROM exercise_records " +
                         "WHERE username = ? AND record_date = CURRENT_DATE";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUsername);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getInt(1);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return 0;
        }

        /** 获取今日运动记录列表 */
        static List<String[]> getTodayExerciseList() {
            List<String[]> list = new ArrayList<>();
            String sql = "SELECT exercise_type, duration, intensity, calories_burned FROM exercise_records " +
                         "WHERE username = ? AND record_date = CURRENT_DATE ORDER BY id DESC";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUsername);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    list.add(new String[]{
                        rs.getString("exercise_type"),
                        rs.getInt("duration") + "分钟",
                        rs.getString("intensity"),
                        rs.getInt("calories_burned") + "kcal"
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }

        // ==================== 饮食记录操作 ====================

        /** 保存饮食记录 */
        static boolean saveDietRecord(String mealType, String foodName, int calories,
                                       double protein, double carbs, double fat) {
            String sql = "INSERT INTO diet_records (username, meal_type, food_name, calories, protein, carbs, fat) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUsername);
                ps.setString(2, mealType);
                ps.setString(3, foodName);
                ps.setInt(4, calories);
                ps.setDouble(5, protein);
                ps.setDouble(6, carbs);
                ps.setDouble(7, fat);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        /** 获取今日饮食汇总 */
        static int[] getTodayDietSummary() {
            // 返回 [总热量, 蛋白质×100, 碳水×100, 脂肪×100] (乘100保留精度)
            String sql = "SELECT COALESCE(SUM(calories),0), COALESCE(SUM(protein),0), " +
                         "COALESCE(SUM(carbs),0), COALESCE(SUM(fat),0) FROM diet_records " +
                         "WHERE username = ? AND record_date = CURRENT_DATE";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUsername);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return new int[]{ rs.getInt(1), (int)(rs.getDouble(2)*100),
                            (int)(rs.getDouble(3)*100), (int)(rs.getDouble(4)*100) };
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return new int[]{0, 0, 0, 0};
        }

        /** 获取食物列表 */
        static List<String[]> getAllFoods() {
            List<String[]> list = new ArrayList<>();
            String sql = "SELECT food_name, calories, protein, carbs, fat FROM foods ORDER BY id";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    list.add(new String[]{
                        rs.getString("food_name"),
                        rs.getInt("calories") + "",
                        df2.format(rs.getDouble("protein")),
                        df2.format(rs.getDouble("carbs")),
                        df2.format(rs.getDouble("fat"))
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }

        // ==================== 目标操作 ====================

        /** 保存或更新目标 */
        static boolean saveGoal(String goalType, double targetValue) {
            // 先检查是否已有目标
            String checkSql = "SELECT id FROM goals WHERE username = ?";
            String updateSql = "UPDATE goals SET goal_type = ?, target_value = ?, start_date = CURRENT_DATE, " +
                               "end_date = NULL, current_stage = 1 WHERE username = ?";
            String insertSql = "INSERT INTO goals (username, goal_type, target_value) VALUES (?, ?, ?)";
            try (Connection conn = getConnection()) {
                PreparedStatement checkPs = conn.prepareStatement(checkSql);
                checkPs.setString(1, currentUsername);
                if (checkPs.executeQuery().next()) {
                    PreparedStatement ps = conn.prepareStatement(updateSql);
                    ps.setString(1, goalType);
                    ps.setDouble(2, targetValue);
                    ps.setString(3, currentUsername);
                    return ps.executeUpdate() > 0;
                } else {
                    PreparedStatement ps = conn.prepareStatement(insertSql);
                    ps.setString(1, currentUsername);
                    ps.setString(2, goalType);
                    ps.setDouble(3, targetValue);
                    return ps.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        /** 获取目标 */
        static Map<String, Object> getGoal() {
            String sql = "SELECT * FROM goals WHERE username = ? ORDER BY id DESC LIMIT 1";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUsername);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("goal_type", rs.getString("goal_type"));
                    map.put("target_value", rs.getDouble("target_value"));
                    map.put("start_date", rs.getDate("start_date"));
                    map.put("end_date", rs.getDate("end_date"));
                    map.put("current_stage", rs.getInt("current_stage"));
                    return map;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }

        // ==================== 成就操作 ====================

        /** 检查并授予成就徽章 */
        static void checkAndGrantAchievements() {
            try (Connection conn = getConnection()) {
                // 连续打卡天数
                int streak = getCheckInStreak(conn);
                if (streak >= 7) grantBadge(conn, "毅力之星");
                if (streak >= 30) grantBadge(conn, "坚持达人");

                // 饮食记录天数
                int dietDays = getDietDays(conn);
                if (dietDays >= 30) grantBadge(conn, "美食家");

                // 运动记录次数
                int exerciseCount = getExerciseCount(conn);
                if (exerciseCount >= 20) grantBadge(conn, "运动健将");

                // 健康评分
                Map<String, Object> latest = getLatestHealthRecord();
                if (latest != null) {
                    int score = HealthCalculator.calcHealthScore(
                        (double)latest.get("bmi"), (double)latest.get("body_fat"),
                        (int)latest.get("visceral_fat"), (double)latest.get("muscle_rate"),
                        (double)latest.get("water_rate"), currentGender);
                    if (score >= 90) grantBadge(conn, "健康标兵");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private static int getCheckInStreak(Connection conn) throws SQLException {
            String sql = "SELECT COUNT(DISTINCT record_date) FROM health_records " +
                         "WHERE username = ? AND record_date >= CURRENT_DATE - INTERVAL '30 days'";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, currentUsername);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }

        private static int getDietDays(Connection conn) throws SQLException {
            String sql = "SELECT COUNT(DISTINCT record_date) FROM diet_records WHERE username = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, currentUsername);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }

        private static int getExerciseCount(Connection conn) throws SQLException {
            String sql = "SELECT COUNT(*) FROM exercise_records WHERE username = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, currentUsername);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }

        /** 授予徽章（不存在才插入） */
        private static void grantBadge(Connection conn, String badgeName) throws SQLException {
            String checkSql = "SELECT 1 FROM achievements WHERE username = ? AND badge_name = ?";
            PreparedStatement checkPs = conn.prepareStatement(checkSql);
            checkPs.setString(1, currentUsername);
            checkPs.setString(2, badgeName);
            if (!checkPs.executeQuery().next()) {
                String insertSql = "INSERT INTO achievements (username, badge_name) VALUES (?, ?)";
                PreparedStatement insertPs = conn.prepareStatement(insertSql);
                insertPs.setString(1, currentUsername);
                insertPs.setString(2, badgeName);
                insertPs.executeUpdate();
            }
        }

        /** 获取已获得徽章列表 */
        static List<String[]> getAchievements() {
            List<String[]> list = new ArrayList<>();
            String sql = "SELECT badge_name, achieved_date FROM achievements WHERE username = ? ORDER BY achieved_date DESC";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUsername);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    list.add(new String[]{ rs.getString("badge_name"), rs.getDate("achieved_date").toString() });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }

        // ==================== AI 报告操作 ====================

        /** 保存 AI 报告 */
        static boolean saveAIReport(String reportType, String content) {
            String sql = "INSERT INTO ai_reports (username, report_type, report_content) VALUES (?, ?, ?)";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUsername);
                ps.setString(2, reportType);
                ps.setString(3, content);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        /** 获取历史 AI 报告列表 */
        static List<String[]> getAIReports() {
            List<String[]> list = new ArrayList<>();
            String sql = "SELECT id, report_type, generated_at FROM ai_reports WHERE username = ? ORDER BY generated_at DESC";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUsername);
                ResultSet rs = ps.executeQuery();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                while (rs.next()) {
                    list.add(new String[]{
                        String.valueOf(rs.getInt("id")),
                        rs.getString("report_type"),
                        sdf.format(rs.getTimestamp("generated_at"))
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }

        /** 获取 AI 报告内容 */
        static String getAIReportContent(int reportId) {
            String sql = "SELECT report_content FROM ai_reports WHERE id = ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, reportId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getString("report_content");
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return "";
        }
    }

    // ================================================================
    //                  第三部分: 健康计算工具类
    // ================================================================

    /**
     * 健康计算工具类 — 封装所有健康指标计算逻辑
     * 包含: BMI / BMR(三公式) / TDEE / 身体年龄 / 体质分类 / 健康评分 / 预测
     */
    static class HealthCalculator {

        // ==================== BMI 计算 ====================

        /** 计算 BMI = 体重 / (身高/100)² */
        static double calcBMI(double weightKg, double heightCm) {
            double h = heightCm / 100.0;
            return weightKg / (h * h);
        }

        /** BMI 分类 (中国标准) */
        static String classifyBMI(double bmi) {
            if (bmi < 18.5) return "偏瘦";
            if (bmi < 24.0) return "正常";
            if (bmi < 28.0) return "超重";
            return "肥胖";
        }

        // ==================== BMR 计算 (三种公式) ====================

        /** Harris-Benedict 公式 (区分性别) */
        static double calcBMR_Harris(double weight, double height, int age, String gender) {
            if ("男".equals(gender)) {
                return 88.362 + 13.397 * weight + 4.799 * height - 5.677 * age;
            } else {
                return 447.593 + 9.247 * weight + 3.098 * height - 4.330 * age;
            }
        }

        /** Mifflin-St Jeor 公式 (区分性别) */
        static double calcBMR_Mifflin(double weight, double height, int age, String gender) {
            double base = 10 * weight + 6.25 * height - 5 * age;
            return "男".equals(gender) ? base + 5 : base - 161;
        }

        /** 中国营养学会公式 (区分性别) */
        static double calcBMR_China(double weight, int age, String gender) {
            double base = weight * 24 - age * 5;
            return "男".equals(gender) ? base + 100 : base - 100;
        }

        /** 三公式平均值 */
        static double calcAvgBMR(double weight, double height, int age, String gender) {
            double h = calcBMR_Harris(weight, height, age, gender);
            double m = calcBMR_Mifflin(weight, height, age, gender);
            double c = calcBMR_China(weight, age, gender);
            return (h + m + c) / 3.0;
        }

        // ==================== TDEE 计算 ====================

        /** 活动系数映射 */
        static double getActivityFactor(String level) {
            switch (level) {
                case "久坐": return 1.2;
                case "轻度活动": return 1.375;
                case "中度活动": return 1.55;
                case "重度活动": return 1.725;
                case "极重度活动": return 1.9;
                default: return 1.2;
            }
        }

        /** TDEE = BMR × 活动系数 */
        static double calcTDEE(double bmr, String activityLevel) {
            return bmr * getActivityFactor(activityLevel);
        }

        // ==================== 内脏脂肪评估 ====================

        static String assessVisceralFat(int level) {
            if (level <= 4) return "正常";
            if (level <= 8) return "偏高";
            return "过高";
        }

        // ==================== 骨骼肌肉量评级 ====================

        static String assessMuscle(double boneMuscle, double weight, String gender) {
            double standard = "男".equals(gender) ? weight * 0.42 : weight * 0.36;
            double ratio = boneMuscle / standard;
            if (ratio < 0.9) return "偏低";
            if (ratio > 1.1) return "偏高";
            return "正常";
        }

        // ==================== 身体年龄估算 ====================

        static int calcBodyAge(int age, double bodyFat, double muscleRate, int visceralFat, String gender) {
            int bodyAge = age;
            // 体脂率调整
            boolean male = "男".equals(gender);
            if ((male && bodyFat < 15) || (!male && bodyFat < 22)) bodyAge -= 5;
            else if ((male && bodyFat > 25) || (!male && bodyFat > 32)) bodyAge += 5;
            // 肌肉率调整
            double stdMuscle = male ? 40.0 : 35.0;
            if (muscleRate > stdMuscle * 1.1) bodyAge -= 3;
            else if (muscleRate < stdMuscle * 0.9) bodyAge += 3;
            // 内脏脂肪调整
            if (visceralFat <= 4) bodyAge -= 2;
            else if (visceralFat <= 8) bodyAge += 2;
            else bodyAge += 5;
            // 限制在 20-60 之间
            return Math.max(20, Math.min(60, bodyAge));
        }

        // ==================== BMI + 体脂率 综合体质分类 ====================

        /**
         * 完整交叉矩阵分类 (6 种类型)
         * 消瘦型 / 标准型 / 肌肉型 / 超重型 / 肥胖型 / 隐性肥胖型
         */
        static String classifyBodyType(double bmi, double bodyFat, String gender) {
            boolean male = "男".equals(gender);
            // 体脂率分级
            boolean fatLow = male ? bodyFat < 12 : bodyFat < 20;
            boolean fatNormal = male ? (bodyFat >= 12 && bodyFat <= 25) : (bodyFat >= 20 && bodyFat <= 32);
            boolean fatHigh = male ? bodyFat > 25 : bodyFat > 32;

            // BMI 分级
            if (bmi < 18.5) {
                return fatHigh ? "隐性肥胖型" : "消瘦型";
            } else if (bmi < 24.0) {
                return fatHigh ? "隐性肥胖型" : "标准型";
            } else if (bmi < 28.0) {
                if (fatLow) return "肌肉型";
                return fatHigh ? "肥胖型" : "超重型";
            } else {
                return fatLow ? "肌肉型" : "肥胖型";
            }
        }

        // ==================== 理想体重 ====================

        static double calcIdealWeight(double heightCm) {
            double h = heightCm / 100.0;
            return h * h * 22;
        }

        // ==================== 身体形态评估 ====================

        /** 腰围身高比 WHtR 评估 */
        static String assessWHtR(double waist, double height) {
            double whtr = waist / height;
            if (whtr < 0.40) return "偏瘦";
            if (whtr < 0.50) return "正常";
            if (whtr < 0.55) return "腹型肥胖风险";
            return "腹型肥胖";
        }

        /** 体型分类 */
        static String classifyBodyShape(double waist, String gender) {
            boolean male = "男".equals(gender);
            if ((male && waist >= 90) || (!male && waist >= 85)) return "苹果型(中心性肥胖)";
            if ((male && waist < 85) || (!male && waist < 80)) return "梨型/标准型";
            return "轻度腹型肥胖";
        }

        // ==================== 健康评分 (0-100, 五维加权) ====================

        static int calcHealthScore(double bmi, double bodyFat, int visceralFat,
                                    double muscleRate, double waterRate, String gender) {
            boolean male = "男".equals(gender);
            int score = 0;

            // 1. BMI 评分 (满分30)
            if (bmi >= 18.5 && bmi < 24.0) score += 30;
            else if ((bmi >= 24.0 && bmi < 28.0) || (bmi >= 17.0 && bmi < 18.5)) score += 20;
            else score += 10;

            // 2. 体脂率评分 (满分25)
            boolean fatNormal = male ? (bodyFat >= 12 && bodyFat <= 25) : (bodyFat >= 20 && bodyFat <= 32);
            boolean fatSevere = male ? bodyFat > 30 : bodyFat > 38;
            if (fatNormal) score += 25;
            else if (fatSevere) score += 5;
            else score += 15;

            // 3. 内脏脂肪评分 (满分20)
            if (visceralFat <= 4) score += 20;
            else if (visceralFat <= 8) score += 12;
            else score += 5;

            // 4. 肌肉量评分 (满分15) — 用肌肉率近似
            double stdMuscle = male ? 40.0 : 35.0;
            if (muscleRate >= stdMuscle * 0.9 && muscleRate <= stdMuscle * 1.1) score += 15;
            else if (muscleRate > stdMuscle * 1.1) score += 15;
            else score += 8;

            // 5. 水分率评分 (满分10)
            boolean waterNormal = male ? (waterRate >= 50 && waterRate <= 65) : (waterRate >= 45 && waterRate <= 60);
            if (waterNormal) score += 10;
            else score += 5;

            return score;
        }

        /** 健康评分等级 */
        static String scoreLevel(int score) {
            if (score >= 90) return "优秀";
            if (score >= 75) return "良好";
            if (score >= 60) return "及格";
            return "需改善";
        }

        // ==================== 运动热量消耗估算 ====================

        /** MET 值表 */
        static double getMET(String exerciseType) {
            switch (exerciseType) {
                case "快走": return 3.5;
                case "跑步": return 9.0;
                case "游泳": return 7.0;
                case "力量训练": return 5.0;
                case "骑行": return 7.5;
                case "瑜伽": return 3.0;
                case "跳绳": return 12.0;
                case "球类": return 6.0;
                default: return 5.0;
            }
        }

        /** 计算运动消耗热量 = MET × 体重 × 时长(小时) × 强度系数 */
        static int calcExerciseCalories(String type, int duration, String intensity, double weight) {
            double met = getMET(type);
            double hours = duration / 60.0;
            double factor = "低".equals(intensity) ? 0.85 : "高".equals(intensity) ? 1.15 : 1.0;
            return (int)(met * weight * hours * factor);
        }

        // ==================== 预测算法 ====================

        /**
         * 线性回归趋势预测
         * @param dates 日期列表 (按时间正序)
         * @param values 对应数值列表
         * @param futureDays 预测未来天数
         * @return 预测值, 若数据不足返回 Double.NaN
         */
        static double predictTrend(List<Date> dates, List<Double> values, int futureDays) {
            int n = dates.size();
            if (n < 3) return Double.NaN;

            // xi = 距第一天的天数
            long baseTime = dates.get(0).getTime();
            double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
            for (int i = 0; i < n; i++) {
                double xi = (dates.get(i).getTime() - baseTime) / (1000.0 * 60 * 60 * 24);
                double yi = values.get(i);
                sumX += xi; sumY += yi;
                sumXY += xi * yi; sumX2 += xi * xi;
            }
            double denominator = n * sumX2 - sumX * sumX;
            if (Math.abs(denominator) < 1e-10) return values.get(n - 1); // 数据无变化

            double k = (n * sumXY - sumX * sumY) / denominator;
            double b = (sumY - k * sumX) / n;

            double lastX = (dates.get(n - 1).getTime() - baseTime) / (1000.0 * 60 * 60 * 24);
            return k * (lastX + futureDays) + b;
        }

        /**
         * 趋势判断
         */
        static String trendDirection(List<Date> dates, List<Double> values) {
            int n = dates.size();
            if (n < 3) return "数据不足";
            long baseTime = dates.get(0).getTime();
            double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
            for (int i = 0; i < n; i++) {
                double xi = (dates.get(i).getTime() - baseTime) / (1000.0 * 60 * 60 * 24);
                double yi = values.get(i);
                sumX += xi; sumY += yi;
                sumXY += xi * yi; sumX2 += xi * xi;
            }
            double denominator = n * sumX2 - sumX * sumX;
            if (Math.abs(denominator) < 1e-10) return "趋于稳定";
            double k = (n * sumXY - sumX * sumY) / denominator;
            if (Math.abs(k) < 0.01) return "趋于稳定";
            return k > 0 ? "上升" : "下降";
        }

        /**
         * 目标达成日期预测
         * @return 预测天数, -1 表示无法达成, -2 表示进度过慢
         */
        static int predictGoalDays(double currentWeight, double targetWeight,
                                    double dailyCalorieDeficit, String goalType) {
            if (Math.abs(dailyCalorieDeficit) < 100) return -2;

            double diff = currentWeight - targetWeight;
            if ("减脂".equals(goalType) || "减重".equals(goalType)) {
                if (diff <= 0) return 0; // 已达标
                if (dailyCalorieDeficit <= 0) return -1;
                return (int)Math.ceil((diff * 7700) / dailyCalorieDeficit);
            } else if ("增肌".equals(goalType)) {
                if (diff >= 0) return 0;
                return (int)Math.ceil((Math.abs(diff) * 5500) / Math.abs(dailyCalorieDeficit));
            }
            return -1;
        }

        /**
         * 健康风险评估
         */
        static String assessRisk(double predictedBMI30) {
            if (predictedBMI30 >= 28.0)
                return "高风险 — 预测30天后BMI将进入肥胖区间, 建议立即调整饮食和运动计划";
            if (predictedBMI30 >= 24.0 || predictedBMI30 < 18.5)
                return "中风险 — 预测30天后BMI偏离正常范围, 需关注体重变化";
            return "低风险 — 预测30天后BMI维持在正常范围, 继续保持";
        }
    }

    // ================================================================
    //                  第四部分: 登录/注册窗口 (v2.0 现代设计)
    // ================================================================

    /** 登录/注册窗口 — 现代渐变卡片式设计 */
    static class LoginFrame extends JFrame {
        private JTextField tfUsername = new JTextField(15);
        private JPasswordField pfPassword = new JPasswordField(15);
        private JTextField tfRegUsername = new JTextField(15);
        private JPasswordField pfRegPassword = new JPasswordField(15);
        private JPasswordField pfRegPassword2 = new JPasswordField(15);
        private JComboBox<String> cbGender = new JComboBox<>(new String[]{"男", "女"});
        private JSpinner spAge = new JSpinner(new SpinnerNumberModel(25, 5, 120, 1));
        private JTextField tfHeight = new JTextField("170");
        private JComboBox<String> cbActivity = new JComboBox<>(
                new String[]{"久坐", "轻度活动", "中度活动", "重度活动", "极重度活动"});

        LoginFrame() {
            setTitle("BMI 体质评估与预测系统 v2.0");
            setSize(500, 620);
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setLocationRelativeTo(null);
            setResizable(false);
            setUndecorated(true);

            // 全屏渐变背景
            GradientPanel bgPanel = new GradientPanel(new BorderLayout(), Theme.GRAD_PRIMARY);
            bgPanel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

            // === 顶部 Logo 区 ===
            JPanel topArea = new JPanel(new BorderLayout());
            topArea.setOpaque(false);

            // Logo 圆形图标
            JPanel logoCircle = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(255, 255, 255, 30));
                    g2.fillOval(0, 0, 56, 56);
                    g2.setColor(new Color(255, 255, 255, 60));
                    g2.drawOval(0, 0, 56, 56);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font(Theme.FONT_NAME, Font.BOLD, 26));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString("♥", (56 - fm.stringWidth("♥")) / 2, 38);
                    g2.dispose();
                }
            };
            logoCircle.setPreferredSize(new Dimension(56, 56));
            topArea.add(logoCircle, BorderLayout.CENTER);

            JLabel lblTitle = new JLabel("BMI 体质评估与预测系统", SwingConstants.CENTER);
            lblTitle.setFont(Theme.FONT_H1);
            lblTitle.setForeground(Color.WHITE);
            lblTitle.setBorder(BorderFactory.createEmptyBorder(12, 0, 2, 0));
            topArea.add(lblTitle, BorderLayout.SOUTH);

            bgPanel.add(topArea, BorderLayout.NORTH);

            // === 中间白色卡片 ===
            RoundedPanel card = new RoundedPanel(new BorderLayout(), 18);
            card.setBackground(Theme.CARD_BG);
            card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            JTabbedPane tabPane = new JTabbedPane();
            tabPane.setFont(Theme.FONT_BODY_B);
            tabPane.setBackground(Theme.CARD_BG);
            tabPane.setForeground(Theme.TEXT_DARK);
            tabPane.setBorder(null);
            tabPane.addTab("  登 录  ", createLoginPanel());
            tabPane.addTab("  注 册  ", createRegisterPanel());
            card.add(tabPane, BorderLayout.CENTER);

            bgPanel.add(card, BorderLayout.CENTER);

            // === 底部关闭按钮 ===
            JButton btnClose = new JButton("✕");
            btnClose.setFont(new Font(Theme.FONT_NAME, Font.PLAIN, 14));
            btnClose.setForeground(new Color(255, 255, 255, 180));
            btnClose.setContentAreaFilled(false);
            btnClose.setBorderPainted(false);
            btnClose.setFocusPainted(false);
            btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnClose.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            btnClose.addActionListener(e -> System.exit(0));
            btnClose.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) { btnClose.setForeground(Color.WHITE); }
                @Override
                public void mouseExited(MouseEvent e) { btnClose.setForeground(new Color(255, 255, 255, 180)); }
            });
            JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            closePanel.setOpaque(false);
            closePanel.add(btnClose);
            bgPanel.add(closePanel, BorderLayout.SOUTH);

            add(bgPanel);

            // 淡入动画
            FadeIn.apply(bgPanel, 400);
        }

        /** 登录面板 — 现代紧凑布局 */
        private JPanel createLoginPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setOpaque(false);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6, 8, 6, 8);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            JLabel lblHint = new JLabel("  欢迎回来，请登录您的账号");
            lblHint.setFont(Theme.FONT_SMALL);
            lblHint.setForeground(Theme.TEXT_GRAY);
            gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
            gbc.insets = new Insets(4, 8, 16, 8);
            panel.add(lblHint, gbc);
            gbc.insets = new Insets(6, 8, 6, 8);
            gbc.gridwidth = 1;

            // 用户名
            JLabel labUser = new JLabel("  用户名");
            labUser.setFont(Theme.FONT_SMALL);
            labUser.setForeground(Theme.TEXT_GRAY);
            gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
            panel.add(labUser, gbc);
            Theme.styleTextField(tfUsername);
            gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
            panel.add(tfUsername, gbc);
            gbc.gridwidth = 1;

            // 密码
            JLabel labPass = new JLabel("  密码");
            labPass.setFont(Theme.FONT_SMALL);
            labPass.setForeground(Theme.TEXT_GRAY);
            gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
            panel.add(labPass, gbc);
            Theme.stylePasswordField(pfPassword);
            gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
            panel.add(pfPassword, gbc);
            gbc.gridwidth = 1;

            // 登录按钮
            JButton btnLogin = new RoundButton("登  录", Theme.PRIMARY, Theme.PRIMARY_D);
            btnLogin.addActionListener(e -> doLogin());
            gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(16, 8, 6, 8);
            panel.add(btnLogin, gbc);

            // 回车登录
            ActionListener loginAction = e -> doLogin();
            tfUsername.addActionListener(loginAction);
            pfPassword.addActionListener(loginAction);

            return panel;
        }

        /** 注册面板 — 现代表单 */
        private JPanel createRegisterPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setOpaque(false);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(4, 8, 4, 8);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;

            Theme.styleTextField(tfRegUsername);
            Theme.stylePasswordField(pfRegPassword);
            Theme.stylePasswordField(pfRegPassword2);
            Theme.styleComboBox(cbGender);
            Theme.styleComboBox(cbActivity);
            Theme.styleSpinner(spAge);

            int row = 0;
            String[][] fields = {
                {"  用户名", "tfRegUsername"},
                {"  密码", "pfRegPassword"},
                {"  确认密码", "pfRegPassword2"},
                {"  性别", "cbGender"},
                {"  年龄", "spAge"},
                {"  身高 (cm)", "tfHeight"},
                {"  活动等级", "cbActivity"}
            };
            Component[] comps = {tfRegUsername, pfRegPassword, pfRegPassword2, cbGender, spAge, tfHeight, cbActivity};
            Theme.styleTextField(tfHeight);

            for (int i = 0; i < fields.length; i++) {
                JLabel lab = new JLabel(fields[i][0]);
                lab.setFont(Theme.FONT_SMALL);
                lab.setForeground(Theme.TEXT_GRAY);
                gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
                panel.add(lab, gbc);
                gbc.gridwidth = 1;
                gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
                panel.add(comps[i], gbc);
                gbc.gridwidth = 1;
            }

            JButton btnRegister = new RoundButton("注  册", Theme.ACCENT, new Color(13, 148, 136));
            btnRegister.addActionListener(e -> doRegister());
            gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(12, 8, 4, 8);
            panel.add(btnRegister, gbc);

            return panel;
        }

        /** 执行登录 */
        private void doLogin() {
            String username = tfUsername.getText().trim();
            String password = new String(pfPassword.getPassword());

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "请输入用户名和密码", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (DBUtil.loginUser(username, password)) {
                dispose();
                try {
                    MainFrame frame = new MainFrame();
                    frame.setVisible(true);
                    FadeIn.apply(frame.getRootPane(), 500);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "主界面启动失败: " + ex.getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
                    new LoginFrame().setVisible(true);
                }
            } else {
                JOptionPane.showMessageDialog(this, "用户名或密码错误", "登录失败", JOptionPane.ERROR_MESSAGE);
            }
        }

        /** 执行注册 */
        private void doRegister() {
            String username = tfRegUsername.getText().trim();
            String pass1 = new String(pfRegPassword.getPassword());
            String pass2 = new String(pfRegPassword2.getPassword());
            String gender = (String) cbGender.getSelectedItem();
            int age = (int) spAge.getValue();
            String heightStr = tfHeight.getText().trim();
            String activity = (String) cbActivity.getSelectedItem();

            if (username.isEmpty() || pass1.isEmpty()) {
                JOptionPane.showMessageDialog(this, "用户名和密码不能为空", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!pass1.equals(pass2)) {
                JOptionPane.showMessageDialog(this, "两次密码不一致", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            double height;
            try {
                height = Double.parseDouble(heightStr);
                if (height < 100 || height > 250) {
                    JOptionPane.showMessageDialog(this, "身高范围 100-250cm", "提示", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "身高请输入数字", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (DBUtil.registerUser(username, pass1, gender, age, height, activity)) {
                JOptionPane.showMessageDialog(this, "注册成功！请登录");
                tfUsername.setText(username);
            }
        }
    }

    // ================================================================
    //                  第五部分: 主窗口 (v2.0 现代布局)
    // ================================================================

    /** 主窗口 — 现代顶部栏+标签页+底部状态栏布局 */
    static class MainFrame extends JFrame {
        private JLabel lblUserInfo;
        private JLabel lblScore;
        private JLabel lblCalorieDiff;
        private JLabel lblCheckStatus;
        private JTabbedPane tabPane;

        MainFrame() {
            setTitle("BMI 体质评估与预测系统 v2.0");
            setSize(1200, 800);
            setMinimumSize(new Dimension(1000, 700));
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setLocationRelativeTo(null);

            initUI();

            SwingUtilities.invokeLater(() -> {
                if (!DBUtil.isCheckedToday()) {
                    JOptionPane.showMessageDialog(this, "今天称重了吗？记得打卡记录健康数据！",
                            "每日提醒", JOptionPane.INFORMATION_MESSAGE);
                }
                checkAbnormalData();
            });
        }

        /** 初始化界面 — 现代分层布局 */
        private void initUI() {
            setLayout(new BorderLayout());

            // === 顶部栏 — 深色渐变，紧凑信息条 ===
            GradientPanel topPanel = new GradientPanel(new BorderLayout(), Theme.GRAD_PRIMARY);
            topPanel.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));

            // 左侧：Logo + 用户信息
            JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
            leftPanel.setOpaque(false);

            // 小 Logo
            JLabel logo = new JLabel("♥") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(255, 255, 255, 30));
                    g2.fillOval(0, 0, 32, 32);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font(Theme.FONT_NAME, Font.BOLD, 16));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString("♥", (32 - fm.stringWidth("♥")) / 2, 22);
                    g2.dispose();
                }
            };
            logo.setPreferredSize(new Dimension(32, 32));
            leftPanel.add(logo);

            lblUserInfo = new JLabel();
            lblUserInfo.setFont(Theme.FONT_BODY);
            lblUserInfo.setForeground(new Color(255, 255, 255, 220));
            leftPanel.add(lblUserInfo);
            topPanel.add(leftPanel, BorderLayout.WEST);

            // 右侧：健康评分
            lblScore = new JLabel("健康评分: --", SwingConstants.RIGHT);
            lblScore.setFont(Theme.FONT_BODY_B);
            lblScore.setForeground(new Color(255, 255, 255, 240));
            topPanel.add(lblScore, BorderLayout.EAST);

            add(topPanel, BorderLayout.NORTH);

            // === 中间标签页区域 ===
            JPanel centerWrapper = new JPanel(new BorderLayout());
            centerWrapper.setBackground(Theme.BG);
            centerWrapper.setBorder(BorderFactory.createEmptyBorder(10, 12, 6, 12));

            tabPane = new JTabbedPane();
            tabPane.setFont(Theme.FONT_BODY_B);
            tabPane.setForeground(Theme.TEXT_DARK);
            tabPane.setBackground(Theme.BG);
            tabPane.setBorder(null);
            tabPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

            tabPane.addTab("  数据录入  ", new DataInputPanel(this));
            tabPane.addTab("  历史趋势  ", new HistoryTrendPanel());
            tabPane.addTab("  分析评估  ", new AnalysisPanel());
            tabPane.addTab("  预测分析  ", new PredictionPanel());
            tabPane.addTab("  目标计划  ", new GoalPlanPanel());
            tabPane.addTab("  饮食管理  ", new DietPanel(this));
            tabPane.addTab("  成就徽章  ", new AchievementPanel());
            tabPane.addTab("  数据大屏  ", new DashboardPanel());

            centerWrapper.add(tabPane, BorderLayout.CENTER);
            add(centerWrapper, BorderLayout.CENTER);

            // === 底部状态栏 — 浅色简洁条 ===
            JPanel bottomPanel = new JPanel(new BorderLayout());
            bottomPanel.setBackground(Theme.FOOTER_BG);
            bottomPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                    BorderFactory.createEmptyBorder(6, 18, 6, 18)));

            lblCalorieDiff = new JLabel("今日热量差: --");
            lblCalorieDiff.setFont(Theme.FONT_SMALL);
            lblCalorieDiff.setForeground(Theme.TEXT_BODY);
            bottomPanel.add(lblCalorieDiff, BorderLayout.WEST);

            lblCheckStatus = new JLabel("打卡状态: --", SwingConstants.RIGHT);
            lblCheckStatus.setFont(Theme.FONT_SMALL);
            lblCheckStatus.setForeground(Theme.TEXT_BODY);
            bottomPanel.add(lblCheckStatus, BorderLayout.EAST);

            add(bottomPanel, BorderLayout.SOUTH);

            refreshData();
        }

        /** 刷新所有数据 */
        void refreshData() {
            lblUserInfo.setText(String.format("👤 %s  |  %s  |  %d岁  |  %.1fcm  |  %s",
                    currentUsername, currentGender, currentAge, currentHeight, currentActivityLevel));

            Map<String, Object> latest = DBUtil.getLatestHealthRecord();
            if (latest != null) {
                int score = HealthCalculator.calcHealthScore(
                        (double) latest.get("bmi"), (double) latest.get("body_fat"),
                        (int) latest.get("visceral_fat"), (double) latest.get("muscle_rate"),
                        (double) latest.get("water_rate"), currentGender);
                lblScore.setText("健康评分: " + score + "/100 (" + HealthCalculator.scoreLevel(score) + ")");
            } else {
                lblScore.setText("健康评分: 暂无数据");
            }

            if (latest != null) {
                double tdee = (double) latest.get("tdee");
                int exerciseCal = DBUtil.getTodayExerciseCalories();
                int[] diet = DBUtil.getTodayDietSummary();
                int intakeCal = diet[0];
                int diff = (int)(tdee + exerciseCal - intakeCal);
                lblCalorieDiff.setText("今日热量差: " + (diff >= 0 ? "+" : "") + diff + " kcal " +
                        "(消耗:" + (int)(tdee + exerciseCal) + " / 摄入:" + intakeCal + ")");
            } else {
                lblCalorieDiff.setText("今日热量差: 暂无数据");
            }

            lblCheckStatus.setText("打卡状态: " + (DBUtil.isCheckedToday() ? "已打卡 ✓" : "未打卡"));

            DBUtil.checkAndGrantAchievements();
        }

        /** 切换到指定标签页 */
        void switchToTab(int index) {
            tabPane.setSelectedIndex(index);
        }

        /** 异常数据预警 */
        private void checkAbnormalData() {
            List<Map<String, Object>> records = DBUtil.getHealthRecords(7);
            if (records.size() >= 2) {
                double latestWeight = (double) records.get(0).get("weight");
                double oldestWeight = (double) records.get(records.size() - 1).get("weight");
                if (Math.abs(latestWeight - oldestWeight) > 5) {
                    JOptionPane.showMessageDialog(this,
                            "⚠️ 一周内体重变化超过 5kg，建议就医检查！",
                            "异常数据预警", JOptionPane.WARNING_MESSAGE);
                }
            }
        }
    }

    // ================================================================
    //               第六部分: Tab1 数据录入面板
    // ================================================================

    /** 数据录入面板 — v2.0 现代卡片布局 */
    static class DataInputPanel extends JPanel {
        private MainFrame mainFrame;
        private JTextField tfWeight = new JTextField("65.0");
        private JTextField tfBodyFat = new JTextField("20.0");
        private JTextField tfWater = new JTextField("55.0");
        private JTextField tfMuscle = new JTextField("35.0");
        private JTextField tfVisceral = new JTextField("5");
        private JTextField tfBoneMuscle = new JTextField("28.0");
        private JTextField tfWaist = new JTextField("80.0");
        private JComboBox<String> cbExerciseType = new JComboBox<>(
                new String[]{"跑步", "游泳", "力量训练", "骑行", "瑜伽", "跳绳", "快走", "球类"});
        private JTextField tfDuration = new JTextField("30");
        private JComboBox<String> cbIntensity = new JComboBox<>(new String[]{"低", "中", "高"});
        private JTable exerciseTable;
        private DefaultTableModel exerciseModel;

        DataInputPanel(MainFrame frame) {
            this.mainFrame = frame;
            setLayout(new BorderLayout(10, 10));
            setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            setBackground(Theme.BG);

            // === 健康数据录入卡片 ===
            JPanel healthPanel = Theme.createCardPanel("健康数据录入", Theme.PRIMARY);
            healthPanel.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 8, 5, 8);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            // 按钮行 — 模拟称重
            JButton btnSimulate = new RoundButton("  📊 模拟称重  ", Theme.ACCENT, new Color(13, 148, 136));
            btnSimulate.addActionListener(e -> simulateWeighing());
            gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 6; gbc.anchor = GridBagConstraints.CENTER;
            gbc.insets = new Insets(4, 8, 12, 8);
            healthPanel.add(btnSimulate, gbc);
            gbc.insets = new Insets(5, 8, 5, 8);
            gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.WEST;

            // 输入字段
            addField(healthPanel, gbc, 1, "体重(kg)", tfWeight, 0);
            addField(healthPanel, gbc, 1, "体脂率(%)", tfBodyFat, 2);
            addField(healthPanel, gbc, 2, "水分率(%)", tfWater, 0);
            addField(healthPanel, gbc, 2, "肌肉率(%)", tfMuscle, 2);
            addField(healthPanel, gbc, 3, "内脏脂肪(级)", tfVisceral, 0);
            addField(healthPanel, gbc, 3, "骨骼肌肉量(kg)", tfBoneMuscle, 2);
            addField(healthPanel, gbc, 4, "腰围(cm)", tfWaist, 0);

            JButton btnSave = new RoundButton("  💾 保存健康记录  ", Theme.PRIMARY, Theme.PRIMARY_D);
            btnSave.addActionListener(e -> saveHealthData());
            gbc.gridx = 4; gbc.gridy = 4; gbc.gridwidth = 2;
            gbc.insets = new Insets(8, 8, 4, 8);
            healthPanel.add(btnSave, gbc);
            gbc.gridwidth = 1;
            gbc.insets = new Insets(5, 8, 5, 8);

            Theme.styleTextField(tfWeight);
            Theme.styleTextField(tfBodyFat);
            Theme.styleTextField(tfWater);
            Theme.styleTextField(tfMuscle);
            Theme.styleTextField(tfVisceral);
            Theme.styleTextField(tfBoneMuscle);
            Theme.styleTextField(tfWaist);

            add(healthPanel, BorderLayout.NORTH);

            // === 运动记录卡片 ===
            JPanel exercisePanel = Theme.createCardPanel("运动记录", Theme.ACCENT);

            JPanel exInputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
            exInputPanel.setOpaque(false);
            Theme.styleComboBox(cbExerciseType);
            Theme.styleTextField(tfDuration);
            Theme.styleComboBox(cbIntensity);

            JLabel lblEx = new JLabel("运动类型"); lblEx.setFont(Theme.FONT_SMALL); lblEx.setForeground(Theme.TEXT_GRAY);
            exInputPanel.add(lblEx); exInputPanel.add(cbExerciseType);
            lblEx = new JLabel("时长(分钟)"); lblEx.setFont(Theme.FONT_SMALL); lblEx.setForeground(Theme.TEXT_GRAY);
            exInputPanel.add(lblEx); exInputPanel.add(tfDuration);
            lblEx = new JLabel("强度"); lblEx.setFont(Theme.FONT_SMALL); lblEx.setForeground(Theme.TEXT_GRAY);
            exInputPanel.add(lblEx); exInputPanel.add(cbIntensity);
            JButton btnAddExercise = new RoundButton("记录运动", Theme.PRIMARY, Theme.PRIMARY_D);
            btnAddExercise.addActionListener(e -> addExercise());
            exInputPanel.add(btnAddExercise);
            exercisePanel.add(exInputPanel, BorderLayout.NORTH);

            exerciseModel = new DefaultTableModel(new String[]{"运动类型", "时长", "强度", "消耗热量"}, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            exerciseTable = new JTable(exerciseModel);
            Theme.styleTable(exerciseTable);
            JScrollPane scrollEx = new JScrollPane(exerciseTable);
            scrollEx.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
            scrollEx.getViewport().setBackground(Theme.CARD_BG);
            exercisePanel.add(scrollEx, BorderLayout.CENTER);

            add(exercisePanel, BorderLayout.CENTER);

            refreshExerciseTable();
        }

        /** 辅助方法: 添加标签和输入框 */
        private void addField(JPanel panel, GridBagConstraints gbc, int row,
                              String label, JTextField field, int col) {
            gbc.gridy = row;
            gbc.gridx = col;
            JLabel lbl = new JLabel(label);
            lbl.setFont(Theme.FONT_SMALL);
            lbl.setForeground(Theme.TEXT_GRAY);
            panel.add(lbl, gbc);
            gbc.gridx = col + 1;
            field.setColumns(8);
            panel.add(field, gbc);
        }

        /** 模拟称重 — 随机生成各项指标 */
        private void simulateWeighing() {
            Random rand = new Random();
            tfWeight.setText(df1.format(50 + rand.nextDouble() * 50));
            tfBodyFat.setText(df1.format(10 + rand.nextDouble() * 30));
            tfWater.setText(df1.format(30 + rand.nextDouble() * 40));
            tfMuscle.setText(df1.format(20 + rand.nextDouble() * 30));
            tfVisceral.setText(String.valueOf(1 + rand.nextInt(10)));
            tfBoneMuscle.setText(df1.format(20 + rand.nextDouble() * 30));
            tfWaist.setText(df1.format(60 + rand.nextDouble() * 50));
            JOptionPane.showMessageDialog(this, "模拟称重完成! 数据已填入, 点击保存记录到数据库");
        }

        /** 保存健康数据 */
        private void saveHealthData() {
            try {
                double weight = Double.parseDouble(tfWeight.getText().trim());
                double bodyFat = Double.parseDouble(tfBodyFat.getText().trim());
                double water = Double.parseDouble(tfWater.getText().trim());
                double muscle = Double.parseDouble(tfMuscle.getText().trim());
                int visceral = Integer.parseInt(tfVisceral.getText().trim());
                double boneMuscle = Double.parseDouble(tfBoneMuscle.getText().trim());
                double waist = Double.parseDouble(tfWaist.getText().trim());

                // 数据校验
                if (!validateData(weight, bodyFat, water, muscle, visceral, waist)) return;

                if (DBUtil.saveHealthRecord(weight, bodyFat, water, muscle, visceral, boneMuscle, waist)) {
                    JOptionPane.showMessageDialog(this, "健康记录保存成功!");
                    mainFrame.refreshData();
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "请输入有效的数字", "输入错误", JOptionPane.ERROR_MESSAGE);
            }
        }

        /** 数据校验 */
        private boolean validateData(double weight, double bodyFat, double water,
                                      double muscle, int visceral, double waist) {
            if (weight < 20 || weight > 300) {
                JOptionPane.showMessageDialog(this, "体重输入异常, 请重新输入 (20-300kg)", "校验失败", JOptionPane.WARNING_MESSAGE);
                return false;
            }
            if (bodyFat < 3 || bodyFat > 60) {
                JOptionPane.showMessageDialog(this, "体脂率输入异常, 请重新输入 (3-60%)", "校验失败", JOptionPane.WARNING_MESSAGE);
                return false;
            }
            if (water < 20 || water > 80) {
                JOptionPane.showMessageDialog(this, "水分率输入异常, 请重新输入 (20-80%)", "校验失败", JOptionPane.WARNING_MESSAGE);
                return false;
            }
            if (muscle < 10 || muscle > 70) {
                JOptionPane.showMessageDialog(this, "肌肉率输入异常, 请重新输入 (10-70%)", "校验失败", JOptionPane.WARNING_MESSAGE);
                return false;
            }
            if (visceral < 1 || visceral > 30) {
                JOptionPane.showMessageDialog(this, "内脏脂肪等级异常, 请重新输入 (1-30)", "校验失败", JOptionPane.WARNING_MESSAGE);
                return false;
            }
            if (waist < 40 || waist > 200) {
                JOptionPane.showMessageDialog(this, "腰围输入异常, 请重新输入 (40-200cm)", "校验失败", JOptionPane.WARNING_MESSAGE);
                return false;
            }
            return true;
        }

        /** 添加运动记录 */
        private void addExercise() {
            try {
                String type = (String) cbExerciseType.getSelectedItem();
                int duration = Integer.parseInt(tfDuration.getText().trim());
                String intensity = (String) cbIntensity.getSelectedItem();

                if (duration <= 0 || duration >= 600) {
                    JOptionPane.showMessageDialog(this, "时长范围 1-600 分钟", "提示", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // 获取最新体重用于计算消耗
                Map<String, Object> latest = DBUtil.getLatestHealthRecord();
                double weight = latest != null ? (double) latest.get("weight") : 65.0;
                int calories = HealthCalculator.calcExerciseCalories(type, duration, intensity, weight);

                if (DBUtil.saveExerciseRecord(type, duration, intensity, calories)) {
                    JOptionPane.showMessageDialog(this, "运动记录保存成功! 消耗 " + calories + " kcal");
                    refreshExerciseTable();
                    mainFrame.refreshData();
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "请输入有效的数字", "输入错误", JOptionPane.ERROR_MESSAGE);
            }
        }

        /** 刷新运动列表 */
        private void refreshExerciseTable() {
            exerciseModel.setRowCount(0);
            for (String[] row : DBUtil.getTodayExerciseList()) {
                exerciseModel.addRow(row);
            }
        }
    }

    // ================================================================
    //             第七部分: Tab2 历史趋势面板 (折线图)
    // ================================================================

    /** 历史趋势面板 — 折线图 + 历史记录表格 */
    static class HistoryTrendPanel extends JPanel {
        private JComboBox<String> cbMetric = new JComboBox<>(
                new String[]{"体重", "体脂率", "肌肉率", "BMI", "腰围"});
        private LineChartPanel chartPanel;
        private JTable historyTable;
        private DefaultTableModel historyModel;

        HistoryTrendPanel() {
            setLayout(new BorderLayout(10, 10));
            setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            setBackground(Theme.BG);

            // === 控制卡片 ===
            JPanel ctrlCard = Theme.createCardPanel("趋势指标选择", Theme.PRIMARY);
            JPanel ctrlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            ctrlPanel.setOpaque(false);
            JLabel lblMetric = new JLabel("选择指标");
            lblMetric.setFont(Theme.FONT_SMALL);
            lblMetric.setForeground(Theme.TEXT_GRAY);
            ctrlPanel.add(lblMetric);
            Theme.styleComboBox(cbMetric);
            ctrlPanel.add(cbMetric);
            JButton btnRefresh = new RoundButton("刷新趋势", Theme.PRIMARY, Theme.PRIMARY_D);
            btnRefresh.addActionListener(e -> refresh());
            ctrlPanel.add(btnRefresh);
            ctrlCard.add(ctrlPanel, BorderLayout.CENTER);
            add(ctrlCard, BorderLayout.NORTH);

            // === 图表卡片 ===
            JPanel chartCard = Theme.createCardPanel("历史趋势图", Theme.PRIMARY);
            chartPanel = new LineChartPanel();
            chartPanel.setPreferredSize(new Dimension(700, 300));
            chartPanel.setMinimumSize(new Dimension(400, 200));
            chartCard.add(chartPanel, BorderLayout.CENTER);

            // === 历史数据表格卡片 ===
            JPanel tableCard = Theme.createCardPanel("历史记录明细", Theme.ACCENT);
            historyModel = new DefaultTableModel(
                    new String[]{"日期", "体重", "体脂率", "水分率", "肌肉率", "内脏脂肪", "BMI", "腰围", "身体年龄", "体质分类"}, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            historyTable = new JTable(historyModel);
            Theme.styleTable(historyTable);
            tableCard.add(new JScrollPane(historyTable), BorderLayout.CENTER);
            tableCard.setMinimumSize(new Dimension(400, 120));

            // 中间区域: 图表在上, 表格在下 (用 JSplitPane 确保两者都有空间)
            JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, chartCard, tableCard);
            splitPane.setOpaque(false);
            splitPane.setDividerLocation(300);
            splitPane.setResizeWeight(0.7);
            splitPane.setBorder(BorderFactory.createEmptyBorder());
            add(splitPane, BorderLayout.CENTER);

            cbMetric.addActionListener(e -> refresh());
            refresh();
        }

        /** 刷新图表和表格 */
        void refresh() {
            String metric = (String) cbMetric.getSelectedItem();
            List<Map<String, Object>> records = DBUtil.getHealthRecords(60);

            // 更新图表
            List<Date> dates = new ArrayList<>();
            List<Double> values = new ArrayList<>();
            for (int i = records.size() - 1; i >= 0; i--) {
                Map<String, Object> r = records.get(i);
                dates.add((Date) r.get("record_date"));
                switch (metric) {
                    case "体重": values.add((double) r.get("weight")); break;
                    case "体脂率": values.add((double) r.get("body_fat")); break;
                    case "肌肉率": values.add((double) r.get("muscle_rate")); break;
                    case "BMI": values.add((double) r.get("bmi")); break;
                    case "腰围": values.add((double) r.get("waist")); break;
                }
            }
            chartPanel.setData(dates, values, metric);

            // 更新表格
            historyModel.setRowCount(0);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            for (Map<String, Object> r : records) {
                historyModel.addRow(new Object[]{
                        sdf.format((Date) r.get("record_date")),
                        df1.format(r.get("weight")) + "kg",
                        df1.format(r.get("body_fat")) + "%",
                        df1.format(r.get("water_rate")) + "%",
                        df1.format(r.get("muscle_rate")) + "%",
                        r.get("visceral_fat") + "级",
                        df1.format(r.get("bmi")),
                        df1.format(r.get("waist")) + "cm",
                        r.get("body_age") + "岁",
                        r.get("body_type")
                });
            }
        }
    }

    /** 折线图面板 — v2.0 现代图表样式 */
    static class LineChartPanel extends JPanel {
        private List<Date> dates = new ArrayList<>();
        private List<Double> values = new ArrayList<>();
        private String metricName = "";
        private List<Double> predictedValues = new ArrayList<>();

        void setData(List<Date> dates, List<Double> values, String metricName) {
            this.dates = dates;
            this.values = values;
            this.metricName = metricName;
            predictedValues.clear();
            if (dates.size() >= 3) {
                for (int d : new int[]{7, 14, 30}) {
                    double pred = HealthCalculator.predictTrend(dates, values, d);
                    if (!Double.isNaN(pred)) predictedValues.add(pred);
                }
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int padLeft = 60, padRight = 30, padTop = 36, padBottom = 50;
            int chartW = w - padLeft - padRight;
            int chartH = h - padTop - padBottom;

            // 背景 — 圆角区域
            g2.setColor(Theme.CARD_BG);
            g2.fillRect(padLeft, padTop, chartW, chartH);

            // 标题
            g2.setColor(Theme.TEXT_DARK);
            g2.setFont(new Font(Theme.FONT_NAME, Font.BOLD, 14));
            g2.drawString(metricName + " 历史趋势" + (predictedValues.size() > 0 ? "  (虚线为预测)" : ""),
                    padLeft, padTop - 12);

            if (values.isEmpty()) {
                g2.setColor(Theme.TEXT_LIGHT);
                g2.setFont(new Font(Theme.FONT_NAME, Font.PLAIN, 13));
                FontMetrics fm = g2.getFontMetrics();
                String msg = "暂无数据，请先录入健康记录";
                g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
                return;
            }

            // 计算范围
            double minVal = Double.MAX_VALUE, maxVal = Double.MIN_VALUE;
            for (double v : values) {
                minVal = Math.min(minVal, v);
                maxVal = Math.max(maxVal, v);
            }
            for (double v : predictedValues) {
                minVal = Math.min(minVal, v);
                maxVal = Math.max(maxVal, v);
            }
            double range = maxVal - minVal;
            if (range < 1) range = 1;
            minVal -= range * 0.1;
            maxVal += range * 0.1;
            range = maxVal - minVal;

            // Y 轴刻度 — 浅色网格线
            g2.setFont(new Font(Theme.FONT_NAME, Font.PLAIN, 11));
            for (int i = 0; i <= 5; i++) {
                int y = padTop + chartH - (int)(chartH * i / 5.0);
                double val = minVal + range * i / 5.0;
                g2.setColor(Theme.TEXT_LIGHT);
                g2.drawString(df1.format(val), 10, y + 5);
                g2.setColor(new Color(241, 245, 249));
                g2.drawLine(padLeft, y, padLeft + chartW, y);
            }

            // X 轴日期
            int n = dates.size();
            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd");
            g2.setColor(Theme.TEXT_LIGHT);
            int maxLabels = Math.min(n, 8);
            for (int i = 0; i < maxLabels; i++) {
                int idx = n > 1 ? i * (n - 1) / (maxLabels - 1) : 0;
                int x = padLeft + (int)(chartW * idx / Math.max(n - 1, 1));
                g2.drawString(sdf.format(dates.get(idx)), x - 20, padTop + chartH + 18);
            }

            // 画渐变填充区域 (实际数据下方)
            if (n >= 2) {
                GradientPaint fillGrad = new GradientPaint(0, padTop,
                        new Color(99, 102, 241, 40), 0, padTop + chartH, new Color(99, 102, 241, 5));
                g2.setPaint(fillGrad);
                g2.fillRect(padLeft, padTop, chartW, chartH);
            }

            // 画折线 (实际数据) — 主色
            if (n >= 2) {
                g2.setColor(Theme.PRIMARY);
                g2.setStroke(new BasicStroke(2.5f));
                for (int i = 0; i < n - 1; i++) {
                    int x1 = padLeft + (int)(chartW * i / (n - 1));
                    int y1 = padTop + chartH - (int)(chartH * (values.get(i) - minVal) / range);
                    int x2 = padLeft + (int)(chartW * (i + 1) / (n - 1));
                    int y2 = padTop + chartH - (int)(chartH * (values.get(i + 1) - minVal) / range);
                    g2.drawLine(x1, y1, x2, y2);
                }
                // 画数据点 — 带白色边框
                for (int i = 0; i < n; i++) {
                    int x = padLeft + (int)(chartW * i / Math.max(n - 1, 1));
                    int y = padTop + chartH - (int)(chartH * (values.get(i) - minVal) / range);
                    g2.setColor(Color.WHITE);
                    g2.fillOval(x - 5, y - 5, 10, 10);
                    g2.setColor(Theme.PRIMARY);
                    g2.fillOval(x - 4, y - 4, 8, 8);
                }
            }

            // 画预测虚线 — 强调色
            if (predictedValues.size() > 0 && n >= 2) {
                g2.setColor(Theme.ACCENT);
                float[] dash = {6f, 4f};
                g2.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, dash, 0));

                int lastX = padLeft + chartW;
                int lastY = padTop + chartH - (int)(chartH * (values.get(n - 1) - minVal) / range);

                for (int i = 0; i < predictedValues.size(); i++) {
                    double pred = predictedValues.get(i);
                    int predX = lastX + (int)(chartW * 0.3 * (i + 1) / predictedValues.size());
                    int predY = padTop + chartH - (int)(chartH * (pred - minVal) / range);
                    predX = Math.min(predX, padLeft + chartW);
                    g2.drawLine(lastX, lastY, predX, predY);
                    g2.setColor(Theme.ACCENT);
                    g2.fillOval(predX - 4, predY - 4, 8, 8);
                    g2.setFont(new Font(Theme.FONT_NAME, Font.PLAIN, 10));
                    g2.setColor(Theme.TEXT_GRAY);
                    g2.drawString(df1.format(pred), predX - 15, predY - 10);
                    lastX = predX;
                    lastY = predY;
                    g2.setColor(Theme.ACCENT);
                    g2.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, dash, 0));
                }
            }

            // 边框 — 底部和左侧轴线
            g2.setColor(Theme.BORDER);
            g2.setStroke(new BasicStroke(1));
            g2.drawLine(padLeft, padTop + chartH, padLeft + chartW, padTop + chartH);
            g2.drawLine(padLeft, padTop, padLeft, padTop + chartH);
        }
    }

    // ================================================================
    //             第八部分: Tab3 分析评估面板
    // ================================================================

    /** 分析评估面板 — v2.0 现代报告布局 */
    static class AnalysisPanel extends JPanel {
        private JTextArea taResult;

        AnalysisPanel() {
            setLayout(new BorderLayout(10, 10));
            setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            setBackground(Theme.BG);

            // === 按钮卡片 ===
            JPanel btnCard = Theme.createCardPanel("分析评估", Theme.PRIMARY);
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            btnPanel.setOpaque(false);
            JButton btnRefresh = new RoundButton("🔄 刷新分析结果", Theme.PRIMARY, Theme.PRIMARY_D);
            btnRefresh.addActionListener(e -> refresh());
            btnPanel.add(btnRefresh);
            JLabel lblHint = new JLabel("综合 BMI、体脂率、BMR、TDEE 等指标生成评估报告");
            lblHint.setFont(Theme.FONT_SMALL);
            lblHint.setForeground(Theme.TEXT_GRAY);
            btnPanel.add(lblHint);
            btnCard.add(btnPanel, BorderLayout.CENTER);
            add(btnCard, BorderLayout.NORTH);

            // === 报告卡片 ===
            JPanel reportCard = Theme.createCardPanel("健康分析评估报告", Theme.PRIMARY);
            taResult = new JTextArea();
            taResult.setFont(new Font("Consolas", Font.PLAIN, 13));
            taResult.setEditable(false);
            taResult.setForeground(Theme.TEXT_DARK);
            taResult.setBackground(Theme.CARD_BG);
            taResult.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            JScrollPane scroll = new JScrollPane(taResult);
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            reportCard.add(scroll, BorderLayout.CENTER);
            add(reportCard, BorderLayout.CENTER);

            refresh();
        }

        void refresh() {
            Map<String, Object> latest = DBUtil.getLatestHealthRecord();
            if (latest == null) {
                taResult.setText("暂无健康记录, 请先在「数据录入」页面录入数据");
                return;
            }

            double weight = (double) latest.get("weight");
            double bodyFat = (double) latest.get("body_fat");
            double water = (double) latest.get("water_rate");
            double muscle = (double) latest.get("muscle_rate");
            int visceral = (int) latest.get("visceral_fat");
            double boneMuscle = (double) latest.get("bone_muscle");
            double bmi = (double) latest.get("bmi");
            double bmr = (double) latest.get("bmr");
            double tdee = (double) latest.get("tdee");
            double waist = (double) latest.get("waist");
            int bodyAge = (int) latest.get("body_age");
            String bodyType = (String) latest.get("body_type");

            // 计算三公式 BMR
            double bmrH = HealthCalculator.calcBMR_Harris(weight, currentHeight, currentAge, currentGender);
            double bmrM = HealthCalculator.calcBMR_Mifflin(weight, currentHeight, currentAge, currentGender);
            double bmrC = HealthCalculator.calcBMR_China(weight, currentAge, currentGender);

            // 理想体重
            double idealWeight = HealthCalculator.calcIdealWeight(currentHeight);

            // 健康评分
            int score = HealthCalculator.calcHealthScore(bmi, bodyFat, visceral, muscle, water, currentGender);

            StringBuilder sb = new StringBuilder();
            sb.append("═══════════════════════════════════════════\n");
            sb.append("              健康分析评估报告\n");
            sb.append("═══════════════════════════════════════════\n\n");

            sb.append("【BMI 体质指数】\n");
            sb.append("  BMI = ").append(df1.format(bmi)).append(" (").append(HealthCalculator.classifyBMI(bmi)).append(")\n");
            sb.append("  中国标准: <18.5偏瘦 | 18.5-23.9正常 | 24-27.9超重 | >=28肥胖\n\n");

            sb.append("【基础代谢率 BMR (三种公式对比)】\n");
            sb.append("  Harris-Benedict : ").append(df0.format(bmrH)).append(" kcal\n");
            sb.append("  Mifflin-St Jeor : ").append(df0.format(bmrM)).append(" kcal\n");
            sb.append("  中国营养学会    : ").append(df0.format(bmrC)).append(" kcal\n");
            sb.append("  ────────────────────────────────\n");
            sb.append("  平均值          : ").append(df0.format(bmr)).append(" kcal\n\n");

            sb.append("【每日总能量消耗 TDEE】\n");
            sb.append("  活动等级: ").append(currentActivityLevel);
            sb.append(" (系数: ").append(df2.format(HealthCalculator.getActivityFactor(currentActivityLevel))).append(")\n");
            sb.append("  TDEE = BMR × 活动系数 = ").append(df0.format(tdee)).append(" kcal\n\n");

            sb.append("【理想体重】\n");
            sb.append("  理想体重 = 身高² × 22 = ").append(df1.format(idealWeight)).append(" kg\n");
            sb.append("  正常范围: ").append(df1.format(currentHeight/100*currentHeight/100*18.5)).append(" - ");
            sb.append(df1.format(currentHeight/100*currentHeight/100*23.9)).append(" kg\n\n");

            sb.append("【体脂率】\n");
            sb.append("  体脂率: ").append(df1.format(bodyFat)).append("%\n");
            String fatLevel = currentGender.equals("男")
                    ? (bodyFat < 12 ? "偏低" : bodyFat <= 25 ? "正常" : bodyFat <= 30 ? "偏高" : "严重偏高")
                    : (bodyFat < 20 ? "偏低" : bodyFat <= 32 ? "正常" : bodyFat <= 38 ? "偏高" : "严重偏高");
            sb.append("  评级: ").append(fatLevel).append("\n\n");

            sb.append("【内脏脂肪等级】\n");
            sb.append("  等级: ").append(visceral).append(" 级 (").append(HealthCalculator.assessVisceralFat(visceral)).append(")\n");
            sb.append("  标准: 1-4正常 | 5-8偏高 | 9-10过高\n\n");

            sb.append("【骨骼肌肉量】\n");
            sb.append("  骨骼肌肉量: ").append(df1.format(boneMuscle)).append(" kg (");
            sb.append(HealthCalculator.assessMuscle(boneMuscle, weight, currentGender)).append(")\n\n");

            sb.append("【身体年龄】\n");
            sb.append("  身体年龄: ").append(bodyAge).append(" 岁 (实际年龄: ").append(currentAge).append(" 岁)\n");
            if (bodyAge < currentAge) sb.append("  身体比实际年龄年轻 ").append(currentAge - bodyAge).append(" 岁!\n\n");
            else if (bodyAge > currentAge) sb.append("  身体比实际年龄大 ").append(bodyAge - currentAge).append(" 岁, 需关注\n\n");
            else sb.append("  身体年龄与实际年龄一致\n\n");

            sb.append("【体质分类 (BMI + 体脂率 交叉矩阵)】\n");
            sb.append("  分类: ").append(bodyType).append("\n");
            sb.append("  分类说明:\n");
            sb.append("    消瘦型    — 体重不足, 需增加营养摄入\n");
            sb.append("    标准型    — 身体成分比例良好\n");
            sb.append("    肌肉型    — BMI偏高但体脂低, 肌肉发达\n");
            sb.append("    超重型    — 体重超标, 需控制\n");
            sb.append("    肥胖型    — 体脂率过高, 需减脂\n");
            sb.append("    隐性肥胖型 — 体重正常但体脂高, 建议力量训练\n\n");

            sb.append("【身体形态评估】\n");
            double whtr = waist / currentHeight;
            sb.append("  腰围身高比 WHtR = ").append(df2.format(whtr));
            sb.append(" (").append(HealthCalculator.assessWHtR(waist, currentHeight)).append(")\n");
            sb.append("  体型分类: ").append(HealthCalculator.classifyBodyShape(waist, currentGender)).append("\n\n");

            sb.append("【健康评分】\n");
            sb.append("  总分: ").append(score).append("/100 (").append(HealthCalculator.scoreLevel(score)).append(")\n");
            sb.append("  评分维度: BMI(30分) + 体脂率(25分) + 内脏脂肪(20分) + 肌肉量(15分) + 水分率(10分)\n");
            sb.append("  等级: 90-100优秀 | 75-89良好 | 60-74及格 | <60需改善\n\n");

            sb.append("═══════════════════════════════════════════\n");

            taResult.setText(sb.toString());
        }
    }

    // ================================================================
    //             第九部分: Tab4 预测分析面板 (新增)
    // ================================================================

    /** 预测分析面板 — 趋势预测 + 目标预测 + 风险评估 */
    static class PredictionPanel extends JPanel {
        private JTextArea taResult;

        PredictionPanel() {
            setLayout(new BorderLayout(8, 8));
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            setBackground(Theme.BG);

            // === 按钮卡片 ===
            JPanel btnCard = Theme.createCardPanel("预测分析", Theme.PRIMARY);
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            btnPanel.setOpaque(false);
            JButton btnRefresh = new RoundButton("🔄 刷新预测结果", Theme.PRIMARY, Theme.PRIMARY_D);
            btnRefresh.addActionListener(e -> refresh());
            btnPanel.add(btnRefresh);
            JLabel lblHint = new JLabel("基于历史数据进行线性回归趋势预测");
            lblHint.setFont(Theme.FONT_SMALL);
            lblHint.setForeground(Theme.TEXT_GRAY);
            btnPanel.add(lblHint);
            btnCard.add(btnPanel, BorderLayout.CENTER);
            add(btnCard, BorderLayout.NORTH);

            // === 预测报告卡片 ===
            JPanel reportCard = Theme.createCardPanel("预测分析报告", Theme.PRIMARY);
            reportCard.setPreferredSize(new Dimension(700, 450));
            reportCard.setMinimumSize(new Dimension(400, 250));
            taResult = new JTextArea();
            taResult.setFont(Theme.FONT_BODY);
            taResult.setEditable(false);
            taResult.setBackground(Theme.CARD_BG);
            taResult.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            JScrollPane scroll = new JScrollPane(taResult);
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            reportCard.add(scroll, BorderLayout.CENTER);
            add(reportCard, BorderLayout.CENTER);

            refresh();
        }

        void refresh() {
            List<Map<String, Object>> records = DBUtil.getHealthRecords(60);
            if (records.size() < 3) {
                taResult.setText("数据不足, 至少需要 3 条健康记录才能进行预测分析\n当前记录数: " + records.size());
                return;
            }

            // 反转为正序
            List<Date> dates = new ArrayList<>();
            List<Double> weights = new ArrayList<>();
            List<Double> bmis = new ArrayList<>();
            List<Double> bodyFats = new ArrayList<>();
            for (int i = records.size() - 1; i >= 0; i--) {
                Map<String, Object> r = records.get(i);
                dates.add((Date) r.get("record_date"));
                weights.add((double) r.get("weight"));
                bmis.add((double) r.get("bmi"));
                bodyFats.add((double) r.get("body_fat"));
            }

            Map<String, Object> latest = records.get(0);
            double currentWeight = (double) latest.get("weight");
            double currentBMI = (double) latest.get("bmi");
            double tdee = (double) latest.get("tdee");

            // 预测值
            double pred7 = HealthCalculator.predictTrend(dates, weights, 7);
            double pred14 = HealthCalculator.predictTrend(dates, weights, 14);
            double pred30 = HealthCalculator.predictTrend(dates, weights, 30);
            double predBMI30 = HealthCalculator.calcBMI(pred30, currentHeight);

            String trend = HealthCalculator.trendDirection(dates, weights);

            // 热量差
            int exerciseCal = DBUtil.getTodayExerciseCalories();
            int[] diet = DBUtil.getTodayDietSummary();
            int intakeCal = diet[0];
            double dailyDeficit = tdee + exerciseCal - intakeCal;

            // 目标预测
            Map<String, Object> goal = DBUtil.getGoal();

            // 风险评估
            String risk = HealthCalculator.assessRisk(predBMI30);

            StringBuilder sb = new StringBuilder();
            sb.append("═══════════════════════════════════════════\n");
            sb.append("              预测分析报告\n");
            sb.append("═══════════════════════════════════════════\n\n");

            sb.append("【趋势预测 (线性回归)】\n");
            sb.append("  当前体重: ").append(df1.format(currentWeight)).append(" kg (BMI: ").append(df1.format(currentBMI)).append(")\n");
            sb.append("  趋势方向: ").append(trend).append("\n\n");
            sb.append("  预测结果:\n");
            sb.append("    7天后  → 体重 ").append(df1.format(pred7)).append(" kg (BMI: ").append(df1.format(HealthCalculator.calcBMI(pred7, currentHeight))).append(")\n");
            sb.append("   14天后  → 体重 ").append(df1.format(pred14)).append(" kg (BMI: ").append(df1.format(HealthCalculator.calcBMI(pred14, currentHeight))).append(")\n");
            sb.append("   30天后  → 体重 ").append(df1.format(pred30)).append(" kg (BMI: ").append(df1.format(predBMI30)).append(")\n\n");
            sb.append("  (预测基于最近 ").append(dates.size()).append(" 条记录, 实际结果受饮食和运动影响)\n\n");

            sb.append("【热量差分析】\n");
            sb.append("  TDEE (每日消耗): ").append(df0.format(tdee)).append(" kcal\n");
            sb.append("  运动消耗: ").append(exerciseCal).append(" kcal\n");
            sb.append("  饮食摄入: ").append(intakeCal).append(" kcal\n");
            sb.append("  每日热量差: ").append(dailyDeficit >= 0 ? "+" : "").append(df0.format(dailyDeficit)).append(" kcal\n");
            if (dailyDeficit > 0) {
                sb.append("  → 处于热量缺口状态, 有利于减脂/减重\n");
                double kgPerWeek = dailyDeficit * 7 / 7700;
                sb.append("  → 预计每周减重约 ").append(df2.format(kgPerWeek)).append(" kg\n");
            } else if (dailyDeficit < 0) {
                sb.append("  → 处于热量盈余状态, 有利于增肌但可能导致脂肪增加\n");
                double kgPerWeek = Math.abs(dailyDeficit) * 7 / 7700;
                sb.append("  → 预计每周增重约 ").append(df2.format(kgPerWeek)).append(" kg\n");
            } else {
                sb.append("  → 热量收支平衡, 体重将维持稳定\n");
            }
            sb.append("\n");

            sb.append("【目标达成预测】\n");
            if (goal != null) {
                String goalType = (String) goal.get("goal_type");
                double targetValue = (double) goal.get("target_value");
                sb.append("  目标类型: ").append(goalType).append("\n");
                sb.append("  目标体重: ").append(df1.format(targetValue)).append(" kg\n");
                sb.append("  当前体重: ").append(df1.format(currentWeight)).append(" kg\n");

                int days = HealthCalculator.predictGoalDays(currentWeight, targetValue, dailyDeficit, goalType);
                if (days == -2) {
                    sb.append("  ⚠️ 当前进度过慢 (热量差<100kcal), 建议调整饮食或运动计划\n");
                } else if (days == -1) {
                    sb.append("  ⚠️ 当前饮食超过消耗, 无法达成目标, 需减少摄入或增加运动\n");
                } else if (days == 0) {
                    sb.append("  ✅ 已达成目标!\n");
                } else {
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.DAY_OF_MONTH, days);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    sb.append("  预测达成日期: ").append(sdf.format(cal.getTime()));
                    sb.append(" (还需 ").append(days).append(" 天)\n");
                }
            } else {
                sb.append("  尚未设置目标, 请到「目标计划」页面设置\n");
            }
            sb.append("\n");

            sb.append("【健康风险评估】\n");
            sb.append("  预测30天后BMI: ").append(df1.format(predBMI30)).append("\n");
            String riskLevel = predBMI30 >= 28.0 ? "高风险" : (predBMI30 >= 24.0 || predBMI30 < 18.5 ? "中风险" : "低风险");
            sb.append("  风险等级: ").append(riskLevel).append("\n");
            sb.append("  ").append(risk).append("\n\n");

            // 体脂率趋势
            String fatTrend = HealthCalculator.trendDirection(dates, bodyFats);
            sb.append("【体脂率趋势】\n");
            sb.append("  当前体脂率: ").append(df1.format(bodyFats.get(bodyFats.size() - 1))).append("%\n");
            sb.append("  趋势方向: ").append(fatTrend).append("\n");
            if (fatTrend.equals("上升") && bodyFats.size() >= 30) {
                sb.append("  ⚠️ 体脂率持续上升, 建议调整饮食结构\n");
            }
            sb.append("\n");

            sb.append("═══════════════════════════════════════════\n");

            taResult.setText(sb.toString());
        }
    }

    // ================================================================
    //             第十部分: Tab5 目标计划面板
    // ================================================================

    /** 目标计划面板 */
    static class GoalPlanPanel extends JPanel {
        private JComboBox<String> cbGoalType = new JComboBox<>(
                new String[]{"减脂", "减重", "增肌", "保持健康"});
        private JTextField tfTargetWeight = new JTextField("60.0");
        private JTextArea taPlan;
        private JProgressBar[] progressBars = new JProgressBar[4];

        GoalPlanPanel() {
            setLayout(new BorderLayout(8, 8));
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            setBackground(Theme.BG);

            // === 控制卡片 ===
            JPanel ctrlCard = Theme.createCardPanel("目标设定", Theme.PRIMARY);
            JPanel ctrlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
            ctrlPanel.setOpaque(false);
            JLabel lblGoal = new JLabel("目标类型:"); lblGoal.setFont(Theme.FONT_BODY);
            ctrlPanel.add(lblGoal);
            Theme.styleComboBox(cbGoalType);
            ctrlPanel.add(cbGoalType);
            JLabel lblWeight = new JLabel("目标体重(kg):"); lblWeight.setFont(Theme.FONT_BODY);
            ctrlPanel.add(lblWeight);
            Theme.styleTextField(tfTargetWeight);
            tfTargetWeight.setColumns(6);
            ctrlPanel.add(tfTargetWeight);
            JButton btnRecommend = new RoundButton("推荐目标", Theme.ACCENT, new Color(13, 148, 136));
            btnRecommend.addActionListener(e -> recommendTarget());
            ctrlPanel.add(btnRecommend);
            JButton btnSetGoal = new RoundButton("设置目标", Theme.PRIMARY, Theme.PRIMARY_D);
            btnSetGoal.addActionListener(e -> setGoal());
            ctrlPanel.add(btnSetGoal);
            JButton btnRefresh = new RoundButton("刷新", Theme.PRIMARY, Theme.PRIMARY_D);
            btnRefresh.addActionListener(e -> refresh());
            ctrlPanel.add(btnRefresh);
            ctrlCard.add(ctrlPanel, BorderLayout.CENTER);
            add(ctrlCard, BorderLayout.NORTH);

            // === 进度条卡片 ===
            JPanel progressCard = Theme.createCardPanel("分阶段进度", Theme.ACCENT);
            JPanel progressPanel = new JPanel(new GridLayout(4, 1, 8, 8));
            progressPanel.setOpaque(false);
            for (int i = 0; i < 4; i++) {
                JPanel p = new JPanel(new BorderLayout(8, 2));
                p.setOpaque(false);
                JLabel lbl = new JLabel("第" + (i + 1) + "周:");
                lbl.setFont(Theme.FONT_BODY);
                p.add(lbl, BorderLayout.WEST);
                progressBars[i] = new JProgressBar(0, 100);
                progressBars[i].setStringPainted(true);
                progressBars[i].setFont(Theme.FONT_BODY);
                progressBars[i].setForeground(Theme.PRIMARY);
                progressBars[i].setBackground(Theme.BG);
                p.add(progressBars[i], BorderLayout.CENTER);
                progressPanel.add(p);
            }
            progressCard.add(progressPanel, BorderLayout.CENTER);

            // === 计划文本卡片 ===
            JPanel planCard = Theme.createCardPanel("目标计划详情", Theme.PRIMARY);
            taPlan = new JTextArea();
            taPlan.setFont(Theme.FONT_BODY);
            taPlan.setEditable(false);
            taPlan.setBackground(Theme.CARD_BG);
            taPlan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            JScrollPane scroll = new JScrollPane(taPlan);
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            planCard.add(scroll, BorderLayout.CENTER);

            // 中间区域
            JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
            centerPanel.setOpaque(false);
            centerPanel.add(progressCard, BorderLayout.NORTH);
            centerPanel.add(planCard, BorderLayout.CENTER);
            add(centerPanel, BorderLayout.CENTER);

            refresh();
        }

        /** 推荐目标体重 */
        private void recommendTarget() {
            double ideal = HealthCalculator.calcIdealWeight(currentHeight);
            tfTargetWeight.setText(df1.format(ideal));
            JOptionPane.showMessageDialog(this, "推荐目标体重: " + df1.format(ideal) + " kg (身高²×22)");
        }

        /** 设置目标 */
        private void setGoal() {
            try {
                String goalType = (String) cbGoalType.getSelectedItem();
                double target = Double.parseDouble(tfTargetWeight.getText().trim());
                if (target < 20 || target > 300) {
                    JOptionPane.showMessageDialog(this, "目标体重范围 20-300kg", "提示", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (DBUtil.saveGoal(goalType, target)) {
                    JOptionPane.showMessageDialog(this, "目标设置成功!");
                    refresh();
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "请输入有效数字", "错误", JOptionPane.ERROR_MESSAGE);
            }
        }

        /** 刷新计划展示 */
        void refresh() {
            Map<String, Object> latest = DBUtil.getLatestHealthRecord();
            Map<String, Object> goal = DBUtil.getGoal();

            if (latest == null) {
                taPlan.setText("暂无健康记录, 请先录入数据");
                return;
            }

            double currentWeight = (double) latest.get("weight");
            StringBuilder sb = new StringBuilder();

            if (goal == null) {
                sb.append("尚未设置目标, 请选择目标类型和目标体重后点击「设置目标」\n\n");
                sb.append("推荐目标体重: ").append(df1.format(HealthCalculator.calcIdealWeight(currentHeight))).append(" kg\n");
                taPlan.setText(sb.toString());
                for (JProgressBar pb : progressBars) pb.setValue(0);
                return;
            }

            String goalType = (String) goal.get("goal_type");
            double targetWeight = (double) goal.get("target_value");
            int currentStage = goal.get("current_stage") != null ? (int) goal.get("current_stage") : 1;

            sb.append("═══════════════════════════════════════════\n");
            sb.append("              目标计划\n");
            sb.append("═══════════════════════════════════════════\n\n");
            sb.append("目标类型: ").append(goalType).append("\n");
            sb.append("当前体重: ").append(df1.format(currentWeight)).append(" kg\n");
            sb.append("目标体重: ").append(df1.format(targetWeight)).append(" kg\n");
            sb.append("需变化: ").append(df1.format(Math.abs(currentWeight - targetWeight))).append(" kg\n\n");

            // 分阶段计划
            sb.append("【分阶段计划 (4周递进)】\n");
            double totalChange = targetWeight - currentWeight;
            double[] stageTargets = new double[4];
            // 起步阶段稍慢, 后面加速
            double[] ratios = {0.2, 0.25, 0.275, 0.275};
            double cumRatio = 0;
            for (int i = 0; i < 4; i++) {
                cumRatio += ratios[i];
                stageTargets[i] = currentWeight + totalChange * cumRatio;
                sb.append("  第").append(i + 1).append("周: ").append(df1.format(i == 0 ? currentWeight : stageTargets[i - 1]));
                sb.append(" → ").append(df1.format(stageTargets[i])).append(" kg\n");

                // 计算进度
                double stageStart = (i == 0) ? currentWeight : stageTargets[i - 1];
                double stageEnd = stageTargets[i];
                double progress;
                if (stageEnd > stageStart) {
                    progress = (currentWeight - stageStart) / (stageEnd - stageStart) * 100;
                } else {
                    progress = (stageStart - currentWeight) / (stageStart - stageEnd) * 100;
                }
                progress = Math.max(0, Math.min(100, progress));
                if (i + 1 < currentStage) progress = 100;
                if (i + 1 > currentStage) progress = 0;
                progressBars[i].setValue((int) progress);
            }
            sb.append("\n");

            // 运动建议
            sb.append("【运动建议】\n");
            switch (goalType) {
                case "减脂":
                    sb.append("  有氧运动: 跑步/游泳/跳绳/骑行\n");
                    sb.append("  频率: 每周 4-5 次, 每次 40-60 分钟\n");
                    sb.append("  力量训练: 每周 2 次, 每次 30 分钟\n");
                    sb.append("  建议: 有氧为主燃烧脂肪, 力量训练维持肌肉量\n");
                    break;
                case "减重":
                    sb.append("  有氧运动: 快走/游泳/骑行\n");
                    sb.append("  频率: 每周 5 次, 每次 45-60 分钟\n");
                    sb.append("  力量训练: 每周 2 次\n");
                    sb.append("  建议: 持续中低强度有氧, 配合饮食控制\n");
                    break;
                case "增肌":
                    sb.append("  力量训练: 深蹲/卧推/硬拉/划船\n");
                    sb.append("  频率: 每周 4 次, 每次 60 分钟\n");
                    sb.append("  有氧运动: 每周 2 次, 每次 20 分钟\n");
                    sb.append("  建议: 大重量少次数, 蛋白质摄入 1.5-2g/kg体重\n");
                    break;
                case "保持健康":
                    sb.append("  有氧 + 力量均衡\n");
                    sb.append("  频率: 每周 3-4 次, 每次 30-45 分钟\n");
                    sb.append("  建议: 保持规律运动, 注意拉伸和恢复\n");
                    break;
            }
            sb.append("\n");

            // 预测达成日期
            sb.append("【预测达成日期】\n");
            double tdee = (double) latest.get("tdee");
            int exerciseCal = DBUtil.getTodayExerciseCalories();
            int[] diet = DBUtil.getTodayDietSummary();
            double dailyDeficit = tdee + exerciseCal - diet[0];
            int days = HealthCalculator.predictGoalDays(currentWeight, targetWeight, dailyDeficit, goalType);
            if (days == -2) {
                sb.append("  ⚠️ 当前进度过慢, 建议调整计划\n");
            } else if (days == -1) {
                sb.append("  ⚠️ 当前饮食超过消耗, 无法达成目标\n");
            } else if (days == 0) {
                sb.append("  ✅ 已达成目标!\n");
            } else {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_MONTH, days);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                sb.append("  预测达成日期: ").append(sdf.format(cal.getTime())).append(" (约 ").append(days).append(" 天)\n");
                sb.append("  每日热量差: ").append(df0.format(dailyDeficit)).append(" kcal\n");
            }
            sb.append("\n═══════════════════════════════════════════\n");

            taPlan.setText(sb.toString());
        }
    }

    // ================================================================
    //             第十一部分: Tab6 饮食管理面板
    // ================================================================

    /** 饮食管理面板 */
    static class DietPanel extends JPanel {
        private MainFrame mainFrame;
        private JComboBox<String> cbMealType = new JComboBox<>(new String[]{"早餐", "午餐", "晚餐", "加餐"});
        private JComboBox<String> cbFood = new JComboBox<>();
        private JTextField tfGrams = new JTextField("100");
        private JTextField tfApiImagePath = new JTextField(20);
        private JTextField tfApiKey = new JTextField(20);
        private JTextArea taReport;
        private PieChartPanel pieChart;
        private JLabel lblSummary;

        // 食物数据缓存
        private Map<String, String[]> foodData = new LinkedHashMap<>();

        DietPanel(MainFrame frame) {
            this.mainFrame = frame;
            setLayout(new BorderLayout(8, 8));
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            setBackground(Theme.BG);

            // === 饮食记录录入卡片 ===
            JPanel inputCard = Theme.createCardPanel("饮食记录录入", Theme.PRIMARY);
            JPanel inputPanel = new JPanel(new GridBagLayout());
            inputPanel.setOpaque(false);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(4, 8, 4, 8);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.gridx = 0; gbc.gridy = 0; JLabel lbl = new JLabel("餐次:"); lbl.setFont(Theme.FONT_BODY); inputPanel.add(lbl, gbc);
            gbc.gridx = 1; Theme.styleComboBox(cbMealType); inputPanel.add(cbMealType, gbc);
            gbc.gridx = 2; lbl = new JLabel("食物:"); lbl.setFont(Theme.FONT_BODY); inputPanel.add(lbl, gbc);
            gbc.gridx = 3; gbc.gridwidth = 2; Theme.styleComboBox(cbFood); inputPanel.add(cbFood, gbc);
            gbc.gridwidth = 1;

            gbc.gridx = 0; gbc.gridy = 1; lbl = new JLabel("食用量(g):"); lbl.setFont(Theme.FONT_BODY); inputPanel.add(lbl, gbc);
            gbc.gridx = 1; Theme.styleTextField(tfGrams); inputPanel.add(tfGrams, gbc);
            JButton btnAdd = new RoundButton("记录饮食", Theme.PRIMARY, Theme.PRIMARY_D);
            btnAdd.addActionListener(e -> addDietRecord());
            gbc.gridx = 2; inputPanel.add(btnAdd, gbc);
            JButton btnExport = new RoundButton("导出CSV", Theme.ACCENT, new Color(13, 148, 136));
            btnExport.addActionListener(e -> exportCSV());
            gbc.gridx = 3; inputPanel.add(btnExport, gbc);

            // 拍照识别
            gbc.gridx = 0; gbc.gridy = 2; lbl = new JLabel("图片路径:"); lbl.setFont(Theme.FONT_BODY); inputPanel.add(lbl, gbc);
            gbc.gridx = 1; gbc.gridwidth = 2; Theme.styleTextField(tfApiImagePath); inputPanel.add(tfApiImagePath, gbc);
            gbc.gridwidth = 1;
            JButton btnRecognize = new RoundButton("拍照识别", Theme.PRIMARY, Theme.PRIMARY_D);
            btnRecognize.addActionListener(e -> recognizeFood());
            gbc.gridx = 3; inputPanel.add(btnRecognize, gbc);

            gbc.gridx = 0; gbc.gridy = 3; lbl = new JLabel("API Key:"); lbl.setFont(Theme.FONT_BODY); inputPanel.add(lbl, gbc);
            gbc.gridx = 1; gbc.gridwidth = 3; Theme.styleTextField(tfApiKey); inputPanel.add(tfApiKey, gbc);
            gbc.gridwidth = 1;

            inputCard.add(inputPanel, BorderLayout.CENTER);
            add(inputCard, BorderLayout.NORTH);

            // === 今日汇总卡片 ===
            JPanel summaryCard = Theme.createCardPanel("今日营养汇总", Theme.ACCENT);
            lblSummary = new JLabel("今日汇总: 暂无数据");
            lblSummary.setFont(Theme.FONT_HEADER);
            lblSummary.setForeground(Theme.TEXT_DARK);
            summaryCard.add(lblSummary, BorderLayout.NORTH);

            // 饼图
            pieChart = new PieChartPanel();
            pieChart.setPreferredSize(new Dimension(300, 220));
            pieChart.setOpaque(false);
            summaryCard.add(pieChart, BorderLayout.WEST);

            // 营养报告
            taReport = new JTextArea();
            taReport.setFont(Theme.FONT_BODY);
            taReport.setEditable(false);
            taReport.setBackground(Theme.CARD_BG);
            taReport.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            JScrollPane scroll = new JScrollPane(taReport);
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            summaryCard.add(scroll, BorderLayout.CENTER);

            add(summaryCard, BorderLayout.CENTER);

            loadFoods();
            refreshSummary();
        }

        /** 加载食物列表 */
        private void loadFoods() {
            cbFood.removeAllItems();
            foodData.clear();
            for (String[] food : DBUtil.getAllFoods()) {
                String name = food[0];
                foodData.put(name, food);
                cbFood.addItem(name);
            }
        }

        /** 添加饮食记录 */
        private void addDietRecord() {
            String mealType = (String) cbMealType.getSelectedItem();
            String foodName = (String) cbFood.getSelectedItem();
            if (foodName == null) {
                JOptionPane.showMessageDialog(this, "请选择食物", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                double grams = Double.parseDouble(tfGrams.getText().trim());
                if (grams <= 0 || grams > 5000) {
                    JOptionPane.showMessageDialog(this, "食用量范围 1-5000g", "提示", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                String[] food = foodData.get(foodName);
                double ratio = grams / 100.0;
                int calories = (int)(Integer.parseInt(food[1]) * ratio);
                double protein = Double.parseDouble(food[2]) * ratio;
                double carbs = Double.parseDouble(food[3]) * ratio;
                double fat = Double.parseDouble(food[4]) * ratio;

                if (DBUtil.saveDietRecord(mealType, foodName + "(" + df0.format(grams) + "g)",
                        calories, protein, carbs, fat)) {
                    JOptionPane.showMessageDialog(this, "饮食记录保存成功! 热量: " + calories + " kcal");
                    refreshSummary();
                    mainFrame.refreshData();
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "请输入有效数字", "错误", JOptionPane.ERROR_MESSAGE);
            }
        }

        /** 刷新今日汇总 */
        void refreshSummary() {
            int[] diet = DBUtil.getTodayDietSummary();
            int totalCal = diet[0];
            double protein = diet[1] / 100.0;
            double carbs = diet[2] / 100.0;
            double fat = diet[3] / 100.0;

            // 推荐摄入量
            Map<String, Object> latest = DBUtil.getLatestHealthRecord();
            double tdee = latest != null ? (double) latest.get("tdee") : 1800;
            double recProtein = latest != null ? (double) latest.get("weight") * 1.2 : 60;
            double recCarbs = tdee * 0.5 / 4;
            double recFat = tdee * 0.3 / 9;

            lblSummary.setText(String.format("今日汇总: 总热量 %d kcal / 推荐 %d kcal | 蛋白质 %.1fg | 碳水 %.1fg | 脂肪 %.1fg",
                    totalCal, (int) tdee, protein, carbs, fat));

            // 更新饼图
            pieChart.setData(protein * 4, carbs * 4, fat * 9);

            // 营养报告
            StringBuilder sb = new StringBuilder();
            sb.append("═══ 每日营养报告 ═══\n\n");
            sb.append("热量:\n");
            sb.append(String.format("  摄入 %d kcal / 推荐 %d kcal", totalCal, (int) tdee));
            if (totalCal > tdee * 1.1) sb.append("  [超标]");
            else if (totalCal < tdee * 0.7) sb.append("  [不足]");
            sb.append("\n\n");

            sb.append("蛋白质:\n");
            sb.append(String.format("  摄入 %.1fg / 推荐 %.1fg", protein, recProtein));
            if (protein < recProtein * 0.8) sb.append("  [不足]");
            sb.append("\n\n");

            sb.append("碳水:\n");
            sb.append(String.format("  摄入 %.1fg / 推荐 %.1fg", carbs, recCarbs));
            if (carbs > recCarbs * 1.3) sb.append("  [超标]");
            sb.append("\n\n");

            sb.append("脂肪:\n");
            sb.append(String.format("  摄入 %.1fg / 推荐 %.1fg", fat, recFat));
            if (fat > recFat * 1.3) sb.append("  [超标]");
            sb.append("\n\n");

            // 营养素占比
            double totalNutrientCal = protein * 4 + carbs * 4 + fat * 9;
            if (totalNutrientCal > 0) {
                sb.append("营养素占比:\n");
                sb.append(String.format("  蛋白质: %.1f%%\n", protein * 4 / totalNutrientCal * 100));
                sb.append(String.format("  碳水:   %.1f%%\n", carbs * 4 / totalNutrientCal * 100));
                sb.append(String.format("  脂肪:   %.1f%%\n", fat * 9 / totalNutrientCal * 100));
            }

            taReport.setText(sb.toString());
        }

        /** 食物拍照识别 (调用硅基流动 API) */
        private void recognizeFood() {
            String imagePath = tfApiImagePath.getText().trim();
            String apiKey = tfApiKey.getText().trim();
            if (imagePath.isEmpty() || apiKey.isEmpty()) {
                JOptionPane.showMessageDialog(this, "请输入图片路径和 API Key", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                // 读取图片并转 Base64
                File imgFile = new File(imagePath);
                if (!imgFile.exists()) {
                    JOptionPane.showMessageDialog(this, "图片文件不存在", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                byte[] imgBytes = java.nio.file.Files.readAllBytes(imgFile.toPath());
                String base64 = Base64.getEncoder().encodeToString(imgBytes);
                String dataUrl = "data:image/jpeg;base64," + base64;

                // 调用硅基流动视觉大模型 API
                String result = callSiliconFlowAPI(apiKey, dataUrl);
                taReport.setText("食物识别结果:\n" + result);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "识别失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }

        /** 调用硅基流动 API */
        private String callSiliconFlowAPI(String apiKey, String imageDataUrl) throws Exception {
            URL url = new URL("https://api.siliconflow.cn/v1/chat/completions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);

            String body = "{\"model\":\"Qwen/Qwen2-VL-72B-Instruct\",\"messages\":[{\"role\":\"user\",\"content\":["
                    + "{\"type\":\"text\",\"text\":\"请识别图片中的食物, 估算热量和营养成分(蛋白质/碳水/脂肪), 以JSON格式返回\"},"
                    + "{\"type\":\"image_url\",\"image_url\":{\"url\":\"" + imageDataUrl + "\"}}"
                    + "]}]}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes("UTF-8"));
            }

            int code = conn.getResponseCode();
            InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            if (code >= 400) {
                return "API 调用失败 (HTTP " + code + "): " + response.toString();
            }

            // 简单提取 content 字段
            String resp = response.toString();
            int idx = resp.indexOf("\"content\":\"");
            if (idx >= 0) {
                int end = resp.indexOf("\"", idx + 11);
                if (end > idx) {
                    return resp.substring(idx + 11, end).replace("\\n", "\n");
                }
            }
            return resp;
        }

        /** 导出 CSV */
        private void exportCSV() {
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File("health_data_export.csv"));
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

            List<Map<String, Object>> records = DBUtil.getHealthRecords(999);
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream(chooser.getSelectedFile()), "UTF-8"))) {
                // BOM for Excel
                pw.write('\ufeff');
                pw.println("日期,体重,BMI,体脂率,水分率,肌肉率,内脏脂肪,腰围,身体年龄,体质分类");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                for (Map<String, Object> r : records) {
                    pw.printf("%s,%.1f,%.1f,%.1f,%.1f,%.1f,%d,%.1f,%d,%s%n",
                            sdf.format((Date) r.get("record_date")),
                            (double) r.get("weight"), (double) r.get("bmi"),
                            (double) r.get("body_fat"), (double) r.get("water_rate"),
                            (double) r.get("muscle_rate"), (int) r.get("visceral_fat"),
                            (double) r.get("waist"), (int) r.get("body_age"),
                            r.get("body_type"));
                }
                JOptionPane.showMessageDialog(this, "导出成功! 共 " + records.size() + " 条记录\n文件: " + chooser.getSelectedFile().getAbsolutePath());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "导出失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /** 营养素饼图面板 — 用 Graphics 手绘 */
    static class PieChartPanel extends JPanel {
        private double proteinCal = 0, carbsCal = 0, fatCal = 0;

        void setData(double proteinCal, double carbsCal, double fatCal) {
            this.proteinCal = proteinCal;
            this.carbsCal = carbsCal;
            this.fatCal = fatCal;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int size = Math.min(w - 100, h - 20);
            int cx = 20, cy = (h - size) / 2;

            double total = proteinCal + carbsCal + fatCal;
            if (total <= 0) {
                g2.setColor(Theme.TEXT_LIGHT);
                g2.setFont(new Font(Theme.FONT_NAME, Font.PLAIN, 12));
                g2.drawString("暂无饮食数据", cx + size/2 - 40, h/2);
                return;
            }

            // 画饼图
            int startAngle = 0;
            // 蛋白质 - 主题色
            int proteinAngle = (int)(proteinCal / total * 360);
            g2.setColor(Theme.PRIMARY);
            g2.fillArc(cx, cy, size, size, startAngle, proteinAngle);
            startAngle += proteinAngle;

            // 碳水 - 警告色
            int carbsAngle = (int)(carbsCal / total * 360);
            g2.setColor(Theme.WARNING);
            g2.fillArc(cx, cy, size, size, startAngle, carbsAngle);
            startAngle += carbsAngle;

            // 脂肪 - 危险色
            int fatAngle = 360 - startAngle;
            g2.setColor(Theme.DANGER);
            g2.fillArc(cx, cy, size, size, startAngle, fatAngle);

            // 图例
            int legendX = cx + size + 15;
            int legendY = cy + 10;
            int legendH = 20;
            g2.setFont(new Font(Theme.FONT_NAME, Font.PLAIN, 12));

            g2.setColor(Theme.PRIMARY);
            g2.fillRect(legendX, legendY, 12, 12);
            g2.setColor(Color.BLACK);
            g2.drawString(String.format("蛋白质 %.1f%%", proteinCal / total * 100), legendX + 18, legendY + 11);

            g2.setColor(Theme.WARNING);
            g2.fillRect(legendX, legendY + legendH, 12, 12);
            g2.setColor(Color.BLACK);
            g2.drawString(String.format("碳水 %.1f%%", carbsCal / total * 100), legendX + 18, legendY + legendH + 11);

            g2.setColor(Theme.DANGER);
            g2.fillRect(legendX, legendY + legendH * 2, 12, 12);
            g2.setColor(Color.BLACK);
            g2.drawString(String.format("脂肪 %.1f%%", fatCal / total * 100), legendX + 18, legendY + legendH * 2 + 11);
        }
    }

    // ================================================================
    //             第十二部分: Tab7 成就徽章面板
    // ================================================================

    /** 成就徽章面板 */
    static class AchievementPanel extends JPanel {
        private JTextArea taBadges;
        private JList<String> reportList;
        private DefaultListModel<String> reportModel;
        private JTextArea taReportContent;

        // 所有徽章定义
        private static final String[][] ALL_BADGES = {
            {"毅力之星", "连续打卡 7 天"},
            {"坚持达人", "连续打卡 30 天"},
            {"目标达成者", "达成阶段目标"},
            {"美食家", "记录饮食 30 天"},
            {"运动健将", "记录运动 20 次"},
            {"健康标兵", "健康评分达到 90"},
            {"蜕变之星", "体脂率降至正常"}
        };

        AchievementPanel() {
            setLayout(new BorderLayout(8, 8));
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            setBackground(Theme.BG);

            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            splitPane.setOpaque(false);
            splitPane.setDividerSize(8);

            // 左侧: 徽章列表卡片
            JPanel badgeCard = Theme.createCardPanel("成就徽章", Theme.PRIMARY);
            taBadges = new JTextArea();
            taBadges.setFont(Theme.FONT_BODY);
            taBadges.setEditable(false);
            taBadges.setBackground(Theme.CARD_BG);
            taBadges.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            JScrollPane badgeScroll = new JScrollPane(taBadges);
            badgeScroll.setOpaque(false);
            badgeScroll.getViewport().setOpaque(false);
            badgeScroll.setBorder(BorderFactory.createEmptyBorder());
            badgeCard.add(badgeScroll, BorderLayout.CENTER);
            JButton btnRefresh = new RoundButton("刷新徽章", Theme.PRIMARY, Theme.PRIMARY_D);
            btnRefresh.addActionListener(e -> refresh());
            badgeCard.add(btnRefresh, BorderLayout.SOUTH);
            splitPane.setLeftComponent(badgeCard);

            // 右侧: AI 报告卡片
            JPanel reportCard = Theme.createCardPanel("历史 AI 报告", Theme.ACCENT);
            reportModel = new DefaultListModel<>();
            reportList = new JList<>(reportModel);
            reportList.setFont(Theme.FONT_BODY);
            reportList.setSelectionBackground(Theme.PRIMARY_L);
            reportList.setSelectionForeground(Color.WHITE);
            reportList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            reportList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting() && reportList.getSelectedIndex() >= 0) {
                    String selected = reportList.getSelectedValue();
                    // 提取 ID
                    try {
                        int id = Integer.parseInt(selected.split(" ")[0]);
                        taReportContent.setText(DBUtil.getAIReportContent(id));
                    } catch (Exception ex) {
                        // ignore
                    }
                }
            });
            JScrollPane listScroll = new JScrollPane(reportList);
            listScroll.setOpaque(false);
            listScroll.getViewport().setOpaque(false);
            listScroll.setBorder(BorderFactory.createEmptyBorder());
            listScroll.setPreferredSize(new Dimension(260, 150));
            reportCard.add(listScroll, BorderLayout.NORTH);

            taReportContent = new JTextArea();
            taReportContent.setFont(Theme.FONT_BODY);
            taReportContent.setEditable(false);
            taReportContent.setBackground(Theme.CARD_BG);
            taReportContent.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            JScrollPane contentScroll = new JScrollPane(taReportContent);
            contentScroll.setOpaque(false);
            contentScroll.getViewport().setOpaque(false);
            contentScroll.setBorder(BorderFactory.createEmptyBorder());
            reportCard.add(contentScroll, BorderLayout.CENTER);

            // 生成报告按钮
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));
            btnPanel.setOpaque(false);
            JButton btnGenReport = new RoundButton("生成周报", Theme.PRIMARY, Theme.PRIMARY_D);
            btnGenReport.addActionListener(e -> generateReport("周报"));
            JButton btnGenMonthReport = new RoundButton("生成月报", Theme.PRIMARY, Theme.PRIMARY_D);
            btnGenMonthReport.addActionListener(e -> generateReport("月报"));
            btnPanel.add(btnGenReport);
            btnPanel.add(btnGenMonthReport);
            reportCard.add(btnPanel, BorderLayout.SOUTH);

            splitPane.setRightComponent(reportCard);
            splitPane.setDividerLocation(350);
            add(splitPane, BorderLayout.CENTER);

            refresh();
        }

        void refresh() {
            // 显示徽章
            List<String[]> earned = DBUtil.getAchievements();
            Set<String> earnedNames = new HashSet<>();
            for (String[] badge : earned) {
                earnedNames.add(badge[0]);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("═══ 成就徽章 ═══\n\n");
            for (String[] badge : ALL_BADGES) {
                boolean has = earnedNames.contains(badge[0]);
                sb.append(has ? "★ " : "☆ ").append(badge[0]);
                sb.append(" — ").append(badge[1]);
                if (has) {
                    // 找到获得日期
                    for (String[] e : earned) {
                        if (e[0].equals(badge[0])) {
                            sb.append(" (").append(e[1]).append("获得)");
                            break;
                        }
                    }
                }
                sb.append("\n");
            }
            sb.append("\n已获得: ").append(earnedNames.size()).append("/").append(ALL_BADGES.length).append(" 个徽章\n");
            taBadges.setText(sb.toString());

            // 刷新报告列表
            reportModel.clear();
            for (String[] report : DBUtil.getAIReports()) {
                reportModel.addElement(report[0] + " | " + report[1] + " | " + report[2]);
            }
        }

        /** 生成报告 (调用 AI 或本地生成) */
        private void generateReport(String reportType) {
            String apiKey = JOptionPane.showInputDialog(this, "请输入硅基流动 API Key (留空则本地生成):");
            if (apiKey == null) return;

            List<Map<String, Object>> records = DBUtil.getHealthRecords(30);
            if (records.isEmpty()) {
                JOptionPane.showMessageDialog(this, "暂无健康记录, 无法生成报告", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String content;
            if (apiKey.trim().isEmpty()) {
                content = generateLocalReport(reportType, records);
            } else {
                try {
                    content = callAIForReport(apiKey.trim(), reportType, records);
                } catch (Exception e) {
                    content = "AI 生成失败, 使用本地报告:\n\n" + generateLocalReport(reportType, records);
                }
            }

            DBUtil.saveAIReport(reportType, content);
            taReportContent.setText(content);
            refresh();
            JOptionPane.showMessageDialog(this, reportType + "生成成功!");
        }

        /** 本地生成报告 */
        private String generateLocalReport(String type, List<Map<String, Object>> records) {
            StringBuilder sb = new StringBuilder();
            sb.append("═══════════════════════════════════\n");
            sb.append("       健康").append(type).append("报告\n");
            sb.append("═══════════════════════════════════\n\n");

            Map<String, Object> latest = records.get(0);
            Map<String, Object> oldest = records.get(records.size() - 1);

            double weightChange = (double) latest.get("weight") - (double) oldest.get("weight");
            double fatChange = (double) latest.get("body_fat") - (double) oldest.get("body_fat");
            double bmiChange = (double) latest.get("bmi") - (double) oldest.get("bmi");

            sb.append("【身体变化趋势】\n");
            sb.append(String.format("  体重: %.1f → %.1f kg (变化 %+.1f kg)\n",
                    (double) oldest.get("weight"), (double) latest.get("weight"), weightChange));
            sb.append(String.format("  体脂率: %.1f%% → %.1f%% (变化 %+.1f%%)\n",
                    (double) oldest.get("body_fat"), (double) latest.get("body_fat"), fatChange));
            sb.append(String.format("  BMI: %.1f → %.1f (变化 %+.1f)\n\n",
                    (double) oldest.get("bmi"), (double) latest.get("bmi"), bmiChange));

            sb.append("【当前状态】\n");
            sb.append("  体质分类: ").append(latest.get("body_type")).append("\n");
            sb.append("  身体年龄: ").append(latest.get("body_age")).append(" 岁\n\n");

            sb.append("【建议】\n");
            if (weightChange > 0) {
                sb.append("  体重有所增加, 建议增加有氧运动频率\n");
            } else if (weightChange < 0) {
                sb.append("  体重有所下降, 请确保营养摄入充足\n");
            } else {
                sb.append("  体重保持稳定, 继续保持良好习惯\n");
            }
            if (fatChange > 0) {
                sb.append("  体脂率上升, 建议调整饮食结构, 减少高脂食物\n");
            }
            sb.append("  保持规律运动, 每周至少 3 次有氧运动\n");
            sb.append("  注意蛋白质摄入, 维持肌肉量\n\n");

            sb.append("报告生成时间: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date())).append("\n");

            return sb.toString();
        }

        /** 调用 AI 生成报告 */
        private String callAIForReport(String apiKey, String reportType,
                                        List<Map<String, Object>> records) throws Exception {
            URL url = new URL("https://api.siliconflow.cn/v1/chat/completions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);

            // 构建数据摘要
            StringBuilder dataSb = new StringBuilder();
            for (Map<String, Object> r : records) {
                dataSb.append(String.format("日期:%s 体重:%.1f 体脂:%.1f%% BMI:%.1f 类型:%s\n",
                        r.get("record_date"), (double) r.get("weight"),
                        (double) r.get("body_fat"), (double) r.get("bmi"), r.get("body_type")));
            }

            String prompt = "请根据以下健康数据生成一份" + reportType + ", 包含身体变化趋势分析、饮食建议、运动建议:\n" + dataSb.toString();

            String body = "{\"model\":\"Qwen/Qwen2.5-7B-Instruct\",\"messages\":[{\"role\":\"user\",\"content\":\""
                    + prompt.replace("\"", "\\\"").replace("\n", "\\n")
                    + "\"}]}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes("UTF-8"));
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream(), "UTF-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);

            String resp = response.toString();
            int idx = resp.indexOf("\"content\":\"");
            if (idx >= 0) {
                int end = resp.indexOf("\"", idx + 11);
                if (end > idx) return resp.substring(idx + 11, end).replace("\\n", "\n");
            }
            return "AI 返回: " + resp;
        }
    }

    // ================================================================
    //             第十三部分: Tab8 数据大屏面板
    // ================================================================

    /** 数据大屏面板 — 关键指标卡片布局 */
    static class DashboardPanel extends JPanel {
        DashboardPanel() {
            setLayout(new BorderLayout(8, 8));
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            setBackground(Theme.BG);

            // === 控制卡片 ===
            JPanel btnCard = Theme.createCardPanel("健康数据大屏", Theme.PRIMARY);
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            btnPanel.setOpaque(false);
            JButton btnRefresh = new RoundButton("🔄 刷新大屏", Theme.PRIMARY, Theme.PRIMARY_D);
            btnRefresh.addActionListener(e -> refresh());
            btnPanel.add(btnRefresh);
            JLabel lblHint = new JLabel("实时展示核心健康指标");
            lblHint.setFont(Theme.FONT_SMALL);
            lblHint.setForeground(Theme.TEXT_GRAY);
            btnPanel.add(lblHint);
            btnCard.add(btnPanel, BorderLayout.CENTER);
            add(btnCard, BorderLayout.NORTH);

            // === 指标卡片容器 ===
            JPanel gridCard = Theme.createCardPanel("核心指标", Theme.PRIMARY);
            JPanel grid = new JPanel(new GridLayout(0, 4, 12, 12));
            grid.setOpaque(false);
            gridCard.add(new JScrollPane(grid), BorderLayout.CENTER);
            add(gridCard, BorderLayout.CENTER);

            // 创建卡片
            refreshGrid(grid);
        }

        private void refreshGrid(JPanel grid) {
            grid.removeAll();

            Map<String, Object> latest = DBUtil.getLatestHealthRecord();
            if (latest == null) {
                grid.add(Theme.createMetricCard("暂无数据", "请先录入健康记录", new Color[]{Theme.TEXT_GRAY, Theme.TEXT_LIGHT}));
                grid.revalidate();
                grid.repaint();
                return;
            }

            double weight = (double) latest.get("weight");
            double bmi = (double) latest.get("bmi");
            double bodyFat = (double) latest.get("body_fat");
            double water = (double) latest.get("water_rate");
            double muscle = (double) latest.get("muscle_rate");
            double bmr = (double) latest.get("bmr");
            double tdee = (double) latest.get("tdee");
            int visceral = (int) latest.get("visceral_fat");
            int bodyAge = (int) latest.get("body_age");
            String bodyType = (String) latest.get("body_type");
            int score = HealthCalculator.calcHealthScore(bmi, bodyFat, visceral, muscle, water, currentGender);

            int exerciseCal = DBUtil.getTodayExerciseCalories();
            int[] diet = DBUtil.getTodayDietSummary();
            int intakeCal = diet[0];
            int calDiff = (int)(tdee + exerciseCal - intakeCal);

            // 卡片颜色 — 使用 Theme 语义色
            Color[] blueG  = {Theme.PRIMARY, Theme.PRIMARY_D};
            Color[] greenG = {Theme.SUCCESS, new Color(22, 163, 74)};
            Color[] orangeG = {Theme.WARNING, new Color(217, 119, 6)};
            Color[] redG   = {Theme.DANGER, new Color(220, 38, 38)};
            Color[] purpleG = {Theme.INFO, Theme.PRIMARY_D};
            Color[] tealG  = {Theme.ACCENT, new Color(13, 148, 136)};

            grid.add(Theme.createMetricCard("今日体重", df1.format(weight) + " kg", blueG));
            grid.add(Theme.createMetricCard("BMI", df1.format(bmi) + " (" + HealthCalculator.classifyBMI(bmi) + ")", blueG));
            grid.add(Theme.createMetricCard("体脂率", df1.format(bodyFat) + "%", orangeG));
            grid.add(Theme.createMetricCard("水分率", df1.format(water) + "%", tealG));

            grid.add(Theme.createMetricCard("肌肉率", df1.format(muscle) + "%", greenG));
            grid.add(Theme.createMetricCard("BMR", df0.format(bmr) + " kcal", purpleG));
            grid.add(Theme.createMetricCard("TDEE", df0.format(tdee) + " kcal", purpleG));
            grid.add(Theme.createMetricCard("内脏脂肪", visceral + " 级 (" + HealthCalculator.assessVisceralFat(visceral) + ")", orangeG));

            grid.add(Theme.createMetricCard("身体年龄", bodyAge + " 岁", tealG));
            grid.add(Theme.createMetricCard("体质分类", bodyType, blueG));
            grid.add(Theme.createMetricCard("今日摄入", intakeCal + " kcal", greenG));
            grid.add(Theme.createMetricCard("运动消耗", exerciseCal + " kcal", greenG));

            grid.add(Theme.createMetricCard("热量差", (calDiff >= 0 ? "+" : "") + calDiff + " kcal",
                    calDiff >= 0 ? greenG : redG));
            grid.add(Theme.createMetricCard("健康评分", score + "/100\n" + HealthCalculator.scoreLevel(score),
                    score >= 75 ? greenG : score >= 60 ? orangeG : redG));

            grid.revalidate();
            grid.repaint();
        }

        void refresh() {
            refreshGrid((JPanel) ((JScrollPane) this.getComponent(1)).getViewport().getView());
        }

    }
}
