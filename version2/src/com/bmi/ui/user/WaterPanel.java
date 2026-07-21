package com.bmi.ui.user;

import com.bmi.db.DBUtil;
import com.bmi.util.HealthCalculator;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

import java.util.ArrayList;
import java.util.List;

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
        big.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 24));
        big.setStyle("-fx-text-fill: #2D5A73; -fx-wrap-text: false;");
        Label small = new Label(sub);
        small.setStyle("-fx-text-fill: #5A7A8C; -fx-font-size: 13px;");
        VBox box = new VBox(2, big, small);
        box.setAlignment(leftAlign ? Pos.CENTER_LEFT : Pos.CENTER);
        box.setPrefWidth(120);
        box.setMinWidth(120);
        return box;
    }

        /** 右侧刻度：满 / 3/4 / 1/2 / 1/4（按圆台体积比例定位，与真实水位对齐） */
        private VBox createScale() {
            VBox scale = new VBox(0);
            scale.setAlignment(Pos.TOP_RIGHT);
            scale.setPrefWidth(60);
            scale.setPrefHeight(320);
            String[] labels = {"满", "3/4", "1/2", "1/4"};
            double[] fracs = {1.0, 0.75, 0.5, 0.25};
            for (int i = 0; i < labels.length; i++) {
                double topY = glass.levelYForRatio(fracs[i]);
                double botY = glass.levelYForRatio(i < fracs.length - 1 ? fracs[i + 1] : 0.0);
                Label l = new Label(labels[i]);
                l.setPrefHeight(Math.max(1, botY - topY));   // 体积越大段越高（杯身上宽），刻度对准水位
                l.setAlignment(Pos.TOP_RIGHT);
                l.setStyle("-fx-text-fill: #7C9EAF; -fx-font-size: 12px;");
                l.setPadding(new Insets(2, 6, 0, 0));
                scale.getChildren().add(l);
            }
            return scale;
        }

    private void refresh() {
        todayMl = DBUtil.getTodayWaterTotal();
        goalMl = DBUtil.getDailyWaterGoal();
        double ratio = goalMl > 0 ? Math.min(1.0, (double) todayMl / goalMl) : 0.0;

        lblToday.setText(todayMl + "ml");
        lblGoal.setText(goalMl + "ml");
        if (todayMl >= goalMl) {
            int exceed = todayMl - goalMl;
            lblHint.setText(exceed > 0 ? "今日饮水已达标 (超出 " + exceed + "ml)" : "今日饮水已达标");
        } else {
            lblHint.setText("请选择杯型");
        }
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

    // ==================== 可视化水杯组件（双正弦波 + 上浮气泡的物理化流动） ====================
    private static class WaterGlass extends Pane {
        private final double w, h, botW;
        private final DoubleProperty ratio = new SimpleDoubleProperty(0);
        private double phase = 0;
        private final Path glassBody;            // 玻璃杯身（上宽下窄圆台）
        private final Rectangle highlight;       // 玻璃高光反射
        private final Path backWave;             // 后层波（浅、慢）
        private final Path frontWave;            // 前层波（深、快）
        private final Path crest;                // 波峰亮线
        private final List<Circle> bubbles = new ArrayList<>();
        private final List<Double> bubbleX = new ArrayList<>();
        private final List<Double> bubbleSpeed = new ArrayList<>();
        private long last = 0;
        private final AnimationTimer timer;

        /** 上宽下窄圆台轮廓（顶宽=w，底宽=botW），圆角线连接更柔和 */
        private Path buildGlassPath() {
            Path p = new Path();
            double topL = 0, topR = w;
            double botL = (w - botW) / 2.0, botR = (w + botW) / 2.0;
            p.getElements().addAll(
                    new MoveTo(topL, 0),
                    new LineTo(topR, 0),
                    new LineTo(botR, h),
                    new LineTo(botL, h),
                    new ClosePath());
            p.setStrokeLineJoin(StrokeLineJoin.ROUND);
            return p;
        }

        WaterGlass(double width, double height) {
            this.w = width;
            this.h = height;
            setPrefSize(width, height);
            setMaxSize(width, height);
            this.botW = width * 0.8;            // 底部收窄为顶宽的 80%，呈圆台

            // 杯身：浅色半透明，渐变描边更有质感
            glassBody = buildGlassPath();
            glassBody.setFill(Color.color(1, 1, 1, 0.22));
            LinearGradient stroke = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.color(1, 1, 1, 0.95)),
                    new Stop(0.5, Color.color(0.75, 0.88, 0.98, 0.6)),
                    new Stop(1, Color.color(1, 1, 1, 0.95)));
            glassBody.setStroke(stroke);
            glassBody.setStrokeWidth(3);
            glassBody.setStrokeLineJoin(StrokeLineJoin.ROUND);

            // 左侧竖向高光，模拟玻璃反光
            highlight = new Rectangle(width * 0.14, height * 0.08, width * 0.09, height * 0.80);
            highlight.setArcWidth(12);
            highlight.setArcHeight(12);
            highlight.setFill(Color.color(1, 1, 1, 0.5));

            backWave = new Path();
            backWave.setClip(buildGlassPath());
            backWave.setFill(Color.color(0.55, 0.82, 0.96, 0.5));

            frontWave = new Path();
            frontWave.setClip(buildGlassPath());
            LinearGradient water = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.color(0.45, 0.80, 0.97, 0.92)),
                    new Stop(1, Color.color(0.18, 0.54, 0.90, 0.95)));
            frontWave.setFill(water);

            crest = new Path();
            crest.setClip(buildGlassPath());
            crest.setFill(null);
            crest.setStroke(Color.color(0.88, 0.96, 1.0, 0.75));
            crest.setStrokeWidth(2);

            // 上浮气泡：受“浮力”持续上升，到水面则回到底部
            for (int i = 0; i < 6; i++) {
                Circle b = new Circle(1.6 + Math.random() * 2.2);
                b.setFill(Color.color(1, 1, 1, 0.55));
                // 在底部（最窄处）范围内生成，上升过程中杯身变宽，气泡始终在杯内
                double bx = w / 2 + (Math.random() * 2 - 1) * (botW / 2 - 4);
                double by = Math.random() * height;
                b.setLayoutX(bx);
                b.setLayoutY(by);
                bubbles.add(b);
                bubbleX.add(bx);
                bubbleSpeed.add(18 + Math.random() * 34);
            }

            getChildren().addAll(backWave, frontWave, crest);
            getChildren().addAll(bubbles);
            getChildren().addAll(glassBody, highlight);

            ratio.addListener((obs, o, n) -> drawWaves());
            drawWaves();

            timer = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    if (last == 0) last = now;
                    double dt = (now - last) / 1e9;
                    last = now;
                    phase += dt * 2.2;                         // 波相位推进
                    double level = currentLevel();
                    for (int i = 0; i < bubbles.size(); i++) {
                        Circle b = bubbles.get(i);
                        double y = b.getLayoutY() - bubbleSpeed.get(i) * dt;
                        if (y < level + 2) {                    // 到达水面 -> 回到底部（循环）
                            y = h - 6;
                            bubbleX.set(i, w / 2 + (Math.random() * 2 - 1) * (botW / 2 - 4));
                        }
                        b.setLayoutX(bubbleX.get(i));
                        b.setLayoutY(y);
                        b.setVisible(ratio.get() > 0.01 && Math.abs(bubbleX.get(i) - w / 2) < w / 2);
                    }
                    drawWaves();
                }
            };
            sceneProperty().addListener((obs, oldS, newS) -> {
                if (newS != null) timer.start();
                else { timer.stop(); last = 0; }
            });
        }

        void setFillRatio(double r) {
            ratio.set(Math.max(0, Math.min(1, r)));
        }

        private double currentLevel() {
            return h - heightForRatio(ratio.get());
        }

        /** 由底部起算、填充比例为 ratio 时的水面高度（圆台体积积分反解，二分） */
        private double heightForRatio(double ratio) {
            if (ratio <= 0) return 0;
            if (ratio >= 1) return h;
            double lo = 0, hi = h;
            for (int i = 0; i < 42; i++) {
                double mid = (lo + hi) / 2;
                if (volumeFraction(mid) < ratio) lo = mid; else hi = mid;
            }
            return (lo + hi) / 2;
        }

        /** 从底部到高度 y 的累积体积占满杯体积的比例（圆台：V=π∫halfW²） */
        private double volumeFraction(double y) {
            double rb = botW / 2.0, rt = w / 2.0, k = (rt - rb) / h;
            double vY = rb * rb * y + rb * k * y * y + (k * k / 3.0) * y * y * y;
            double vH = rb * rb * h + rb * k * h * h + (k * k / 3.0) * h * h * h;
            return vH > 0 ? vY / vH : 0;
        }

        /** 供右侧刻度对齐：填充比例对应的水面 y 像素（自顶部起算） */
        public double levelYForRatio(double ratio) {
            return h - heightForRatio(ratio);
        }

        /** 用双正弦波（前后两层不同相位/幅度）绘制流动水面 */
        private void drawWaves() {
            if (ratio.get() <= 0.01) {
                frontWave.getElements().clear();
                backWave.getElements().clear();
                crest.getElements().clear();
                return;
            }
            double level = currentLevel();
            double amp = 5.0;
            double freq = 0.045;
            setWave(frontWave, level, amp, freq, phase, true);
            setWave(backWave, level, amp * 1.4, freq * 0.8, phase * 0.7 + 1.6, true);
            setWave(crest, level, amp, freq, phase, false);
        }

        private void setWave(Path path, double level, double amp, double freq, double ph, boolean closeBottom) {
            List<PathElement> els = new ArrayList<>();
            els.add(new MoveTo(0, level + amp * Math.sin(0 * freq + ph)));
            for (double x = 6; x <= w; x += 6) {
                els.add(new LineTo(x, level + amp * Math.sin(x * freq + ph)));
            }
            if (closeBottom) {
                els.add(new LineTo(w, h));
                els.add(new LineTo(0, h));
                els.add(new ClosePath());
            }
            path.getElements().setAll(FXCollections.observableArrayList(els));
        }
    }
}
