package com.bmi.ui.user;

import com.bmi.db.DBUtil;
import com.bmi.util.HealthCalculator;

import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

/**
 * 饮水记录面板 — 参考移动端「今日饮水」App 风格：
 * 浅蓝渐变背景、顶部杯型预设、中间可视化水杯、左侧已喝/目标、右侧刻度、底部喝光/自定义/设置。
 */
public class WaterPanel extends VBox {

    private int goalMl = 2000;
    private int todayMl = 0;
    private int[] cupSizes = {100, 250, 500};

    private final WaterGlass glass = new WaterGlass(180, 320);
    private final Label lblToday = new Label("0ml");
    private final Label lblGoal = new Label("目标 2000ml");
    private final Label lblHint = new Label("请选择杯型");

    public WaterPanel() {
        setSpacing(18);
        setPadding(new Insets(18));
        setAlignment(Pos.TOP_CENTER);
        setStyle("-fx-background-color: linear-gradient(to bottom, #A3CFFA 0%, #D9EEFC 60%, #E8F5FC 100%);");

        // 标题
        Label title = new Label("今日饮水");
        title.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 22));
        title.setStyle("-fx-text-fill: #FFFFFF;");
        title.setPadding(new Insets(6, 0, 0, 0));

        // 顶部杯型预设
        HBox presets = new HBox(16);
        presets.setAlignment(Pos.CENTER);
        for (int i = 0; i < cupSizes.length; i++) {
            int ml = cupSizes[i];
            VBox cup = createCupButton(ml);
            cup.setOnMouseClicked(e -> recordWater(ml, ml + "ml杯"));
            presets.getChildren().add(cup);
        }

        // 提示文字
        lblHint.setFont(Font.font("Microsoft YaHei", FontWeight.NORMAL, 14));
        lblHint.setStyle("-fx-text-fill: #5A7A8C;");

        // 中间可视化区域：左侧已喝/目标 + 水杯 + 右侧刻度
        HBox visual = new HBox(18);
        visual.setAlignment(Pos.CENTER);
        visual.setPadding(new Insets(8, 0, 8, 0));

        VBox left = createStatBox(lblToday, "已喝", true);
        VBox center = new VBox(glass);
        center.setAlignment(Pos.CENTER);
        VBox right = createScale();

        HBox.setHgrow(center, Priority.ALWAYS);
        visual.getChildren().addAll(left, center, right);

        // 底部操作栏
        HBox actions = new HBox(24);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(10, 0, 0, 0));

        Button btnSettings = new Button("饮水设置");
        btnSettings.getStyleClass().addAll("water-text-btn");
        btnSettings.setOnAction(e -> showSettingsDialog());

        Button btnDrinkAll = new Button("喝光");
        btnDrinkAll.getStyleClass().addAll("water-fab");
        btnDrinkAll.setOnAction(e -> drinkAll());

        Button btnCustom = new Button("自定义");
        btnCustom.getStyleClass().addAll("water-text-btn");
        btnCustom.setOnAction(e -> showCustomDialog());

        actions.getChildren().addAll(btnSettings, btnDrinkAll, btnCustom);

        getChildren().addAll(title, presets, lblHint, visual,
                new HBox(8, new Label("目标: ") {{ setStyle("-fx-text-fill:#5A7A8C;"); }}, lblGoal) {{
                    setAlignment(Pos.CENTER);
                }}, actions);

        // 切换到本页时刷新（包括从顶栏快捷按钮进入）
        this.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) refresh();
        });

        Platform.runLater(this::refresh);
    }

    /** 创建一个杯型预设按钮（正方形蓝色块，带 + 和 ml） */
    private VBox createCupButton(int ml) {
        Label plus = new Label("+");
        plus.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 24));
        plus.setStyle("-fx-text-fill: #FFFFFF;");
        Label cup = new Label("杯子");
        cup.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 12px;");
        Label amt = new Label(ml + "ml");
        amt.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 11px;");

        VBox box = new VBox(2, plus, cup, amt);
        box.setAlignment(Pos.CENTER);
        box.setPrefSize(76, 76);
        box.setStyle("-fx-background-color: #4A9FE7; -fx-background-radius: 16; -fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(74,159,231,0.35), 8, 0.2, 0, 3);");
        return box;
    }

    private VBox createStatBox(Label big, String sub, boolean leftAlign) {
        big.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 28));
        big.setStyle("-fx-text-fill: #2D5A73;");
        Label small = new Label(sub);
        small.setStyle("-fx-text-fill: #5A7A8C; -fx-font-size: 13px;");
        VBox box = new VBox(2, big, small);
        box.setAlignment(leftAlign ? Pos.CENTER_LEFT : Pos.CENTER);
        box.setPrefWidth(90);
        return box;
    }

    /** 右侧刻度：1/4, 1/2, 3/4, 满 */
    private VBox createScale() {
        VBox scale = new VBox(0);
        scale.setAlignment(Pos.CENTER_RIGHT);
        scale.setPrefWidth(60);
        String[] labels = {"满", "3/4", "1/2", "1/4"};
        for (String lab : labels) {
            Label l = new Label(lab);
            l.setPrefHeight(70);
            l.setAlignment(Pos.CENTER_RIGHT);
            l.setStyle("-fx-text-fill: #7C9EAF; -fx-font-size: 12px;");
            l.setPadding(new Insets(0, 6, 0, 0));
            scale.getChildren().add(l);
        }
        return scale;
    }

    private void refresh() {
        todayMl = DBUtil.getTodayWaterTotal();
        goalMl = DBUtil.getDailyWaterGoal();
        double ratio = goalMl > 0 ? Math.min(1.0, (double) todayMl / goalMl) : 0.0;

        lblToday.setText(todayMl + "ml");
        lblGoal.setText("目标 " + goalMl + "ml");
        lblHint.setText(todayMl >= goalMl ? "今日饮水已达标" : "请选择杯型");
        glass.setFillRatio(ratio);
    }

    private void recordWater(int ml, String note) {
        if (ml <= 0) return;
        if (DBUtil.saveWaterRecord(ml, note)) {
            refresh();
        } else {
            alert("记录失败");
        }
    }

    private void drinkAll() {
        int remain = Math.max(0, goalMl - todayMl);
        if (remain <= 0) {
            alert("今日目标已达成，继续保持！");
            return;
        }
        recordWater(remain, "喝光");
    }

    private void showCustomDialog() {
        TextInputDialog dialog = new TextInputDialog("200");
        dialog.setTitle("自定义饮水");
        dialog.setHeaderText(null);
        dialog.setContentText("输入水量 (ml):");
        dialog.showAndWait().ifPresent(s -> {
            try {
                int ml = Integer.parseInt(s.trim());
                if (ml > 0 && ml <= 3000) recordWater(ml, "自定义");
                else alert("水量范围 1~3000 ml");
            } catch (NumberFormatException ex) {
                alert("请输入有效数字");
            }
        });
    }

    private void showSettingsDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("饮水设置");
        dialog.setHeaderText(null);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));

        TextField tfGoal = new TextField(String.valueOf(goalMl));
        Label auto = new Label("按体重自动估算: " + HealthCalculator.calcDailyWaterGoal(DBUtil.currentWeight) + "ml");
        auto.setStyle("-fx-text-fill: #5A7A8C; -fx-font-size: 12px;");

        grid.add(new Label("每日目标 (ml):"), 0, 0);
        grid.add(tfGoal, 1, 0);
        grid.add(auto, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    int g = Integer.parseInt(tfGoal.getText().trim());
                    if (g >= 500 && g <= 6000) {
                        DBUtil.saveWaterGoal(g);
                        refresh();
                    } else {
                        alert("目标范围 500~6000 ml");
                    }
                } catch (NumberFormatException e) {
                    alert("请输入有效数字");
                }
            }
            return null;
        });
        dialog.showAndWait();
    }

    private void alert(String m) {
        new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait();
    }

    // ==================== 可视化水杯组件 ====================
    private static class WaterGlass extends Pane {
        private final double w, h;
        private final DoubleProperty ratio = new SimpleDoubleProperty(0);
        private final Rectangle water;
        private final Rectangle glass;

        WaterGlass(double width, double height) {
            this.w = width;
            this.h = height;
            setPrefSize(width, height);
            setMaxSize(width, height);

            // 水杯外轮廓（圆角透明杯身）
            glass = new Rectangle(0, 0, width, height);
            glass.setArcWidth(32);
            glass.setArcHeight(32);
            glass.setFill(Color.color(1, 1, 1, 0.45));
            glass.setStroke(Color.color(1, 1, 1, 0.75));
            glass.setStrokeWidth(2.5);

            // 水面
            water = new Rectangle(0, 0, width, 0);
            water.setArcWidth(32);
            water.setArcHeight(32);
            LinearGradient grad = new LinearGradient(
                    0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.color(0.45, 0.78, 0.96, 0.95)),
                    new Stop(1, Color.color(0.25, 0.58, 0.90, 0.95)));
            water.setFill(grad);

            // 用和玻璃相同的圆角矩形裁剪，让水位看起来在杯子里
            Rectangle clip = new Rectangle(0, 0, width, height);
            clip.setArcWidth(32);
            clip.setArcHeight(32);
            water.setClip(clip);

            getChildren().addAll(water, glass);

            ratio.addListener((obs, o, n) -> updateWater());
            Platform.runLater(this::updateWater);
        }

        void setFillRatio(double r) {
            ratio.set(Math.max(0, Math.min(1, r)));
        }

        private void updateWater() {
            double r = ratio.get();
            double wh = h * r;
            water.setHeight(wh);
            water.setY(h - wh);
            water.setVisible(wh > 1);
        }
    }
}
