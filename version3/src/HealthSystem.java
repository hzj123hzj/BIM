import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
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
 * BMI 体质评估与预测系统 v3.0 — Aurora Design System
 * 技术栈: Java Swing + PostgreSQL + JDBC
 * v3.0 UI: 侧边栏导航 + 玻璃态登录 + 增强卡片 + 贝塞尔曲线图表
 */
public class HealthSystem {
    static final String DB_URL = "jdbc:postgresql://localhost:5432/health_db";
    static final String DB_USER = "postgres";
    static final String DB_PASS = "12345678";
    static String currentUsername;
    static String currentGender;
    static int currentAge;
    static double currentHeight;
    static String currentActivityLevel = "久坐";
    static final DecimalFormat df2 = new DecimalFormat("0.00");
    static final DecimalFormat df1 = new DecimalFormat("0.0");
    static final DecimalFormat df0 = new DecimalFormat("0");

    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) { UIManager.setLookAndFeel(info.getClassName()); break; }
            }
        } catch (Exception e) { e.printStackTrace(); }
        initGlobalFont();
        Theme.applyNimbusTheme();
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }

    private static void initGlobalFont() {
        String[] fontNames = {"Microsoft YaHei UI", "Microsoft YaHei", "SimHei", "SimSun", "Noto Sans CJK SC", "Dialog"};
        Font font = null;
        for (String name : fontNames) {
            if (Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()).contains(name)) {
                font = new Font(name, Font.PLAIN, 13); break;
            }
        }
        if (font == null) font = new Font("Dialog", Font.PLAIN, 13);
        String[] keys = {"Button.font","ToggleButton.font","RadioButton.font","CheckBox.font","ColorChooser.font","ComboBox.font","Label.font","List.font","MenuBar.font","MenuItem.font","RadioButtonMenuItem.font","CheckBoxMenuItem.font","Menu.font","PopupMenu.font","OptionPane.font","Panel.font","ProgressBar.font","ScrollPane.font","Viewport.font","TabbedPane.font","Table.font","TableHeader.font","TextField.font","TextArea.font","TextPane.font","EditorPane.font","TitledBorder.font","ToolBar.font","ToolTip.font","Tree.font"};
        for (String key : keys) UIManager.put(key, font);
    }

    // === v3.0 Aurora Theme ===
    static class Theme {
        static final Color PRIMARY = new Color(99,102,241), PRIMARY_L = new Color(129,140,248), PRIMARY_D = new Color(79,70,229), PRIMARY_DD = new Color(67,56,202);
        static final Color ACCENT = new Color(244,63,94), ACCENT_L = new Color(251,113,133), ACCENT_D = new Color(225,29,72);
        static final Color SIDEBAR_BG = new Color(22,20,36), SIDEBAR_BG2 = new Color(35,32,56), SIDEBAR_HOVER = new Color(255,255,255,15);
        static final Color SIDEBAR_TEXT = new Color(165,160,200), SIDEBAR_TEXT_ACTIVE = Color.WHITE;
        static final Color BG = new Color(248,249,251), CARD_BG = Color.WHITE, HEADER_BG = new Color(245,243,255), FOOTER_BG = new Color(241,245,249);
        static final Color TEXT_DARK = new Color(30,27,46), TEXT_BODY = new Color(71,85,105), TEXT_GRAY = new Color(148,163,184), TEXT_LIGHT = new Color(203,213,225);
        static final Color BORDER = new Color(226,232,240), BORDER_L = new Color(241,245,249);
        static final Color SUCCESS = new Color(16,185,129), WARNING = new Color(245,158,11), DANGER = new Color(239,68,68), INFO = new Color(59,130,246);
        static final Color[] GRAD_PRIMARY = {new Color(99,102,241), new Color(139,92,246)};
        static final Color[] GRAD_ACCENT = {new Color(244,63,94), new Color(251,113,133)};
        static final Color[] GRAD_SUNSET = {new Color(251,146,60), new Color(244,63,94)};
        static final Color[] GRAD_OCEAN = {new Color(56,189,248), new Color(59,130,246)};
        static final Color[] GRAD_EMERALD = {new Color(16,185,129), new Color(5,150,105)};
        static final Color[] GRAD_SIDEBAR = {new Color(22,20,36), new Color(35,32,56)};
        static final Color[] GRAD_LOGIN = {new Color(79,70,229), new Color(124,58,237), new Color(192,38,211)};
        static final Color[] GRAD_HEADER = {new Color(245,243,255), new Color(238,242,255)};
        static final String FONT_NAME = getAvailableFont();
        static final Font FONT_H1 = new Font(FONT_NAME, Font.BOLD, 24), FONT_H2 = new Font(FONT_NAME, Font.BOLD, 18);
        static final Font FONT_TITLE = new Font(FONT_NAME, Font.BOLD, 16), FONT_HEADER = new Font(FONT_NAME, Font.BOLD, 14);
        static final Font FONT_BODY = new Font(FONT_NAME, Font.PLAIN, 13), FONT_BODY_B = new Font(FONT_NAME, Font.BOLD, 13);
        static final Font FONT_SMALL = new Font(FONT_NAME, Font.PLAIN, 12), FONT_TINY = new Font(FONT_NAME, Font.PLAIN, 11);
        static final Font FONT_BIG_NUM = new Font(FONT_NAME, Font.BOLD, 28), FONT_NAV = new Font(FONT_NAME, Font.PLAIN, 13);

        static String getAvailableFont() {
            String[] candidates = {"Microsoft YaHei UI","Microsoft YaHei","SimHei","Noto Sans CJK SC","PingFang SC","Dialog"};
            String[] available = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            for (String c : candidates) for (String a : available) if (a.equals(c)) return c;
            return "Dialog";
        }
        static void applyNimbusTheme() {
            UIManager.put("nimbusBase", PRIMARY); UIManager.put("nimbusBlueGrey", new Color(226,232,240));
            UIManager.put("control", BG); UIManager.put("nimbusSelectionBackground", PRIMARY_L);
            UIManager.put("nimbusSelection", PRIMARY_L); UIManager.put("nimbusFocus", new Color(129,140,248,80));
            UIManager.put("nimbusLightBackground", CARD_BG); UIManager.put("text", TEXT_DARK);
            UIManager.put("nimbusBorder", BORDER); UIManager.put("Label.foreground", TEXT_DARK);
            UIManager.put("Panel.background", BG);
        }
        static void stylePrimaryButton(JButton btn) { btn.setFont(FONT_BODY_B); btn.setForeground(Color.WHITE); btn.setBackground(PRIMARY); btn.setFocusPainted(false); btn.setBorderPainted(false); btn.setContentAreaFilled(false); btn.setOpaque(false); btn.setBorder(BorderFactory.createEmptyBorder(10,24,10,24)); btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); addHoverEffect(btn, PRIMARY, PRIMARY_D); }
        static void styleAccentButton(JButton btn) { btn.setFont(FONT_BODY_B); btn.setForeground(Color.WHITE); btn.setBackground(ACCENT); btn.setFocusPainted(false); btn.setBorderPainted(false); btn.setContentAreaFilled(false); btn.setOpaque(false); btn.setBorder(BorderFactory.createEmptyBorder(10,24,10,24)); btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); addHoverEffect(btn, ACCENT, ACCENT_D); }
        static void styleGhostButton(JButton btn) { btn.setFont(FONT_BODY); btn.setForeground(PRIMARY); btn.setBackground(new Color(238,242,255)); btn.setFocusPainted(false); btn.setBorderPainted(false); btn.setContentAreaFilled(false); btn.setOpaque(false); btn.setBorder(BorderFactory.createEmptyBorder(8,18,8,18)); btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); addHoverEffect(btn, new Color(238,242,255), new Color(224,231,255)); }
        static void styleDangerButton(JButton btn) { btn.setFont(FONT_BODY_B); btn.setForeground(Color.WHITE); btn.setBackground(DANGER); btn.setFocusPainted(false); btn.setBorderPainted(false); btn.setContentAreaFilled(false); btn.setOpaque(false); btn.setBorder(BorderFactory.createEmptyBorder(10,24,10,24)); btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); addHoverEffect(btn, DANGER, new Color(220,38,38)); }
        static void styleTextField(JTextField field) { field.setFont(FONT_BODY); field.setForeground(TEXT_DARK); field.setBackground(CARD_BG); field.setCaretColor(PRIMARY); field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER,1),BorderFactory.createEmptyBorder(9,12,9,12))); field.addFocusListener(new FocusAdapter(){public void focusGained(FocusEvent e){field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(PRIMARY_L,2),BorderFactory.createEmptyBorder(8,11,8,11)));}public void focusLost(FocusEvent e){field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER,1),BorderFactory.createEmptyBorder(9,12,9,12)));}}); }
        static void stylePasswordField(JPasswordField field) { field.setFont(FONT_BODY); field.setForeground(TEXT_DARK); field.setBackground(CARD_BG); field.setCaretColor(PRIMARY); field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER,1),BorderFactory.createEmptyBorder(9,12,9,12))); field.addFocusListener(new FocusAdapter(){public void focusGained(FocusEvent e){field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(PRIMARY_L,2),BorderFactory.createEmptyBorder(8,11,8,11)));}public void focusLost(FocusEvent e){field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER,1),BorderFactory.createEmptyBorder(9,12,9,12)));}}); }
        static void styleComboBox(JComboBox<?> cb) { cb.setFont(FONT_BODY); cb.setForeground(TEXT_DARK); cb.setBackground(CARD_BG); cb.setBorder(BorderFactory.createLineBorder(BORDER,1)); cb.setCursor(new Cursor(Cursor.HAND_CURSOR)); }
        static void styleSpinner(JSpinner sp) { sp.setFont(FONT_BODY); sp.setForeground(TEXT_DARK); sp.setBackground(CARD_BG); ((JSpinner.DefaultEditor)sp.getEditor()).getTextField().setBorder(BorderFactory.createEmptyBorder(5,8,5,8)); }
        static void styleTable(JTable table) { table.setFont(FONT_BODY); table.setForeground(TEXT_DARK); table.setBackground(CARD_BG); table.setRowHeight(36); table.setSelectionBackground(new Color(238,242,255)); table.setSelectionForeground(PRIMARY_D); table.setGridColor(BORDER_L); table.setShowVerticalLines(false); table.setShowHorizontalLines(true); table.setIntercellSpacing(new Dimension(0,0)); table.getTableHeader().setFont(FONT_HEADER); table.getTableHeader().setBackground(new Color(248,250,252)); table.getTableHeader().setForeground(TEXT_BODY); table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0,0,2,0,PRIMARY)); table.getTableHeader().setReorderingAllowed(false); }
        static RoundedPanel createCardPanel(String title, Color stripeColor) {
            RoundedPanel card = new RoundedPanel(new BorderLayout(8,8), 16); card.setBackground(CARD_BG); card.setBorder(BorderFactory.createEmptyBorder(16,18,18,18));
            if (title != null && !title.isEmpty()) {
                JPanel headerPanel = new JPanel(new BorderLayout()); headerPanel.setOpaque(false);
                JPanel stripeWrapper = new JPanel() { protected void paintComponent(Graphics g) { Graphics2D g2 = (Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); GradientPaint gp = new GradientPaint(0,0,stripeColor,0,getHeight(),new Color(stripeColor.getRed(),stripeColor.getGreen(),stripeColor.getBlue(),180)); g2.setPaint(gp); g2.fillRoundRect(0,0,4,getHeight(),2,2); g2.dispose(); } };
                stripeWrapper.setOpaque(false); stripeWrapper.setPreferredSize(new Dimension(4,20));
                JLabel lblTitle = new JLabel(title); lblTitle.setFont(FONT_H2); lblTitle.setForeground(TEXT_DARK);
                headerPanel.add(stripeWrapper, BorderLayout.WEST); headerPanel.add(lblTitle, BorderLayout.CENTER);
                headerPanel.setBorder(BorderFactory.createEmptyBorder(0,0,10,0)); card.add(headerPanel, BorderLayout.NORTH);
            } return card;
        }
        static RoundedPanel createMetricCard(String label, String value, Color[] gradient) {
            RoundedPanel card = new RoundedPanel(new BorderLayout(6,2), 16) { protected void paintComponent(Graphics g) { Graphics2D g2 = (Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); int w = Math.max(getWidth()-6,28), h = Math.max(getHeight()-6,28); for(int i=0;i<4;i++){int a=20-i*4; g2.setColor(new Color(gradient[0].getRed(),gradient[0].getGreen(),gradient[0].getBlue(),a)); g2.fillRoundRect(3+i,4+i,w,h,16,16);} GradientPaint gp = new GradientPaint(0,0,gradient[0],w,h,gradient[1]); g2.setPaint(gp); g2.fillRoundRect(0,0,w,h,16,16); g2.setColor(new Color(255,255,255,30)); g2.fillRoundRect(0,0,w,h/3,16,16); g2.dispose(); super.paintComponent(g); } };
            card.setOpaque(false); card.setBorder(BorderFactory.createEmptyBorder(18,18,18,18));
            JLabel lblLabel = new JLabel(label); lblLabel.setFont(FONT_SMALL); lblLabel.setForeground(new Color(255,255,255,200)); card.add(lblLabel, BorderLayout.NORTH);
            JLabel lblValue = new JLabel(value); lblValue.setFont(FONT_BIG_NUM); lblValue.setForeground(Color.WHITE); card.add(lblValue, BorderLayout.CENTER);
            return card;
        }
        static void addHoverEffect(JButton btn, Color normalColor, Color hoverColor) { btn.addMouseListener(new MouseAdapter(){public void mouseEntered(MouseEvent e){btn.putClientProperty("hoverColor",hoverColor);btn.repaint();}public void mouseExited(MouseEvent e){btn.putClientProperty("hoverColor",null);btn.repaint();}}); btn.setBackground(normalColor); }
    }

    static class GradientPanel extends JPanel {
        private final Color c1, c2; private boolean vertical = true;
        GradientPanel(LayoutManager layout, Color c1, Color c2) { super(layout); this.c1=c1; this.c2=c2; setOpaque(false); }
        GradientPanel(LayoutManager layout, Color[] colors) { this(layout, colors[0], colors[1]); }
        protected void paintComponent(Graphics g) { Graphics2D g2 = (Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); int w=getWidth(), h=getHeight(); GradientPaint gp = new GradientPaint(0,0,c1,vertical?0:w,vertical?h:0,c2); g2.setPaint(gp); g2.fillRect(0,0,w,h); g2.dispose(); super.paintComponent(g); }
    }

    static class TriGradientPanel extends JPanel {
        private final Color[] colors;
        TriGradientPanel(LayoutManager layout, Color[] colors) { super(layout); this.colors=colors; setOpaque(false); }
        protected void paintComponent(Graphics g) { Graphics2D g2 = (Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); int w=getWidth(), h=getHeight(); LinearGradientPaint lgp = new LinearGradientPaint(0,0,w,h,new float[]{0f,0.5f,1f},colors); g2.setPaint(lgp); g2.fillRect(0,0,w,h); g2.dispose(); super.paintComponent(g); }
    }

    static class RoundedPanel extends JPanel {
        private final int radius; private Color shadowColor = new Color(100,116,139,18);
        RoundedPanel(LayoutManager layout, int radius) { super(layout); this.radius=radius; setOpaque(false); setBackground(Theme.CARD_BG); }
        RoundedPanel(LayoutManager layout, int radius, Color bg) { this(layout,radius); setBackground(bg); }
        protected void paintComponent(Graphics g) { Graphics2D g2 = (Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); int w = Math.max(getWidth()-4, radius*2), h = Math.max(getHeight()-4, radius*2); for(int i=0;i<4;i++){int a=shadowColor.getAlpha()-i*3; if(a<5) continue; g2.setColor(new Color(shadowColor.getRed(),shadowColor.getGreen(),shadowColor.getBlue(),a)); g2.fillRoundRect(1+i,2+i,w,h,radius,radius);} g2.setColor(getBackground()); g2.fillRoundRect(0,0,w,h,radius,radius); g2.setColor(new Color(255,255,255,80)); g2.drawRoundRect(1,1,w-2,h-2,radius-1,radius-1); g2.dispose(); super.paintComponent(g); }
    }

    static class RoundButton extends JButton {
        private Color bgColor, hoverColor; private int radius = 10; private boolean gradient = false;
        RoundButton(String text, Color bg, Color hover) { super(text); this.bgColor=bg; this.hoverColor=hover; setFont(Theme.FONT_BODY_B); setForeground(Color.WHITE); setFocusPainted(false); setContentAreaFilled(false); setBorderPainted(false); setOpaque(false); setBorder(BorderFactory.createEmptyBorder(10,24,10,24)); setCursor(new Cursor(Cursor.HAND_CURSOR)); addMouseListener(new MouseAdapter(){public void mouseEntered(MouseEvent e){repaint();}public void mouseExited(MouseEvent e){repaint();}}); }
        RoundButton setGradient(boolean g) { this.gradient=g; return this; }
        protected void paintComponent(Graphics g) { Graphics2D g2 = (Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); int w=getWidth()-1, h=getHeight()-1; boolean isHover = getModel().isRollover(); Color c = isHover ? hoverColor : bgColor; g2.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(), isHover?60:35)); g2.fillRoundRect(2,3,w,h,radius,radius); if(gradient){GradientPaint gp=new GradientPaint(0,0,c,0,h,new Color(c.getRed(),c.getGreen(),c.getBlue(),200)); g2.setPaint(gp);}else{g2.setColor(c);} g2.fillRoundRect(0,0,w,h,radius,radius); g2.setColor(new Color(255,255,255, isHover?50:30)); g2.fillRoundRect(1,1,w-2,h/2,radius,radius); g2.dispose(); super.paintComponent(g); }
    }

    static class SidebarNav extends JPanel {
        private final NavItem[] navItems; private int selectedIndex = 0; private final MainFrame mainFrame;
        SidebarNav(MainFrame frame, NavItem[] items) { this.mainFrame=frame; this.navItems=items; setLayout(new BorderLayout()); setOpaque(false); setPreferredSize(new Dimension(220,0));
            JPanel logoArea = createLogoArea();
            JPanel navPanel = new JPanel(); navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS)); navPanel.setOpaque(false); navPanel.setBorder(BorderFactory.createEmptyBorder(8,0,8,0));
            for(int i=0;i<navItems.length;i++){final int idx=i; navItems[i].setActionListener(e->selectTab(idx)); navPanel.add(navItems[i]); navPanel.add(Box.createVerticalStrut(2));}
            JScrollPane scrollNav = new JScrollPane(navPanel); scrollNav.setOpaque(false); scrollNav.getViewport().setOpaque(false); scrollNav.setBorder(null); scrollNav.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            JPanel bottomArea = createBottomArea();
            add(logoArea, BorderLayout.NORTH); add(scrollNav, BorderLayout.CENTER); add(bottomArea, BorderLayout.SOUTH); updateSelection();
        }
        private JPanel createLogoArea() { JPanel panel = new JPanel(new BorderLayout()); panel.setOpaque(false); panel.setBorder(BorderFactory.createEmptyBorder(24,20,16,20));
            JPanel logoIcon = new JPanel() { protected void paintComponent(Graphics g) { Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON); GradientPaint gp=new GradientPaint(0,0,new Color(99,102,241),40,40,new Color(139,92,246)); g2.setPaint(gp); g2.fillOval(0,0,40,40); g2.setColor(Color.WHITE); g2.setFont(new Font(Theme.FONT_NAME,Font.BOLD,20)); FontMetrics fm=g2.getFontMetrics(); String heart="♥"; g2.drawString(heart,(40-fm.stringWidth(heart))/2,28); g2.dispose(); } };
            logoIcon.setPreferredSize(new Dimension(40,40));
            JLabel lblTitle = new JLabel("  BMI 系统"); lblTitle.setFont(new Font(Theme.FONT_NAME,Font.BOLD,16)); lblTitle.setForeground(Theme.SIDEBAR_TEXT_ACTIVE);
            JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0)); titlePanel.setOpaque(false); titlePanel.add(logoIcon); titlePanel.add(lblTitle);
            panel.add(titlePanel, BorderLayout.CENTER); return panel;
        }
        private JPanel createBottomArea() { JPanel panel = new JPanel(new BorderLayout()); panel.setOpaque(false); panel.setBorder(BorderFactory.createEmptyBorder(12,16,16,16));
            JPanel dividerWrapper = new JPanel() { protected void paintComponent(Graphics g) { Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(new Color(255,255,255,20)); g2.fillRoundRect(0,0,getWidth(),1,1,1); g2.dispose(); } };
            dividerWrapper.setPreferredSize(new Dimension(1,1)); dividerWrapper.setOpaque(false);
            JLabel lblUser = new JLabel("  v3.0 Aurora"); lblUser.setFont(Theme.FONT_SMALL); lblUser.setForeground(Theme.SIDEBAR_TEXT);
            panel.add(dividerWrapper, BorderLayout.NORTH); panel.add(Box.createVerticalStrut(8), BorderLayout.CENTER); panel.add(lblUser, BorderLayout.SOUTH); return panel;
        }
        void selectTab(int index) { selectedIndex=index; updateSelection(); mainFrame.switchToTab(index); }
        void updateSelection() { for(int i=0;i<navItems.length;i++) navItems[i].setActive(i==selectedIndex); }
        protected void paintComponent(Graphics g) { Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON); int w=getWidth(),h=getHeight(); GradientPaint gp=new GradientPaint(0,0,Theme.SIDEBAR_BG,0,h,Theme.SIDEBAR_BG2); g2.setPaint(gp); g2.fillRect(0,0,w,h); g2.setColor(new Color(255,255,255,15)); g2.drawLine(w-1,0,w-1,h); g2.dispose(); super.paintComponent(g); }
    }

    static class NavItem extends JPanel {
        private final String icon, label; private boolean active=false, hover=false;
        NavItem(String icon, String label) { this.icon=icon; this.label=label; setOpaque(false); setPreferredSize(new Dimension(220,44)); setMaximumSize(new Dimension(220,44)); setCursor(new Cursor(Cursor.HAND_CURSOR)); addMouseListener(new MouseAdapter(){public void mouseEntered(MouseEvent e){hover=true;repaint();}public void mouseExited(MouseEvent e){hover=false;repaint();}public void mouseClicked(MouseEvent e){fireActionPerformed();}}); }
        void setActive(boolean a) { this.active=a; repaint(); }
        void setActionListener(ActionListener l) { listenerList.add(ActionListener.class, l); }
        private void fireActionPerformed() { ActionListener[] listeners = listenerList.getListeners(ActionListener.class); ActionEvent e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "click"); for(ActionListener l:listeners) l.actionPerformed(e); }
        protected void paintComponent(Graphics g) { Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON); g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON); int w=getWidth(),h=getHeight(); int itemX=12, itemW=w-24, itemH=h-4, itemY=2;
            if(active){GradientPaint gp=new GradientPaint(itemX,0,new Color(99,102,241),itemX+itemW,0,new Color(139,92,246)); g2.setPaint(gp); g2.fillRoundRect(itemX,itemY,itemW,itemH,10,10); g2.setColor(new Color(255,255,255,30)); g2.fillRoundRect(itemX+1,itemY+1,itemW-2,itemH/2,10,10);}
            else if(hover){g2.setColor(Theme.SIDEBAR_HOVER); g2.fillRoundRect(itemX,itemY,itemW,itemH,10,10);}
            g2.setFont(new Font(Theme.FONT_NAME,Font.PLAIN,16)); FontMetrics fmIcon=g2.getFontMetrics(); int iconX=itemX+16, iconY=(h+fmIcon.getAscent()-fmIcon.getDescent())/2; g2.setColor(active?Color.WHITE:Theme.SIDEBAR_TEXT); g2.drawString(icon,iconX,iconY);
            g2.setFont(Theme.FONT_NAV); FontMetrics fmText=g2.getFontMetrics(); int textX=iconX+28, textY=(h+fmText.getAscent()-fmText.getDescent())/2; g2.setColor(active?Theme.SIDEBAR_TEXT_ACTIVE:Theme.SIDEBAR_TEXT); g2.drawString(label,textX,textY);
            g2.dispose();
        }
    }

    static class FadeIn { static void apply(final JComponent comp, final int duration) { comp.setOpaque(false); final int steps=20; final int delay=duration/steps; final javax.swing.Timer[] timer=new javax.swing.Timer[1]; timer[0]=new javax.swing.Timer(delay,new ActionListener(){int alpha=0; public void actionPerformed(ActionEvent e){alpha+=255/steps; if(alpha>=255){alpha=255; comp.putClientProperty("fadeInAlpha",null); timer[0].stop();}else{comp.putClientProperty("fadeInAlpha",alpha);} comp.repaint();}}); comp.putClientProperty("fadeInAlpha",0); timer[0].start(); } }

    // === PasswordUtil ===
    static class PasswordUtil {
        static String generateSalt() { byte[] salt=new byte[16]; new SecureRandom().nextBytes(salt); return bytesToHex(salt); }
        static String hash(String password, String salt) { try { MessageDigest md=MessageDigest.getInstance("SHA-256"); md.update((password+salt).getBytes("UTF-8")); return bytesToHex(md.digest()); } catch(Exception e) { throw new RuntimeException("密码加密失败",e); } }
        static boolean verify(String password, String salt, String hash) { return hash(password,salt).equals(hash); }
        private static String bytesToHex(byte[] bytes) { StringBuilder sb=new StringBuilder(); for(byte b:bytes) sb.append(String.format("%02x",b)); return sb.toString(); }
    }

    // === DBUtil ===
    static class DBUtil {
        static Connection getConnection() throws SQLException { try { Class.forName("org.postgresql.Driver"); } catch(ClassNotFoundException e) { throw new SQLException("PostgreSQL JDBC 驱动未找到"); } return DriverManager.getConnection(DB_URL,DB_USER,DB_PASS); }
        static boolean testConnection() { try(Connection conn=getConnection()){return conn!=null;}catch(SQLException e){return false;} }
        static boolean registerUser(String username, String password, String gender, int age, double height, String activityLevel) { String salt=PasswordUtil.generateSalt(); String hash=PasswordUtil.hash(password,salt); String sql="INSERT INTO users (username,password,salt,gender,age,height,activity_level) VALUES(?,?,?,?,?,?,?)"; try(Connection conn=getConnection(); PreparedStatement ps=conn.prepareStatement(sql)){ps.setString(1,username);ps.setString(2,hash);ps.setString(3,salt);ps.setString(4,gender);ps.setInt(5,age);ps.setDouble(6,height);ps.setString(7,activityLevel);return ps.executeUpdate()>0;}catch(SQLException e){if(e.getSQLState().equals("23505")){JOptionPane.showMessageDialog(null,"用户名已存在, 请更换","注册失败",JOptionPane.ERROR_MESSAGE);}else{JOptionPane.showMessageDialog(null,"注册失败: "+e.getMessage(),"错误",JOptionPane.ERROR_MESSAGE);}return false;} }
        static boolean loginUser(String username, String password) { String sql="SELECT password,salt,gender,age,height,activity_level FROM users WHERE username=?"; try(Connection conn=getConnection();PreparedStatement ps=conn.prepareStatement(sql)){ps.setString(1,username);ResultSet rs=ps.executeQuery();if(rs.next()){String hash=rs.getString("password");String salt=rs.getString("salt");if(PasswordUtil.verify(password,salt,hash)){currentUsername=username;currentGender=rs.getString("gender");currentAge=rs.getInt("age");currentHeight=rs.getDouble("height");currentActivityLevel=rs.getString("activity_level");if(currentActivityLevel==null)currentActivityLevel="久坐";return true;}}}catch(SQLException e){JOptionPane.showMessageDialog(null,"数据库连接失败: "+e.getMessage(),"错误",JOptionPane.ERROR_MESSAGE);}return false; }
        static boolean saveHealthRecord(double weight,double bodyFat,double waterRate,double muscleRate,int visceralFat,double boneMuscle,double waist) { double bmi=HealthCalculator.calcBMI(weight,currentHeight);double bmr=HealthCalculator.calcAvgBMR(weight,currentHeight,currentAge,currentGender);double tdee=HealthCalculator.calcTDEE(bmr,currentActivityLevel);int bodyAge=HealthCalculator.calcBodyAge(currentAge,bodyFat,muscleRate,visceralFat,currentGender);String bodyType=HealthCalculator.classifyBodyType(bmi,bodyFat,currentGender);String sql="INSERT INTO health_records(username,weight,body_fat,water_rate,muscle_rate,visceral_fat,bone_muscle,bmr,tdee,bmi,waist,body_age,body_type) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)"; try(Connection conn=getConnection();PreparedStatement ps=conn.prepareStatement(sql)){ps.setString(1,currentUsername);ps.setDouble(2,weight);ps.setDouble(3,bodyFat);ps.setDouble(4,waterRate);ps.setDouble(5,muscleRate);ps.setInt(6,visceralFat);ps.setDouble(7,boneMuscle);ps.setDouble(8,bmr);ps.setDouble(9,tdee);ps.setDouble(10,bmi);ps.setDouble(11,waist);ps.setInt(12,bodyAge);ps.setString(13,bodyType);return ps.executeUpdate()>0;}catch(SQLException e){JOptionPane.showMessageDialog(null,"保存健康记录失败: "+e.getMessage(),"错误",JOptionPane.ERROR_MESSAGE);return false;} }
        static Map<String,Object> getLatestHealthRecord() { String sql="SELECT * FROM health_records WHERE username=? ORDER BY record_date DESC, id DESC LIMIT 1"; try(Connection conn=getConnection();PreparedStatement ps=conn.prepareStatement(sql)){ps.setString(1,currentUsername);ResultSet rs=ps.executeQuery();if(rs.next()){Map<String,Object> map=new HashMap<>();map.put("weight",rs.getDouble("weight"));map.put("body_fat",rs.getDouble("body_fat"));map.put("water_rate",rs.getDouble("water_rate"));map.put("muscle_rate",rs.getDouble("muscle_rate"));map.put("visceral_fat",rs.getInt("visceral_fat"));map.put("bone_muscle",rs.getDouble("bone_muscle"));map.put("bmr",rs.getDouble("bmr"));map.put("tdee",rs.getDouble("tdee"));map.put("bmi",rs.getDouble("bmi"));map.put("waist",rs.getDouble("waist"));map.put("body_age",rs.getInt("body_age"));try{map.put("body_type",rs.getString("body_type"));}catch(SQLException ex){map.put("body_type","--");}map.put("record_date",rs.getDate("record_date"));return map;}}catch(SQLException e){e.printStackTrace();}return null; }
        static List<Map<String,Object>> getHealthRecords(int limit) { List<Map<String,Object>> list=new ArrayList<>();String sql="SELECT * FROM health_records WHERE username=? ORDER BY record_date DESC, id DESC LIMIT ?";try(Connection conn=getConnection();PreparedStatement ps=conn.prepareStatement(sql)){ps.setString(1,currentUsername);ps.setInt(2,limit);ResultSet rs=ps.executeQuery();while(rs.next()){Map<String,Object> map=new HashMap<>();map.put("record_date",rs.getDate("record_date"));map.put("weight",rs.getDouble("weight"));map.put("body_fat",rs.getDouble("body_fat"));map.put("water_rate",rs.getDouble("water_rate"));map.put("muscle_rate",rs.getDouble("muscle_rate"));map.put("visceral_fat",rs.getInt("visceral_fat"));map.put("bone_muscle",rs.getDouble("bone_muscle"));map.put("bmr",rs.getDouble("bmr"));map.put("tdee",rs.getDouble("tdee"));map.put("bmi",rs.getDouble("bmi"));map.put("waist",rs.getDouble("waist"));map.put("body_age",rs.getInt("body_age"));try{map.put("body_type",rs.getString("body_type"));}catch(SQLException ex){map.put("body_type","--");}list.add(map);}}catch(SQLException e){e.printStackTrace();}return list; }
        static boolean isCheckedToday() { String sql="SELECT 1 FROM health_records WHERE username=? AND record_date=CURRENT_DATE";try(Connection conn=getConnection();PreparedStatement ps=conn.prepareStatement(sql)){ps.setString(1,currentUsername);return ps.executeQuery().next();}catch(SQLException e){return false;} }
        static boolean saveExerciseRecord(String type,int duration,String intensity,int calories) { String sql="INSERT INTO exercise_records(username,exercise_type,duration,intensity,calories_burned) VALUES(?,?,?,?,?)";try(Connection conn=getConnection();PreparedStatement ps=conn.prepareStatement(sql)){ps.setString(1,currentUsername);ps.setString(2,type);ps.setInt(3,duration);ps.setString(4,intensity);ps.setInt(5,calories);return ps.executeUpdate()>0;}catch(SQLException e){e.printStackTrace();return false;} }
        static int getTodayExerciseCalories() { String sql="SELECT COALESCE(SUM(calories_burned),0) FROM exercise_records WHERE username=? AND record_date=CURRENT_DATE";try(Connection conn=getConnection();PreparedStatement ps=conn.prepareStatement(sql)){ps.setString(1,currentUsername);ResultSet rs=ps.executeQuery();if(rs.next())return rs.getInt(1);}catch(SQLException e){e.printStackTrace();}return 0; }
        static List<String[]> getTodayExerciseList() { List<String[]> list=new ArrayList<>();String sql="SELECT exercise_type,duration,intensity,calories_burned FROM exercise_records WHERE username=? AND record_date=CURRENT_DATE ORDER BY id DESC";try(Connection conn=getConnection();PreparedStatement ps=conn.prepareStatement(sql)){ps.setString(1,currentUsername);ResultSet rs=ps.executeQuery();while(rs.next()){list.add(new String[]{rs.getString("exercise_type"),rs.getInt("duration")+"分钟",rs.getString("intensity"),rs.getInt("calories_burned")+"kcal"});}}catch(SQLException e){e.printStackTrace();}return list; }
        static boolean saveDietRecord(String mealType,String foodName,int calories,double protein,double carbs,double fat) { String sql="INSERT INTO diet_records(username,meal_type,food_name,calories,protein,carbs,fat) VALUES(?,?,?,?,?,?,?)";try(Connection conn=getConnection();PreparedStatement ps=conn.prepareStatement(sql)){ps.setString(1,currentUsername);ps.setString(2,mealType);ps.setString(3,foodName);ps.setInt(4,calories);ps.setDouble(5,protein);ps.setDouble(6,carbs);ps.setDouble(7,fat);return ps.executeUpdate()>0;}catch(SQLException e){e.printStackTrace();return false;} }
        static int[] getTodayDietSummary() { String sql="SELECT COALESCE(SUM(calories),0),COALESCE(SUM(protein),0),COALESCE(SUM(carbs),0),COALESCE(SUM(fat),0) FROM diet_records WHERE username=? AND record_date=CURRENT_DATE";try(Connection conn=getConnection();PreparedStatement ps=conn.prepareStatement(sql)){ps.setString(1,currentUsername);ResultSet rs=ps.executeQuery();if(rs.next()){return new int[]{rs.getInt(1),(int)(rs.getDouble(2)*100),(int)(rs.getDouble(3)*100),(int)(rs.getDouble(4)*100)};}}catch(SQLException e){e.printStackTrace();}return new int[]{0,0,0,0}; }
        static List<String[]> getAllFoods() { List<String[]> list=new ArrayList<>();String sql="SELECT food_name,calories,protein,carbs,fat FROM foods ORDER BY id";try(Connection conn=getConnection();PreparedStatement ps=conn.prepareStatement(sql)){ResultSet rs=ps.executeQuery();while(rs.next()){list.add(new String[]{rs.getString("food_name"),rs.getInt("calories")+"",df2.format(rs.getDouble("protein")),df2.format(rs.getDouble("carbs")),df2.format(rs.getDouble("fat"))});}}catch(SQLException e){e.printStackTrace();}return list; }
        static boolean saveGoal(String goalType,double targetValue) { String checkSql="SELECT id FROM goals WHERE username=?";String updateSql="UPDATE goals SET goal_type=?,target_value=?,start_date=CURRENT_DATE,end_date=NULL,current_stage=1 WHERE username=?";String insertSql="INSERT INTO goals(username,goal_type,target_value) VALUES(?,?,?)";try(Connection conn=getConnection()){PreparedStatement checkPs=conn.prepareStatement(checkSql);checkPs.setString(1,currentUsername);if(checkPs.executeQuery().next()){PreparedStatement ps=conn.prepareStatement(updateSql);ps.setString(1,goalType);ps.setDouble(2,targetValue);ps.setString(3,currentUsername);return ps.executeUpdate()>0;}else{PreparedStatement ps=conn.prepareStatement(insertSql);ps.setString(1,currentUsername);ps.setString(2,goalType);ps.setDouble(3,targetValue);return ps.executeUpdate()>0;}}catch(SQLException e){e.printStackTrace();return false;} }
        static Map<String,Object> getGoal() { String sql="SELECT * FROM goals WHERE username=? ORDER BY id DESC LIMIT 1";try(Connection conn=getConnection();PreparedStatement ps=conn.prepareStatement(sql)){ps.setString(1,currentUsername);ResultSet rs=ps.executeQuery();if(rs.next()){Map<String,Object> map=new HashMap<>();map.put("goal_type",rs.getString("goal_type"));map.put("target_value",rs.getDouble("target_value"));map.put("start_date",rs.getDate("start_date"));map.put("end_date",rs.getDate("end_date"));map.put("current_stage",rs.getInt("current_stage"));return map;}}catch(SQLException e){e.printStackTrace();}return null; }
        static void checkAndGrantAchievements() { try(Connection conn=getConnection()){int streak=getCheckInStreak(conn);if(streak>=7)grantBadge(conn,"毅力之星");if(streak>=30)grantBadge(conn,"坚持达人");int dietDays=getDietDays(conn);if(dietDays>=30)grantBadge(conn,"美食家");int exerciseCount=getExerciseCount(conn);if(exerciseCount>=20)grantBadge(conn,"运动健将");Map<String,Object> latest=getLatestHealthRecord();if(latest!=null){int score=HealthCalculator.calcHealthScore((double)latest.get("bmi"),(double)latest.get("body_fat"),(int)latest.get("visceral_fat"),(double)latest.get("muscle_rate"),(double)latest.get("water_rate"),currentGender);if(score>=90)grantBadge(conn,"健康标兵");}}catch(SQLException e){e.printStackTrace();} }
        private static int getCheckInStreak(Connection conn) throws SQLException { String sql="SELECT COUNT(DISTINCT record_date) FROM health_records WHERE username=? AND record_date>=CURRENT_DATE-INTERVAL '30 days'";PreparedStatement ps=conn.prepareStatement(sql);ps.setString(1,currentUsername);ResultSet rs=ps.executeQuery();return rs.next()?rs.getInt(1):0; }
        private static int getDietDays(Connection conn) throws SQLException { String sql="SELECT COUNT(DISTINCT record_date) FROM diet_records WHERE username=?";PreparedStatement ps=conn.prepareStatement(sql);ps.setString(1,currentUsername);ResultSet rs=ps.executeQuery();return rs.next()?rs.getInt(1):0; }
        private static int getExerciseCount(Connection conn) throws SQLException { String sql="SELECT COUNT(*) FROM exercise_records WHERE username=?";PreparedStatement ps=conn.prepareStatement(sql);ps.setString(1,currentUsername);ResultSet rs=ps.executeQuery();return rs.next()?rs.getInt(1):0; }
        private static void grantBadge(Connection conn,String badgeName) throws SQLException { String checkSql="SELECT 1 FROM achievements WHERE username=? AND badge_name=?";PreparedStatement checkPs=conn.prepareStatement(checkSql);checkPs.setString(1,currentUsername);checkPs.setString(2,badgeName);if(!checkPs.executeQuery().next()){String insertSql="INSERT INTO achievements(username,badge_name) VALUES(?,?)";PreparedStatement insertPs=conn.prepareStatement(insertSql);insertPs.setString(1,currentUsername);insertPs.setString(2,badgeName);insertPs.executeUpdate();} }
        static List<String[]> getAchievements() { List<String[]> list=new ArrayList<>();String sql="SELECT badge_name,achieved_date FROM achievements WHERE username=? ORDER BY achieved_date DESC";try(Connection conn=getConnection();PreparedStatement ps=conn.prepareStatement(sql)){ps.setString(1,currentUsername);ResultSet rs=ps.executeQuery();while(rs.next()){list.add(new String[]{rs.getString("badge_name"),rs.getDate("achieved_date").toString()});}}catch(SQLException e){e.printStackTrace();}return list; }
        static boolean saveAIReport(String reportType,String content) { String sql="INSERT INTO ai_reports(username,report_type,report_content) VALUES(?,?,?)";try(Connection conn=getConnection();PreparedStatement ps=conn.prepareStatement(sql)){ps.setString(1,currentUsername);ps.setString(2,reportType);ps.setString(3,content);return ps.executeUpdate()>0;}catch(SQLException e){e.printStackTrace();return false;} }
        static List<String[]> getAIReports() { List<String[]> list=new ArrayList<>();String sql="SELECT id,report_type,generated_at FROM ai_reports WHERE username=? ORDER BY generated_at DESC";try(Connection conn=getConnection();PreparedStatement ps=conn.prepareStatement(sql)){ps.setString(1,currentUsername);ResultSet rs=ps.executeQuery();SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm");while(rs.next()){list.add(new String[]{String.valueOf(rs.getInt("id")),rs.getString("report_type"),sdf.format(rs.getTimestamp("generated_at"))});}}catch(SQLException e){e.printStackTrace();}return list; }
        static String getAIReportContent(int reportId) { String sql="SELECT report_content FROM ai_reports WHERE id=?";try(Connection conn=getConnection();PreparedStatement ps=conn.prepareStatement(sql)){ps.setInt(1,reportId);ResultSet rs=ps.executeQuery();if(rs.next())return rs.getString("report_content");}catch(SQLException e){e.printStackTrace();}return ""; }
    }

    // === HealthCalculator ===
    static class HealthCalculator {
        static double calcBMI(double w,double h){return w/((h/100)*(h/100));}
        static String classifyBMI(double b){if(b<18.5)return"偏瘦";if(b<24)return"正常";if(b<28)return"超重";return"肥胖";}
        static double calcBMR_Harris(double w,double h,int a,String g){return"男".equals(g)?88.362+13.397*w+4.799*h-5.677*a:447.593+9.247*w+3.098*h-4.330*a;}
        static double calcBMR_Mifflin(double w,double h,int a,String g){double b=10*w+6.25*h-5*a;return"男".equals(g)?b+5:b-161;}
        static double calcBMR_China(double w,int a,String g){double b=w*24-a*5;return"男".equals(g)?b+100:b-100;}
        static double calcAvgBMR(double w,double h,int a,String g){return(calcBMR_Harris(w,h,a,g)+calcBMR_Mifflin(w,h,a,g)+calcBMR_China(w,a,g))/3.0;}
        static double getActivityFactor(String l){switch(l){case"久坐":return 1.2;case"轻度活动":return 1.375;case"中度活动":return 1.55;case"重度活动":return 1.725;case"极重度活动":return 1.9;default:return 1.2;}}
        static double calcTDEE(double bmr,String al){return bmr*getActivityFactor(al);}
        static String assessVisceralFat(int l){if(l<=4)return"正常";if(l<=8)return"偏高";return"过高";}
        static String assessMuscle(double bm,double w,String g){double s="男".equals(g)?w*0.42:w*0.36;double r=bm/s;if(r<0.9)return"偏低";if(r>1.1)return"偏高";return"正常";}
        static int calcBodyAge(int age,double bf,double mr,int vf,String g){int ba=age;boolean m="男".equals(g);if((m&&bf<15)||(!m&&bf<22))ba-=5;else if((m&&bf>25)||(!m&&bf>32))ba+=5;double sm=m?40.0:35.0;if(mr>sm*1.1)ba-=3;else if(mr<sm*0.9)ba+=3;if(vf<=4)ba-=2;else if(vf<=8)ba+=2;else ba+=5;return Math.max(20,Math.min(60,ba));}
        static String classifyBodyType(double bmi,double bf,String g){boolean m="男".equals(g);boolean fl=m?bf<12:bf<20;boolean fh=m?bf>25:bf>32;if(bmi<18.5)return fh?"隐性肥胖型":"消瘦型";else if(bmi<24)return fh?"隐性肥胖型":"标准型";else if(bmi<28){if(fl)return"肌肉型";return fh?"肥胖型":"超重型";}else return fl?"肌肉型":"肥胖型";}
        static double calcIdealWeight(double h){double m=h/100;return m*m*22;}
        static String assessWHtR(double waist,double height){double w=waist/height;if(w<0.40)return"偏瘦";if(w<0.50)return"正常";if(w<0.55)return"腹型肥胖风险";return"腹型肥胖";}
        static String classifyBodyShape(double waist,String g){boolean m="男".equals(g);if((m&&waist>=90)||(!m&&waist>=85))return"苹果型(中心性肥胖)";if((m&&waist<85)||(!m&&waist<80))return"梨型/标准型";return"轻度腹型肥胖";}
        static int calcHealthScore(double bmi,double bf,int vf,double mr,double wr,String g){boolean m="男".equals(g);int s=0;if(bmi>=18.5&&bmi<24)s+=30;else if((bmi>=24&&bmi<28)||(bmi>=17&&bmi<18.5))s+=20;else s+=10;boolean fn=m?(bf>=12&&bf<=25):(bf>=20&&bf<=32);boolean fs=m?bf>30:bf>38;if(fn)s+=25;else if(fs)s+=5;else s+=15;if(vf<=4)s+=20;else if(vf<=8)s+=12;else s+=5;double sm=m?40.0:35.0;if(mr>=sm*0.9&&mr<=sm*1.1)s+=15;else if(mr>sm*1.1)s+=15;else s+=8;boolean wn=m?(wr>=50&&wr<=65):(wr>=45&&wr<=60);if(wn)s+=10;else s+=5;return s;}
        static String scoreLevel(int s){if(s>=90)return"优秀";if(s>=75)return"良好";if(s>=60)return"及格";return"需改善";}
        static double getMET(String t){switch(t){case"快走":return 3.5;case"跑步":return 9.0;case"游泳":return 7.0;case"力量训练":return 5.0;case"骑行":return 7.5;case"瑜伽":return 3.0;case"跳绳":return 12.0;case"球类":return 6.0;default:return 5.0;}}
        static int calcExerciseCalories(String t,int d,String i,double w){double met=getMET(t);double h=d/60.0;double f="低".equals(i)?0.85:"高".equals(i)?1.15:1.0;return(int)(met*w*h*f);}
        static double predictTrend(List<Date> dates,List<Double> values,int futureDays){int n=dates.size();if(n<3)return Double.NaN;long bt=dates.get(0).getTime();double sx=0,sy=0,sxy=0,sx2=0;for(int i=0;i<n;i++){double xi=(dates.get(i).getTime()-bt)/86400000.0;double yi=values.get(i);sx+=xi;sy+=yi;sxy+=xi*yi;sx2+=xi*xi;}double den=n*sx2-sx*sx;if(Math.abs(den)<1e-10)return values.get(n-1);double k=(n*sxy-sx*sy)/den;double b=(sy-k*sx)/n;double lx=(dates.get(n-1).getTime()-bt)/86400000.0;return k*(lx+futureDays)+b;}
        static String trendDirection(List<Date> dates,List<Double> values){int n=dates.size();if(n<3)return"数据不足";long bt=dates.get(0).getTime();double sx=0,sy=0,sxy=0,sx2=0;for(int i=0;i<n;i++){double xi=(dates.get(i).getTime()-bt)/86400000.0;double yi=values.get(i);sx+=xi;sy+=yi;sxy+=xi*yi;sx2+=xi*xi;}double den=n*sx2-sx*sx;if(Math.abs(den)<1e-10)return"趋于稳定";double k=(n*sxy-sx*sy)/den;if(Math.abs(k)<0.01)return"趋于稳定";return k>0?"上升":"下降";}
        static int predictGoalDays(double cw,double tw,double dcd,String gt){if(Math.abs(dcd)<100)return-2;double diff=cw-tw;if("减脂".equals(gt)||"减重".equals(gt)){if(diff<=0)return 0;if(dcd<=0)return-1;return(int)Math.ceil((diff*7700)/dcd);}else if("增肌".equals(gt)){if(diff>=0)return 0;return(int)Math.ceil((Math.abs(diff)*5500)/Math.abs(dcd));}return-1;}
        static String assessRisk(double p){if(p>=28)return"高风险 — 预测30天后BMI将进入肥胖区间, 建议立即调整饮食和运动计划";if(p>=24||p<18.5)return"中风险 — 预测30天后BMI偏离正常范围, 需关注体重变化";return"低风险 — 预测30天后BMI维持在正常范围, 继续保持";}
    }

    // === LoginFrame (v3.0 split-screen glass design) ===
    static class LoginFrame extends JFrame {
        private JTextField tfUsername = new JTextField(15);
        private JPasswordField pfPassword = new JPasswordField(15);
        private JTextField tfRegUsername = new JTextField(15);
        private JPasswordField pfRegPassword = new JPasswordField(15);
        private JPasswordField pfRegPassword2 = new JPasswordField(15);
        private JComboBox<String> cbGender = new JComboBox<>(new String[]{"男","女"});
        private JSpinner spAge = new JSpinner(new SpinnerNumberModel(25,5,120,1));
        private JTextField tfHeight = new JTextField("170");
        private JComboBox<String> cbActivity = new JComboBox<>(new String[]{"久坐","轻度活动","中度活动","重度活动","极重度活动"});
        LoginFrame() {
            setTitle("BMI 体质评估与预测系统 v3.0 — Aurora");
            setSize(900,580); setDefaultCloseOperation(EXIT_ON_CLOSE);
            setLocationRelativeTo(null); setResizable(false); setUndecorated(true);
            JPanel root = new JPanel(new BorderLayout()); root.setBackground(Theme.CARD_BG);
            // Left: gradient brand panel
            TriGradientPanel leftPanel = new TriGradientPanel(new BorderLayout(), Theme.GRAD_LOGIN);
            leftPanel.setPreferredSize(new Dimension(380,580));
            leftPanel.setBorder(BorderFactory.createEmptyBorder(60,40,40,40));
            JPanel logoCircle = new JPanel(){protected void paintComponent(Graphics g){Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g2.setColor(new Color(255,255,255,40));g2.fillOval(0,0,56,56);g2.setColor(new Color(255,255,255,80));g2.drawOval(0,0,56,56);g2.setColor(Color.WHITE);g2.setFont(new Font(Theme.FONT_NAME,Font.BOLD,26));FontMetrics fm=g2.getFontMetrics();String h="♥";g2.drawString(h,(56-fm.stringWidth(h))/2,38);g2.dispose();}};
            logoCircle.setPreferredSize(new Dimension(56,56)); logoCircle.setOpaque(false);
            leftPanel.add(logoCircle, BorderLayout.NORTH);
            JPanel centerText = new JPanel(); centerText.setLayout(new BoxLayout(centerText,BoxLayout.Y_AXIS)); centerText.setOpaque(false);
            JLabel lblT1=new JLabel("BMI 体质评估"); lblT1.setFont(new Font(Theme.FONT_NAME,Font.BOLD,28)); lblT1.setForeground(Color.WHITE); lblT1.setAlignmentX(LEFT_ALIGNMENT);
            JLabel lblT2=new JLabel("与预测系统"); lblT2.setFont(new Font(Theme.FONT_NAME,Font.BOLD,28)); lblT2.setForeground(Color.WHITE); lblT2.setAlignmentX(LEFT_ALIGNMENT);
            JLabel lblSlogan=new JLabel("Aurora Design System v3.0"); lblSlogan.setFont(Theme.FONT_SMALL); lblSlogan.setForeground(new Color(255,255,255,180)); lblSlogan.setBorder(BorderFactory.createEmptyBorder(16,0,0,0)); lblSlogan.setAlignmentX(LEFT_ALIGNMENT);
            centerText.add(lblT1); centerText.add(lblT2); centerText.add(lblSlogan);
            leftPanel.add(centerText, BorderLayout.CENTER);
            JLabel lblVer=new JLabel("© 2026 Health System · Aurora Edition"); lblVer.setFont(Theme.FONT_TINY); lblVer.setForeground(new Color(255,255,255,120));
            leftPanel.add(lblVer, BorderLayout.SOUTH);
            root.add(leftPanel, BorderLayout.WEST);
            // Right: form area
            JPanel rightPanel = new JPanel(new BorderLayout()); rightPanel.setBackground(Theme.CARD_BG); rightPanel.setBorder(BorderFactory.createEmptyBorder(40,40,40,40));
            JButton btnClose=new JButton("✕"); btnClose.setFont(new Font(Theme.FONT_NAME,Font.PLAIN,16)); btnClose.setForeground(Theme.TEXT_GRAY); btnClose.setContentAreaFilled(false); btnClose.setBorderPainted(false); btnClose.setFocusPainted(false); btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR)); btnClose.setBorder(BorderFactory.createEmptyBorder(4,8,4,8));
            btnClose.addActionListener(e->System.exit(0));
            btnClose.addMouseListener(new MouseAdapter(){public void mouseEntered(MouseEvent e){btnClose.setForeground(Theme.DANGER);}public void mouseExited(MouseEvent e){btnClose.setForeground(Theme.TEXT_GRAY);}});
            JPanel closePanel=new JPanel(new FlowLayout(FlowLayout.RIGHT,0,0)); closePanel.setOpaque(false); closePanel.add(btnClose);
            rightPanel.add(closePanel, BorderLayout.NORTH);
            JTabbedPane tabPane=new JTabbedPane(); tabPane.setFont(Theme.FONT_BODY_B); tabPane.setBackground(Theme.CARD_BG); tabPane.setForeground(Theme.TEXT_DARK); tabPane.setBorder(null);
            tabPane.addTab("  登 录  ", createLoginPanel());
            tabPane.addTab("  注 册  ", createRegisterPanel());
            rightPanel.add(tabPane, BorderLayout.CENTER);
            root.add(rightPanel, BorderLayout.CENTER);
            add(root);
            FadeIn.apply(root, 400);
        }
        private JPanel createLoginPanel() {
            JPanel panel=new JPanel(new GridBagLayout()); panel.setOpaque(false);
            GridBagConstraints gbc=new GridBagConstraints(); gbc.insets=new Insets(6,8,6,8); gbc.fill=GridBagConstraints.HORIZONTAL;
            JLabel lblHint=new JLabel("  欢迎回来，请登录您的账号"); lblHint.setFont(Theme.FONT_SMALL); lblHint.setForeground(Theme.TEXT_GRAY);
            gbc.gridx=0; gbc.gridy=0; gbc.gridwidth=2; gbc.insets=new Insets(4,8,16,8); panel.add(lblHint,gbc);
            gbc.insets=new Insets(6,8,6,8); gbc.gridwidth=1;
            JLabel labUser=new JLabel("  用户名"); labUser.setFont(Theme.FONT_SMALL); labUser.setForeground(Theme.TEXT_GRAY);
            gbc.gridx=0; gbc.gridy=1; gbc.gridwidth=2; panel.add(labUser,gbc);
            Theme.styleTextField(tfUsername); gbc.gridx=0; gbc.gridy=2; gbc.gridwidth=2; panel.add(tfUsername,gbc); gbc.gridwidth=1;
            JLabel labPass=new JLabel("  密码"); labPass.setFont(Theme.FONT_SMALL); labPass.setForeground(Theme.TEXT_GRAY);
            gbc.gridx=0; gbc.gridy=3; gbc.gridwidth=2; panel.add(labPass,gbc);
            Theme.stylePasswordField(pfPassword); gbc.gridx=0; gbc.gridy=4; gbc.gridwidth=2; panel.add(pfPassword,gbc); gbc.gridwidth=1;
            RoundButton btnLogin=new RoundButton("登  录",Theme.PRIMARY,Theme.PRIMARY_D).setGradient(true);
            btnLogin.addActionListener(e->doLogin());
            gbc.gridx=0; gbc.gridy=5; gbc.gridwidth=2; gbc.fill=GridBagConstraints.HORIZONTAL; gbc.insets=new Insets(16,8,6,8); panel.add(btnLogin,gbc);
            ActionListener loginAction=e->doLogin(); tfUsername.addActionListener(loginAction); pfPassword.addActionListener(loginAction);
            return panel;
        }
        private JPanel createRegisterPanel() {
            JPanel panel=new JPanel(new GridBagLayout()); panel.setOpaque(false);
            GridBagConstraints gbc=new GridBagConstraints(); gbc.insets=new Insets(4,8,4,8); gbc.fill=GridBagConstraints.HORIZONTAL; gbc.anchor=GridBagConstraints.WEST;
            Theme.styleTextField(tfRegUsername); Theme.stylePasswordField(pfRegPassword); Theme.stylePasswordField(pfRegPassword2); Theme.styleComboBox(cbGender); Theme.styleComboBox(cbActivity); Theme.styleSpinner(spAge); Theme.styleTextField(tfHeight);
            int row=0; String[][] fields={{"  用户名"},{"  密码"},{"  确认密码"},{"  性别"},{"  年龄"},{"  身高 (cm)"},{"  活动等级"}};
            Component[] comps={tfRegUsername,pfRegPassword,pfRegPassword2,cbGender,spAge,tfHeight,cbActivity};
            for(int i=0;i<fields.length;i++){
                JLabel lab=new JLabel(fields[i][0]); lab.setFont(Theme.FONT_SMALL); lab.setForeground(Theme.TEXT_GRAY);
                gbc.gridx=0; gbc.gridy=row++; gbc.gridwidth=2; panel.add(lab,gbc); gbc.gridwidth=1;
                gbc.gridx=0; gbc.gridy=row++; gbc.gridwidth=2; panel.add(comps[i],gbc); gbc.gridwidth=1;
            }
            RoundButton btnRegister=new RoundButton("注  册",Theme.ACCENT,Theme.ACCENT_D).setGradient(true);
            btnRegister.addActionListener(e->doRegister());
            gbc.gridx=0; gbc.gridy=row; gbc.gridwidth=2; gbc.fill=GridBagConstraints.HORIZONTAL; gbc.insets=new Insets(12,8,4,8); panel.add(btnRegister,gbc);
            return panel;
        }
        private void doLogin() {
            String username=tfUsername.getText().trim(); String password=new String(pfPassword.getPassword());
            if(username.isEmpty()||password.isEmpty()){JOptionPane.showMessageDialog(this,"请输入用户名和密码","提示",JOptionPane.WARNING_MESSAGE);return;}
            if(DBUtil.loginUser(username,password)){
                dispose();
                try{MainFrame frame=new MainFrame(); frame.setVisible(true); FadeIn.apply(frame.getRootPane(),500);}
                catch(Exception ex){ex.printStackTrace(); JOptionPane.showMessageDialog(null,"主界面启动失败: "+ex.getMessage(),"错误",JOptionPane.ERROR_MESSAGE); new LoginFrame().setVisible(true);}
            }else{JOptionPane.showMessageDialog(this,"用户名或密码错误","登录失败",JOptionPane.ERROR_MESSAGE);}
        }
        private void doRegister() {
            String username=tfRegUsername.getText().trim(); String pass1=new String(pfRegPassword.getPassword()); String pass2=new String(pfRegPassword2.getPassword());
            String gender=(String)cbGender.getSelectedItem(); int age=(int)spAge.getValue(); String heightStr=tfHeight.getText().trim(); String activity=(String)cbActivity.getSelectedItem();
            if(username.isEmpty()||pass1.isEmpty()){JOptionPane.showMessageDialog(this,"用户名和密码不能为空","提示",JOptionPane.WARNING_MESSAGE);return;}
            if(!pass1.equals(pass2)){JOptionPane.showMessageDialog(this,"两次密码不一致","提示",JOptionPane.WARNING_MESSAGE);return;}
            double height; try{height=Double.parseDouble(heightStr); if(height<100||height>250){JOptionPane.showMessageDialog(this,"身高范围 100-250cm","提示",JOptionPane.WARNING_MESSAGE);return;}}
            catch(NumberFormatException e){JOptionPane.showMessageDialog(this,"身高请输入数字","提示",JOptionPane.WARNING_MESSAGE);return;}
            if(DBUtil.registerUser(username,pass1,gender,age,height,activity)){JOptionPane.showMessageDialog(this,"注册成功！请登录"); tfUsername.setText(username);}
        }
    }

    // === MainFrame (v3.0 sidebar + CardLayout) ===
    static class MainFrame extends JFrame {
        private JLabel lblUserInfo, lblScore, lblCalorieDiff, lblCheckStatus;
        private CardLayout cardLayout; private JPanel cardPanel;
        MainFrame() {
            setTitle("BMI 体质评估与预测系统 v3.0 — Aurora");
            setSize(1200,760); setMinimumSize(new Dimension(1000,680));
            setDefaultCloseOperation(EXIT_ON_CLOSE); setLocationRelativeTo(null);
            initUI();
            SwingUtilities.invokeLater(()->{
                if(!DBUtil.isCheckedToday()) JOptionPane.showMessageDialog(this,"今天称重了吗？记得打卡记录健康数据！","每日提醒",JOptionPane.INFORMATION_MESSAGE);
                checkAbnormalData();
            });
        }
        private void initUI() {
            setLayout(new BorderLayout());
            // Top bar
            GradientPanel topPanel=new GradientPanel(new BorderLayout(),Theme.GRAD_PRIMARY);
            topPanel.setBorder(BorderFactory.createEmptyBorder(10,18,10,18));
            JPanel leftPanel=new JPanel(new FlowLayout(FlowLayout.LEFT,12,0)); leftPanel.setOpaque(false);
            JLabel logo=new JLabel("♥"){protected void paintComponent(Graphics g){Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g2.setColor(new Color(255,255,255,30));g2.fillOval(0,0,32,32);g2.setColor(Color.WHITE);g2.setFont(new Font(Theme.FONT_NAME,Font.BOLD,16));FontMetrics fm=g2.getFontMetrics();String h="♥";g2.drawString(h,(32-fm.stringWidth(h))/2,22);g2.dispose();}};
            logo.setPreferredSize(new Dimension(32,32)); leftPanel.add(logo);
            lblUserInfo=new JLabel(); lblUserInfo.setFont(Theme.FONT_BODY); lblUserInfo.setForeground(new Color(255,255,255,220));
            leftPanel.add(lblUserInfo); topPanel.add(leftPanel,BorderLayout.WEST);
            lblScore=new JLabel("健康评分: --",SwingConstants.RIGHT); lblScore.setFont(Theme.FONT_BODY_B); lblScore.setForeground(new Color(255,255,255,240));
            topPanel.add(lblScore,BorderLayout.EAST); add(topPanel,BorderLayout.NORTH);
            // Sidebar + Content
            NavItem[] navItems={
                new NavItem("📊","数据录入"), new NavItem("📈","历史趋势"),
                new NavItem("🔬","分析评估"), new NavItem("🔮","预测分析"),
                new NavItem("🎯","目标计划"), new NavItem("🍎","饮食管理"),
                new NavItem("🏆","成就徽章"), new NavItem("📋","数据大屏")
            };
            SidebarNav sidebar=new SidebarNav(this, navItems);
            add(sidebar, BorderLayout.WEST);
            cardLayout=new CardLayout(); cardPanel=new JPanel(cardLayout);
            cardPanel.setBackground(Theme.BG); cardPanel.setBorder(BorderFactory.createEmptyBorder(10,12,6,12));
            cardPanel.add(new DataInputPanel(this),"0"); cardPanel.add(new HistoryTrendPanel(),"1");
            cardPanel.add(new AnalysisPanel(),"2"); cardPanel.add(new PredictionPanel(),"3");
            cardPanel.add(new GoalPlanPanel(),"4"); cardPanel.add(new DietPanel(this),"5");
            cardPanel.add(new AchievementPanel(),"6"); cardPanel.add(new DashboardPanel(),"7");
            add(cardPanel,BorderLayout.CENTER);
            // Bottom status bar
            JPanel bottomPanel=new JPanel(new BorderLayout()); bottomPanel.setBackground(Theme.FOOTER_BG);
            bottomPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1,0,0,0,Theme.BORDER),BorderFactory.createEmptyBorder(6,18,6,18)));
            lblCalorieDiff=new JLabel("今日热量差: --"); lblCalorieDiff.setFont(Theme.FONT_SMALL); lblCalorieDiff.setForeground(Theme.TEXT_BODY);
            bottomPanel.add(lblCalorieDiff,BorderLayout.WEST);
            lblCheckStatus=new JLabel("打卡状态: --",SwingConstants.RIGHT); lblCheckStatus.setFont(Theme.FONT_SMALL); lblCheckStatus.setForeground(Theme.TEXT_BODY);
            bottomPanel.add(lblCheckStatus,BorderLayout.EAST);
            add(bottomPanel,BorderLayout.SOUTH);
            refreshData();
        }
        void refreshData() {
            lblUserInfo.setText(String.format("👤 %s  |  %s  |  %d岁  |  %.1fcm  |  %s",currentUsername,currentGender,currentAge,currentHeight,currentActivityLevel));
            Map<String,Object> latest=DBUtil.getLatestHealthRecord();
            if(latest!=null){
                int score=HealthCalculator.calcHealthScore((double)latest.get("bmi"),(double)latest.get("body_fat"),(int)latest.get("visceral_fat"),(double)latest.get("muscle_rate"),(double)latest.get("water_rate"),currentGender);
                lblScore.setText("健康评分: "+score+"/100 ("+HealthCalculator.scoreLevel(score)+")");
            }else{lblScore.setText("健康评分: 暂无数据");}
            if(latest!=null){
                double tdee=(double)latest.get("tdee"); int exerciseCal=DBUtil.getTodayExerciseCalories(); int[] diet=DBUtil.getTodayDietSummary(); int intakeCal=diet[0];
                int diff=(int)(tdee+exerciseCal-intakeCal);
                lblCalorieDiff.setText("今日热量差: "+(diff>=0?"+":"")+diff+" kcal (消耗:"+(int)(tdee+exerciseCal)+" / 摄入:"+intakeCal+")");
            }else{lblCalorieDiff.setText("今日热量差: 暂无数据");}
            lblCheckStatus.setText("打卡状态: "+(DBUtil.isCheckedToday()?"已打卡 ✓":"未打卡"));
            DBUtil.checkAndGrantAchievements();
        }
        void switchToTab(int index){cardLayout.show(cardPanel,String.valueOf(index));}
        private void checkAbnormalData(){
            List<Map<String,Object>> records=DBUtil.getHealthRecords(7);
            if(records.size()>=2){
                double latestWeight=(double)records.get(0).get("weight"); double oldestWeight=(double)records.get(records.size()-1).get("weight");
                if(Math.abs(latestWeight-oldestWeight)>5) JOptionPane.showMessageDialog(this,"⚠️ 一周内体重变化超过 5kg，建议就医检查！","异常数据预警",JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    // === DataInputPanel ===
    static class DataInputPanel extends JPanel {
        private MainFrame mainFrame;
        private JTextField tfWeight=new JTextField("65.0"),tfBodyFat=new JTextField("20.0"),tfWater=new JTextField("55.0"),tfMuscle=new JTextField("35.0"),tfVisceral=new JTextField("5"),tfBoneMuscle=new JTextField("28.0"),tfWaist=new JTextField("80.0");
        private JComboBox<String> cbExerciseType=new JComboBox<>(new String[]{"跑步","游泳","力量训练","骑行","瑜伽","跳绳","快走","球类"});
        private JTextField tfDuration=new JTextField("30");
        private JComboBox<String> cbIntensity=new JComboBox<>(new String[]{"低","中","高"});
        private JTable exerciseTable; private DefaultTableModel exerciseModel;
        DataInputPanel(MainFrame frame){this.mainFrame=frame; setLayout(new BorderLayout(10,10)); setBorder(BorderFactory.createEmptyBorder(12,12,12,12)); setBackground(Theme.BG);
            // Health data input card
            RoundedPanel healthPanel=Theme.createCardPanel("健康数据录入",Theme.PRIMARY);
            healthPanel.setLayout(new GridBagLayout());
            GridBagConstraints gbc=new GridBagConstraints(); gbc.insets=new Insets(5,8,5,8); gbc.fill=GridBagConstraints.HORIZONTAL;
            RoundButton btnSimulate=new RoundButton("  📊 模拟称重  ",Theme.ACCENT,Theme.ACCENT_D).setGradient(true);
            btnSimulate.addActionListener(e->simulateWeighing());
            gbc.gridx=0; gbc.gridy=0; gbc.gridwidth=6; gbc.anchor=GridBagConstraints.CENTER; gbc.insets=new Insets(4,8,12,8); healthPanel.add(btnSimulate,gbc);
            gbc.insets=new Insets(5,8,5,8); gbc.gridwidth=1; gbc.anchor=GridBagConstraints.WEST;
            addField(healthPanel,gbc,1,"体重(kg)",tfWeight,0); addField(healthPanel,gbc,1,"体脂率(%)",tfBodyFat,2);
            addField(healthPanel,gbc,2,"水分率(%)",tfWater,0); addField(healthPanel,gbc,2,"肌肉率(%)",tfMuscle,2);
            addField(healthPanel,gbc,3,"内脏脂肪(级)",tfVisceral,0); addField(healthPanel,gbc,3,"骨骼肌肉量(kg)",tfBoneMuscle,2);
            addField(healthPanel,gbc,4,"腰围(cm)",tfWaist,0);
            Theme.styleTextField(tfWeight);Theme.styleTextField(tfBodyFat);Theme.styleTextField(tfWater);Theme.styleTextField(tfMuscle);Theme.styleTextField(tfVisceral);Theme.styleTextField(tfBoneMuscle);Theme.styleTextField(tfWaist);
            RoundButton btnSave=new RoundButton("  💾 保存健康记录  ",Theme.PRIMARY,Theme.PRIMARY_D).setGradient(true);
            btnSave.addActionListener(e->saveHealthData());
            gbc.gridx=4; gbc.gridy=4; gbc.gridwidth=2; gbc.insets=new Insets(8,8,4,8); healthPanel.add(btnSave,gbc);
            gbc.gridwidth=1; gbc.insets=new Insets(5,8,5,8);
            add(healthPanel,BorderLayout.NORTH);
            // Exercise record card
            RoundedPanel exercisePanel=Theme.createCardPanel("运动记录",Theme.ACCENT);
            JPanel exInputPanel=new JPanel(new FlowLayout(FlowLayout.LEFT,8,6)); exInputPanel.setOpaque(false);
            Theme.styleComboBox(cbExerciseType); Theme.styleTextField(tfDuration); Theme.styleComboBox(cbIntensity);
            JLabel lblEx=new JLabel("运动类型"); lblEx.setFont(Theme.FONT_SMALL); lblEx.setForeground(Theme.TEXT_GRAY);
            exInputPanel.add(lblEx); exInputPanel.add(cbExerciseType);
            lblEx=new JLabel("时长(分钟)"); lblEx.setFont(Theme.FONT_SMALL); lblEx.setForeground(Theme.TEXT_GRAY);
            exInputPanel.add(lblEx); exInputPanel.add(tfDuration);
            lblEx=new JLabel("强度"); lblEx.setFont(Theme.FONT_SMALL); lblEx.setForeground(Theme.TEXT_GRAY);
            exInputPanel.add(lblEx); exInputPanel.add(cbIntensity);
            RoundButton btnAddExercise=new RoundButton("记录运动",Theme.PRIMARY,Theme.PRIMARY_D).setGradient(true);
            btnAddExercise.addActionListener(e->addExercise()); exInputPanel.add(btnAddExercise);
            exercisePanel.add(exInputPanel,BorderLayout.NORTH);
            exerciseModel=new DefaultTableModel(new String[]{"运动类型","时长","强度","消耗热量"},0){public boolean isCellEditable(int r,int c){return false;}};
            exerciseTable=new JTable(exerciseModel); Theme.styleTable(exerciseTable);
            JScrollPane scrollEx=new JScrollPane(exerciseTable); scrollEx.setBorder(BorderFactory.createEmptyBorder(4,0,0,0)); scrollEx.getViewport().setBackground(Theme.CARD_BG);
            exercisePanel.add(scrollEx,BorderLayout.CENTER);
            add(exercisePanel,BorderLayout.CENTER);
            refreshExerciseTable();
        }
        private void addField(JPanel panel,GridBagConstraints gbc,int row,String label,JTextField field,int col){
            gbc.gridy=row; gbc.gridx=col;
            JLabel lbl=new JLabel(label); lbl.setFont(Theme.FONT_SMALL); lbl.setForeground(Theme.TEXT_GRAY); panel.add(lbl,gbc);
            gbc.gridx=col+1; field.setColumns(8); panel.add(field,gbc);
        }
        private void simulateWeighing(){
            Random rand=new Random();
            tfWeight.setText(df1.format(50+rand.nextDouble()*50)); tfBodyFat.setText(df1.format(10+rand.nextDouble()*30));
            tfWater.setText(df1.format(30+rand.nextDouble()*40)); tfMuscle.setText(df1.format(20+rand.nextDouble()*30));
            tfVisceral.setText(String.valueOf(1+rand.nextInt(10))); tfBoneMuscle.setText(df1.format(20+rand.nextDouble()*30));
            tfWaist.setText(df1.format(60+rand.nextDouble()*50));
            JOptionPane.showMessageDialog(this,"模拟称重完成! 数据已填入, 点击保存记录到数据库");
        }
        private void saveHealthData(){
            try{
                double weight=Double.parseDouble(tfWeight.getText().trim()),bodyFat=Double.parseDouble(tfBodyFat.getText().trim()),water=Double.parseDouble(tfWater.getText().trim()),muscle=Double.parseDouble(tfMuscle.getText().trim());
                int visceral=Integer.parseInt(tfVisceral.getText().trim());
                double boneMuscle=Double.parseDouble(tfBoneMuscle.getText().trim()),waist=Double.parseDouble(tfWaist.getText().trim());
                if(!validateData(weight,bodyFat,water,muscle,visceral,waist))return;
                if(DBUtil.saveHealthRecord(weight,bodyFat,water,muscle,visceral,boneMuscle,waist)){JOptionPane.showMessageDialog(this,"健康记录保存成功!"); mainFrame.refreshData();}
            }catch(NumberFormatException e){JOptionPane.showMessageDialog(this,"请输入有效的数字","输入错误",JOptionPane.ERROR_MESSAGE);}
        }
        private boolean validateData(double weight,double bodyFat,double water,double muscle,int visceral,double waist){
            if(weight<20||weight>300){JOptionPane.showMessageDialog(this,"体重输入异常, 请重新输入 (20-300kg)","校验失败",JOptionPane.WARNING_MESSAGE);return false;}
            if(bodyFat<3||bodyFat>60){JOptionPane.showMessageDialog(this,"体脂率输入异常, 请重新输入 (3-60%)","校验失败",JOptionPane.WARNING_MESSAGE);return false;}
            if(water<20||water>80){JOptionPane.showMessageDialog(this,"水分率输入异常, 请重新输入 (20-80%)","校验失败",JOptionPane.WARNING_MESSAGE);return false;}
            if(muscle<10||muscle>70){JOptionPane.showMessageDialog(this,"肌肉率输入异常, 请重新输入 (10-70%)","校验失败",JOptionPane.WARNING_MESSAGE);return false;}
            if(visceral<1||visceral>30){JOptionPane.showMessageDialog(this,"内脏脂肪等级异常, 请重新输入 (1-30)","校验失败",JOptionPane.WARNING_MESSAGE);return false;}
            if(waist<40||waist>200){JOptionPane.showMessageDialog(this,"腰围输入异常, 请重新输入 (40-200cm)","校验失败",JOptionPane.WARNING_MESSAGE);return false;}
            return true;
        }
        private void addExercise(){
            try{
                String type=(String)cbExerciseType.getSelectedItem(); int duration=Integer.parseInt(tfDuration.getText().trim()); String intensity=(String)cbIntensity.getSelectedItem();
                if(duration<=0||duration>=600){JOptionPane.showMessageDialog(this,"时长范围 1-600 分钟","提示",JOptionPane.WARNING_MESSAGE);return;}
                Map<String,Object> latest=DBUtil.getLatestHealthRecord(); double weight=latest!=null?(double)latest.get("weight"):65.0;
                int calories=HealthCalculator.calcExerciseCalories(type,duration,intensity,weight);
                if(DBUtil.saveExerciseRecord(type,duration,intensity,calories)){JOptionPane.showMessageDialog(this,"运动记录保存成功! 消耗 "+calories+" kcal"); refreshExerciseTable(); mainFrame.refreshData();}
            }catch(NumberFormatException e){JOptionPane.showMessageDialog(this,"请输入有效的数字","输入错误",JOptionPane.ERROR_MESSAGE);}
        }
        private void refreshExerciseTable(){exerciseModel.setRowCount(0); for(String[] row:DBUtil.getTodayExerciseList()) exerciseModel.addRow(row);}
    }

    // === HistoryTrendPanel ===
    static class HistoryTrendPanel extends JPanel {
        private JComboBox<String> cbMetric=new JComboBox<>(new String[]{"体重","体脂率","肌肉率","BMI","腰围"});
        private LineChartPanel chartPanel; private JTable historyTable; private DefaultTableModel historyModel;
        HistoryTrendPanel(){setLayout(new BorderLayout(10,10)); setBorder(BorderFactory.createEmptyBorder(12,12,12,12)); setBackground(Theme.BG);
            RoundedPanel ctrlCard=Theme.createCardPanel("趋势指标选择",Theme.PRIMARY);
            JPanel ctrlPanel=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); ctrlPanel.setOpaque(false);
            JLabel lblMetric=new JLabel("选择指标"); lblMetric.setFont(Theme.FONT_SMALL); lblMetric.setForeground(Theme.TEXT_GRAY); ctrlPanel.add(lblMetric);
            Theme.styleComboBox(cbMetric); ctrlPanel.add(cbMetric);
            RoundButton btnRefresh=new RoundButton("刷新趋势",Theme.PRIMARY,Theme.PRIMARY_D).setGradient(true);
            btnRefresh.addActionListener(e->refresh()); ctrlPanel.add(btnRefresh);
            ctrlCard.add(ctrlPanel,BorderLayout.CENTER); add(ctrlCard,BorderLayout.NORTH);
            RoundedPanel chartCard=Theme.createCardPanel("历史趋势图 (贝塞尔平滑曲线)",Theme.PRIMARY);
            chartPanel=new LineChartPanel(); chartPanel.setPreferredSize(new Dimension(700,300)); chartPanel.setMinimumSize(new Dimension(400,200));
            chartCard.add(chartPanel,BorderLayout.CENTER);
            RoundedPanel tableCard=Theme.createCardPanel("历史记录明细",Theme.ACCENT);
            historyModel=new DefaultTableModel(new String[]{"日期","体重","体脂率","水分率","肌肉率","内脏脂肪","BMI","腰围","身体年龄","体质分类"},0){public boolean isCellEditable(int r,int c){return false;}};
            historyTable=new JTable(historyModel); Theme.styleTable(historyTable);
            tableCard.add(new JScrollPane(historyTable),BorderLayout.CENTER); tableCard.setMinimumSize(new Dimension(400,120));
            JSplitPane splitPane=new JSplitPane(JSplitPane.VERTICAL_SPLIT,chartCard,tableCard);
            splitPane.setOpaque(false); splitPane.setDividerLocation(300); splitPane.setResizeWeight(0.7); splitPane.setBorder(BorderFactory.createEmptyBorder());
            add(splitPane,BorderLayout.CENTER);
            cbMetric.addActionListener(e->refresh()); refresh();
        }
        void refresh(){
            String metric=(String)cbMetric.getSelectedItem(); List<Map<String,Object>> records=DBUtil.getHealthRecords(60);
            List<Date> dates=new ArrayList<>(); List<Double> values=new ArrayList<>();
            for(int i=records.size()-1;i>=0;i--){Map<String,Object> r=records.get(i); dates.add((Date)r.get("record_date"));
                switch(metric){case "体重":values.add((double)r.get("weight"));break;case "体脂率":values.add((double)r.get("body_fat"));break;case "肌肉率":values.add((double)r.get("muscle_rate"));break;case "BMI":values.add((double)r.get("bmi"));break;case "腰围":values.add((double)r.get("waist"));break;}}
            chartPanel.setData(dates,values,metric);
            historyModel.setRowCount(0); SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
            for(Map<String,Object> r:records){
                historyModel.addRow(new Object[]{sdf.format((Date)r.get("record_date")),df1.format(r.get("weight"))+"kg",df1.format(r.get("body_fat"))+"%",df1.format(r.get("water_rate"))+"%",df1.format(r.get("muscle_rate"))+"%",r.get("visceral_fat")+"级",df1.format(r.get("bmi")),df1.format(r.get("waist"))+"cm",r.get("body_age")+"岁",r.get("body_type")});
            }
        }
    }

    // === LineChartPanel (v3.0 Bézier curve rendering) ===
    static class LineChartPanel extends JPanel {
        private List<Date> dates=new ArrayList<>(); private List<Double> values=new ArrayList<>(); private String metricName="";
        private List<Double> predictedValues=new ArrayList<>();
        void setData(List<Date> dates,List<Double> values,String metricName){
            this.dates=dates; this.values=values; this.metricName=metricName; predictedValues.clear();
            if(dates.size()>=3){for(int d:new int[]{7,14,30}){double pred=HealthCalculator.predictTrend(dates,values,d); if(!Double.isNaN(pred)) predictedValues.add(pred);}}
            repaint();
        }
        protected void paintComponent(Graphics g){
            super.paintComponent(g); Graphics2D g2=(Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w=getWidth(),h=getHeight(); int padLeft=60,padRight=30,padTop=36,padBottom=50;
            int chartW=w-padLeft-padRight, chartH=h-padTop-padBottom;
            // Background
            g2.setColor(new Color(248,250,252)); g2.fillRoundRect(padLeft,padTop,chartW,chartH,8,8);
            // Title
            g2.setColor(Theme.TEXT_DARK); g2.setFont(new Font(Theme.FONT_NAME,Font.BOLD,14));
            g2.drawString(metricName+" 历史趋势"+(predictedValues.size()>0?"  (虚线为预测)":""),padLeft,padTop-12);
            if(values.isEmpty()){
                g2.setColor(Theme.TEXT_LIGHT); g2.setFont(new Font(Theme.FONT_NAME,Font.PLAIN,13));
                FontMetrics fm=g2.getFontMetrics(); String msg="暂无数据，请先录入健康记录";
                g2.drawString(msg,(w-fm.stringWidth(msg))/2,h/2); return;
            }
            double minVal=Double.MAX_VALUE,maxVal=Double.MIN_VALUE;
            for(double v:values){minVal=Math.min(minVal,v); maxVal=Math.max(maxVal,v);}
            for(double v:predictedValues){minVal=Math.min(minVal,v); maxVal=Math.max(maxVal,v);}
            double range=maxVal-minVal; if(range<1)range=1;
            minVal-=range*0.1; maxVal+=range*0.1; range=maxVal-minVal;
            // Y axis grid
            g2.setFont(new Font(Theme.FONT_NAME,Font.PLAIN,11));
            for(int i=0;i<=5;i++){
                int y=padTop+chartH-(int)(chartH*i/5.0); double val=minVal+range*i/5.0;
                g2.setColor(Theme.TEXT_LIGHT); g2.drawString(df1.format(val),10,y+5);
                g2.setColor(new Color(241,245,249)); g2.drawLine(padLeft,y,padLeft+chartW,y);
            }
            // X axis dates
            int n=dates.size(); SimpleDateFormat sdf=new SimpleDateFormat("MM-dd");
            g2.setColor(Theme.TEXT_LIGHT); int maxLabels=Math.min(n,8);
            for(int i=0;i<maxLabels;i++){
                int idx=n>1?i*(n-1)/(maxLabels-1):0; int x=padLeft+(int)(chartW*idx/Math.max(n-1,1));
                g2.drawString(sdf.format(dates.get(idx)),x-20,padTop+chartH+18);
            }
            // Compute point coordinates
            int[] xs=new int[n],ys=new int[n];
            for(int i=0;i<n;i++){
                xs[i]=padLeft+(int)(chartW*i/Math.max(n-1,1));
                ys[i]=padTop+chartH-(int)(chartH*(values.get(i)-minVal)/range);
            }
            // Gradient fill area under curve
            if(n>=2){
                GradientPaint fillGrad=new GradientPaint(0,padTop,new Color(99,102,241,50),0,padTop+chartH,new Color(99,102,241,5));
                g2.setPaint(fillGrad);
                java.awt.geom.Path2D fillPath=new java.awt.geom.Path2D.Double();
                fillPath.moveTo(xs[0],padTop+chartH);
                fillPath.lineTo(xs[0],ys[0]);
                for(int i=1;i<n;i++){
                    int cpx=(xs[i-1]+xs[i])/2;
                    fillPath.curveTo(cpx,ys[i-1],cpx,ys[i],xs[i],ys[i]);
                }
                fillPath.lineTo(xs[n-1],padTop+chartH); fillPath.closePath(); g2.fill(fillPath);
            }
            // Bézier smooth curve
            if(n>=2){
                g2.setColor(Theme.PRIMARY); g2.setStroke(new BasicStroke(2.5f));
                java.awt.geom.Path2D path=new java.awt.geom.Path2D.Double();
                path.moveTo(xs[0],ys[0]);
                for(int i=1;i<n;i++){
                    int cpx=(xs[i-1]+xs[i])/2;
                    path.curveTo(cpx,ys[i-1],cpx,ys[i],xs[i],ys[i]);
                }
                g2.draw(path);
                // Data points with white border
                for(int i=0;i<n;i++){
                    g2.setColor(Color.WHITE); g2.fillOval(xs[i]-5,ys[i]-5,10,10);
                    g2.setColor(Theme.PRIMARY); g2.fillOval(xs[i]-4,ys[i]-4,8,8);
                }
            }
            // Prediction dashed line
            if(predictedValues.size()>0&&n>=2){
                g2.setColor(Theme.ACCENT); float[] dash={6f,4f};
                g2.setStroke(new BasicStroke(2,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,1,dash,0));
                int lastX=xs[n-1],lastY=ys[n-1];
                for(int i=0;i<predictedValues.size();i++){
                    double pred=predictedValues.get(i);
                    int predX=lastX+(int)(chartW*0.3*(i+1)/predictedValues.size());
                    int predY=padTop+chartH-(int)(chartH*(pred-minVal)/range);
                    predX=Math.min(predX,padLeft+chartW);
                    g2.drawLine(lastX,lastY,predX,predY);
                    g2.setColor(Theme.ACCENT); g2.fillOval(predX-4,predY-4,8,8);
                    g2.setFont(new Font(Theme.FONT_NAME,Font.PLAIN,10)); g2.setColor(Theme.TEXT_GRAY);
                    g2.drawString(df1.format(pred),predX-15,predY-10);
                    lastX=predX; lastY=predY; g2.setColor(Theme.ACCENT);
                    g2.setStroke(new BasicStroke(2,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,1,dash,0));
                }
            }
            // Axes
            g2.setColor(Theme.BORDER); g2.setStroke(new BasicStroke(1));
            g2.drawLine(padLeft,padTop+chartH,padLeft+chartW,padTop+chartH);
            g2.drawLine(padLeft,padTop,padLeft,padTop+chartH);
        }
    }

    // === AnalysisPanel ===
    static class AnalysisPanel extends JPanel {
        private JTextArea taResult;
        AnalysisPanel(){setLayout(new BorderLayout(10,10)); setBorder(BorderFactory.createEmptyBorder(12,12,12,12)); setBackground(Theme.BG);
            RoundedPanel btnCard=Theme.createCardPanel("分析评估",Theme.PRIMARY);
            JPanel btnPanel=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); btnPanel.setOpaque(false);
            RoundButton btnRefresh=new RoundButton("🔄 刷新分析结果",Theme.PRIMARY,Theme.PRIMARY_D).setGradient(true);
            btnRefresh.addActionListener(e->refresh()); btnPanel.add(btnRefresh);
            JLabel lblHint=new JLabel("综合 BMI、体脂率、BMR、TDEE 等指标生成评估报告"); lblHint.setFont(Theme.FONT_SMALL); lblHint.setForeground(Theme.TEXT_GRAY);
            btnPanel.add(lblHint); btnCard.add(btnPanel,BorderLayout.CENTER); add(btnCard,BorderLayout.NORTH);
            RoundedPanel reportCard=Theme.createCardPanel("健康分析评估报告",Theme.PRIMARY);
            taResult=new JTextArea(); taResult.setFont(new Font("Consolas",Font.PLAIN,13)); taResult.setEditable(false);
            taResult.setForeground(Theme.TEXT_DARK); taResult.setBackground(Theme.CARD_BG); taResult.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
            JScrollPane scroll=new JScrollPane(taResult); scroll.setOpaque(false); scroll.getViewport().setOpaque(false); scroll.setBorder(BorderFactory.createEmptyBorder());
            reportCard.add(scroll,BorderLayout.CENTER); add(reportCard,BorderLayout.CENTER); refresh();
        }
        void refresh(){
            Map<String,Object> latest=DBUtil.getLatestHealthRecord();
            if(latest==null){taResult.setText("暂无健康记录, 请先在「数据录入」页面录入数据"); return;}
            double weight=(double)latest.get("weight"),bodyFat=(double)latest.get("body_fat"),water=(double)latest.get("water_rate"),muscle=(double)latest.get("muscle_rate");
            int visceral=(int)latest.get("visceral_fat"); double boneMuscle=(double)latest.get("bone_muscle"),bmi=(double)latest.get("bmi"),bmr=(double)latest.get("bmr"),tdee=(double)latest.get("tdee"),waist=(double)latest.get("waist");
            int bodyAge=(int)latest.get("body_age"); String bodyType=(String)latest.get("body_type");
            double bmrH=HealthCalculator.calcBMR_Harris(weight,currentHeight,currentAge,currentGender),bmrM=HealthCalculator.calcBMR_Mifflin(weight,currentHeight,currentAge,currentGender),bmrC=HealthCalculator.calcBMR_China(weight,currentAge,currentGender);
            double idealWeight=HealthCalculator.calcIdealWeight(currentHeight);
            int score=HealthCalculator.calcHealthScore(bmi,bodyFat,visceral,muscle,water,currentGender);
            StringBuilder sb=new StringBuilder();
            sb.append("═══════════════════════════════════════════\n");
            sb.append("              健康分析评估报告\n");
            sb.append("═══════════════════════════════════════════\n\n");
            sb.append("【BMI 体质指数】\n"); sb.append("  BMI = ").append(df1.format(bmi)).append(" (").append(HealthCalculator.classifyBMI(bmi)).append(")\n");
            sb.append("  中国标准: <18.5偏瘦 | 18.5-23.9正常 | 24-27.9超重 | >=28肥胖\n\n");
            sb.append("【基础代谢率 BMR (三种公式对比)】\n");
            sb.append("  Harris-Benedict : ").append(df0.format(bmrH)).append(" kcal\n");
            sb.append("  Mifflin-St Jeor : ").append(df0.format(bmrM)).append(" kcal\n");
            sb.append("  中国营养学会    : ").append(df0.format(bmrC)).append(" kcal\n");
            sb.append("  ────────────────────────────────\n");
            sb.append("  平均值          : ").append(df0.format(bmr)).append(" kcal\n\n");
            sb.append("【每日总能量消耗 TDEE】\n"); sb.append("  活动等级: ").append(currentActivityLevel);
            sb.append(" (系数: ").append(df2.format(HealthCalculator.getActivityFactor(currentActivityLevel))).append(")\n");
            sb.append("  TDEE = BMR × 活动系数 = ").append(df0.format(tdee)).append(" kcal\n\n");
            sb.append("【理想体重】\n"); sb.append("  理想体重 = 身高² × 22 = ").append(df1.format(idealWeight)).append(" kg\n");
            sb.append("  正常范围: ").append(df1.format(currentHeight/100*currentHeight/100*18.5)).append(" - ");
            sb.append(df1.format(currentHeight/100*currentHeight/100*23.9)).append(" kg\n\n");
            sb.append("【体脂率】\n"); sb.append("  体脂率: ").append(df1.format(bodyFat)).append("%\n");
            String fatLevel=currentGender.equals("男")?(bodyFat<12?"偏低":bodyFat<=25?"正常":bodyFat<=30?"偏高":"严重偏高"):(bodyFat<20?"偏低":bodyFat<=32?"正常":bodyFat<=38?"偏高":"严重偏高");
            sb.append("  评级: ").append(fatLevel).append("\n\n");
            sb.append("【内脏脂肪等级】\n"); sb.append("  等级: ").append(visceral).append(" 级 (").append(HealthCalculator.assessVisceralFat(visceral)).append(")\n");
            sb.append("  标准: 1-4正常 | 5-8偏高 | 9-10过高\n\n");
            sb.append("【骨骼肌肉量】\n"); sb.append("  骨骼肌肉量: ").append(df1.format(boneMuscle)).append(" kg (");
            sb.append(HealthCalculator.assessMuscle(boneMuscle,weight,currentGender)).append(")\n\n");
            sb.append("【身体年龄】\n"); sb.append("  身体年龄: ").append(bodyAge).append(" 岁 (实际年龄: ").append(currentAge).append(" 岁)\n");
            if(bodyAge<currentAge) sb.append("  身体比实际年龄年轻 ").append(currentAge-bodyAge).append(" 岁!\n\n");
            else if(bodyAge>currentAge) sb.append("  身体比实际年龄大 ").append(bodyAge-currentAge).append(" 岁, 需关注\n\n");
            else sb.append("  身体年龄与实际年龄一致\n\n");
            sb.append("【体质分类 (BMI + 体脂率 交叉矩阵)】\n"); sb.append("  分类: ").append(bodyType).append("\n  分类说明:\n");
            sb.append("    消瘦型    — 体重不足, 需增加营养摄入\n    标准型    — 身体成分比例良好\n    肌肉型    — BMI偏高但体脂低, 肌肉发达\n    超重型    — 体重超标, 需控制\n    肥胖型    — 体脂率过高, 需减脂\n    隐性肥胖型 — 体重正常但体脂高, 建议力量训练\n\n");
            sb.append("【身体形态评估】\n"); double whtr=waist/currentHeight;
            sb.append("  腰围身高比 WHtR = ").append(df2.format(whtr)).append(" (").append(HealthCalculator.assessWHtR(waist,currentHeight)).append(")\n");
            sb.append("  体型分类: ").append(HealthCalculator.classifyBodyShape(waist,currentGender)).append("\n\n");
            sb.append("【健康评分】\n"); sb.append("  总分: ").append(score).append("/100 (").append(HealthCalculator.scoreLevel(score)).append(")\n");
            sb.append("  评分维度: BMI(30分) + 体脂率(25分) + 内脏脂肪(20分) + 肌肉量(15分) + 水分率(10分)\n");
            sb.append("  等级: 90-100优秀 | 75-89良好 | 60-74及格 | <60需改善\n\n");
            sb.append("═══════════════════════════════════════════\n");
            taResult.setText(sb.toString());
        }
    }

    // === PredictionPanel ===
    static class PredictionPanel extends JPanel {
        private JTextArea taResult;
        PredictionPanel(){setLayout(new BorderLayout(8,8)); setBorder(BorderFactory.createEmptyBorder(10,10,10,10)); setBackground(Theme.BG);
            RoundedPanel btnCard=Theme.createCardPanel("预测分析",Theme.PRIMARY);
            JPanel btnPanel=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); btnPanel.setOpaque(false);
            RoundButton btnRefresh=new RoundButton("🔄 刷新预测结果",Theme.PRIMARY,Theme.PRIMARY_D).setGradient(true);
            btnRefresh.addActionListener(e->refresh()); btnPanel.add(btnRefresh);
            JLabel lblHint=new JLabel("基于历史数据进行线性回归趋势预测"); lblHint.setFont(Theme.FONT_SMALL); lblHint.setForeground(Theme.TEXT_GRAY);
            btnPanel.add(lblHint); btnCard.add(btnPanel,BorderLayout.CENTER); add(btnCard,BorderLayout.NORTH);
            RoundedPanel reportCard=Theme.createCardPanel("预测分析报告",Theme.ACCENT);
            reportCard.setPreferredSize(new Dimension(700,450)); reportCard.setMinimumSize(new Dimension(400,250));
            taResult=new JTextArea(); taResult.setFont(Theme.FONT_BODY); taResult.setEditable(false);
            taResult.setBackground(Theme.CARD_BG); taResult.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
            JScrollPane scroll=new JScrollPane(taResult); scroll.setOpaque(false); scroll.getViewport().setOpaque(false); scroll.setBorder(BorderFactory.createEmptyBorder());
            reportCard.add(scroll,BorderLayout.CENTER); add(reportCard,BorderLayout.CENTER); refresh();
        }
        void refresh(){
            List<Map<String,Object>> records=DBUtil.getHealthRecords(60);
            if(records.size()<3){taResult.setText("数据不足, 至少需要 3 条健康记录才能进行预测分析\n当前记录数: "+records.size()); return;}
            List<Date> dates=new ArrayList<>(); List<Double> weights=new ArrayList<>(),bmis=new ArrayList<>(),bodyFats=new ArrayList<>();
            for(int i=records.size()-1;i>=0;i--){Map<String,Object> r=records.get(i); dates.add((Date)r.get("record_date")); weights.add((double)r.get("weight")); bmis.add((double)r.get("bmi")); bodyFats.add((double)r.get("body_fat"));}
            Map<String,Object> latest=records.get(0);
            double currentWeight=(double)latest.get("weight"),currentBMI=(double)latest.get("bmi"),tdee=(double)latest.get("tdee");
            double pred7=HealthCalculator.predictTrend(dates,weights,7),pred14=HealthCalculator.predictTrend(dates,weights,14),pred30=HealthCalculator.predictTrend(dates,weights,30);
            double predBMI30=HealthCalculator.calcBMI(pred30,currentHeight);
            String trend=HealthCalculator.trendDirection(dates,weights);
            int exerciseCal=DBUtil.getTodayExerciseCalories(); int[] diet=DBUtil.getTodayDietSummary(); int intakeCal=diet[0];
            double dailyDeficit=tdee+exerciseCal-intakeCal;
            Map<String,Object> goal=DBUtil.getGoal();
            String risk=HealthCalculator.assessRisk(predBMI30);
            StringBuilder sb=new StringBuilder();
            sb.append("═══════════════════════════════════════════\n              预测分析报告\n═══════════════════════════════════════════\n\n");
            sb.append("【趋势预测 (线性回归)】\n"); sb.append("  当前体重: ").append(df1.format(currentWeight)).append(" kg (BMI: ").append(df1.format(currentBMI)).append(")\n");
            sb.append("  趋势方向: ").append(trend).append("\n\n  预测结果:\n");
            sb.append("    7天后  → 体重 ").append(df1.format(pred7)).append(" kg (BMI: ").append(df1.format(HealthCalculator.calcBMI(pred7,currentHeight))).append(")\n");
            sb.append("   14天后  → 体重 ").append(df1.format(pred14)).append(" kg (BMI: ").append(df1.format(HealthCalculator.calcBMI(pred14,currentHeight))).append(")\n");
            sb.append("   30天后  → 体重 ").append(df1.format(pred30)).append(" kg (BMI: ").append(df1.format(predBMI30)).append(")\n\n");
            sb.append("  (预测基于最近 ").append(dates.size()).append(" 条记录, 实际结果受饮食和运动影响)\n\n");
            sb.append("【热量差分析】\n"); sb.append("  TDEE (每日消耗): ").append(df0.format(tdee)).append(" kcal\n  运动消耗: ").append(exerciseCal).append(" kcal\n  饮食摄入: ").append(intakeCal).append(" kcal\n  每日热量差: ").append(dailyDeficit>=0?"+":"").append(df0.format(dailyDeficit)).append(" kcal\n");
            if(dailyDeficit>0){sb.append("  → 处于热量缺口状态, 有利于减脂/减重\n  → 预计每周减重约 ").append(df2.format(dailyDeficit*7/7700)).append(" kg\n");}
            else if(dailyDeficit<0){sb.append("  → 处于热量盈余状态, 有利于增肌但可能导致脂肪增加\n  → 预计每周增重约 ").append(df2.format(Math.abs(dailyDeficit)*7/7700)).append(" kg\n");}
            else{sb.append("  → 热量收支平衡, 体重将维持稳定\n");}
            sb.append("\n【目标达成预测】\n");
            if(goal!=null){
                String goalType=(String)goal.get("goal_type"); double targetValue=(double)goal.get("target_value");
                sb.append("  目标类型: ").append(goalType).append("\n  目标体重: ").append(df1.format(targetValue)).append(" kg\n  当前体重: ").append(df1.format(currentWeight)).append(" kg\n");
                int days=HealthCalculator.predictGoalDays(currentWeight,targetValue,dailyDeficit,goalType);
                if(days==-2){sb.append("  ⚠️ 当前进度过慢 (热量差<100kcal), 建议调整饮食或运动计划\n");}
                else if(days==-1){sb.append("  ⚠️ 当前饮食超过消耗, 无法达成目标, 需减少摄入或增加运动\n");}
                else if(days==0){sb.append("  ✅ 已达成目标!\n");}
                else{Calendar cal=Calendar.getInstance(); cal.add(Calendar.DAY_OF_MONTH,days); SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd"); sb.append("  预测达成日期: ").append(sdf.format(cal.getTime())).append(" (还需 ").append(days).append(" 天)\n");}
            }else{sb.append("  尚未设置目标, 请到「目标计划」页面设置\n");}
            sb.append("\n【健康风险评估】\n  预测30天后BMI: ").append(df1.format(predBMI30)).append("\n  风险等级: ").append(predBMI30>=28.0?"高风险":(predBMI30>=24.0||predBMI30<18.5?"中风险":"低风险")).append("\n  ").append(risk).append("\n\n");
            String fatTrend=HealthCalculator.trendDirection(dates,bodyFats);
            sb.append("【体脂率趋势】\n  当前体脂率: ").append(df1.format(bodyFats.get(bodyFats.size()-1))).append("%\n  趋势方向: ").append(fatTrend).append("\n");
            if(fatTrend.equals("上升")&&bodyFats.size()>=30) sb.append("  ⚠️ 体脂率持续上升, 建议调整饮食结构\n");
            sb.append("\n═══════════════════════════════════════════\n");
            taResult.setText(sb.toString());
        }
    }

    // === GoalPlanPanel ===
    static class GoalPlanPanel extends JPanel {
        private JComboBox<String> cbGoalType=new JComboBox<>(new String[]{"减脂","减重","增肌","保持健康"});
        private JTextField tfTargetWeight=new JTextField("60.0"); private JTextArea taPlan; private JProgressBar[] progressBars=new JProgressBar[4];
        GoalPlanPanel(){setLayout(new BorderLayout(8,8)); setBorder(BorderFactory.createEmptyBorder(10,10,10,10)); setBackground(Theme.BG);
            RoundedPanel ctrlCard=Theme.createCardPanel("目标设定",Theme.PRIMARY);
            JPanel ctrlPanel=new JPanel(new FlowLayout(FlowLayout.LEFT,8,5)); ctrlPanel.setOpaque(false);
            JLabel lblGoal=new JLabel("目标类型:"); lblGoal.setFont(Theme.FONT_BODY); ctrlPanel.add(lblGoal);
            Theme.styleComboBox(cbGoalType); ctrlPanel.add(cbGoalType);
            JLabel lblWeight=new JLabel("目标体重(kg):"); lblWeight.setFont(Theme.FONT_BODY); ctrlPanel.add(lblWeight);
            Theme.styleTextField(tfTargetWeight); tfTargetWeight.setColumns(6); ctrlPanel.add(tfTargetWeight);
            RoundButton btnRecommend=new RoundButton("推荐目标",Theme.ACCENT,Theme.ACCENT_D).setGradient(true);
            btnRecommend.addActionListener(e->recommendTarget()); ctrlPanel.add(btnRecommend);
            RoundButton btnSetGoal=new RoundButton("设置目标",Theme.PRIMARY,Theme.PRIMARY_D).setGradient(true);
            btnSetGoal.addActionListener(e->setGoal()); ctrlPanel.add(btnSetGoal);
            RoundButton btnRefresh=new RoundButton("刷新",Theme.PRIMARY,Theme.PRIMARY_D).setGradient(true);
            btnRefresh.addActionListener(e->refresh()); ctrlPanel.add(btnRefresh);
            ctrlCard.add(ctrlPanel,BorderLayout.CENTER); add(ctrlCard,BorderLayout.NORTH);
            RoundedPanel progressCard=Theme.createCardPanel("分阶段进度",Theme.ACCENT);
            JPanel progressPanel=new JPanel(new GridLayout(4,1,8,8)); progressPanel.setOpaque(false);
            for(int i=0;i<4;i++){JPanel p=new JPanel(new BorderLayout(8,2)); p.setOpaque(false);
                JLabel lbl=new JLabel("第"+(i+1)+"周:"); lbl.setFont(Theme.FONT_BODY); p.add(lbl,BorderLayout.WEST);
                progressBars[i]=new JProgressBar(0,100); progressBars[i].setStringPainted(true); progressBars[i].setFont(Theme.FONT_BODY); progressBars[i].setForeground(Theme.PRIMARY); progressBars[i].setBackground(Theme.BG);
                p.add(progressBars[i],BorderLayout.CENTER); progressPanel.add(p);}
            progressCard.add(progressPanel,BorderLayout.CENTER);
            RoundedPanel planCard=Theme.createCardPanel("目标计划详情",Theme.PRIMARY);
            taPlan=new JTextArea(); taPlan.setFont(Theme.FONT_BODY); taPlan.setEditable(false); taPlan.setBackground(Theme.CARD_BG); taPlan.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
            JScrollPane scroll=new JScrollPane(taPlan); scroll.setOpaque(false); scroll.getViewport().setOpaque(false); scroll.setBorder(BorderFactory.createEmptyBorder());
            planCard.add(scroll,BorderLayout.CENTER);
            JPanel centerPanel=new JPanel(new BorderLayout(8,8)); centerPanel.setOpaque(false);
            centerPanel.add(progressCard,BorderLayout.NORTH); centerPanel.add(planCard,BorderLayout.CENTER);
            add(centerPanel,BorderLayout.CENTER); refresh();
        }
        private void recommendTarget(){double ideal=HealthCalculator.calcIdealWeight(currentHeight); tfTargetWeight.setText(df1.format(ideal)); JOptionPane.showMessageDialog(this,"推荐目标体重: "+df1.format(ideal)+" kg (身高²×22)");}
        private void setGoal(){try{String goalType=(String)cbGoalType.getSelectedItem(); double target=Double.parseDouble(tfTargetWeight.getText().trim());
            if(target<20||target>300){JOptionPane.showMessageDialog(this,"目标体重范围 20-300kg","提示",JOptionPane.WARNING_MESSAGE);return;}
            if(DBUtil.saveGoal(goalType,target)){JOptionPane.showMessageDialog(this,"目标设置成功!"); refresh();}
        }catch(NumberFormatException e){JOptionPane.showMessageDialog(this,"请输入有效数字","错误",JOptionPane.ERROR_MESSAGE);}}
        void refresh(){
            Map<String,Object> latest=DBUtil.getLatestHealthRecord(); Map<String,Object> goal=DBUtil.getGoal();
            if(latest==null){taPlan.setText("暂无健康记录, 请先录入数据"); return;}
            double currentWeight=(double)latest.get("weight"); StringBuilder sb=new StringBuilder();
            if(goal==null){sb.append("尚未设置目标, 请选择目标类型和目标体重后点击「设置目标」\n\n推荐目标体重: ").append(df1.format(HealthCalculator.calcIdealWeight(currentHeight))).append(" kg\n");
                taPlan.setText(sb.toString()); for(JProgressBar pb:progressBars) pb.setValue(0); return;}
            String goalType=(String)goal.get("goal_type"); double targetWeight=(double)goal.get("target_value");
            int currentStage=goal.get("current_stage")!=null?(int)goal.get("current_stage"):1;
            sb.append("═══════════════════════════════════════════\n              目标计划\n═══════════════════════════════════════════\n\n");
            sb.append("目标类型: ").append(goalType).append("\n当前体重: ").append(df1.format(currentWeight)).append(" kg\n目标体重: ").append(df1.format(targetWeight)).append(" kg\n需变化: ").append(df1.format(Math.abs(currentWeight-targetWeight))).append(" kg\n\n");
            sb.append("【分阶段计划 (4周递进)】\n"); double totalChange=targetWeight-currentWeight;
            double[] stageTargets=new double[4]; double[] ratios={0.2,0.25,0.275,0.275}; double cumRatio=0;
            for(int i=0;i<4;i++){cumRatio+=ratios[i]; stageTargets[i]=currentWeight+totalChange*cumRatio;
                sb.append("  第").append(i+1).append("周: ").append(df1.format(i==0?currentWeight:stageTargets[i-1])).append(" → ").append(df1.format(stageTargets[i])).append(" kg\n");
                double stageStart=(i==0)?currentWeight:stageTargets[i-1],stageEnd=stageTargets[i],progress;
                if(stageEnd>stageStart) progress=(currentWeight-stageStart)/(stageEnd-stageStart)*100;
                else progress=(stageStart-currentWeight)/(stageStart-stageEnd)*100;
                progress=Math.max(0,Math.min(100,progress));
                if(i+1<currentStage)progress=100; if(i+1>currentStage)progress=0;
                progressBars[i].setValue((int)progress);}
            sb.append("\n【运动建议】\n");
            switch(goalType){
                case "减脂":sb.append("  有氧运动: 跑步/游泳/跳绳/骑行\n  频率: 每周 4-5 次, 每次 40-60 分钟\n  力量训练: 每周 2 次, 每次 30 分钟\n  建议: 有氧为主燃烧脂肪, 力量训练维持肌肉量\n");break;
                case "减重":sb.append("  有氧运动: 快走/游泳/骑行\n  频率: 每周 5 次, 每次 45-60 分钟\n  力量训练: 每周 2 次\n  建议: 持续中低强度有氧, 配合饮食控制\n");break;
                case "增肌":sb.append("  力量训练: 深蹲/卧推/硬拉/划船\n  频率: 每周 4 次, 每次 60 分钟\n  有氧运动: 每周 2 次, 每次 20 分钟\n  建议: 大重量少次数, 蛋白质摄入 1.5-2g/kg体重\n");break;
                case "保持健康":sb.append("  有氧 + 力量均衡\n  频率: 每周 3-4 次, 每次 30-45 分钟\n  建议: 保持规律运动, 注意拉伸和恢复\n");break;
            }
            sb.append("\n【预测达成日期】\n"); double tdee=(double)latest.get("tdee"); int exerciseCal=DBUtil.getTodayExerciseCalories(); int[] diet=DBUtil.getTodayDietSummary();
            double dailyDeficit=tdee+exerciseCal-diet[0]; int days=HealthCalculator.predictGoalDays(currentWeight,targetWeight,dailyDeficit,goalType);
            if(days==-2){sb.append("  ⚠️ 当前进度过慢, 建议调整计划\n");}
            else if(days==-1){sb.append("  ⚠️ 当前饮食超过消耗, 无法达成目标\n");}
            else if(days==0){sb.append("  ✅ 已达成目标!\n");}
            else{Calendar cal=Calendar.getInstance(); cal.add(Calendar.DAY_OF_MONTH,days); SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
                sb.append("  预测达成日期: ").append(sdf.format(cal.getTime())).append(" (约 ").append(days).append(" 天)\n  每日热量差: ").append(df0.format(dailyDeficit)).append(" kcal\n");}
            sb.append("\n═══════════════════════════════════════════\n");
            taPlan.setText(sb.toString());
        }
    }

    // === DietPanel ===
    static class DietPanel extends JPanel {
        private MainFrame mainFrame;
        private JComboBox<String> cbMealType=new JComboBox<>(new String[]{"早餐","午餐","晚餐","加餐"});
        private JComboBox<String> cbFood=new JComboBox<>(); private JTextField tfGrams=new JTextField("100");
        private JTextField tfApiImagePath=new JTextField(20),tfApiKey=new JTextField(20);
        private JTextArea taReport; private PieChartPanel pieChart; private JLabel lblSummary;
        private Map<String,String[]> foodData=new LinkedHashMap<>();
        DietPanel(MainFrame frame){this.mainFrame=frame; setLayout(new BorderLayout(8,8)); setBorder(BorderFactory.createEmptyBorder(10,10,10,10)); setBackground(Theme.BG);
            RoundedPanel inputCard=Theme.createCardPanel("饮食记录录入",Theme.PRIMARY);
            JPanel inputPanel=new JPanel(new GridBagLayout()); inputPanel.setOpaque(false);
            GridBagConstraints gbc=new GridBagConstraints(); gbc.insets=new Insets(4,8,4,8); gbc.fill=GridBagConstraints.HORIZONTAL;
            gbc.gridx=0; gbc.gridy=0; JLabel lbl=new JLabel("餐次:"); lbl.setFont(Theme.FONT_BODY); inputPanel.add(lbl,gbc);
            gbc.gridx=1; Theme.styleComboBox(cbMealType); inputPanel.add(cbMealType,gbc);
            gbc.gridx=2; lbl=new JLabel("食物:"); lbl.setFont(Theme.FONT_BODY); inputPanel.add(lbl,gbc);
            gbc.gridx=3; gbc.gridwidth=2; Theme.styleComboBox(cbFood); inputPanel.add(cbFood,gbc); gbc.gridwidth=1;
            gbc.gridx=0; gbc.gridy=1; lbl=new JLabel("食用量(g):"); lbl.setFont(Theme.FONT_BODY); inputPanel.add(lbl,gbc);
            gbc.gridx=1; Theme.styleTextField(tfGrams); inputPanel.add(tfGrams,gbc);
            RoundButton btnAdd=new RoundButton("记录饮食",Theme.PRIMARY,Theme.PRIMARY_D).setGradient(true); btnAdd.addActionListener(e->addDietRecord());
            gbc.gridx=2; inputPanel.add(btnAdd,gbc);
            RoundButton btnExport=new RoundButton("导出CSV",Theme.ACCENT,Theme.ACCENT_D).setGradient(true); btnExport.addActionListener(e->exportCSV());
            gbc.gridx=3; inputPanel.add(btnExport,gbc);
            gbc.gridx=0; gbc.gridy=2; lbl=new JLabel("图片路径:"); lbl.setFont(Theme.FONT_BODY); inputPanel.add(lbl,gbc);
            gbc.gridx=1; gbc.gridwidth=2; Theme.styleTextField(tfApiImagePath); inputPanel.add(tfApiImagePath,gbc); gbc.gridwidth=1;
            RoundButton btnRecognize=new RoundButton("拍照识别",Theme.PRIMARY,Theme.PRIMARY_D).setGradient(true); btnRecognize.addActionListener(e->recognizeFood());
            gbc.gridx=3; inputPanel.add(btnRecognize,gbc);
            gbc.gridx=0; gbc.gridy=3; lbl=new JLabel("API Key:"); lbl.setFont(Theme.FONT_BODY); inputPanel.add(lbl,gbc);
            gbc.gridx=1; gbc.gridwidth=3; Theme.styleTextField(tfApiKey); inputPanel.add(tfApiKey,gbc); gbc.gridwidth=1;
            inputCard.add(inputPanel,BorderLayout.CENTER); add(inputCard,BorderLayout.NORTH);
            RoundedPanel summaryCard=Theme.createCardPanel("今日营养汇总",Theme.ACCENT);
            lblSummary=new JLabel("今日汇总: 暂无数据"); lblSummary.setFont(Theme.FONT_HEADER); lblSummary.setForeground(Theme.TEXT_DARK);
            summaryCard.add(lblSummary,BorderLayout.NORTH);
            pieChart=new PieChartPanel(); pieChart.setPreferredSize(new Dimension(300,220)); pieChart.setOpaque(false);
            summaryCard.add(pieChart,BorderLayout.WEST);
            taReport=new JTextArea(); taReport.setFont(Theme.FONT_BODY); taReport.setEditable(false); taReport.setBackground(Theme.CARD_BG); taReport.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
            JScrollPane scroll=new JScrollPane(taReport); scroll.setOpaque(false); scroll.getViewport().setOpaque(false); scroll.setBorder(BorderFactory.createEmptyBorder());
            summaryCard.add(scroll,BorderLayout.CENTER); add(summaryCard,BorderLayout.CENTER);
            loadFoods(); refreshSummary();
        }
        private void loadFoods(){cbFood.removeAllItems(); foodData.clear(); for(String[] food:DBUtil.getAllFoods()){String name=food[0]; foodData.put(name,food); cbFood.addItem(name);}}
        private void addDietRecord(){
            String mealType=(String)cbMealType.getSelectedItem(); String foodName=(String)cbFood.getSelectedItem();
            if(foodName==null){JOptionPane.showMessageDialog(this,"请选择食物","提示",JOptionPane.WARNING_MESSAGE);return;}
            try{double grams=Double.parseDouble(tfGrams.getText().trim());
                if(grams<=0||grams>5000){JOptionPane.showMessageDialog(this,"食用量范围 1-5000g","提示",JOptionPane.WARNING_MESSAGE);return;}
                String[] food=foodData.get(foodName); double ratio=grams/100.0;
                int calories=(int)(Integer.parseInt(food[1])*ratio); double protein=Double.parseDouble(food[2])*ratio,carbs=Double.parseDouble(food[3])*ratio,fat=Double.parseDouble(food[4])*ratio;
                if(DBUtil.saveDietRecord(mealType,foodName+"("+df0.format(grams)+"g)",calories,protein,carbs,fat)){JOptionPane.showMessageDialog(this,"饮食记录保存成功! 热量: "+calories+" kcal"); refreshSummary(); mainFrame.refreshData();}
            }catch(NumberFormatException e){JOptionPane.showMessageDialog(this,"请输入有效数字","错误",JOptionPane.ERROR_MESSAGE);}
        }
        void refreshSummary(){
            int[] diet=DBUtil.getTodayDietSummary(); int totalCal=diet[0]; double protein=diet[1]/100.0,carbs=diet[2]/100.0,fat=diet[3]/100.0;
            Map<String,Object> latest=DBUtil.getLatestHealthRecord(); double tdee=latest!=null?(double)latest.get("tdee"):1800;
            double recProtein=latest!=null?(double)latest.get("weight")*1.2:60; double recCarbs=tdee*0.5/4,recFat=tdee*0.3/9;
            lblSummary.setText(String.format("今日汇总: 总热量 %d kcal / 推荐 %d kcal | 蛋白质 %.1fg | 碳水 %.1fg | 脂肪 %.1fg",totalCal,(int)tdee,protein,carbs,fat));
            pieChart.setData(protein*4,carbs*4,fat*9);
            StringBuilder sb=new StringBuilder();
            sb.append("═══ 每日营养报告 ═══\n\n热量:\n"); sb.append(String.format("  摄入 %d kcal / 推荐 %d kcal",totalCal,(int)tdee));
            if(totalCal>tdee*1.1)sb.append("  [超标]"); else if(totalCal<tdee*0.7)sb.append("  [不足]"); sb.append("\n\n");
            sb.append("蛋白质:\n"); sb.append(String.format("  摄入 %.1fg / 推荐 %.1fg",protein,recProtein)); if(protein<recProtein*0.8)sb.append("  [不足]"); sb.append("\n\n");
            sb.append("碳水:\n"); sb.append(String.format("  摄入 %.1fg / 推荐 %.1fg",carbs,recCarbs)); if(carbs>recCarbs*1.3)sb.append("  [超标]"); sb.append("\n\n");
            sb.append("脂肪:\n"); sb.append(String.format("  摄入 %.1fg / 推荐 %.1fg",fat,recFat)); if(fat>recFat*1.3)sb.append("  [超标]"); sb.append("\n\n");
            double totalNutrientCal=protein*4+carbs*4+fat*9;
            if(totalNutrientCal>0){sb.append("营养素占比:\n"); sb.append(String.format("  蛋白质: %.1f%%\n  碳水:   %.1f%%\n  脂肪:   %.1f%%\n",protein*4/totalNutrientCal*100,carbs*4/totalNutrientCal*100,fat*9/totalNutrientCal*100));}
            taReport.setText(sb.toString());
        }
        private void recognizeFood(){
            String imagePath=tfApiImagePath.getText().trim(),apiKey=tfApiKey.getText().trim();
            if(imagePath.isEmpty()||apiKey.isEmpty()){JOptionPane.showMessageDialog(this,"请输入图片路径和 API Key","提示",JOptionPane.WARNING_MESSAGE);return;}
            try{File imgFile=new File(imagePath); if(!imgFile.exists()){JOptionPane.showMessageDialog(this,"图片文件不存在","错误",JOptionPane.ERROR_MESSAGE);return;}
                byte[] imgBytes=java.nio.file.Files.readAllBytes(imgFile.toPath()); String base64=Base64.getEncoder().encodeToString(imgBytes);
                String dataUrl="data:image/jpeg;base64,"+base64; String result=callSiliconFlowAPI(apiKey,dataUrl); taReport.setText("食物识别结果:\n"+result);
            }catch(Exception e){JOptionPane.showMessageDialog(this,"识别失败: "+e.getMessage(),"错误",JOptionPane.ERROR_MESSAGE);}
        }
        private String callSiliconFlowAPI(String apiKey,String imageDataUrl) throws Exception {
            URL url=new URL("https://api.siliconflow.cn/v1/chat/completions"); HttpURLConnection conn=(HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST"); conn.setRequestProperty("Content-Type","application/json"); conn.setRequestProperty("Authorization","Bearer "+apiKey);
            conn.setDoOutput(true); conn.setConnectTimeout(30000); conn.setReadTimeout(60000);
            String body="{\"model\":\"Qwen/Qwen2-VL-72B-Instruct\",\"messages\":[{\"role\":\"user\",\"content\":[{\"type\":\"text\",\"text\":\"请识别图片中的食物, 估算热量和营养成分(蛋白质/碳水/脂肪), 以JSON格式返回\"},{\"type\":\"image_url\",\"image_url\":{\"url\":\""+imageDataUrl+"\"}}]}]}";
            try(OutputStream os=conn.getOutputStream()){os.write(body.getBytes("UTF-8"));}
            int code=conn.getResponseCode(); InputStream is=code>=400?conn.getErrorStream():conn.getInputStream();
            BufferedReader reader=new BufferedReader(new InputStreamReader(is,"UTF-8")); StringBuilder response=new StringBuilder(); String line;
            while((line=reader.readLine())!=null) response.append(line);
            if(code>=400) return "API 调用失败 (HTTP "+code+"): "+response.toString();
            String resp=response.toString(); int idx=resp.indexOf("\"content\":\"");
            if(idx>=0){int end=resp.indexOf("\"",idx+11); if(end>idx) return resp.substring(idx+11,end).replace("\\n","\n");}
            return resp;
        }
        private void exportCSV(){
            JFileChooser chooser=new JFileChooser(); chooser.setSelectedFile(new File("health_data_export.csv"));
            if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;
            List<Map<String,Object>> records=DBUtil.getHealthRecords(999);
            try(PrintWriter pw=new PrintWriter(new OutputStreamWriter(new FileOutputStream(chooser.getSelectedFile()),"UTF-8"))){
                pw.write('\ufeff'); pw.println("日期,体重,BMI,体脂率,水分率,肌肉率,内脏脂肪,腰围,身体年龄,体质分类");
                SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
                for(Map<String,Object> r:records){pw.printf("%s,%.1f,%.1f,%.1f,%.1f,%.1f,%d,%.1f,%d,%s%n",sdf.format((Date)r.get("record_date")),(double)r.get("weight"),(double)r.get("bmi"),(double)r.get("body_fat"),(double)r.get("water_rate"),(double)r.get("muscle_rate"),(int)r.get("visceral_fat"),(double)r.get("waist"),(int)r.get("body_age"),r.get("body_type"));}
                JOptionPane.showMessageDialog(this,"导出成功! 共 "+records.size()+" 条记录\n文件: "+chooser.getSelectedFile().getAbsolutePath());
            }catch(Exception e){JOptionPane.showMessageDialog(this,"导出失败: "+e.getMessage(),"错误",JOptionPane.ERROR_MESSAGE);}
        }
    }

    // === PieChartPanel ===
    static class PieChartPanel extends JPanel {
        private double proteinCal=0,carbsCal=0,fatCal=0;
        void setData(double proteinCal,double carbsCal,double fatCal){this.proteinCal=proteinCal;this.carbsCal=carbsCal;this.fatCal=fatCal;repaint();}
        protected void paintComponent(Graphics g){
            super.paintComponent(g); Graphics2D g2=(Graphics2D)g; g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(),h=getHeight(); int size=Math.min(w-100,h-20); int cx=20,cy=(h-size)/2;
            double total=proteinCal+carbsCal+fatCal;
            if(total<=0){g2.setColor(Theme.TEXT_LIGHT); g2.setFont(new Font(Theme.FONT_NAME,Font.PLAIN,12)); g2.drawString("暂无饮食数据",cx+size/2-40,h/2); return;}
            int startAngle=0;
            int proteinAngle=(int)(proteinCal/total*360);
            GradientPaint pgp=new GradientPaint(cx,cy,Theme.PRIMARY,cx+size,cy+size,Theme.PRIMARY_D); g2.setPaint(pgp);
            g2.fillArc(cx,cy,size,size,startAngle,proteinAngle); startAngle+=proteinAngle;
            int carbsAngle=(int)(carbsCal/total*360);
            GradientPaint cgp=new GradientPaint(cx,cy,Theme.WARNING,cx+size,cy+size,new Color(217,119,6)); g2.setPaint(cgp);
            g2.fillArc(cx,cy,size,size,startAngle,carbsAngle); startAngle+=carbsAngle;
            int fatAngle=360-startAngle;
            GradientPaint fgp=new GradientPaint(cx,cy,Theme.DANGER,cx+size,cy+size,new Color(220,38,38)); g2.setPaint(fgp);
            g2.fillArc(cx,cy,size,size,startAngle,fatAngle);
            // Donut hole
            g2.setColor(Theme.CARD_BG); g2.fillOval(cx+size/4,cy+size/4,size/2,size/2);
            // Legend
            int legendX=cx+size+15,legendY=cy+10,legendH=20;
            g2.setFont(new Font(Theme.FONT_NAME,Font.PLAIN,12));
            g2.setColor(Theme.PRIMARY); g2.fillRoundRect(legendX,legendY,12,12,3,3);
            g2.setColor(Theme.TEXT_DARK); g2.drawString(String.format("蛋白质 %.1f%%",proteinCal/total*100),legendX+18,legendY+11);
            g2.setColor(Theme.WARNING); g2.fillRoundRect(legendX,legendY+legendH,12,12,3,3);
            g2.setColor(Theme.TEXT_DARK); g2.drawString(String.format("碳水 %.1f%%",carbsCal/total*100),legendX+18,legendY+legendH+11);
            g2.setColor(Theme.DANGER); g2.fillRoundRect(legendX,legendY+legendH*2,12,12,3,3);
            g2.setColor(Theme.TEXT_DARK); g2.drawString(String.format("脂肪 %.1f%%",fatCal/total*100),legendX+18,legendY+legendH*2+11);
        }
    }

    // === AchievementPanel ===
    static class AchievementPanel extends JPanel {
        private JTextArea taBadges; private JList<String> reportList; private DefaultListModel<String> reportModel; private JTextArea taReportContent;
        private static final String[][] ALL_BADGES={{"毅力之星","连续打卡 7 天"},{"坚持达人","连续打卡 30 天"},{"目标达成者","达成阶段目标"},{"美食家","记录饮食 30 天"},{"运动健将","记录运动 20 次"},{"健康标兵","健康评分达到 90"},{"蜕变之星","体脂率降至正常"}};
        AchievementPanel(){setLayout(new BorderLayout(8,8)); setBorder(BorderFactory.createEmptyBorder(10,10,10,10)); setBackground(Theme.BG);
            JSplitPane splitPane=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT); splitPane.setOpaque(false); splitPane.setDividerSize(8);
            RoundedPanel badgeCard=Theme.createCardPanel("成就徽章",Theme.PRIMARY);
            taBadges=new JTextArea(); taBadges.setFont(Theme.FONT_BODY); taBadges.setEditable(false); taBadges.setBackground(Theme.CARD_BG); taBadges.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
            JScrollPane badgeScroll=new JScrollPane(taBadges); badgeScroll.setOpaque(false); badgeScroll.getViewport().setOpaque(false); badgeScroll.setBorder(BorderFactory.createEmptyBorder());
            badgeCard.add(badgeScroll,BorderLayout.CENTER);
            RoundButton btnRefresh=new RoundButton("刷新徽章",Theme.PRIMARY,Theme.PRIMARY_D).setGradient(true); btnRefresh.addActionListener(e->refresh());
            badgeCard.add(btnRefresh,BorderLayout.SOUTH); splitPane.setLeftComponent(badgeCard);
            RoundedPanel reportCard=Theme.createCardPanel("历史 AI 报告",Theme.ACCENT);
            reportModel=new DefaultListModel<>(); reportList=new JList<>(reportModel);
            reportList.setFont(Theme.FONT_BODY); reportList.setSelectionBackground(Theme.PRIMARY_L); reportList.setSelectionForeground(Color.WHITE);
            reportList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            reportList.addListSelectionListener(e->{if(!e.getValueIsAdjusting()&&reportList.getSelectedIndex()>=0){String selected=reportList.getSelectedValue(); try{int id=Integer.parseInt(selected.split(" ")[0]); taReportContent.setText(DBUtil.getAIReportContent(id));}catch(Exception ex){}}});
            JScrollPane listScroll=new JScrollPane(reportList); listScroll.setOpaque(false); listScroll.getViewport().setOpaque(false); listScroll.setBorder(BorderFactory.createEmptyBorder()); listScroll.setPreferredSize(new Dimension(260,150));
            reportCard.add(listScroll,BorderLayout.NORTH);
            taReportContent=new JTextArea(); taReportContent.setFont(Theme.FONT_BODY); taReportContent.setEditable(false); taReportContent.setBackground(Theme.CARD_BG); taReportContent.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
            JScrollPane contentScroll=new JScrollPane(taReportContent); contentScroll.setOpaque(false); contentScroll.getViewport().setOpaque(false); contentScroll.setBorder(BorderFactory.createEmptyBorder());
            reportCard.add(contentScroll,BorderLayout.CENTER);
            JPanel btnPanel=new JPanel(new FlowLayout(FlowLayout.CENTER,8,5)); btnPanel.setOpaque(false);
            RoundButton btnGenReport=new RoundButton("生成周报",Theme.PRIMARY,Theme.PRIMARY_D).setGradient(true); btnGenReport.addActionListener(e->generateReport("周报"));
            RoundButton btnGenMonthReport=new RoundButton("生成月报",Theme.PRIMARY,Theme.PRIMARY_D).setGradient(true); btnGenMonthReport.addActionListener(e->generateReport("月报"));
            btnPanel.add(btnGenReport); btnPanel.add(btnGenMonthReport); reportCard.add(btnPanel,BorderLayout.SOUTH);
            splitPane.setRightComponent(reportCard); splitPane.setDividerLocation(350);
            add(splitPane,BorderLayout.CENTER); refresh();
        }
        void refresh(){
            List<String[]> earned=DBUtil.getAchievements(); Set<String> earnedNames=new HashSet<>();
            for(String[] badge:earned) earnedNames.add(badge[0]);
            StringBuilder sb=new StringBuilder(); sb.append("═══ 成就徽章 ═══\n\n");
            for(String[] badge:ALL_BADGES){boolean has=earnedNames.contains(badge[0]);
                sb.append(has?"★ ":"☆ ").append(badge[0]).append(" — ").append(badge[1]);
                if(has){for(String[] e:earned){if(e[0].equals(badge[0])){sb.append(" (").append(e[1]).append("获得)");break;}}}
                sb.append("\n");}
            sb.append("\n已获得: ").append(earnedNames.size()).append("/").append(ALL_BADGES.length).append(" 个徽章\n");
            taBadges.setText(sb.toString());
            reportModel.clear(); for(String[] report:DBUtil.getAIReports()) reportModel.addElement(report[0]+" | "+report[1]+" | "+report[2]);
        }
        private void generateReport(String reportType){
            String apiKey=JOptionPane.showInputDialog(this,"请输入硅基流动 API Key (留空则本地生成):"); if(apiKey==null)return;
            List<Map<String,Object>> records=DBUtil.getHealthRecords(30);
            if(records.isEmpty()){JOptionPane.showMessageDialog(this,"暂无健康记录, 无法生成报告","提示",JOptionPane.WARNING_MESSAGE);return;}
            String content;
            if(apiKey.trim().isEmpty()) content=generateLocalReport(reportType,records);
            else{try{content=callAIForReport(apiKey.trim(),reportType,records);}catch(Exception e){content="AI 生成失败, 使用本地报告:\n\n"+generateLocalReport(reportType,records);}}
            DBUtil.saveAIReport(reportType,content); taReportContent.setText(content); refresh();
            JOptionPane.showMessageDialog(this,reportType+"生成成功!");
        }
        private String generateLocalReport(String type,List<Map<String,Object>> records){
            StringBuilder sb=new StringBuilder();
            sb.append("═══════════════════════════════════\n       健康").append(type).append("报告\n═══════════════════════════════════\n\n");
            Map<String,Object> latest=records.get(0),oldest=records.get(records.size()-1);
            double weightChange=(double)latest.get("weight")-(double)oldest.get("weight"),fatChange=(double)latest.get("body_fat")-(double)oldest.get("body_fat"),bmiChange=(double)latest.get("bmi")-(double)oldest.get("bmi");
            sb.append("【身体变化趋势】\n");
            sb.append(String.format("  体重: %.1f → %.1f kg (变化 %+.1f kg)\n",(double)oldest.get("weight"),(double)latest.get("weight"),weightChange));
            sb.append(String.format("  体脂率: %.1f%% → %.1f%% (变化 %+.1f%%)\n",(double)oldest.get("body_fat"),(double)latest.get("body_fat"),fatChange));
            sb.append(String.format("  BMI: %.1f → %.1f (变化 %+.1f)\n\n",(double)oldest.get("bmi"),(double)latest.get("bmi"),bmiChange));
            sb.append("【当前状态】\n  体质分类: ").append(latest.get("body_type")).append("\n  身体年龄: ").append(latest.get("body_age")).append(" 岁\n\n");
            sb.append("【建议】\n");
            if(weightChange>0)sb.append("  体重有所增加, 建议增加有氧运动频率\n");
            else if(weightChange<0)sb.append("  体重有所下降, 请确保营养摄入充足\n");
            else sb.append("  体重保持稳定, 继续保持良好习惯\n");
            if(fatChange>0)sb.append("  体脂率上升, 建议调整饮食结构, 减少高脂食物\n");
            sb.append("  保持规律运动, 每周至少 3 次有氧运动\n  注意蛋白质摄入, 维持肌肉量\n\n");
            sb.append("报告生成时间: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date())).append("\n");
            return sb.toString();
        }
        private String callAIForReport(String apiKey,String reportType,List<Map<String,Object>> records) throws Exception {
            URL url=new URL("https://api.siliconflow.cn/v1/chat/completions"); HttpURLConnection conn=(HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST"); conn.setRequestProperty("Content-Type","application/json"); conn.setRequestProperty("Authorization","Bearer "+apiKey);
            conn.setDoOutput(true); conn.setConnectTimeout(30000); conn.setReadTimeout(60000);
            StringBuilder dataSb=new StringBuilder();
            for(Map<String,Object> r:records){dataSb.append(String.format("日期:%s 体重:%.1f 体脂:%.1f%% BMI:%.1f 类型:%s\n",r.get("record_date"),(double)r.get("weight"),(double)r.get("body_fat"),(double)r.get("bmi"),r.get("body_type")));}
            String prompt="请根据以下健康数据生成一份"+reportType+", 包含身体变化趋势分析、饮食建议、运动建议:\n"+dataSb.toString();
            String body="{\"model\":\"Qwen/Qwen2.5-7B-Instruct\",\"messages\":[{\"role\":\"user\",\"content\":\""+prompt.replace("\"","\\\"").replace("\n","\\n")+"\"}]}";
            try(OutputStream os=conn.getOutputStream()){os.write(body.getBytes("UTF-8"));}
            BufferedReader reader=new BufferedReader(new InputStreamReader(conn.getResponseCode()>=400?conn.getErrorStream():conn.getInputStream(),"UTF-8"));
            StringBuilder response=new StringBuilder(); String line; while((line=reader.readLine())!=null) response.append(line);
            String resp=response.toString(); int idx=resp.indexOf("\"content\":\"");
            if(idx>=0){int end=resp.indexOf("\"",idx+11); if(end>idx) return resp.substring(idx+11,end).replace("\\n","\n");}
            return "AI 返回: "+resp;
        }
    }

    // === DashboardPanel ===
    static class DashboardPanel extends JPanel {
        private JPanel grid;
        DashboardPanel(){setLayout(new BorderLayout(8,8)); setBorder(BorderFactory.createEmptyBorder(10,10,10,10)); setBackground(Theme.BG);
            RoundedPanel btnCard=Theme.createCardPanel("健康数据大屏",Theme.PRIMARY);
            JPanel btnPanel=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); btnPanel.setOpaque(false);
            RoundButton btnRefresh=new RoundButton("🔄 刷新大屏",Theme.PRIMARY,Theme.PRIMARY_D).setGradient(true);
            btnRefresh.addActionListener(e->refresh()); btnPanel.add(btnRefresh);
            JLabel lblHint=new JLabel("实时展示核心健康指标"); lblHint.setFont(Theme.FONT_SMALL); lblHint.setForeground(Theme.TEXT_GRAY);
            btnPanel.add(lblHint); btnCard.add(btnPanel,BorderLayout.CENTER); add(btnCard,BorderLayout.NORTH);
            RoundedPanel gridCard=Theme.createCardPanel("核心指标",Theme.ACCENT);
            grid=new JPanel(new GridLayout(0,4,12,12)); grid.setOpaque(false);
            JScrollPane scrollGrid=new JScrollPane(grid); scrollGrid.setOpaque(false); scrollGrid.getViewport().setOpaque(false); scrollGrid.setBorder(BorderFactory.createEmptyBorder());
            gridCard.add(scrollGrid,BorderLayout.CENTER); add(gridCard,BorderLayout.CENTER);
            refreshGrid();
        }
        private void refreshGrid(){
            grid.removeAll();
            Map<String,Object> latest=DBUtil.getLatestHealthRecord();
            if(latest==null){grid.add(Theme.createMetricCard("暂无数据","请先录入健康记录",new Color[]{Theme.TEXT_GRAY,Theme.TEXT_LIGHT})); grid.revalidate(); grid.repaint(); return;}
            double weight=(double)latest.get("weight"),bmi=(double)latest.get("bmi"),bodyFat=(double)latest.get("body_fat"),water=(double)latest.get("water_rate"),muscle=(double)latest.get("muscle_rate"),bmr=(double)latest.get("bmr"),tdee=(double)latest.get("tdee");
            int visceral=(int)latest.get("visceral_fat"),bodyAge=(int)latest.get("body_age"); String bodyType=(String)latest.get("body_type");
            int score=HealthCalculator.calcHealthScore(bmi,bodyFat,visceral,muscle,water,currentGender);
            int exerciseCal=DBUtil.getTodayExerciseCalories(); int[] diet=DBUtil.getTodayDietSummary(); int intakeCal=diet[0];
            int calDiff=(int)(tdee+exerciseCal-intakeCal);
            Color[] blueG={Theme.PRIMARY,Theme.PRIMARY_D},greenG={Theme.SUCCESS,new Color(22,163,74)},orangeG={Theme.WARNING,new Color(217,119,6)},redG={Theme.DANGER,new Color(220,38,38)},purpleG={Theme.INFO,Theme.PRIMARY_D},roseG={Theme.ACCENT,Theme.ACCENT_D};
            grid.add(Theme.createMetricCard("今日体重",df1.format(weight)+" kg",blueG));
            grid.add(Theme.createMetricCard("BMI",df1.format(bmi)+" ("+HealthCalculator.classifyBMI(bmi)+")",blueG));
            grid.add(Theme.createMetricCard("体脂率",df1.format(bodyFat)+"%",orangeG));
            grid.add(Theme.createMetricCard("水分率",df1.format(water)+"%",roseG));
            grid.add(Theme.createMetricCard("肌肉率",df1.format(muscle)+"%",greenG));
            grid.add(Theme.createMetricCard("BMR",df0.format(bmr)+" kcal",purpleG));
            grid.add(Theme.createMetricCard("TDEE",df0.format(tdee)+" kcal",purpleG));
            grid.add(Theme.createMetricCard("内脏脂肪",visceral+" 级 ("+HealthCalculator.assessVisceralFat(visceral)+")",orangeG));
            grid.add(Theme.createMetricCard("身体年龄",bodyAge+" 岁",roseG));
            grid.add(Theme.createMetricCard("体质分类",bodyType,blueG));
            grid.add(Theme.createMetricCard("今日摄入",intakeCal+" kcal",greenG));
            grid.add(Theme.createMetricCard("运动消耗",exerciseCal+" kcal",greenG));
            grid.add(Theme.createMetricCard("热量差",(calDiff>=0?"+":"")+calDiff+" kcal",calDiff>=0?greenG:redG));
            grid.add(Theme.createMetricCard("健康评分",score+"/100 ("+HealthCalculator.scoreLevel(score)+")",score>=75?greenG:score>=60?orangeG:redG));
            grid.revalidate(); grid.repaint();
        }
        void refresh(){refreshGrid();}
    }
}
