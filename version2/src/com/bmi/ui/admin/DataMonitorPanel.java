package com.bmi.ui.admin;

import com.bmi.db.DBUtil;
import com.bmi.util.Theme;

import javafx.geometry.*;
import javafx.scene.Cursor;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map;
import java.util.List;

/**
 * 数据监控面板（JavaFX 8 重写 version1 DataDashboardPanel）
 * 4 个指标卡片（可点击弹用户列表）+ 异常用户列表 + 制定干预建议按钮。
 */
public class DataMonitorPanel extends VBox {
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private final Label lblTotal = new Label("0");
    private final Label lblActive = new Label("0");
    private final Label lblToday = new Label("0");
    private final Label lblAvg = new Label("0");
    private final TableView<Map<String, Object>> abnormalTable = new TableView<>();

    public DataMonitorPanel() {
        setSpacing(16);
        setPadding(new Insets(18));
        setStyle("-fx-background-color: transparent;");

        // 顶部指标卡片
        HBox cards = new HBox(12);
        cards.setAlignment(Pos.CENTER);
        cards.getChildren().addAll(
                metricCard("总用户数", lblTotal, Theme.hex(Theme.PRIMARY), () -> showUserList("total")),
                metricCard("7日活跃用户", lblActive, Theme.hex(Theme.SUCCESS), () -> showUserList("active7")),
                metricCard("今日打卡", lblToday, Theme.hex(Theme.ACCENT), () -> showUserList("today")),
                metricCard("平均BMI", lblAvg, Theme.hex(Theme.WARNING), () -> showUserList("avgbmi"))
        );
        getChildren().add(cards);

        // 异常用户 + 操作栏
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        Label title = new Label("异常用户列表（双击查看详情）");
        title.getStyleClass().add("card-title");
        card.getChildren().add(title);

        HBox ctrl = new HBox(8);
        ctrl.setAlignment(Pos.CENTER_LEFT);
        Button btnRefresh = new Button("刷新数据");
        Button btnIntervene = new Button("制定干预建议");
        btnRefresh.getStyleClass().add("button-primary");
        btnIntervene.getStyleClass().add("button-accent");
        ctrl.getChildren().addAll(btnRefresh, btnIntervene);
        card.getChildren().add(ctrl);

        abnormalTable.getColumns().addAll(
                colM("用户名", "username", 140),
                colM("BMI", "bmi", 100),
                colM("体重变化", "weight_diff", 110),
                colM("异常原因", "reason", 200)
        );
        abnormalTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        abnormalTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Map<String, Object> sel = abnormalTable.getSelectionModel().getSelectedItem();
                if (sel != null) showProfile(String.valueOf(sel.get("username")));
            }
        });
        card.getChildren().add(abnormalTable);
        getChildren().add(card);

        btnRefresh.setOnAction(e -> refresh());
        btnIntervene.setOnAction(e -> intervene());

        refresh();
    }

    private VBox metricCard(String title, Label valueLabel, String colorHex, Runnable action) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER);
        card.setPrefSize(220, 110);
        card.setCursor(Cursor.HAND);
        valueLabel.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: " + colorHex + ";");
        Label t = new Label(title);
        t.getStyleClass().add("sub-title");
        card.getChildren().addAll(valueLabel, t);
        card.setOnMouseClicked(e -> action.run());
        return card;
    }

    private TableColumn<Map<String, Object>, Object> colM(String name, String key, double w) {
        TableColumn<Map<String, Object>, Object> c = new TableColumn<>(name);
        c.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().get(key)));
        c.setPrefWidth(w);
        c.setCellFactory(tc -> new TableCell<Map<String, Object>, Object>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else if (item instanceof Number) {
                    setText(String.format("%.1f", ((Number) item).doubleValue()));
                } else {
                    setText(item.toString());
                }
            }
        });
        return c;
    }

    private void refresh() {
        Map<String, Object> stats = DBUtil.getGlobalStats();
        lblTotal.setText(String.valueOf(stats.get("total_users")));
        lblActive.setText(String.valueOf(stats.get("active_users_7d")));
        lblToday.setText(String.valueOf(stats.get("today_checkin")));
        lblAvg.setText(String.format("%.1f", ((Number) stats.get("avg_bmi")).doubleValue()));

        abnormalTable.getItems().clear();
        for (Map<String, Object> u : DBUtil.getAbnormalUsers()) {
            abnormalTable.getItems().add(u);
        }
    }

    private void showUserList(String type) {
        TableView<Map<String, Object>> t = new TableView<>();
        List<Map<String, Object>> data;
        String title;
        switch (type) {
            case "total":
                title = "总用户列表";
                t.getColumns().addAll(
                        colM("用户名", "username", 120), colM("性别", "gender", 70),
                        colM("年龄", "age", 70), colM("身高", "height", 90),
                        colM("活跃度", "activity_level", 110), colM("注册时间", "created_at", 150));
                data = DBUtil.getTotalUsersList();
                break;
            case "active7":
                title = "7日活跃用户列表";
                t.getColumns().addAll(
                        colM("用户名", "username", 120), colM("性别", "gender", 70),
                        colM("年龄", "age", 70), colM("最近记录日期", "last_date", 140),
                        colM("7天内记录数", "record_count", 120));
                data = DBUtil.getActiveUsers7dList();
                break;
            case "today":
                title = "今日打卡用户列表";
                t.getColumns().addAll(
                        colM("用户名", "username", 120), colM("性别", "gender", 70),
                        colM("年龄", "age", 70), colM("BMI", "bmi", 90),
                        colM("记录数", "record_count", 90), colM("打卡日期", "record_date", 140));
                data = DBUtil.getTodayCheckinUsersList();
                break;
            case "avgbmi":
                title = "平均 BMI 用户列表";
                t.getColumns().addAll(
                        colM("用户名", "username", 120), colM("性别", "gender", 70),
                        colM("年龄", "age", 70), colM("BMI", "bmi", 90),
                        colM("体重", "weight", 90), colM("体脂率", "body_fat", 90),
                        colM("记录日期", "record_date", 140));
                data = DBUtil.getAvgBMIUsersList();
                break;
            default:
                return;
        }
        t.getItems().addAll(data);
        t.setPrefSize(640, 360);
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title + " (共 " + data.size() + " 人)");
        a.setHeaderText(null);
        a.getDialogPane().setContent(t);
        a.setResizable(true);
        a.showAndWait();
    }

    private void showProfile(String username) {
        Map<String, Object> p = DBUtil.getUserHealthProfile(username);
        StringBuilder sb = new StringBuilder();
        sb.append("用户: ").append(p.get("username")).append("\n");
        sb.append("健康记录数: ").append(p.get("record_count")).append("\n");
        sb.append("饮食记录数: ").append(p.get("diet_count")).append("\n");
        sb.append("运动记录数: ").append(p.get("exercise_count")).append("\n");
        @SuppressWarnings("unchecked")
        Map<String, Object> latest = (Map<String, Object>) p.get("latest_record");
        if (latest != null) {
            sb.append("\n最新记录:\n");
            sb.append("  体重: ").append(latest.get("weight")).append(" kg\n");
            sb.append("  BMI: ").append(latest.get("bmi")).append("\n");
            sb.append("  体脂率: ").append(latest.get("body_fat")).append("%\n");
            sb.append("  身体年龄: ").append(latest.get("body_age")).append(" 岁\n");
            sb.append("  类型: ").append(latest.get("body_type")).append("\n");
        }
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("用户健康档案");
        a.setHeaderText(username);
        TextArea ta = new TextArea(sb.toString());
        ta.setEditable(false);
        ta.setPrefSize(420, 280);
        ta.getStyleClass().add("text-field");
        a.getDialogPane().setContent(ta);
        a.showAndWait();
    }

    private void intervene() {
        // 1. 选择用户：从所有用户中选，不局限于异常用户列表
        List<Map<String, Object>> users = DBUtil.getTotalUsersList();
        if (users.isEmpty()) {
            warn("系统中暂无用户");
            return;
        }

        TableView<Map<String, Object>> t = new TableView<>();
        t.getColumns().addAll(
                colM("用户名", "username", 120), colM("性别", "gender", 70),
                colM("年龄", "age", 70), colM("身高", "height", 90),
                colM("活跃度", "activity_level", 110), colM("注册时间", "created_at", 150));
        t.getItems().addAll(users);
        t.setPrefSize(640, 360);
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Alert choose = new Alert(Alert.AlertType.CONFIRMATION, "", ButtonType.OK, ButtonType.CANCEL);
        choose.setTitle("选择要干预的用户");
        choose.setHeaderText("所有用户列表（共 " + users.size() + " 人）");
        choose.getDialogPane().setContent(t);
        choose.setResizable(true);
        if (!choose.showAndWait().filter(b -> b == ButtonType.OK).isPresent()) return;

        Map<String, Object> sel = t.getSelectionModel().getSelectedItem();
        if (sel == null) {
            warn("请选择一个用户");
            return;
        }
        String username = String.valueOf(sel.get("username"));

        // 2. 编辑干预建议
        TextField tfTitle = new TextField("健康干预建议");
        TextArea taContent = new TextArea();
        taContent.setPrefRowCount(5);
        taContent.setWrapText(true);
        tfTitle.getStyleClass().add("text-field");
        taContent.getStyleClass().add("text-field");

        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(8);
        g.addRow(0, new Label("接收用户:"), new Label(username));
        g.addRow(1, new Label("标题:"), tfTitle);
        g.add(new Label("内容:"), 0, 2);
        g.add(taContent, 1, 2);

        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "", ButtonType.OK, ButtonType.CANCEL);
        a.setTitle("制定干预建议 - " + username);
        a.getDialogPane().setContent(g);
        if (a.showAndWait().filter(b -> b == ButtonType.OK).isPresent()) {
            String title = tfTitle.getText().trim();
            String content = taContent.getText().trim();
            if (title.isEmpty() || content.isEmpty()) {
                warn("标题和内容不能为空");
                return;
            }
            if (DBUtil.saveNotification(DBUtil.currentUsername, username, title, content, "干预建议")) {
                DBUtil.logAction("ADMIN", DBUtil.currentUsername, "制定干预建议", "接收者=" + username);
                info("干预建议已发送给 " + username);
            } else {
                err("发送失败");
            }
        }
    }

    private void info(String m) {
        new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait();
    }

    private void warn(String m) {
        new Alert(Alert.AlertType.WARNING, m, ButtonType.OK).showAndWait();
    }

    private void err(String m) {
        new Alert(Alert.AlertType.ERROR, m, ButtonType.OK).showAndWait();
    }
}
