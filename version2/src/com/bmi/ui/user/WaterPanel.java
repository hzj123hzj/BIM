package com.bmi.ui.user;

import com.bmi.db.DBUtil;
import com.bmi.util.HealthCalculator;

import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.*;

/** 饮水记录面板 — 设定每日目标、记录喝水、今日汇总(进度条)、低水量醒目提醒、今日记录 */
public class WaterPanel extends VBox {
    private final Label lblGoal = new Label();
    private final TextField tfCustomGoal = new TextField();
    private final TextField tfAmount = new TextField("200");
    private final TextField tfNote = new TextField();
    private final Label lblSummary = new Label();
    private final ProgressBar progress = new ProgressBar(0);
    private final Label lblWarn = new Label();
    private final TableView<String[]> todayTable = new TableView<>();
    private final ObservableList<String[]> todayData = FXCollections.observableArrayList();

    public WaterPanel() {
        setSpacing(16);
        setPadding(new Insets(18));
        setStyle("-fx-background-color: transparent;");

        tfCustomGoal.setPrefWidth(90);
        tfAmount.setPrefWidth(80);
        tfNote.setPrefWidth(160);

        // ===== 目标设定卡片 =====
        HBox goalRow = new HBox(10);
        goalRow.setAlignment(Pos.CENTER_LEFT);
        Label g1 = new Label("每日目标(ml):"); g1.getStyleClass().add("sub-title");
        Button btnSetGoal = new Button("设定目标");
        btnSetGoal.getStyleClass().add("button-accent");
        Label g2 = new Label("自定义:"); g2.getStyleClass().add("sub-title");
        Button btnResetGoal = new Button("恢复按体重自动");
        btnResetGoal.getStyleClass().add("button-ghost");
        goalRow.getChildren().addAll(g1, lblGoal, g2, tfCustomGoal, btnSetGoal, btnResetGoal);

        VBox goalCard = new VBox(10);
        goalCard.getStyleClass().add("card");
        Label t0 = new Label("每日饮水目标");
        t0.getStyleClass().add("card-title");
        goalCard.getChildren().addAll(t0, goalRow);

        // ===== 录入卡片 =====
        HBox quickRow = new HBox(8);
        quickRow.setAlignment(Pos.CENTER_LEFT);
        for (int q : new int[]{100, 200, 250, 500}) {
            Button b = new Button(q + " ml");
            b.getStyleClass().add("button-primary");
            int v = q;
            b.setOnAction(e -> { tfAmount.setText(String.valueOf(v)); addWater(); });
            quickRow.getChildren().add(b);
        }
        HBox input = new HBox(10);
        input.setAlignment(Pos.CENTER_LEFT);
        Label l1 = new Label("水量(ml):"); l1.getStyleClass().add("sub-title");
        Label l2 = new Label("备注:"); l2.getStyleClass().add("sub-title");
        Button btnAdd = new Button("记录喝水");
        btnAdd.getStyleClass().add("button-primary");
        input.getChildren().addAll(l1, tfAmount, l2, tfNote, btnAdd);

        VBox inputCard = new VBox(10);
        inputCard.getStyleClass().add("card");
        Label t1 = new Label("记录喝水");
        t1.getStyleClass().add("card-title");
        inputCard.getChildren().addAll(t1, quickRow, input);

        // ===== 汇总卡片 =====
        progress.setPrefWidth(360);
        progress.setPrefHeight(18);
        VBox sumBox = new VBox(8, lblSummary, progress, lblWarn);
        sumBox.setAlignment(Pos.TOP_LEFT);
        VBox left = new VBox(8, sumBox);
        left.setAlignment(Pos.TOP_LEFT);

        buildTodayTable();
        VBox right = new VBox(8, new Label("今日饮水记录") {{ getStyleClass().add("card-title"); }}, todayTable);
        VBox.setVgrow(todayTable, Priority.ALWAYS);
        right.setAlignment(Pos.TOP_LEFT);

        HBox top = new HBox(14);
        top.setAlignment(Pos.CENTER_LEFT);
        top.getChildren().addAll(left, right);
        HBox.setHgrow(right, Priority.ALWAYS);

        VBox summaryCard = new VBox(10);
        summaryCard.getStyleClass().add("card");
        Label t2 = new Label("今日饮水汇总");
        t2.getStyleClass().add("card-title");
        summaryCard.getChildren().addAll(t2, top);
        VBox.setVgrow(summaryCard, Priority.ALWAYS);

        getChildren().addAll(goalCard, inputCard, summaryCard);
        VBox.setVgrow(summaryCard, Priority.ALWAYS);

        btnSetGoal.setOnAction(e -> setGoal());
        btnResetGoal.setOnAction(e -> resetGoal());
        btnAdd.setOnAction(e -> addWater());

        refresh();

        // 切换到本页时重新拉取（目标/记录保持最新）
        this.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) refresh();
        });
    }

    private void buildTodayTable() {
        String[] cols = {"时间", "水量(ml)", "备注"};
        for (int i = 0; i < cols.length; i++) {
            final int idx = i;
            TableColumn<String[], String> c = new TableColumn<>(cols[i]);
            c.setCellValueFactory(cb -> new ReadOnlyStringWrapper(cb.getValue()[idx]));
            todayTable.getColumns().add(c);
        }
        todayTable.setItems(todayData);
    }

    private void setGoal() {
        try {
            int g = Integer.parseInt(tfCustomGoal.getText().trim());
            if (g < 500 || g > 6000) { alert("目标范围 500~6000 ml"); return; }
            if (DBUtil.saveWaterGoal(g)) {
                tfCustomGoal.clear();
                refresh();
                alert("每日饮水目标已设为 " + g + " ml");
            } else {
                alert("目标保存失败");
            }
        } catch (NumberFormatException e) {
            alert("请输入有效数字");
        }
    }

    private void resetGoal() {
        // 删除自定义目标，恢复按体重自动估算
        String sql = "DELETE FROM water_goals WHERE username = ?";
        try (java.sql.Connection conn = DBUtil.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, DBUtil.currentUsername);
            ps.executeUpdate();
            refresh();
            alert("已恢复按体重自动估算目标: " + DBUtil.getDailyWaterGoal() + " ml");
        } catch (Exception e) {
            alert("恢复失败: " + e.getMessage());
        }
    }

    private void addWater() {
        try {
            int amount = Integer.parseInt(tfAmount.getText().trim());
            if (amount <= 0 || amount > 3000) { alert("水量范围 1~3000 ml"); return; }
            if (DBUtil.saveWaterRecord(amount, tfNote.getText())) {
                tfNote.clear();
                refresh();
                alert("已记录喝水 " + amount + " ml");
            } else {
                alert("记录失败");
            }
        } catch (NumberFormatException e) {
            alert("请输入有效数字");
        }
    }

    private void refresh() {
        int total = DBUtil.getTodayWaterTotal();
        int goal = DBUtil.getDailyWaterGoal();
        int remain = Math.max(0, goal - total);
        double ratio = goal > 0 ? Math.min(1.0, (double) total / goal) : 0.0;

        lblGoal.setText(goal + " ml");
        lblSummary.setText(String.format("今日已喝 %d / %d ml ｜ 还需 %d ml", total, goal, remain));
        progress.setProgress(ratio);

        if (DBUtil.isWaterIntakeLow()) {
            lblWarn.setText("💧 今日饮水量偏少，该喝水了！");
            lblWarn.setStyle("-fx-text-fill: #E67E22; -fx-font-weight: bold; -fx-font-size: 14px;");
        } else {
            lblWarn.setText("✅ 今日饮水达标，继续保持！");
            lblWarn.setStyle("-fx-text-fill: #27AE60; -fx-font-weight: bold; -fx-font-size: 14px;");
        }

        todayData.setAll(DBUtil.getTodayWaterRecords());
    }

    private void alert(String m) {
        new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait();
    }
}
