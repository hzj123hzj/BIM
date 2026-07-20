package com.bmi.ui.admin;

import com.bmi.db.DBUtil;

import javafx.geometry.*;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.*;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map;
import java.util.List;

/**
 * 用户管理面板（JavaFX 8 重写 version1 UserManagementPanel）
 * 列表、搜索、查看详情、启用/禁用/冻结、软删/硬删、导出用户 CSV。
 */
public class UserManagePanel extends VBox {
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private final TableView<Map<String, Object>> table = new TableView<>();
    private final TextField tfSearch = new TextField();

    public UserManagePanel() {
        setSpacing(16);
        setPadding(new Insets(18));
        setStyle("-fx-background-color: transparent;");

        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        Label title = new Label("用户管理");
        title.getStyleClass().add("card-title");
        card.getChildren().add(title);

        HBox ctrl = new HBox(8);
        ctrl.setAlignment(Pos.CENTER_LEFT);
        tfSearch.setPromptText("搜索用户名");
        tfSearch.setPrefWidth(160);
        Button btnSearch = new Button("查询");
        btnSearch.getStyleClass().add("button-primary");
        Button btnDetail = new Button("查看详情");
        btnDetail.getStyleClass().add("button-primary");
        Button btnEnable = new Button("启用");
        Button btnDisable = new Button("禁用");
        Button btnFreeze = new Button("冻结");
        Button btnSoft = new Button("软删除");
        Button btnHard = new Button("硬删除");
        Button btnExport = new Button("导出用户CSV");
        btnExport.getStyleClass().add("button-accent");
        for (Button b : new Button[]{btnEnable, btnDisable, btnFreeze, btnSoft, btnHard}) {
            b.getStyleClass().add("button-ghost");
        }
        ctrl.getChildren().addAll(tfSearch, btnSearch, btnDetail, btnEnable, btnDisable,
                btnFreeze, btnSoft, btnHard, btnExport);
        card.getChildren().add(ctrl);

        buildTable();
        card.getChildren().add(table);
        getChildren().add(card);

        btnSearch.setOnAction(e -> loadUsers());
        tfSearch.setOnAction(e -> loadUsers());
        btnDetail.setOnAction(e -> viewDetail());
        btnEnable.setOnAction(e -> setStatus("启用"));
        btnDisable.setOnAction(e -> setStatus("禁用"));
        btnFreeze.setOnAction(e -> setStatus("冻结"));
        btnSoft.setOnAction(e -> softDelete());
        btnHard.setOnAction(e -> hardDelete());
        btnExport.setOnAction(e -> exportUsers());

        loadUsers();
    }

    private void buildTable() {
        table.getColumns().addAll(
                colM("ID", "id", 60),
                colM("用户名", "username", 120),
                colM("性别", "gender", 60),
                colM("年龄", "age", 60),
                colM("身高", "height", 80),
                colM("活动等级", "activity_level", 100),
                colM("状态", "account_status", 80),
                colM("注册时间", "created_at", 150),
                colM("打卡天数", "checkin_days", 80)
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
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
                } else if (item instanceof Date) {
                    setText(sdf.format((Date) item));
                } else {
                    setText(item.toString());
                }
            }
        });
        return c;
    }

    private void loadUsers() {
        table.getItems().clear();
        String kw = tfSearch.getText().trim().toLowerCase();
        for (Map<String, Object> u : DBUtil.getAllUsers()) {
            String un = String.valueOf(u.get("username"));
            if (!kw.isEmpty() && !un.toLowerCase().contains(kw)) continue;
            table.getItems().add(u);
        }
    }

    private int getSelectedId() {
        Map<String, Object> sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            warn("请先选择一行");
            return -1;
        }
        return ((Number) sel.get("id")).intValue();
    }

    private void setStatus(String status) {
        int id = getSelectedId();
        if (id < 0) return;
        if (DBUtil.updateUserStatus(id, status)) {
            DBUtil.logAction("ADMIN", DBUtil.currentUsername, "修改用户状态", "ID=" + id + ", 状态=" + status);
            info("状态更新成功");
            loadUsers();
        } else {
            err("更新失败");
        }
    }

    private void softDelete() {
        int id = getSelectedId();
        if (id < 0) return;
        if (!confirm("确认", "确定软删除该用户吗？")) return;
        if (DBUtil.softDeleteUser(id)) {
            DBUtil.logAction("ADMIN", DBUtil.currentUsername, "软删除用户", "ID=" + id);
            info("已软删除");
            loadUsers();
        }
    }

    private void hardDelete() {
        int id = getSelectedId();
        if (id < 0) return;
        if (!confirm("危险操作", "确定彻底删除该用户吗？此操作不可恢复！")) return;
        if (DBUtil.hardDeleteUser(id)) {
            DBUtil.logAction("ADMIN", DBUtil.currentUsername, "硬删除用户", "ID=" + id);
            info("已删除");
            loadUsers();
        }
    }

    private void viewDetail() {
        Map<String, Object> sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            warn("请先选择用户");
            return;
        }
        String username = String.valueOf(sel.get("username"));
        Map<String, Object> p = DBUtil.getUserHealthProfile(username);
        StringBuilder sb = new StringBuilder();
        sb.append("用户名: ").append(p.get("username")).append("\n");
        sb.append("健康记录数: ").append(p.get("record_count")).append("\n");
        sb.append("饮食记录数: ").append(p.get("diet_count")).append("\n");
        sb.append("运动记录数: ").append(p.get("exercise_count")).append("\n");
        sb.append("成就徽章数: ").append(p.get("achievement_count")).append("\n");
        @SuppressWarnings("unchecked")
        Map<String, Object> latest = (Map<String, Object>) p.get("latest_record");
        if (latest != null) {
            sb.append("\n最新记录:\n");
            sb.append("  体重: ").append(latest.get("weight")).append(" kg\n");
            sb.append("  BMI: ").append(latest.get("bmi")).append("\n");
            sb.append("  体脂率: ").append(latest.get("body_fat")).append("%\n");
            sb.append("  腰围: ").append(latest.get("waist")).append(" cm\n");
            sb.append("  记录日期: ").append(latest.get("record_date")).append("\n");
        }
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("用户详情");
        a.setHeaderText(username);
        TextArea ta = new TextArea(sb.toString());
        ta.setEditable(false);
        ta.setPrefSize(420, 280);
        ta.setStyle("-fx-font-family: 'Microsoft YaHei UI', 'Microsoft YaHei', sans-serif;");
        a.getDialogPane().setContent(ta);
        a.showAndWait();
    }

    private void exportUsers() {
        String csv = DBUtil.exportUsersCSV();
        FileChooser fc = new FileChooser();
        fc.setInitialFileName("users_export.csv");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV 文件", "*.csv"));
        Stage stage = (Stage) getScene().getWindow();
        File f = fc.showSaveDialog(stage);
        if (f == null) return;
        try (FileWriter fw = new FileWriter(f)) {
            fw.write(csv);
            info("导出成功！\n保存路径：\n" + f.getAbsolutePath().replace("\\", "/"));
        } catch (Exception ex) {
            err("导出失败: " + ex.getMessage());
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

    private boolean confirm(String title, String m) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, m, ButtonType.YES, ButtonType.NO);
        a.setTitle(title);
        return a.showAndWait().filter(b -> b == ButtonType.YES).isPresent();
    }
}
