import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * ============================================================
 * BMI 体质评估与预测系统 — 管理员后台
 * 说明：与 HealthSystem.java 在同一默认包下，共享 DBUtil 和 Theme
 * ============================================================
 */
public class AdminSystem {

    // ==================== 管理员后台主窗口 ====================
    public static class AdminMainFrame extends JFrame {
        public AdminMainFrame() {
            setTitle("BMI 健康管理系统 — 管理员后台");
            setSize(1250, 820);
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setLocationRelativeTo(null);
            initUI();
        }

        private void logout() {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "确定要退出登录并返回登录界面吗？",
                    "退出登录", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                HealthSystem.DBUtil.logAction("ADMIN", HealthSystem.currentUsername, "退出登录", "返回登录界面");
                dispose();
                SwingUtilities.invokeLater(() -> {
                    try {
                        new HealthSystem.LoginFrame().setVisible(true);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(null, "登录窗口启动失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }
        }

        private void initUI() {
            setLayout(new BorderLayout());

            // 顶部渐变栏
            HealthSystem.GradientPanel topPanel = new HealthSystem.GradientPanel(
                    new BorderLayout(), HealthSystem.Theme.PRIMARY, HealthSystem.Theme.PRIMARY_D);
            topPanel.setBorder(new EmptyBorder(12, 18, 12, 18));
            JLabel lblTitle = new JLabel("健康管理系统 · 管理员后台", SwingConstants.LEFT);
            lblTitle.setFont(HealthSystem.Theme.FONT_TITLE);
            lblTitle.setForeground(Color.WHITE);
            JLabel lblAdmin = new JLabel("当前管理员: " + HealthSystem.currentUsername, SwingConstants.RIGHT);
            lblAdmin.setFont(HealthSystem.Theme.FONT_HEADER);
            lblAdmin.setForeground(Color.WHITE);

            JButton btnLogout = createPrimaryBtn("退出登录");
            btnLogout.setText("退出登录");
            btnLogout.addActionListener(e -> logout());
            JPanel eastPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            eastPanel.setOpaque(false);
            eastPanel.add(lblAdmin);
            eastPanel.add(btnLogout);

            topPanel.add(lblTitle, BorderLayout.WEST);
            topPanel.add(eastPanel, BorderLayout.EAST);
            add(topPanel, BorderLayout.NORTH);

            // 中间标签页
            JTabbedPane tabPane = new JTabbedPane();
            tabPane.setFont(HealthSystem.Theme.FONT_HEADER);
            tabPane.addTab("用户管理", new UserManagementPanel());
            tabPane.addTab("数据监控", new DataDashboardPanel());
            tabPane.addTab("内容管理", new ContentManagementPanel());
            tabPane.addTab("AI系统", new AISystemPanel());
            tabPane.addTab("系统配置", new SystemConfigPanel());
            tabPane.addTab("报表导出", new ReportExportPanel());
            tabPane.addTab("消息推送", new MessagePushPanel());
            tabPane.addTab("健康顾问", new HealthAdvisorPanel());
            add(tabPane, BorderLayout.CENTER);

            // 底部状态栏
            HealthSystem.GradientPanel bottomPanel = new HealthSystem.GradientPanel(
                    new BorderLayout(), HealthSystem.Theme.FOOTER_BG, HealthSystem.Theme.PRIMARY_L);
            bottomPanel.setBorder(new EmptyBorder(6, 18, 6, 18));
            JLabel lblBottom = new JLabel("v1.0 Admin 后台  |  数据实时同步");
            lblBottom.setFont(HealthSystem.Theme.FONT_BODY);
            bottomPanel.add(lblBottom, BorderLayout.WEST);
            add(bottomPanel, BorderLayout.SOUTH);
        }
    }

    // ==================== 通用 UI 工具 ====================
    static JPanel createCard(String title) {
        JPanel card = new HealthSystem.RoundedPanel(new BorderLayout(8, 8), 12);
        card.setBackground(HealthSystem.Theme.CARD_BG);
        card.setBorder(new EmptyBorder(14, 16, 16, 16));
        if (title != null && !title.isEmpty()) {
            JLabel lblTitle = new JLabel(title);
            lblTitle.setFont(HealthSystem.Theme.FONT_HEADER);
            lblTitle.setForeground(HealthSystem.Theme.PRIMARY_D);
            lblTitle.setBorder(new EmptyBorder(0, 0, 8, 0));
            card.add(lblTitle, BorderLayout.NORTH);
        }
        return card;
    }

    static JButton createPrimaryBtn(String text) {
        JButton btn = new JButton(text);
        HealthSystem.Theme.stylePrimaryButton(btn);
        return btn;
    }

    static JButton createAccentBtn(String text) {
        JButton btn = new JButton(text);
        HealthSystem.Theme.styleAccentButton(btn);
        return btn;
    }

    static void styleTextField(JTextField tf) {
        HealthSystem.Theme.styleTextField(tf);
    }

    static void styleTable(JTable table) {
        HealthSystem.Theme.styleTable(table);
    }

    // ==================== 1. 用户管理面板 ====================
    static class UserManagementPanel extends JPanel {
        private JTable userTable;
        private DefaultTableModel userModel;
        private JTextField tfSearch;

        UserManagementPanel() {
            setLayout(new BorderLayout(8, 8));
            setBackground(HealthSystem.Theme.BG);
            setBorder(new EmptyBorder(12, 12, 12, 12));

            // 顶部操作栏
            JPanel topPanel = createCard("用户管理");
            JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            ctrl.setOpaque(false);
            tfSearch = new JTextField(12);
            styleTextField(tfSearch);
            ctrl.add(new JLabel("搜索:"));
            ctrl.add(tfSearch);
            JButton btnSearch = createPrimaryBtn("查询");
            btnSearch.addActionListener(e -> loadUsers());
            ctrl.add(btnSearch);
            JButton btnDetail = createPrimaryBtn("查看详情");
            btnDetail.addActionListener(e -> showUserDetail());
            ctrl.add(btnDetail);
            JButton btnEnable = createAccentBtn("启用");
            btnEnable.addActionListener(e -> setUserStatus("启用"));
            ctrl.add(btnEnable);
            JButton btnDisable = createAccentBtn("禁用");
            btnDisable.addActionListener(e -> setUserStatus("禁用"));
            ctrl.add(btnDisable);
            JButton btnFreeze = createAccentBtn("冻结");
            btnFreeze.addActionListener(e -> setUserStatus("冻结"));
            ctrl.add(btnFreeze);
            JButton btnDelete = createAccentBtn("软删除");
            btnDelete.addActionListener(e -> softDeleteUser());
            ctrl.add(btnDelete);
            JButton btnExport = createPrimaryBtn("导出用户CSV");
            btnExport.addActionListener(e -> exportUsers());
            ctrl.add(btnExport);
            JButton btnExportHealth = createPrimaryBtn("导出打卡记录CSV");
            btnExportHealth.addActionListener(e -> exportHealthRecords());
            ctrl.add(btnExportHealth);
            topPanel.add(ctrl, BorderLayout.CENTER);
            add(topPanel, BorderLayout.NORTH);

            // 用户表格
            JPanel tableCard = createCard(null);
            userModel = new DefaultTableModel(
                    new String[]{"ID", "用户名", "性别", "年龄", "身高", "活动等级", "状态", "注册时间", "打卡天数"}, 0) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            userTable = new JTable(userModel);
            styleTable(userTable);
            tableCard.add(new JScrollPane(userTable), BorderLayout.CENTER);
            add(tableCard, BorderLayout.CENTER);

            loadUsers();
        }

        private void loadUsers() {
            userModel.setRowCount(0);
            String keyword = tfSearch.getText().trim().toLowerCase();
            List<Map<String, Object>> users = HealthSystem.DBUtil.getAllUsers();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            for (Map<String, Object> u : users) {
                String username = (String) u.get("username");
                if (!keyword.isEmpty() && !username.toLowerCase().contains(keyword)) continue;
                userModel.addRow(new Object[]{
                        u.get("id"), username, u.get("gender"), u.get("age"),
                        u.get("height"), u.get("activity_level"), u.get("account_status"),
                        u.get("created_at") == null ? "" : sdf.format(u.get("created_at")),
                        u.get("checkin_days")
                });
            }
        }

        private int getSelectedUserId() {
            int row = userTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "请先选择一行", "提示", JOptionPane.WARNING_MESSAGE);
                return -1;
            }
            return (int) userModel.getValueAt(row, 0);
        }

        private void showUserDetail() {
            int row = userTable.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this, "请先选择用户"); return; }
            String username = (String) userModel.getValueAt(row, 1);
            Map<String, Object> profile = HealthSystem.DBUtil.getUserHealthProfile(username);
            StringBuilder sb = new StringBuilder();
            sb.append("用户名: ").append(profile.get("username")).append("\n");
            sb.append("健康记录数: ").append(profile.get("record_count")).append("\n");
            sb.append("饮食记录数: ").append(profile.get("diet_count")).append("\n");
            sb.append("运动记录数: ").append(profile.get("exercise_count")).append("\n");
            sb.append("成就徽章数: ").append(profile.get("achievement_count")).append("\n");
            @SuppressWarnings("unchecked")
            Map<String, Object> latest = (Map<String, Object>) profile.get("latest_record");
            if (latest != null) {
                sb.append("\n最新记录:\n");
                sb.append("  体重: ").append(latest.get("weight")).append(" kg\n");
                sb.append("  BMI: ").append(latest.get("bmi")).append("\n");
                sb.append("  体脂率: ").append(latest.get("body_fat")).append("%\n");
                sb.append("  腰围: ").append(latest.get("waist")).append(" cm\n");
                sb.append("  记录日期: ").append(latest.get("record_date")).append("\n");
            }
            JTextArea ta = new JTextArea(sb.toString());
            ta.setFont(HealthSystem.Theme.FONT_BODY);
            ta.setEditable(false);
            ta.setRows(12);
            ta.setColumns(40);
            JOptionPane.showMessageDialog(this, new JScrollPane(ta), "用户详情", JOptionPane.INFORMATION_MESSAGE);
        }

        private void setUserStatus(String status) {
            int id = getSelectedUserId();
            if (id < 0) return;
            if (HealthSystem.DBUtil.updateUserStatus(id, status)) {
                HealthSystem.DBUtil.logAction("ADMIN", HealthSystem.currentUsername, "修改用户状态", "ID=" + id + ", 状态=" + status);
                JOptionPane.showMessageDialog(this, "状态更新成功");
                loadUsers();
            } else {
                JOptionPane.showMessageDialog(this, "更新失败", "错误", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void softDeleteUser() {
            int id = getSelectedUserId();
            if (id < 0) return;
            int confirm = JOptionPane.showConfirmDialog(this, "确定软删除该用户吗？", "确认", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
            if (HealthSystem.DBUtil.softDeleteUser(id)) {
                HealthSystem.DBUtil.logAction("ADMIN", HealthSystem.currentUsername, "软删除用户", "ID=" + id);
                JOptionPane.showMessageDialog(this, "已软删除");
                loadUsers();
            }
        }

        private void exportUsers() {
            String csv = HealthSystem.DBUtil.exportUsersCSV();
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("users_export.csv"));
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File f = fc.getSelectedFile();
                try (FileWriter fw = new FileWriter(f)) {
                    fw.write(csv);
                    JOptionPane.showMessageDialog(this,
                            "导出成功！\n保存路径：\n" + f.getAbsolutePath().replace("\\", "/") + "\n\n共 " + csv.length() + " 字符",
                            "导出完成", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "导出失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        private void exportHealthRecords() {
            String csv = HealthSystem.DBUtil.exportHealthRecordsCSV();
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("health_records_export.csv"));
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File f = fc.getSelectedFile();
                try (FileWriter fw = new FileWriter(f)) {
                    fw.write(csv);
                    JOptionPane.showMessageDialog(this,
                            "导出成功！\n保存路径：\n" + f.getAbsolutePath().replace("\\", "/") + "\n\n共 " + csv.length() + " 字符",
                            "导出完成", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "导出失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    // ==================== 2. 数据监控与统计面板 ====================
    static class DataDashboardPanel extends JPanel {
        private DefaultTableModel abnormalModel;
        private JTable abnormalTable;

        DataDashboardPanel() {
            setLayout(new BorderLayout(8, 8));
            setBackground(HealthSystem.Theme.BG);
            setBorder(new EmptyBorder(12, 12, 12, 12));

            // 顶部控制栏 + 指标卡片
            JPanel topPanel = new JPanel(new BorderLayout(8, 8));
            topPanel.setOpaque(false);

            JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            ctrl.setOpaque(false);
            JButton btnRefresh = createPrimaryBtn("刷新数据");
            btnRefresh.addActionListener(e -> refreshData());
            JButton btnExport = createPrimaryBtn("导出异常用户");
            btnExport.addActionListener(e -> exportAbnormalUsers());
            ctrl.add(btnRefresh);
            ctrl.add(btnExport);
            topPanel.add(ctrl, BorderLayout.NORTH);

            JPanel topCards = new JPanel(new GridLayout(1, 4, 10, 10));
            topCards.setOpaque(false);
            topCards.add(metricCard("总用户数", "0", HealthSystem.Theme.PRIMARY, "注册并激活的用户总数"));
            topCards.add(metricCard("7日活跃用户", "0", HealthSystem.Theme.SUCCESS, "最近 7 天有过登录或打卡的用户数"));
            topCards.add(metricCard("今日打卡", "0", HealthSystem.Theme.ACCENT, "今日已提交健康打卡记录的用户数"));
            topCards.add(metricCard("平均BMI", "0", HealthSystem.Theme.WARNING, "所有健康记录用户的平均 BMI 指数"));
            topPanel.add(topCards, BorderLayout.CENTER);
            add(topPanel, BorderLayout.NORTH);

            // 中间：异常用户列表 + 趋势说明
            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            split.setDividerLocation(500);
            split.setResizeWeight(0.4);

            JPanel leftCard = createCard("异常用户列表（双击查看详情）");
            abnormalModel = new DefaultTableModel(
                    new String[]{"用户名", "BMI", "体重变化", "异常原因"}, 0) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            abnormalTable = new JTable(abnormalModel);
            styleTable(abnormalTable);
            abnormalTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        int row = abnormalTable.getSelectedRow();
                        if (row >= 0) showUserDetail((String) abnormalModel.getValueAt(row, 0));
                    }
                }
            });
            leftCard.add(new JScrollPane(abnormalTable), BorderLayout.CENTER);
            split.setLeftComponent(leftCard);

            JPanel rightCard = createCard("健康风险评估说明");
            JTextArea ta = new JTextArea(
                    "异常检测规则:\n" +
                    "1. 7天内体重变化超过 5kg\n" +
                    "2. BMI < 18.5 或 BMI > 28\n" +
                    "3. 体脂率连续30天上升\n" +
                    "4. 超过7天未打卡\n\n" +
                    "风险等级:\n" +
                    "低风险: 指标在正常范围内\n" +
                    "中风险: 1-2项指标异常\n" +
                    "高风险: 多项指标异常或骤变\n\n" +
                    "使用说明:\n" +
                    "• 点击上方指标卡片查看统计口径\n" +
                    "• 双击异常用户列表查看健康档案\n" +
                    "• 点击「导出异常用户」可导出 CSV");
            ta.setFont(HealthSystem.Theme.FONT_BODY);
            ta.setEditable(false);
            ta.setBackground(HealthSystem.Theme.CARD_BG);
            rightCard.add(new JScrollPane(ta), BorderLayout.CENTER);
            split.setRightComponent(rightCard);

            add(split, BorderLayout.CENTER);
            refreshData();
        }

        private void refreshData() {
            Map<String, Object> stats = HealthSystem.DBUtil.getGlobalStats();
            JPanel topCards = (JPanel) ((BorderLayout) getLayout()).getLayoutComponent(BorderLayout.NORTH);
            if (topCards != null) {
                JPanel cards = (JPanel) topCards.getComponent(1);
                updateMetricCard(cards, 0, String.valueOf(stats.get("total_users")));
                updateMetricCard(cards, 1, String.valueOf(stats.get("active_users_7d")));
                updateMetricCard(cards, 2, String.valueOf(stats.get("today_checkin")));
                updateMetricCard(cards, 3, HealthSystem.df1.format(stats.get("avg_bmi")));
            }

            abnormalModel.setRowCount(0);
            List<Map<String, Object>> abnormals = HealthSystem.DBUtil.getAbnormalUsers();
            for (Map<String, Object> u : abnormals) {
                abnormalModel.addRow(new Object[]{
                        u.get("username"),
                        HealthSystem.df1.format(u.get("bmi")),
                        HealthSystem.df1.format(u.get("weight_diff")),
                        u.get("reason")
                });
            }
        }

        private void updateMetricCard(JPanel cards, int index, String value) {
            HealthSystem.RoundedPanel card = (HealthSystem.RoundedPanel) cards.getComponent(index);
            JLabel lbl = (JLabel) card.getComponent(0);
            lbl.setText(value);
        }

        private void showUserDetail(String username) {
            Map<String, Object> profile = HealthSystem.DBUtil.getUserHealthProfile(username);
            StringBuilder sb = new StringBuilder();
            sb.append("用户: ").append(username).append("\n");
            sb.append("健康记录数: ").append(profile.get("record_count")).append("\n");
            sb.append("饮食记录数: ").append(profile.get("diet_count")).append("\n");
            sb.append("运动记录数: ").append(profile.get("exercise_count")).append("\n");
            @SuppressWarnings("unchecked")
            Map<String, Object> latest = (Map<String, Object>) profile.get("latest_record");
            if (latest != null) {
                sb.append("\n最新记录:\n");
                sb.append("  体重: ").append(latest.get("weight")).append(" kg\n");
                sb.append("  BMI: ").append(latest.get("bmi")).append("\n");
                sb.append("  体脂率: ").append(latest.get("body_fat")).append("%\n");
                sb.append("  身体年龄: ").append(latest.get("body_age")).append(" 岁\n");
                sb.append("  类型: ").append(latest.get("body_type")).append("\n");
            }
            JTextArea ta = new JTextArea(sb.toString(), 12, 40);
            ta.setFont(HealthSystem.Theme.FONT_BODY);
            ta.setEditable(false);
            JOptionPane.showMessageDialog(this, new JScrollPane(ta), "用户健康档案", JOptionPane.INFORMATION_MESSAGE);
        }

        private void exportAbnormalUsers() {
            StringBuilder csv = new StringBuilder("用户名,BMI,体重变化,异常原因\n");
            for (int i = 0; i < abnormalModel.getRowCount(); i++) {
                csv.append(abnormalModel.getValueAt(i, 0)).append(",");
                csv.append(abnormalModel.getValueAt(i, 1)).append(",");
                csv.append(abnormalModel.getValueAt(i, 2)).append(",");
                csv.append(abnormalModel.getValueAt(i, 3)).append("\n");
            }
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("abnormal_users_export.csv"));
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try (FileWriter fw = new FileWriter(fc.getSelectedFile())) {
                    fw.write(csv.toString());
                    JOptionPane.showMessageDialog(this, "导出成功：" + fc.getSelectedFile().getAbsolutePath().replace("\\", "/"));
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "导出失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        private JPanel metricCard(String title, String value, Color color, String tooltip) {
            HealthSystem.RoundedPanel card = new HealthSystem.RoundedPanel(new BorderLayout(), 12);
            card.setBackground(Color.WHITE);
            card.setCursor(new Cursor(Cursor.HAND_CURSOR));
            JLabel lblValue = new JLabel(value, SwingConstants.CENTER);
            lblValue.setFont(new Font("Microsoft YaHei", Font.BOLD, 28));
            lblValue.setForeground(color);
            JLabel lblTitle = new JLabel(title, SwingConstants.CENTER);
            lblTitle.setFont(HealthSystem.Theme.FONT_BODY);
            lblTitle.setForeground(HealthSystem.Theme.TEXT_GRAY);
            card.add(lblValue, BorderLayout.CENTER);
            card.add(lblTitle, BorderLayout.SOUTH);
            card.setBorder(new EmptyBorder(16, 12, 16, 12));
            card.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    JOptionPane.showMessageDialog(DataDashboardPanel.this, tooltip, "指标说明: " + title, JOptionPane.INFORMATION_MESSAGE);
                }
            });
            return card;
        }
    }

    // ==================== 3. 内容管理面板 ====================
    static class ContentManagementPanel extends JPanel {
        private JTable foodTable, exerciseTable, articleTable;
        private DefaultTableModel foodModel, exerciseModel, articleModel;

        ContentManagementPanel() {
            setLayout(new BorderLayout(8, 8));
            setBackground(HealthSystem.Theme.BG);
            setBorder(new EmptyBorder(12, 12, 12, 12));

            JTabbedPane tabPane = new JTabbedPane();
            tabPane.setFont(HealthSystem.Theme.FONT_HEADER);
            tabPane.addTab("食物数据库", createFoodPanel());
            tabPane.addTab("运动库", createExercisePanel());
            tabPane.addTab("健康文章", createArticlePanel());
            add(tabPane, BorderLayout.CENTER);
        }

        private JPanel createFoodPanel() {
            JPanel panel = new JPanel(new BorderLayout(8, 8));
            panel.setOpaque(false);
            JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            ctrl.setOpaque(false);
            JButton btnAdd = createPrimaryBtn("新增");
            btnAdd.addActionListener(e -> editFood(0));
            JButton btnEdit = createPrimaryBtn("编辑");
            btnEdit.addActionListener(e -> editFood(getSelectedId(foodTable, foodModel)));
            JButton btnDel = createAccentBtn("删除");
            btnDel.addActionListener(e -> deleteFood());
            ctrl.add(btnAdd); ctrl.add(btnEdit); ctrl.add(btnDel);
            panel.add(ctrl, BorderLayout.NORTH);

            foodModel = new DefaultTableModel(new String[]{"ID", "名称", "热量", "蛋白质", "碳水", "脂肪"}, 0) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            foodTable = new JTable(foodModel);
            styleTable(foodTable);
            panel.add(new JScrollPane(foodTable), BorderLayout.CENTER);
            loadFoods();
            return panel;
        }

        private void loadFoods() {
            foodModel.setRowCount(0);
            for (String[] row : HealthSystem.DBUtil.getFoods()) {
                foodModel.addRow(row);
            }
        }

        private void editFood(int id) {
            String name = ""; int cal = 0; double p = 0, c = 0, f = 0;
            if (id > 0) {
                int row = foodTable.getSelectedRow();
                name = (String) foodModel.getValueAt(row, 1);
                cal = Integer.parseInt((String) foodModel.getValueAt(row, 2));
                p = Double.parseDouble((String) foodModel.getValueAt(row, 3));
                c = Double.parseDouble((String) foodModel.getValueAt(row, 4));
                f = Double.parseDouble((String) foodModel.getValueAt(row, 5));
            }
            JTextField tfName = new JTextField(name);
            JTextField tfCal = new JTextField(String.valueOf(cal));
            JTextField tfP = new JTextField(String.valueOf(p));
            JTextField tfC = new JTextField(String.valueOf(c));
            JTextField tfF = new JTextField(String.valueOf(f));
            styleTextField(tfName); styleTextField(tfCal); styleTextField(tfP); styleTextField(tfC); styleTextField(tfF);
            Object[] msg = {"名称", tfName, "热量", tfCal, "蛋白质", tfP, "碳水", tfC, "脂肪", tfF};
            if (JOptionPane.showConfirmDialog(this, msg, id > 0 ? "编辑食物" : "新增食物", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                try {
                    if (HealthSystem.DBUtil.saveFood(id, tfName.getText(), Integer.parseInt(tfCal.getText()),
                            Double.parseDouble(tfP.getText()), Double.parseDouble(tfC.getText()), Double.parseDouble(tfF.getText()))) {
                        HealthSystem.DBUtil.logAction("ADMIN", HealthSystem.currentUsername, "保存食物", tfName.getText());
                        loadFoods();
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "输入格式错误", "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        private void deleteFood() {
            int id = getSelectedId(foodTable, foodModel);
            if (id < 0) return;
            if (JOptionPane.showConfirmDialog(this, "确定删除？", "确认", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                HealthSystem.DBUtil.deleteFood(id);
                HealthSystem.DBUtil.logAction("ADMIN", HealthSystem.currentUsername, "删除食物", "ID=" + id);
                loadFoods();
            }
        }

        private JPanel createExercisePanel() {
            JPanel panel = new JPanel(new BorderLayout(8, 8));
            panel.setOpaque(false);
            JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            ctrl.setOpaque(false);
            JButton btnAdd = createPrimaryBtn("新增");
            btnAdd.addActionListener(e -> editExercise(0));
            JButton btnEdit = createPrimaryBtn("编辑");
            btnEdit.addActionListener(e -> editExercise(getSelectedId(exerciseTable, exerciseModel)));
            JButton btnDel = createAccentBtn("删除");
            btnDel.addActionListener(e -> deleteExercise());
            ctrl.add(btnAdd); ctrl.add(btnEdit); ctrl.add(btnDel);
            panel.add(ctrl, BorderLayout.NORTH);

            exerciseModel = new DefaultTableModel(new String[]{"ID", "名称", "类型", "热量/小时", "强度", "说明"}, 0) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            exerciseTable = new JTable(exerciseModel);
            styleTable(exerciseTable);
            panel.add(new JScrollPane(exerciseTable), BorderLayout.CENTER);
            loadExercises();
            return panel;
        }

        private void loadExercises() {
            exerciseModel.setRowCount(0);
            for (String[] row : HealthSystem.DBUtil.getExerciseLibrary()) {
                exerciseModel.addRow(row);
            }
        }

        private void editExercise(int id) {
            String name = "", type = "有氧", intensity = "中", desc = "";
            int cal = 0;
            if (id > 0) {
                int row = exerciseTable.getSelectedRow();
                name = (String) exerciseModel.getValueAt(row, 1);
                type = (String) exerciseModel.getValueAt(row, 2);
                cal = Integer.parseInt((String) exerciseModel.getValueAt(row, 3));
                intensity = (String) exerciseModel.getValueAt(row, 4);
                desc = (String) exerciseModel.getValueAt(row, 5);
            }
            JTextField tfName = new JTextField(name);
            JComboBox<String> cbType = new JComboBox<>(new String[]{"有氧", "力量"});
            cbType.setSelectedItem(type);
            JTextField tfCal = new JTextField(String.valueOf(cal));
            JComboBox<String> cbInt = new JComboBox<>(new String[]{"低", "中", "高"});
            cbInt.setSelectedItem(intensity);
            JTextField tfDesc = new JTextField(desc);
            styleTextField(tfName); styleTextField(tfCal); styleTextField(tfDesc);
            Object[] msg = {"名称", tfName, "类型", cbType, "热量/小时", tfCal, "强度", cbInt, "说明", tfDesc};
            if (JOptionPane.showConfirmDialog(this, msg, id > 0 ? "编辑运动" : "新增运动", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                try {
                    if (HealthSystem.DBUtil.saveExerciseLibrary(id, tfName.getText(), (String) cbType.getSelectedItem(),
                            Integer.parseInt(tfCal.getText()), (String) cbInt.getSelectedItem(), tfDesc.getText())) {
                        HealthSystem.DBUtil.logAction("ADMIN", HealthSystem.currentUsername, "保存运动", tfName.getText());
                        loadExercises();
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "输入格式错误", "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        private void deleteExercise() {
            int id = getSelectedId(exerciseTable, exerciseModel);
            if (id < 0) return;
            if (JOptionPane.showConfirmDialog(this, "确定删除？", "确认", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                HealthSystem.DBUtil.deleteExerciseLibrary(id);
                HealthSystem.DBUtil.logAction("ADMIN", HealthSystem.currentUsername, "删除运动", "ID=" + id);
                loadExercises();
            }
        }

        private JPanel createArticlePanel() {
            JPanel panel = new JPanel(new BorderLayout(8, 8));
            panel.setOpaque(false);
            JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            ctrl.setOpaque(false);
            JButton btnAdd = createPrimaryBtn("新增");
            btnAdd.addActionListener(e -> editArticle(0));
            JButton btnEdit = createPrimaryBtn("编辑");
            btnEdit.addActionListener(e -> editArticle(getSelectedId(articleTable, articleModel)));
            ctrl.add(btnAdd); ctrl.add(btnEdit);
            panel.add(ctrl, BorderLayout.NORTH);

            articleModel = new DefaultTableModel(new String[]{"ID", "标题", "分类", "状态", "发布时间"}, 0) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            articleTable = new JTable(articleModel);
            styleTable(articleTable);
            panel.add(new JScrollPane(articleTable), BorderLayout.CENTER);
            loadArticles();
            return panel;
        }

        private void loadArticles() {
            articleModel.setRowCount(0);
            for (String[] row : HealthSystem.DBUtil.getHealthArticles()) {
                articleModel.addRow(row);
            }
        }

        private void editArticle(int id) {
            String title = "", content = "", category = "健康科普";
            if (id > 0) {
                int row = articleTable.getSelectedRow();
                title = (String) articleModel.getValueAt(row, 1);
                category = (String) articleModel.getValueAt(row, 2);
            }
            JTextField tfTitle = new JTextField(title);
            JTextField tfCategory = new JTextField(category);
            JTextArea taContent = new JTextArea(6, 30);
            taContent.setLineWrap(true);
            styleTextField(tfTitle); styleTextField(tfCategory);
            Object[] msg = {"标题", tfTitle, "分类", tfCategory, "内容", new JScrollPane(taContent)};
            if (JOptionPane.showConfirmDialog(this, msg, id > 0 ? "编辑文章" : "新增文章", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                if (HealthSystem.DBUtil.saveHealthArticle(id, tfTitle.getText(), taContent.getText(), tfCategory.getText())) {
                    HealthSystem.DBUtil.logAction("ADMIN", HealthSystem.currentUsername, "保存文章", tfTitle.getText());
                    loadArticles();
                }
            }
        }

        private int getSelectedId(JTable table, DefaultTableModel model) {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this, "请先选择一行"); return -1; }
            return Integer.parseInt((String) model.getValueAt(row, 0));
        }
    }

    // ==================== 4. AI 系统管理面板 ====================
    static class AISystemPanel extends JPanel {
        private DefaultTableModel chatModel, templateModel;
        private JTextField tfApiKey, tfModel, tfEndpoint;

        AISystemPanel() {
            setLayout(new BorderLayout(8, 8));
            setBackground(HealthSystem.Theme.BG);
            setBorder(new EmptyBorder(12, 12, 12, 12));

            JTabbedPane tabPane = new JTabbedPane();
            tabPane.setFont(HealthSystem.Theme.FONT_HEADER);
            tabPane.addTab("AI 问答记录", createChatPanel());
            tabPane.addTab("Prompt 模板", createTemplatePanel());
            tabPane.addTab("API 配置", createApiConfigPanel());
            add(tabPane, BorderLayout.CENTER);
        }

        private JPanel createChatPanel() {
            JPanel panel = new JPanel(new BorderLayout(8, 8));
            panel.setOpaque(false);
            JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            ctrl.setOpaque(false);
            JButton btnReview = createPrimaryBtn("标记有效");
            btnReview.addActionListener(e -> updateChatStatus("有效"));
            JButton btnInvalid = createAccentBtn("标记无效");
            btnInvalid.addActionListener(e -> updateChatStatus("无效"));
            ctrl.add(btnReview); ctrl.add(btnInvalid);
            panel.add(ctrl, BorderLayout.NORTH);

            chatModel = new DefaultTableModel(new String[]{"ID", "用户", "问题", "状态", "时间"}, 0) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            JTable table = new JTable(chatModel);
            styleTable(table);
            panel.add(new JScrollPane(table), BorderLayout.CENTER);
            loadChatRecords();
            return panel;
        }

        private void loadChatRecords() {
            chatModel.setRowCount(0);
            for (String[] row : HealthSystem.DBUtil.getAIChatRecords()) {
                chatModel.addRow(row);
            }
        }

        private void updateChatStatus(String status) {
            int row = ((JTable) ((JScrollPane) ((JPanel) ((JTabbedPane) getComponent(0)).getComponentAt(0)).getComponent(1)).getViewport().getView()).getSelectedRow();
            // 简化处理，通过查找表格组件获取选中行
            JTable table = findTableInComponent(((JTabbedPane) getComponent(0)).getComponentAt(0));
            if (table == null) return;
            int selected = table.getSelectedRow();
            if (selected < 0) { JOptionPane.showMessageDialog(this, "请先选择一行"); return; }
            int id = Integer.parseInt((String) chatModel.getValueAt(selected, 0));
            if (HealthSystem.DBUtil.updateAIChatStatus(id, status)) {
                HealthSystem.DBUtil.logAction("ADMIN", HealthSystem.currentUsername, "审核AI问答", "ID=" + id + ", 状态=" + status);
                loadChatRecords();
            }
        }

        private JTable findTableInComponent(Component c) {
            if (c instanceof JTable) return (JTable) c;
            if (c instanceof JScrollPane) return findTableInComponent(((JScrollPane) c).getViewport().getView());
            if (c instanceof JPanel) {
                for (Component child : ((JPanel) c).getComponents()) {
                    JTable t = findTableInComponent(child);
                    if (t != null) return t;
                }
            }
            if (c instanceof JTabbedPane) {
                return findTableInComponent(((JTabbedPane) c).getSelectedComponent());
            }
            return null;
        }

        private JPanel createTemplatePanel() {
            JPanel panel = new JPanel(new BorderLayout(8, 8));
            panel.setOpaque(false);
            JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            ctrl.setOpaque(false);
            JButton btnAdd = createPrimaryBtn("新增模板");
            btnAdd.addActionListener(e -> editTemplate(0));
            JButton btnEdit = createPrimaryBtn("编辑");
            btnEdit.addActionListener(e -> editTemplate(getSelectedIdFromTable(panel, templateModel)));
            ctrl.add(btnAdd); ctrl.add(btnEdit);
            panel.add(ctrl, BorderLayout.NORTH);

            templateModel = new DefaultTableModel(new String[]{"ID", "名称", "类型", "状态", "更新时间"}, 0) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            JTable table = new JTable(templateModel);
            styleTable(table);
            table.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        int id = getSelectedIdFromTable(panel, templateModel);
                        if (id > 0) viewTemplate(id);
                    }
                }
            });
            panel.add(new JScrollPane(table), BorderLayout.CENTER);
            loadTemplates();
            return panel;
        }

        private void viewTemplate(int id) {
            Map<String, String> t = HealthSystem.DBUtil.getAITemplateById(id);
            if (t.isEmpty()) {
                JOptionPane.showMessageDialog(this, "未找到模板内容"); return;
            }
            JTextArea ta = new JTextArea(t.get("prompt_text"), 10, 50);
            ta.setLineWrap(true);
            ta.setFont(HealthSystem.Theme.FONT_BODY);
            ta.setEditable(false);
            JOptionPane.showMessageDialog(this, new JScrollPane(ta),
                    "模板: " + t.get("template_name") + " [" + t.get("template_type") + "]",
                    JOptionPane.INFORMATION_MESSAGE);
        }

        private void loadTemplates() {
            templateModel.setRowCount(0);
            for (String[] row : HealthSystem.DBUtil.getAITemplates()) {
                templateModel.addRow(row);
            }
        }

        private void editTemplate(int id) {
            String name = "", type = "建议", content = "";
            if (id > 0) {
                int row = findTableInComponent(this).getSelectedRow();
                name = (String) templateModel.getValueAt(row, 1);
                type = (String) templateModel.getValueAt(row, 2);
                Map<String, String> t = HealthSystem.DBUtil.getAITemplateById(id);
                if (!t.isEmpty()) content = t.get("prompt_text");
            }
            JTextField tfName = new JTextField(name);
            JComboBox<String> cbType = new JComboBox<>(new String[]{"建议", "周报", "饮食推荐"});
            cbType.setSelectedItem(type);
            JTextArea taContent = new JTextArea(content, 8, 40);
            taContent.setLineWrap(true);
            styleTextField(tfName);
            Object[] msg = {"名称", tfName, "类型", cbType, "Prompt内容", new JScrollPane(taContent)};
            if (JOptionPane.showConfirmDialog(this, msg, id > 0 ? "编辑模板" : "新增模板", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                if (HealthSystem.DBUtil.saveAITemplate(id, tfName.getText(), (String) cbType.getSelectedItem(), taContent.getText())) {
                    HealthSystem.DBUtil.logAction("ADMIN", HealthSystem.currentUsername, "保存AI模板", tfName.getText());
                    loadTemplates();
                }
            }
        }

        private int getSelectedIdFromTable(JPanel panel, DefaultTableModel model) {
            JTable table = findTableInComponent(panel);
            if (table == null) return -1;
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this, "请先选择一行"); return -1; }
            return Integer.parseInt((String) model.getValueAt(row, 0));
        }

        private JPanel createApiConfigPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setOpaque(false);
            JPanel form = new JPanel(new GridLayout(6, 2, 8, 8));
            form.setOpaque(false);
            form.setBorder(new EmptyBorder(20, 20, 20, 20));
            tfApiKey = new JTextField();
            tfModel = new JTextField("deepseek-chat");
            tfEndpoint = new JTextField("https://api.siliconflow.cn/v1");
            styleTextField(tfApiKey); styleTextField(tfModel); styleTextField(tfEndpoint);
            form.add(new JLabel("API Key:")); form.add(tfApiKey);
            form.add(new JLabel("模型名称:")); form.add(tfModel);
            form.add(new JLabel("接口地址:")); form.add(tfEndpoint);
            form.add(new JLabel("说明:")); form.add(new JLabel("配置硅基流动 API 信息（保存到数据库）"));
            panel.add(form, BorderLayout.NORTH);

            // 加载已有配置
            Map<String, String> cfg = HealthSystem.DBUtil.getAIApiConfig();
            if (cfg != null) {
                tfApiKey.setText(cfg.getOrDefault("api_key", ""));
                tfModel.setText(cfg.getOrDefault("model_name", "deepseek-chat"));
                tfEndpoint.setText(cfg.getOrDefault("endpoint_url", "https://api.siliconflow.cn/v1"));
            }

            JButton btnSave = createPrimaryBtn("保存配置");
            btnSave.addActionListener(e -> saveApiConfig());
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            btnPanel.setOpaque(false);
            btnPanel.add(btnSave);
            panel.add(btnPanel, BorderLayout.CENTER);
            return panel;
        }

        private void saveApiConfig() {
            String apiKey = tfApiKey.getText().trim();
            String modelName = tfModel.getText().trim();
            String endpoint = tfEndpoint.getText().trim();
            if (modelName.isEmpty() || endpoint.isEmpty()) {
                JOptionPane.showMessageDialog(this, "模型名称和接口地址不能为空", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (HealthSystem.DBUtil.saveAIApiConfig(apiKey, modelName, endpoint)) {
                HealthSystem.DBUtil.logAction("ADMIN", HealthSystem.currentUsername, "更新API配置", modelName);
                JOptionPane.showMessageDialog(this, "API 配置已保存到数据库\n\n后续可在 Prompt 模板或健康顾问模块中调用此配置生成 AI 建议。", "保存成功", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "保存失败，请检查数据库连接", "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ==================== 5. 系统配置面板 ====================
    static class SystemConfigPanel extends JPanel {
        private JTextField tfWeightThresh, tfFatDays, tfNoCheckin, tfBmiLow, tfBmiHigh, tfLoss, tfGain, tfReminder;

        SystemConfigPanel() {
            setLayout(new BorderLayout(8, 8));
            setBackground(HealthSystem.Theme.BG);
            setBorder(new EmptyBorder(12, 12, 12, 12));

            JPanel card = createCard("预警与系统参数配置");
            JPanel form = new JPanel(new GridLayout(8, 2, 12, 12));
            form.setOpaque(false);
            form.setBorder(new EmptyBorder(10, 10, 10, 10));

            Map<String, String> cfg = HealthSystem.DBUtil.getSystemConfig();
            tfWeightThresh = createCfgField(cfg, "weight_change_threshold");
            tfFatDays = createCfgField(cfg, "body_fat_rise_days");
            tfNoCheckin = createCfgField(cfg, "no_checkin_days");
            tfBmiLow = createCfgField(cfg, "bmi_low");
            tfBmiHigh = createCfgField(cfg, "bmi_high");
            tfLoss = createCfgField(cfg, "weight_loss_speed");
            tfGain = createCfgField(cfg, "muscle_gain_speed");
            tfReminder = createCfgField(cfg, "checkin_reminder_time");

            form.add(new JLabel("体重变化阈值(kg):")); form.add(tfWeightThresh);
            form.add(new JLabel("体脂上升天数阈值:")); form.add(tfFatDays);
            form.add(new JLabel("未打卡天数阈值:")); form.add(tfNoCheckin);
            form.add(new JLabel("BMI 正常下限:")); form.add(tfBmiLow);
            form.add(new JLabel("BMI 正常上限:")); form.add(tfBmiHigh);
            form.add(new JLabel("减脂速度(kg/周):")); form.add(tfLoss);
            form.add(new JLabel("增肌速度(kg/周):")); form.add(tfGain);
            form.add(new JLabel("打卡提醒时间:")); form.add(tfReminder);
            card.add(form, BorderLayout.CENTER);

            JButton btnSave = createPrimaryBtn("保存配置");
            btnSave.addActionListener(e -> saveConfig());
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            btnPanel.setOpaque(false);
            btnPanel.add(btnSave);
            card.add(btnPanel, BorderLayout.SOUTH);

            add(card, BorderLayout.NORTH);

            // 数据备份与日志
            JPanel bottomCard = createCard("数据备份与系统日志");
            bottomCard.setLayout(new BorderLayout(8, 8));
            JPanel bottomCtrl = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            bottomCtrl.setOpaque(false);
            JButton btnBackup = createPrimaryBtn("立即备份数据库");
            btnBackup.addActionListener(e -> backupDB());
            JButton btnLogs = createPrimaryBtn("查看日志");
            btnLogs.addActionListener(e -> showLogs());
            bottomCtrl.add(btnBackup); bottomCtrl.add(btnLogs);
            bottomCard.add(bottomCtrl, BorderLayout.NORTH);
            add(bottomCard, BorderLayout.CENTER);
        }

        private JTextField createCfgField(Map<String, String> cfg, String key) {
            JTextField tf = new JTextField(cfg.getOrDefault(key, ""));
            styleTextField(tf);
            return tf;
        }

        private void saveConfig() {
            HealthSystem.DBUtil.updateSystemConfig("weight_change_threshold", tfWeightThresh.getText());
            HealthSystem.DBUtil.updateSystemConfig("body_fat_rise_days", tfFatDays.getText());
            HealthSystem.DBUtil.updateSystemConfig("no_checkin_days", tfNoCheckin.getText());
            HealthSystem.DBUtil.updateSystemConfig("bmi_low", tfBmiLow.getText());
            HealthSystem.DBUtil.updateSystemConfig("bmi_high", tfBmiHigh.getText());
            HealthSystem.DBUtil.updateSystemConfig("weight_loss_speed", tfLoss.getText());
            HealthSystem.DBUtil.updateSystemConfig("muscle_gain_speed", tfGain.getText());
            HealthSystem.DBUtil.updateSystemConfig("checkin_reminder_time", tfReminder.getText());
            HealthSystem.DBUtil.logAction("ADMIN", HealthSystem.currentUsername, "更新系统配置", "预警与参数");
            JOptionPane.showMessageDialog(this, "配置已保存");
        }

        private void backupDB() {
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("health_db_backup_" + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".sql"));
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                if (HealthSystem.DBUtil.backupDatabase(fc.getSelectedFile().getAbsolutePath())) {
                    HealthSystem.DBUtil.logAction("ADMIN", HealthSystem.currentUsername, "数据库备份", fc.getSelectedFile().getName());
                    JOptionPane.showMessageDialog(this, "备份成功");
                } else {
                    JOptionPane.showMessageDialog(this, "备份失败", "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        private void showLogs() {
            DefaultTableModel logModel = new DefaultTableModel(new String[]{"类型", "操作者", "操作", "详情", "时间"}, 0) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            JTable table = new JTable(logModel);
            styleTable(table);
            for (String[] row : HealthSystem.DBUtil.getSystemLogs("")) {
                logModel.addRow(row);
            }
            JScrollPane scroll = new JScrollPane(table);
            scroll.setPreferredSize(new Dimension(700, 400));
            JOptionPane.showMessageDialog(this, scroll, "系统日志", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // ==================== 6. 报表与导出面板 ====================
    static class ReportExportPanel extends JPanel {
        ReportExportPanel() {
            setLayout(new BorderLayout(8, 8));
            setBackground(HealthSystem.Theme.BG);
            setBorder(new EmptyBorder(12, 12, 12, 12));

            JPanel card = createCard("报表与数据导出");
            card.setLayout(new GridLayout(5, 1, 10, 10));
            card.setBorder(new EmptyBorder(30, 30, 30, 30));

            JButton btnUserReport = createPrimaryBtn("导出用户健康报告 (CSV)");
            btnUserReport.addActionListener(e -> exportUsers());
            JButton btnHealthJson = createPrimaryBtn("导出健康数据 (JSON)");
            btnHealthJson.addActionListener(e -> exportHealthJSON());
            JButton btnDietJson = createPrimaryBtn("导出饮食记录 (CSV)");
            btnDietJson.addActionListener(e -> exportDietCSV());
            JButton btnChart = createPrimaryBtn("生成统计图表");
            btnChart.addActionListener(e -> showStatsDialog());

            card.add(btnUserReport);
            card.add(btnHealthJson);
            card.add(btnDietJson);
            card.add(btnChart);
            add(card, BorderLayout.CENTER);
        }

        private void exportUsers() {
            String csv = HealthSystem.DBUtil.exportUsersCSV();
            saveToFile(csv, "users_export.csv");
        }

        private void exportHealthJSON() {
            StringBuilder sb = new StringBuilder("[\n");
            String sql = "SELECT username, weight, bmi, body_fat, muscle_rate, water_rate, visceral_fat, bone_muscle, waist, bmr, tdee, record_date FROM health_records ORDER BY record_date DESC LIMIT 5000";
            try (java.sql.Connection conn = HealthSystem.DBUtil.getConnection();
                 java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                java.sql.ResultSet rs = ps.executeQuery();
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(",\n");
                    first = false;
                    sb.append("  {\n");
                    sb.append("    \"username\": \"").append(escapeJson(rs.getString("username"))).append("\",\n");
                    sb.append("    \"weight\": ").append(rs.getDouble("weight")).append(",\n");
                    sb.append("    \"bmi\": ").append(rs.getDouble("bmi")).append(",\n");
                    sb.append("    \"body_fat\": ").append(rs.getDouble("body_fat")).append(",\n");
                    sb.append("    \"muscle_rate\": ").append(rs.getDouble("muscle_rate")).append(",\n");
                    sb.append("    \"water_rate\": ").append(rs.getDouble("water_rate")).append(",\n");
                    sb.append("    \"visceral_fat\": ").append(rs.getInt("visceral_fat")).append(",\n");
                    sb.append("    \"bone_muscle\": ").append(rs.getDouble("bone_muscle")).append(",\n");
                    sb.append("    \"waist\": ").append(rs.getDouble("waist")).append(",\n");
                    sb.append("    \"bmr\": ").append(rs.getDouble("bmr")).append(",\n");
                    sb.append("    \"tdee\": ").append(rs.getDouble("tdee")).append(",\n");
                    sb.append("    \"record_date\": \"").append(rs.getTimestamp("record_date")).append("\"\n");
                    sb.append("  }");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "读取健康数据失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            sb.append("\n]");
            saveToFile(sb.toString(), "health_data.json");
        }

        private String escapeJson(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
        }

        private void exportDietCSV() {
            StringBuilder sb = new StringBuilder("ID,用户名,日期,餐次,食物,热量,蛋白质,碳水,脂肪\n");
            String sql = "SELECT id, username, record_date, meal_type, food_name, calories, protein, carbs, fat FROM diet_records ORDER BY id DESC LIMIT 1000";
            try (java.sql.Connection conn = HealthSystem.DBUtil.getConnection();
                 java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                java.sql.ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    sb.append(rs.getInt("id")).append(",");
                    sb.append(rs.getString("username")).append(",");
                    sb.append(rs.getDate("record_date")).append(",");
                    sb.append(rs.getString("meal_type")).append(",");
                    sb.append(rs.getString("food_name")).append(",");
                    sb.append(rs.getInt("calories")).append(",");
                    sb.append(rs.getDouble("protein")).append(",");
                    sb.append(rs.getDouble("carbs")).append(",");
                    sb.append(rs.getDouble("fat")).append("\n");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            saveToFile(sb.toString(), "diet_records.csv");
        }

        private void saveToFile(String content, String defaultName) {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("导出文件 - " + defaultName);
            fc.setSelectedFile(new File(defaultName));
            if (fc.showSaveDialog(SwingUtilities.getWindowAncestor(this)) == JFileChooser.APPROVE_OPTION) {
                File f = fc.getSelectedFile();
                try (FileWriter fw = new FileWriter(f)) {
                    fw.write(content);
                    JOptionPane.showMessageDialog(this,
                            "导出成功！\n保存路径：\n" + f.getAbsolutePath().replace("\\", "/") + "\n\n共 " + content.length() + " 字符",
                            "导出完成", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "导出失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        private void showStatsDialog() {
            Map<String, Object> stats = HealthSystem.DBUtil.getGlobalStats();
            StringBuilder sb = new StringBuilder();
            sb.append("全局健康统计\n");
            sb.append("=========================\n");
            sb.append("总用户数：").append(stats.get("total_users")).append("\n");
            sb.append("7日活跃用户：").append(stats.get("active_users_7d")).append("\n");
            sb.append("今日打卡：").append(stats.get("today_checkin")).append("\n");
            sb.append("平均BMI：").append(HealthSystem.df1.format(stats.get("avg_bmi"))).append("\n");
            sb.append("异常用户数：").append(stats.get("abnormal_users")).append("\n\n");
            sb.append("提示：详细图表请切换到「数据监控」面板查看。");
            JTextArea ta = new JTextArea(sb.toString());
            ta.setFont(HealthSystem.Theme.FONT_BODY);
            ta.setEditable(false);
            ta.setRows(10);
            ta.setColumns(35);
            JOptionPane.showMessageDialog(this, new JScrollPane(ta), "统计摘要", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // ==================== 7. 消息推送面板 ====================
    static class MessagePushPanel extends JPanel {
        private JComboBox<String> cbType, cbReceiver;
        private JTextField tfTitle;
        private JTextArea taContent;

        MessagePushPanel() {
            setLayout(new BorderLayout(8, 8));
            setBackground(HealthSystem.Theme.BG);
            setBorder(new EmptyBorder(12, 12, 12, 12));

            JPanel card = createCard("发送消息通知");
            card.setLayout(new BorderLayout(8, 8));
            JPanel form = new JPanel(new GridLayout(4, 2, 8, 8));
            form.setOpaque(false);
            form.setBorder(new EmptyBorder(10, 10, 10, 10));

            cbType = new JComboBox<>(new String[]{"系统通知", "健康提醒", "活动推送"});
            List<String> receiverOptions = new ArrayList<>();
            receiverOptions.add("所有用户");
            receiverOptions.add("异常用户");
            for (Map<String, Object> u : HealthSystem.DBUtil.getAllUsers()) {
                receiverOptions.add("用户:" + u.get("username"));
            }
            cbReceiver = new JComboBox<>(receiverOptions.toArray(new String[0]));
            tfTitle = new JTextField();
            taContent = new JTextArea(5, 30);
            taContent.setLineWrap(true);
            styleTextField(tfTitle);
            HealthSystem.Theme.styleComboBox(cbType);
            HealthSystem.Theme.styleComboBox(cbReceiver);

            form.add(new JLabel("消息类型:")); form.add(cbType);
            form.add(new JLabel("接收对象:")); form.add(cbReceiver);
            form.add(new JLabel("标题:")); form.add(tfTitle);
            form.add(new JLabel("内容:")); form.add(new JScrollPane(taContent));
            card.add(form, BorderLayout.CENTER);

            JButton btnSend = createPrimaryBtn("发送消息");
            btnSend.addActionListener(e -> sendMessage());
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            btnPanel.setOpaque(false);
            btnPanel.add(btnSend);
            card.add(btnPanel, BorderLayout.SOUTH);

            add(card, BorderLayout.NORTH);

            // 已发送列表
            JPanel listCard = createCard("已发送消息");
            DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "发送者", "接收者", "标题", "类型", "状态", "时间"}, 0) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            JTable table = new JTable(model);
            styleTable(table);
            for (String[] row : HealthSystem.DBUtil.getAllNotifications()) {
                model.addRow(row);
            }
            listCard.add(new JScrollPane(table), BorderLayout.CENTER);
            add(listCard, BorderLayout.CENTER);
        }

        private void sendMessage() {
            String title = tfTitle.getText().trim();
            String content = taContent.getText().trim();
            if (title.isEmpty() || content.isEmpty()) {
                JOptionPane.showMessageDialog(this, "标题和内容不能为空"); return;
            }
            String type = (String) cbType.getSelectedItem();
            String receiver = (String) cbReceiver.getSelectedItem();
            String sender = HealthSystem.currentUsername;
            int sentCount = 0;
            boolean ok = true;

            if ("所有用户".equals(receiver)) {
                for (Map<String, Object> u : HealthSystem.DBUtil.getAllUsers()) {
                    String uname = (String) u.get("username");
                    if (HealthSystem.DBUtil.saveNotification(sender, uname, title, content, type)) sentCount++;
                }
            } else if ("异常用户".equals(receiver)) {
                for (Map<String, Object> u : HealthSystem.DBUtil.getAbnormalUsers()) {
                    String uname = (String) u.get("username");
                    if (HealthSystem.DBUtil.saveNotification(sender, uname, title, content, type)) sentCount++;
                }
            } else if (receiver != null && receiver.startsWith("用户:")) {
                String uname = receiver.substring(3);
                if (HealthSystem.DBUtil.saveNotification(sender, uname, title, content, type)) {
                    sentCount = 1;
                } else {
                    ok = false;
                }
            }

            if (ok && sentCount > 0) {
                HealthSystem.DBUtil.logAction("ADMIN", sender, "发送消息", title + " -> " + receiver + ", 人数=" + sentCount);
                JOptionPane.showMessageDialog(this, "消息已发送，共 " + sentCount + " 人");
                tfTitle.setText(""); taContent.setText("");
            } else {
                JOptionPane.showMessageDialog(this, "消息发送失败", "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ==================== 8. 健康顾问功能面板 ====================
    static class HealthAdvisorPanel extends JPanel {
        private JTable userTable;
        private DefaultTableModel userModel;

        HealthAdvisorPanel() {
            setLayout(new BorderLayout(8, 8));
            setBackground(HealthSystem.Theme.BG);
            setBorder(new EmptyBorder(12, 12, 12, 12));

            JPanel topCard = createCard("健康顾问工作台");
            JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            ctrl.setOpaque(false);
            JButton btnProfile = createPrimaryBtn("查看健康档案");
            btnProfile.addActionListener(e -> showProfile());
            JButton btnAdvice = createPrimaryBtn("制定干预建议");
            btnAdvice.addActionListener(e -> makeAdvice());
            JButton btnFollow = createPrimaryBtn("随访记录");
            btnFollow.addActionListener(e -> followUp());
            ctrl.add(btnProfile); ctrl.add(btnAdvice); ctrl.add(btnFollow);
            topCard.add(ctrl, BorderLayout.CENTER);
            add(topCard, BorderLayout.NORTH);

            JPanel tableCard = createCard("用户列表");
            userModel = new DefaultTableModel(new String[]{"ID", "用户名", "性别", "年龄", "状态"}, 0) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            userTable = new JTable(userModel);
            styleTable(userTable);
            tableCard.add(new JScrollPane(userTable), BorderLayout.CENTER);
            add(tableCard, BorderLayout.CENTER);
            loadUsers();
        }

        private void loadUsers() {
            userModel.setRowCount(0);
            for (Map<String, Object> u : HealthSystem.DBUtil.getAllUsers()) {
                userModel.addRow(new Object[]{u.get("id"), u.get("username"), u.get("gender"), u.get("age"), u.get("account_status")});
            }
            if (userModel.getRowCount() > 0) {
                userTable.setRowSelectionInterval(0, 0);
            }
        }

        private int getSelectedUserId() {
            int row = userTable.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this, "请先选择用户"); return -1; }
            return (int) userModel.getValueAt(row, 0);
        }

        private String getSelectedUsername() {
            int row = userTable.getSelectedRow();
            if (row < 0) return null;
            return (String) userModel.getValueAt(row, 1);
        }

        private void showProfile() {
            String username = getSelectedUsername();
            if (username == null) return;
            Map<String, Object> profile = HealthSystem.DBUtil.getUserHealthProfile(username);
            StringBuilder sb = new StringBuilder();
            sb.append("用户: ").append(username).append("\n");
            sb.append("健康记录数: ").append(profile.get("record_count")).append("\n");
            sb.append("饮食记录数: ").append(profile.get("diet_count")).append("\n");
            sb.append("运动记录数: ").append(profile.get("exercise_count")).append("\n");
            @SuppressWarnings("unchecked")
            Map<String, Object> latest = (Map<String, Object>) profile.get("latest_record");
            if (latest != null) {
                sb.append("\n最新记录:\n");
                sb.append("  体重: ").append(latest.get("weight")).append(" kg\n");
                sb.append("  BMI: ").append(latest.get("bmi")).append("\n");
                sb.append("  体脂率: ").append(latest.get("body_fat")).append("%\n");
            }
            JTextArea ta = new JTextArea(sb.toString());
            ta.setFont(HealthSystem.Theme.FONT_BODY);
            ta.setEditable(false);
            ta.setRows(12); ta.setColumns(40);
            JOptionPane.showMessageDialog(this, new JScrollPane(ta), "健康档案", JOptionPane.INFORMATION_MESSAGE);
        }

        private void makeAdvice() {
            String username = getSelectedUsername();
            if (username == null) return;
            JTextArea ta = new JTextArea(6, 40);
            ta.setLineWrap(true);
            Object[] msg = {"为 " + username + " 制定干预建议:", new JScrollPane(ta)};
            if (JOptionPane.showConfirmDialog(this, msg, "干预建议", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                HealthSystem.DBUtil.saveNotification(HealthSystem.currentUsername, username, "健康干预建议", ta.getText(), "健康提醒");
                HealthSystem.DBUtil.logAction("ADMIN", HealthSystem.currentUsername, "制定干预建议", "用户=" + username);
                JOptionPane.showMessageDialog(this, "干预建议已发送给用户");
            }
        }

        private void followUp() {
            String username = getSelectedUsername();
            if (username == null) return;
            JTextArea ta = new JTextArea(5, 35);
            ta.setLineWrap(true);
            Object[] msg = {"随访记录 - " + username, new JScrollPane(ta)};
            if (JOptionPane.showConfirmDialog(this, msg, "随访记录", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                HealthSystem.DBUtil.logAction("ADMIN", HealthSystem.currentUsername, "随访记录", "用户=" + username + ", 记录=" + ta.getText());
                JOptionPane.showMessageDialog(this, "随访记录已保存");
            }
        }
    }
}
