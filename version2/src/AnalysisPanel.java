import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;

import java.util.*;

/** 分析评估面板 — 综合 BMI、体脂率、BMR、TDEE 等指标生成评估报告 */
public class AnalysisPanel extends VBox {
    private final TextArea ta = new TextArea();

    public AnalysisPanel() {
        setSpacing(12);
        setPadding(new Insets(14));
        setStyle("-fx-background-color: #F0F6F9;");

        HBox ctrl = new HBox(10);
        ctrl.setAlignment(Pos.CENTER_LEFT);
        Button btnRefresh = new Button("刷新分析结果");
        btnRefresh.getStyleClass().add("button-primary");
        Label hint = new Label("综合 BMI、体脂率、BMR、TDEE 等指标生成评估报告");
        hint.getStyleClass().add("hint");
        ctrl.getChildren().addAll(btnRefresh, hint);

        VBox ctrlCard = new VBox(10);
        ctrlCard.getStyleClass().add("card");
        Label t1 = new Label("分析评估");
        t1.getStyleClass().add("card-title");
        ctrlCard.getChildren().addAll(t1, ctrl);

        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setStyle("-fx-font-family: 'Microsoft YaHei UI', 'Microsoft YaHei', sans-serif; -fx-font-size: 13px;");
        ScrollPane sp = new ScrollPane(ta);
        sp.setFitToWidth(true);
        VBox reportCard = new VBox(10);
        reportCard.getStyleClass().add("card");
        reportCard.setPrefHeight(520);
        Label t2 = new Label("健康分析评估报告");
        t2.getStyleClass().add("card-title");
        reportCard.getChildren().addAll(t2, sp);

        getChildren().addAll(ctrlCard, reportCard);
        btnRefresh.setOnAction(e -> refresh());
        refresh();
    }

    private void refresh() {
        Map<String, Object> latest = DBUtil.getLatestHealthRecord();
        if (latest == null) {
            ta.setText("暂无健康记录, 请先在「数据录入」页面录入数据");
            return;
        }

        double weight = num(latest, "weight");
        double bodyFat = num(latest, "body_fat");
        double water = num(latest, "water_rate");
        double muscle = num(latest, "muscle_rate");
        int visceral = (int) num(latest, "visceral_fat");
        double boneMuscle = num(latest, "bone_muscle");
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

        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════\n");
        sb.append("              健康分析评估报告\n");
        sb.append("═══════════════════════════════════════════\n\n");

        sb.append("【BMI 体质指数】\n");
        sb.append("  BMI = ").append(f1(bmi)).append(" (").append(HealthCalculator.classifyBMI(bmi)).append(")\n");
        sb.append("  中国标准: <18.5偏瘦 | 18.5-23.9正常 | 24-27.9超重 | >=28肥胖\n\n");

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

        ta.setText(sb.toString());
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
}
