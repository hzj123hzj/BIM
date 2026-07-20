package com.bmi.ui.user;

import com.bmi.db.DBUtil;
import com.bmi.ui.ReportDialog;
import com.bmi.util.HealthCalculator;
import com.bmi.util.Theme;

import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.scene.chart.*;

import java.util.*;

/** 分析评估面板 — 综合 BMI、体脂率、BMR、TDEE 等指标生成评估报告 */
public class AnalysisPanel extends VBox {
    private String reportContent = "";

    private final BarChart<String, Number> scoreChart;
    private final StackedBarChart<String, Number> compChart;

    public AnalysisPanel() {
        setSpacing(16);
        setPadding(new Insets(18));
        setStyle("-fx-background-color: transparent;");

        HBox ctrl = new HBox(10);
        ctrl.setAlignment(Pos.CENTER_LEFT);
        Button btnRefresh = new Button("刷新分析结果");
        btnRefresh.getStyleClass().add("button-primary");
        Button btnReport = new Button("查看报告");
        btnReport.getStyleClass().add("button-accent");
        btnReport.setOnAction(e -> showReportDialog("健康分析评估报告", reportContent));
        Label hint = new Label("综合 BMI、体脂率、BMR、TDEE 等指标生成评估报告");
        hint.getStyleClass().add("hint");
        ctrl.getChildren().addAll(btnRefresh, btnReport, hint);

        VBox ctrlCard = new VBox(10);
        ctrlCard.getStyleClass().add("card");
        Label t1 = new Label("分析评估");
        t1.getStyleClass().add("card-title");
        ctrlCard.getChildren().addAll(t1, ctrl);

        VBox metricsCard = new VBox(10);
        metricsCard.getStyleClass().add("card");
        Label t2 = new Label("关键指标概览");
        t2.getStyleClass().add("card-title");
        GridPane metricsGrid = new GridPane();
        metricsGrid.setHgap(24);
        metricsGrid.setVgap(14);
        metricsGrid.setPadding(new Insets(4));
        metricsGrid.addRow(0, metricBox("BMI", "--", Theme.hex(Theme.PRIMARY)),
                              metricBox("基础代谢 BMR", "-- kcal", Theme.hex(Theme.ACCENT)));
        metricsGrid.addRow(1, metricBox("每日消耗 TDEE", "-- kcal", Theme.hex(Theme.PRIMARY)),
                              metricBox("体脂率", "-- %", Theme.hex(Theme.ACCENT)));
        metricsGrid.addRow(2, metricBox("内脏脂肪", "--", Theme.hex(Theme.DANGER)),
                              metricBox("身体年龄", "--", Theme.hex(Theme.SUCCESS)));
        metricsGrid.addRow(3, metricBox("骨骼肌肉量", "-- kg", "#8050A0"),
                              metricBox("健康评分", "--", Theme.hex(Theme.SUCCESS)));
        metricsGrid.addRow(4, metricBox("体重", "-- kg", "#2D8CA0"),
                              metricBox("腰围", "-- cm", "#2D8CA0"));
        metricsGrid.addRow(5, metricBox("蛋白质率", "-- %", "#6A4CA0"),
                              metricBox("骨量", "-- kg", "#6A4CA0"));
        metricsCard.getChildren().addAll(t2, metricsGrid);

        // 体质多维对比（本环境 JavaFX 未含 RadarChart，改用柱状图做多维度归一化对比）
        CategoryAxis scoreCat = new CategoryAxis();
        NumberAxis scoreVal = new NumberAxis(0, 100, 20);
        scoreVal.setLabel("健康度(0-100)");
        scoreChart = new BarChart<>(scoreCat, scoreVal);
        scoreChart.setTitle("体质多维对比（雷达替代）");
        scoreChart.setPrefHeight(320);
        scoreChart.setLegendVisible(false);
        scoreChart.setAnimated(false);
        scoreChart.setCategoryGap(20);
        VBox radarCard = new VBox(10);
        radarCard.getStyleClass().add("card");
        Label tRadar = new Label("体质多维对比（归一化，仅作可视化）");
        tRadar.getStyleClass().add("card-title");
        radarCard.getChildren().addAll(tRadar, scoreChart);

        // 身体成分构成图
        CategoryAxis compCat = new CategoryAxis();
        NumberAxis compVal = new NumberAxis();
        compVal.setLabel("kg");
        compChart = new StackedBarChart<>(compCat, compVal);
        compChart.setTitle("身体成分构成");
        compChart.setPrefHeight(320);
        compChart.setLegendVisible(true);
        compChart.setAnimated(false);
        compChart.setCategoryGap(40);
        VBox compBox = new VBox(10);
        compBox.getStyleClass().add("card");
        Label tComp = new Label("身体成分构成（脂肪体重 / 去脂体重）");
        tComp.getStyleClass().add("card-title");
        compBox.getChildren().addAll(tComp, compChart);

        HBox chartsRow = new HBox(16);
        chartsRow.setSpacing(16);
        HBox.setHgrow(radarCard, Priority.ALWAYS);
        HBox.setHgrow(compBox, Priority.ALWAYS);
        chartsRow.getChildren().addAll(radarCard, compBox);

        getChildren().addAll(ctrlCard, metricsCard, chartsRow);
        VBox.setVgrow(metricsCard, Priority.ALWAYS);
        btnRefresh.setOnAction(e -> refresh());
        refresh();
    }

    private void refresh() {
        Map<String, Object> latest = DBUtil.getLatestHealthRecord();
        if (latest == null) {
            reportContent = "暂无健康记录, 请先在「数据录入」页面录入数据";
            scoreChart.getData().clear();
            compChart.getData().clear();
            return;
        }

        double weight = num(latest, "weight");
        double bodyFat = num(latest, "body_fat");
        double water = num(latest, "water_rate");
        double muscle = num(latest, "muscle_rate");
        int visceral = (int) num(latest, "visceral_fat");
        double boneMuscle = num(latest, "bone_muscle");
        double boneMass = num(latest, "bone_mass");
        double protein = num(latest, "protein_rate");
        double bmi = num(latest, "bmi");
        double bmr = num(latest, "bmr");
        double tdee = num(latest, "tdee");
        double waist = num(latest, "waist");
        int bodyAge = (int) num(latest, "body_age");
        String bodyType = str(latest, "body_type");
        String gender = DBUtil.currentGender;
        double height = DBUtil.currentHeight;
        int age = DBUtil.currentAge;
        String activity = DBUtil.currentActivityLevel;

        double bmrH = HealthCalculator.calcBMR_Harris(weight, height, age, gender);
        double bmrM = HealthCalculator.calcBMR_Mifflin(weight, height, age, gender);
        double bmrC = HealthCalculator.calcBMR_China(weight, age, gender);
        double idealWeight = HealthCalculator.calcIdealWeight(height);
        int score = HealthCalculator.calcHealthScore(bmi, bodyFat, visceral, muscle, water, gender);

        // 更新指标概览
        GridPane grid = (GridPane) ((VBox) getChildren().get(1)).getChildren().get(1);
        updateMetric(grid, 0, 0, "BMI", f1(bmi) + " (" + HealthCalculator.classifyBMI(bmi) + ")");
        updateMetric(grid, 0, 1, "基础代谢 BMR", f0(bmr) + " kcal");
        updateMetric(grid, 1, 0, "每日消耗 TDEE", f0(tdee) + " kcal");
        updateMetric(grid, 1, 1, "体脂率", f1(bodyFat) + "%");
        updateMetric(grid, 2, 0, "内脏脂肪", visceral + " 级");
        updateMetric(grid, 2, 1, "身体年龄", bodyAge + " 岁");
        updateMetric(grid, 3, 0, "骨骼肌肉量", f1(boneMuscle) + " kg");
        updateMetric(grid, 3, 1, "健康评分", score + " 分");
        updateMetric(grid, 4, 0, "体重", f1(weight) + " kg");
        updateMetric(grid, 4, 1, "腰围", waist > 0 ? f1(waist) + " cm" : "未记录");
        updateMetric(grid, 5, 0, "蛋白质率", f1(protein) + "%");
        updateMetric(grid, 5, 1, "骨量", f1(boneMass) + " kg");

        // 体质多维对比（展示用启发式归一化，不影响任何计算/评分）
        XYChart.Series<String, Number> scoreSeries = new XYChart.Series<>();
        scoreSeries.setName("体质");
        scoreSeries.getData().add(new XYChart.Data<>("BMI", scoreBMI(bmi)));
        scoreSeries.getData().add(new XYChart.Data<>("体脂率", scoreBodyFat(bodyFat, gender)));
        scoreSeries.getData().add(new XYChart.Data<>("肌肉率", scoreMuscle(muscle, gender)));
        scoreSeries.getData().add(new XYChart.Data<>("水分率", scoreWater(water, gender)));
        scoreSeries.getData().add(new XYChart.Data<>("内脏脂肪", scoreVisceral(visceral)));
        scoreSeries.getData().add(new XYChart.Data<>("蛋白质率", scoreProtein(protein, gender)));
        scoreChart.getData().clear();
        scoreChart.getData().add(scoreSeries);

        // 身体成分构成（脂肪体重 / 去脂体重）
        double fatW = HealthCalculator.calcFatWeight(weight, bodyFat);
        double leanW = HealthCalculator.calcLeanBodyMass(weight, bodyFat);
        XYChart.Series<String, Number> sFat = new XYChart.Series<>();
        sFat.setName("脂肪体重 " + f1(fatW) + "kg");
        sFat.getData().add(new XYChart.Data<>("身体成分", fatW));
        XYChart.Series<String, Number> sLean = new XYChart.Series<>();
        sLean.setName("去脂体重 " + f1(leanW) + "kg");
        sLean.getData().add(new XYChart.Data<>("身体成分", leanW));
        compChart.getData().clear();
        compChart.getData().addAll(sFat, sLean);

        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════\n");
        sb.append("              健康分析评估报告\n");
        sb.append("═══════════════════════════════════════════\n\n");

        sb.append("【BMI 体质指数】\n");
        sb.append("  BMI = ").append(f1(bmi)).append(" (").append(HealthCalculator.classifyBMI(bmi)).append(")\n");
        sb.append("  中国标准: <18.5偏瘦 | 18.5-23.9正常 | 24-27.9超重 | >=28肥胖\n");
        sb.append("  粗略体型: ").append(HealthCalculator.classifyBodyShapeRough(bmi)).append("\n");
        sb.append("  粗略肥胖分级: ").append(HealthCalculator.classifyObesityLevelRough(bmi)).append("\n");

        double[] range = HealthCalculator.calcStandardWeightRange(height);
        sb.append("  标准体重区间: ").append(f1(range[0])).append(" ~ ").append(f1(range[1])).append(" kg\n");
        double diff = weight - (range[0] + range[1]) / 2.0;
        sb.append("  与区间中值差距: ").append(diff >= 0 ? "+" : "").append(f1(diff)).append(" kg\n\n");

        sb.append("【基础代谢率 BMR (三种公式对比)】\n");
        sb.append("  Harris-Benedict : ").append(f0(bmrH)).append(" kcal\n");
        sb.append("  Mifflin-St Jeor : ").append(f0(bmrM)).append(" kcal\n");
        sb.append("  中国营养学会    : ").append(f0(bmrC)).append(" kcal\n");
        sb.append("  ────────────────────────────────\n");
        sb.append("  平均值          : ").append(f0(bmr)).append(" kcal\n\n");

        sb.append("【每日总能量消耗 TDEE】\n");
        sb.append("  活动等级: ").append(activity);
        sb.append(" (系数: ").append(f2(HealthCalculator.getActivityFactor(activity))).append(")\n");
        sb.append("  TDEE = BMR × 活动系数 = ").append(f0(tdee)).append(" kcal\n\n");

        sb.append("【理想体重】\n");
        sb.append("  理想体重 = 身高² × 22 = ").append(f1(idealWeight)).append(" kg\n");
        sb.append("  正常范围: ").append(f1(height / 100 * height / 100 * 18.5)).append(" - ");
        sb.append(f1(height / 100 * height / 100 * 23.9)).append(" kg\n\n");

        sb.append("【体脂率】\n");
        sb.append("  体脂率: ").append(f1(bodyFat)).append("%\n");
        String fatLevel = "男".equals(gender)
                ? (bodyFat < 12 ? "偏低" : bodyFat <= 25 ? "正常" : bodyFat <= 30 ? "偏高" : "严重偏高")
                : (bodyFat < 20 ? "偏低" : bodyFat <= 32 ? "正常" : bodyFat <= 38 ? "偏高" : "严重偏高");
        sb.append("  评级: ").append(fatLevel).append("\n\n");

        sb.append("【内脏脂肪等级】\n");
        sb.append("  等级: ").append(visceral).append(" 级 (").append(HealthCalculator.assessVisceralFat(visceral)).append(")\n");
        sb.append("  标准: 1-4正常 | 5-8偏高 | 9-10过高\n\n");

        sb.append("【骨骼肌肉量】\n");
        sb.append("  骨骼肌肉量: ").append(f1(boneMuscle)).append(" kg (");
        sb.append(HealthCalculator.assessMuscle(boneMuscle, weight, gender)).append(")\n\n");

        sb.append("【蛋白质率与骨量】\n");
        sb.append("  蛋白质率: ").append(f1(protein)).append("%\n");
        String proteinLevel = "男".equals(gender)
                ? (protein < 16 ? "偏低" : protein <= 20 ? "正常" : "偏高")
                : (protein < 14 ? "偏低" : protein <= 18 ? "正常" : "偏高");
        sb.append("  评级: ").append(proteinLevel).append("\n");
        sb.append("  骨量: ").append(f1(boneMass)).append(" kg\n\n");

        sb.append("【身体成分构成】\n");
        sb.append("  脂肪体重: ").append(f1(fatW)).append(" kg (").append(f1(bodyFat)).append("%)\n");
        sb.append("  去脂体重: ").append(f1(leanW)).append(" kg (").append(f1(100 - bodyFat)).append("%)\n\n");

        sb.append("【身体年龄】\n");
        sb.append("  身体年龄: ").append(bodyAge).append(" 岁 (实际年龄: ").append(age).append(" 岁)\n");
        if (bodyAge < age) sb.append("  身体比实际年龄年轻 ").append(age - bodyAge).append(" 岁!\n\n");
        else if (bodyAge > age) sb.append("  身体比实际年龄大 ").append(bodyAge - age).append(" 岁, 需关注\n\n");
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
        double whtr = waist / height;
        sb.append("  腰围身高比 WHtR = ").append(f2(whtr));
        sb.append(" (").append(HealthCalculator.assessWHtR(waist, height)).append(")\n");
        sb.append("  体型分类: ").append(HealthCalculator.classifyBodyShape(waist, gender)).append("\n\n");

        sb.append("【健康评分】\n");
        sb.append("  总分: ").append(score).append("/100 (").append(HealthCalculator.scoreLevel(score)).append(")\n");
        sb.append("  评分维度: BMI(30分) + 体脂率(25分) + 内脏脂肪(20分) + 肌肉量(15分) + 水分率(10分)\n");
        sb.append("  等级: 90-100优秀 | 75-89良好 | 60-74及格 | <60需改善\n\n");

        sb.append("═══════════════════════════════════════════\n");

        reportContent = sb.toString();
    }

    private VBox metricBox(String title, String defaultValue, String color) {
        VBox vb = new VBox(4);
        Label lt = new Label(title);
        lt.getStyleClass().add("sub-title");
        Label lv = new Label(defaultValue);
        lv.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        lv.setWrapText(true);
        vb.getChildren().addAll(lt, lv);
        return vb;
    }

    private void updateMetric(GridPane grid, int row, int col, String title, String value) {
        VBox vb = (VBox) grid.getChildren().get(row * 2 + col);
        Label lv = (Label) vb.getChildren().get(1);
        lv.setText(value);
    }

    private double num(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v instanceof Number ? ((Number) v).doubleValue() : 0.0;
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? "-" : v.toString();
    }

    private static String f0(double v) { return String.format("%.0f", v); }
    private static String f1(double v) { return String.format("%.1f", v); }
    private static String f2(double v) { return String.format("%.2f", v); }

    // ===== 雷达图归一化（仅可视化用，不影响计算/评分逻辑）=====
    private static double clamp100(double v) { return Math.max(0, Math.min(100, v)); }
    private static double scoreBMI(double bmi) {
        return clamp100(100 - Math.abs(bmi - 22) * 10);
    }
    private static double scoreBodyFat(double bf, String gender) {
        boolean male = "男".equals(gender);
        double lo = male ? 12 : 20, hi = male ? 25 : 32;
        if (bf >= lo && bf <= hi) return 100;
        return clamp100(100 - Math.abs(bf - (bf < lo ? lo : hi)) * 8);
    }
    private static double scoreMuscle(double mr, String gender) {
        double base = "男".equals(gender) ? 40 : 35;
        return clamp100(mr / base * 100);
    }
    private static double scoreWater(double wr, String gender) {
        boolean male = "男".equals(gender);
        double lo = male ? 50 : 45, hi = male ? 65 : 60;
        if (wr >= lo && wr <= hi) return 100;
        return clamp100(100 - Math.abs(wr - (wr < lo ? lo : hi)) * 5);
    }
    private static double scoreVisceral(int vf) {
        if (vf <= 4) return 100;
        if (vf <= 8) return 75;
        if (vf <= 12) return 45;
        return 20;
    }
    private static double scoreProtein(double pr, String gender) {
        boolean male = "男".equals(gender);
        double lo = male ? 16 : 14, hi = male ? 20 : 18;
        if (pr >= lo && pr <= hi) return 100;
        return clamp100(100 - Math.abs(pr - (pr < lo ? lo : hi)) * 10);
    }

    private void showReportDialog(String title, String content) {
        ReportDialog.showText(title, content);
    }
}
