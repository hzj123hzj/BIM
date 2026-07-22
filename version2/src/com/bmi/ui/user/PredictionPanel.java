package com.bmi.ui.user;

import com.bmi.db.DBUtil;
import com.bmi.ui.ReportDialog;
import com.bmi.util.HealthCalculator;
import com.bmi.util.Theme;

import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.chart.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.Node;

import java.util.*;
import java.util.Map;
import java.util.List;
import java.text.SimpleDateFormat;

/** 预测分析面板 — 线性回归趋势预测 + 目标达成预测 + 风险评估 */
public class PredictionPanel extends VBox {
    private final LineChart<Number, Number> chart;
    private final ComboBox<String> cbMetric = new ComboBox<>();
    private String reportContent = "";
    private final Label lbl7 = new Label("--");
    private final Label lbl14 = new Label("--");
    private final Label lbl30 = new Label("--");
    private final Label lblRisk = new Label("--");
    private final Label lblCalDiff = new Label("--");
    private final Label lblGoal = new Label("--");
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    public PredictionPanel() {
        setSpacing(16);
        setPadding(new Insets(18));
        setStyle("-fx-background-color: transparent;");

        cbMetric.getItems().addAll("体重", "BMI", "体脂率");
        cbMetric.setValue("体重");

        HBox ctrl = new HBox(10);
        ctrl.setAlignment(Pos.CENTER_LEFT);
        Label lblMetric = new Label("预测指标:");
        lblMetric.getStyleClass().add("sub-title");
        Button btnRefresh = new Button("刷新预测结果");
        btnRefresh.getStyleClass().add("button-primary");
        Button btnReport = new Button("查看预测报告");
        btnReport.getStyleClass().add("button-accent");
        btnReport.setOnAction(e -> showReportDialog("预测分析报告", reportContent));
        Label hint = new Label("基于历史数据进行线性回归趋势预测");
        hint.getStyleClass().add("hint");
        ctrl.getChildren().addAll(lblMetric, cbMetric, btnRefresh, btnReport, hint);

        VBox ctrlCard = new VBox(10);
        ctrlCard.getStyleClass().add("card");
        Label t1 = new Label("预测分析");
        t1.getStyleClass().add("card-title");
        ctrlCard.getChildren().addAll(t1, ctrl);

        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("距首次记录天数");
        xAxis.setMinorTickVisible(false);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("数值");
        chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("趋势预测");
        chart.setPrefHeight(380);
        chart.setMinHeight(260);
        chart.setCreateSymbols(true);
        chart.setStyle("-fx-series-color: " + Theme.hex(Theme.PRIMARY) + ";");

        VBox chartCard = new VBox(10);
        chartCard.getStyleClass().add("card");
        VBox.setVgrow(chartCard, Priority.ALWAYS);
        Label t2 = new Label("历史 + 预测趋势图");
        t2.getStyleClass().add("card-title");
        chartCard.getChildren().addAll(t2, chart);

        // 右侧栏：预测结果 + 热量差 + 目标达成
        VBox rightCol = new VBox(16);
        rightCol.setPrefWidth(340);
        rightCol.setMaxWidth(360);

        VBox predCard = new VBox(10);
        predCard.getStyleClass().add("card");
        Label t3 = new Label("预测结果概览");
        t3.getStyleClass().add("card-title");
        GridPane predGrid = new GridPane();
        predGrid.setHgap(24);
        predGrid.setVgap(14);
        predGrid.setPadding(new Insets(4));
        predGrid.addRow(0, predBox("7 天后", lbl7, Theme.hex(Theme.PRIMARY)),
                             predBox("14 天后", lbl14, Theme.hex(Theme.ACCENT)));
        predGrid.addRow(1, predBox("30 天后", lbl30, Theme.hex(Theme.SUCCESS)),
                             predBox("30 天后风险", lblRisk, Theme.hex(Theme.DANGER)));
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(50);
        predGrid.getColumnConstraints().addAll(c1, c2);
        predCard.getChildren().addAll(t3, predGrid);

        VBox calCard = new VBox(10);
        calCard.getStyleClass().add("card");
        Label t4 = new Label("每日热量差");
        t4.getStyleClass().add("card-title");
        lblCalDiff.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + Theme.hex(Theme.PRIMARY) + ";");
        lblCalDiff.setWrapText(true);
        calCard.getChildren().addAll(t4, lblCalDiff);

        VBox goalCard = new VBox(10);
        goalCard.getStyleClass().add("card");
        Label t5 = new Label("目标达成预测");
        t5.getStyleClass().add("card-title");
        lblGoal.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + Theme.hex(Theme.ACCENT) + ";");
        lblGoal.setWrapText(true);
        goalCard.getChildren().addAll(t5, lblGoal);

        rightCol.getChildren().addAll(predCard, calCard, goalCard);

        HBox main = new HBox(16);
        main.setSpacing(16);
        HBox.setHgrow(chartCard, Priority.ALWAYS);
        main.getChildren().addAll(chartCard, rightCol);

        getChildren().addAll(ctrlCard, main);
        btnRefresh.setOnAction(e -> refresh());
        cbMetric.setOnAction(e -> refresh());
        refresh();
    }

    private VBox predBox(String title, Label valueLabel, String color) {
        VBox vb = new VBox(4);
        vb.setMinHeight(58);
        Label lt = new Label(title);
        lt.getStyleClass().add("sub-title");
        valueLabel.setWrapText(true);
        valueLabel.setMinHeight(38);
        valueLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        vb.getChildren().addAll(lt, valueLabel);
        return vb;
    }

    private void refresh() {
        List<Map<String, Object>> records = DBUtil.getHealthRecords(60);
        if (records.size() < 3) {
            reportContent = "数据不足, 至少需要 3 条健康记录才能进行预测分析\n当前记录数: " + records.size();
            lbl7.setText("--");
            lbl14.setText("--");
            lbl30.setText("--");
            lblRisk.setText("数据不足");
            lblCalDiff.setText("--");
            lblGoal.setText("--");
            chart.getData().clear();
            return;
        }

        String metric = cbMetric.getValue();
        List<Date> dates = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        List<Double> bmis = new ArrayList<>();
        List<Double> bodyFats = new ArrayList<>();
        for (int i = records.size() - 1; i >= 0; i--) {
            Map<String, Object> r = records.get(i);
            dates.add((Date) r.get("record_date"));
            weights.add(num(r, "weight"));
            bmis.add(num(r, "bmi"));
            bodyFats.add(num(r, "body_fat"));
        }

        Map<String, Object> latest = records.get(0);
        double currentWeight = num(latest, "weight");
        double currentBMI = num(latest, "bmi");
        double tdee = num(latest, "tdee");

        // 所选指标的历史序列
        List<Double> metricVals = new ArrayList<>();
        for (int i = 0; i < weights.size(); i++) {
            metricVals.add(metric.equals("BMI") ? bmis.get(i) : metric.equals("体脂率") ? bodyFats.get(i) : weights.get(i));
        }

        // 指标维度预测（复用线性回归，不换模型）
        double mp7 = HealthCalculator.predictTrend(dates, metricVals, 7);
        double mp14 = HealthCalculator.predictTrend(dates, metricVals, 14);
        double mp30 = HealthCalculator.predictTrend(dates, metricVals, 30);

        // 体重维度预测（用于 BMI 风险与目标，沿用核心逻辑）
        double pred30 = HealthCalculator.predictTrend(dates, weights, 30);
        double predBMI30 = HealthCalculator.calcBMI(pred30, DBUtil.currentHeight);

        String trend = HealthCalculator.trendDirection(dates, metricVals);

        int exerciseCal = DBUtil.getTodayExerciseCalories();
        int[] diet = DBUtil.getTodayDietSummary();
        int intakeCal = diet[0];
        // 热量差 = TDEE - 摄入量 (TDEE 已含活动/运动消耗, 不再叠加运动记录避免重复计算)
        double dailyDeficit = tdee - intakeCal;

        Map<String, Object> goal = DBUtil.getGoal();
        String risk = HealthCalculator.assessRisk(predBMI30);
        String riskLevel = predBMI30 >= 28.0 ? "高风险" : (predBMI30 >= 24.0 || predBMI30 < 18.5 ? "中风险" : "低风险");

        String unit = metric.equals("体重") ? " kg" : metric.equals("体脂率") ? "%" : "";
        lbl7.setText(f1(mp7) + unit + metricCtx(metric, mp7));
        lbl14.setText(f1(mp14) + unit + metricCtx(metric, mp14));
        lbl30.setText(f1(mp30) + unit + metricCtx(metric, mp30));
        lblRisk.setText(riskLevel + " - " + risk);

        // 热量差卡
        lblCalDiff.setText((dailyDeficit >= 0 ? "+" : "") + f0(dailyDeficit) + " kcal\n"
                + (dailyDeficit > 0 ? "热量缺口, 利于减脂" : dailyDeficit < 0 ? "热量盈余, 利于增肌" : "收支平衡"));

        // 目标达成卡
        if (goal != null) {
            String goalType = str(goal, "goal_type");
            double targetValue = num(goal, "target_value");
            int days = HealthCalculator.predictGoalDays(currentWeight, targetValue, dailyDeficit, goalType);
            if (days == -2) lblGoal.setText("进度过慢(热量差<100)\n建议调整计划");
            else if (days == -1) lblGoal.setText("摄入>消耗\n无法达成");
            else if (days == 0) lblGoal.setText("已达成目标!");
            else lblGoal.setText(days + " 天后达成\n(目标 " + f1(targetValue) + "kg)");
        } else {
            lblGoal.setText("尚未设置目标\n请到目标计划页");
        }

        // --- 图表（所选指标）---
        final long base = dates.get(0).getTime();
        XYChart.Series<Number, Number> hist = new XYChart.Series<>();
        hist.setName("历史" + metric);
        for (int i = 0; i < dates.size(); i++) {
            double dx = (dates.get(i).getTime() - base) / 86400000.0;
            XYChart.Data<Number, Number> d = new XYChart.Data<>(dx, metricVals.get(i));
            final int idx = i;
            d.setNode(tipDot(fmtDate(dates.get(idx)) + "  " + metric + ": " + f1(metricVals.get(idx)) + unit));
            hist.getData().add(d);
        }
        double lastDay = (dates.get(dates.size() - 1).getTime() - base) / 86400000.0;
        XYChart.Series<Number, Number> pred = new XYChart.Series<>();
        pred.setName("预测");
        pred.getData().add(new XYChart.Data<>(lastDay, metricVals.get(metricVals.size() - 1)));
        if (!Double.isNaN(mp7)) pred.getData().add(new XYChart.Data<>(lastDay + 7, mp7));
        if (!Double.isNaN(mp14)) pred.getData().add(new XYChart.Data<>(lastDay + 14, mp14));
        if (!Double.isNaN(mp30)) pred.getData().add(new XYChart.Data<>(lastDay + 30, mp30));
        styleSeries(pred, Theme.ACCENT);

        chart.setTitle(metric + " 趋势预测");
        chart.getYAxis().setLabel(metric.equals("体重") ? "体重 (kg)" : metric.equals("BMI") ? "BMI" : "体脂率 (%)");
        chart.getData().clear();
        chart.getData().addAll(hist, pred);

        // --- 报告 ---
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════\n");
        sb.append("              预测分析报告\n");
        sb.append("═══════════════════════════════════════════\n\n");

        sb.append("【趋势预测 (线性回归 · ").append(metric).append(")】\n");
        sb.append("  当前").append(metric).append(": ").append(f1(metricVals.get(metricVals.size() - 1))).append(unit);
        sb.append("  (体重 ").append(f1(currentWeight)).append(" kg, BMI ").append(f1(currentBMI)).append(")\n");
        sb.append("  趋势方向: ").append(trend).append("\n\n");
        sb.append("  预测结果 (").append(metric).append("):\n");
        sb.append("    7天后  → ").append(f1(mp7)).append(unit).append("\n");
        sb.append("   14天后  → ").append(f1(mp14)).append(unit).append("\n");
        sb.append("   30天后  → ").append(f1(mp30)).append(unit).append("\n\n");
        sb.append("  (预测基于最近 ").append(dates.size()).append(" 条记录, 实际结果受饮食和运动影响)\n\n");

        sb.append("【热量差分析】\n");
        sb.append("  TDEE (每日消耗): ").append(f0(tdee)).append(" kcal\n");
        sb.append("  运动消耗: ").append(exerciseCal).append(" kcal\n");
        sb.append("  饮食摄入: ").append(intakeCal).append(" kcal\n");
        sb.append("  每日热量差: ").append(dailyDeficit >= 0 ? "+" : "").append(f0(dailyDeficit)).append(" kcal\n");
        if (dailyDeficit > 0) {
            sb.append("  → 处于热量缺口状态, 有利于减脂/减重\n");
            double kgPerWeek = dailyDeficit * 7 / 7700;
            sb.append("  → 预计每周减重约 ").append(f2(kgPerWeek)).append(" kg\n");
        } else if (dailyDeficit < 0) {
            sb.append("  → 处于热量盈余状态, 有利于增肌但可能导致脂肪增加\n");
            double kgPerWeek = Math.abs(dailyDeficit) * 7 / 7700;
            sb.append("  → 预计每周增重约 ").append(f2(kgPerWeek)).append(" kg\n");
        } else {
            sb.append("  → 热量收支平衡, 体重将维持稳定\n");
        }
        sb.append("\n");

        sb.append("【目标达成预测】\n");
        if (goal != null) {
            String goalType = str(goal, "goal_type");
            double targetValue = num(goal, "target_value");
            sb.append("  目标类型: ").append(goalType).append("\n");
            sb.append("  目标体重: ").append(f1(targetValue)).append(" kg\n");
            sb.append("  当前体重: ").append(f1(currentWeight)).append(" kg\n");

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
                sb.append("  预测达成日期: ").append(sdf.format(cal.getTime()));
                sb.append(" (还需 ").append(days).append(" 天)\n");
            }
        } else {
            sb.append("  尚未设置目标, 请到「目标计划」页面设置\n");
        }
        sb.append("\n");

        sb.append("【健康风险评估】\n");
        sb.append("  预测30天后BMI: ").append(f1(predBMI30)).append("\n");
        sb.append("  风险等级: ").append(riskLevel).append("\n");
        sb.append("  ").append(risk).append("\n\n");

        String fatTrend = HealthCalculator.trendDirection(dates, bodyFats);
        sb.append("【体脂率趋势】\n");
        sb.append("  当前体脂率: ").append(f1(bodyFats.get(bodyFats.size() - 1))).append("%\n");
        sb.append("  趋势方向: ").append(fatTrend).append("\n");
        if ("上升".equals(fatTrend) && bodyFats.size() >= 30) {
            sb.append("  ⚠️ 体脂率持续上升, 建议调整饮食结构\n");
        }
        sb.append("\n═══════════════════════════════════════════\n");

        reportContent = sb.toString();
    }

    private String metricCtx(String metric, double v) {
        if (metric.equals("体重")) return " (BMI " + f1(HealthCalculator.calcBMI(v, DBUtil.currentHeight)) + ")";
        if (metric.equals("BMI")) {
            double h = DBUtil.currentHeight / 100.0;
            return " (体重 " + f1(v * h * h) + "kg)";
        }
        return "";
    }

    private void showReportDialog(String title, String content) {
        ReportDialog.showText(title, content);
    }

    private void styleSeries(final XYChart.Series<Number, Number> s, final Color c) {
        s.nodeProperty().addListener((o, ov, nv) -> {
            if (nv != null) {
                Node line = nv.lookup(".chart-series-line");
                if (line != null) line.setStyle("-fx-stroke: " + Theme.hex(c) + "; -fx-stroke-width: 2.5;");
            }
        });
    }

    private Node tipDot(String text) {
        Label dot = new Label();
        dot.setStyle("-fx-background-color: " + Theme.hex(Theme.ACCENT) + "; -fx-background-radius: 4; -fx-min-width: 7; -fx-min-height: 7;");
        Tooltip.install(dot, new Tooltip(text));
        return dot;
    }

    private double num(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v instanceof Number ? ((Number) v).doubleValue() : 0.0;
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? "-" : v.toString();
    }

    private String fmtDate(Date d) { return d == null ? "-" : sdf.format(d); }
    private static String f0(double v) { return String.format("%.0f", v); }
    private static String f1(double v) { return String.format("%.1f", v); }
    private static String f2(double v) { return String.format("%.2f", v); }
}
