import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.*;

/**
 * 身体属性中心 — 按三档数据模型集中展示用户全部身体属性。
 *
 * 🟢 必备属性(根)：性别/年龄/身高/体重/腰围/活动水平 —— 用户自知、注册时录入、无需仪器
 * 🟡 计算属性：BMI/BMR/TDEE/标准体重范围/去脂·脂肪重量/肥胖等级(粗略)/体质(粗略) —— 由必备项推导
 * 🔴 仪器测量属性：体脂率/水分率/蛋白质率/骨量/肌肉量/内脏脂肪/皮下脂肪/代谢年龄/身体评分/精确评估 —— 需体脂秤·手环
 *
 * 设计原则：注册只填 🟢 必需数据；🟡 实时由公式算出并标注"估算/实测"；🔴 展示最新一次仪器读数与缺失提示。
 */
public class AttributeCenterPanel extends VBox {

    private final VBox sectionGreen = new VBox(14);
    private final VBox sectionYellow = new VBox(14);
    private final VBox sectionRed = new VBox(14);

    private static final String GREEN = "#3CB478";
    private static final String YELLOW = "#E8A33D";
    private static final String RED = "#E0655F";

    public AttributeCenterPanel() {
        setSpacing(18);
        setPadding(new Insets(18));
        setStyle("-fx-background-color: transparent;");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("身体属性中心");
        title.getStyleClass().add("h-page");
        Button btnRefresh = new Button("刷新");
        btnRefresh.getStyleClass().add("button-primary");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        header.getChildren().addAll(title, sp, btnRefresh);

        Label intro = new Label("数据分三层：🟢 必需属性（系统之根，注册时录入）→ 🟡 计算属性（由必需项推导）→ 🔴 仪器测量（需体脂秤/手环）。注册只需填写 🟢 必需数据。");
        intro.getStyleClass().add("text-muted");
        intro.setWrapText(true);

        // 三档卡片容器
        VBox cardGreen = tierCard("🟢 必备属性（系统根 · 注册录入）", GREEN, sectionGreen);
        VBox cardYellow = tierCard("🟡 计算属性（由必备项推导）", YELLOW, sectionYellow);
        VBox cardRed = tierCard("🔴 仪器测量属性（需体脂秤/手环）", RED, sectionRed);

        getChildren().addAll(header, intro, cardGreen, cardYellow, cardRed);
        btnRefresh.setOnAction(e -> render());
        render();
    }

    private VBox tierCard(String title, String color, VBox body) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setStyle("-fx-border-color: " + color + " transparent transparent " + color + "; -fx-border-width: 0 0 0 5;");
        Label t = new Label(title);
        t.getStyleClass().add("card-title");
        t.setStyle("-fx-text-fill: " + color + ";");
        card.getChildren().addAll(t, body);
        return card;
    }

    private void render() {
        sectionGreen.getChildren().clear();
        sectionYellow.getChildren().clear();
        sectionRed.getChildren().clear();

        Map<String, Object> profile = DBUtil.getUserProfile();
        Map<String, Object> latest = DBUtil.getLatestHealthRecord();

        String gender = str(profile.get("gender"), DBUtil.currentGender);
        int age = toInt(profile.get("age")); if (age <= 0) age = DBUtil.currentAge;
        double height = toDouble(profile.get("height")); if (height <= 0) height = DBUtil.currentHeight;
        String activity = str(profile.get("activity_level"), DBUtil.currentActivityLevel);

        double weight = toDouble(profile.get("weight"));
        if (weight <= 0 && latest != null) weight = toDouble(latest.get("weight"));
        if (weight <= 0) weight = DBUtil.currentWeight;
        double waist = toDouble(profile.get("waist"));
        if (waist <= 0 && latest != null) waist = toDouble(latest.get("waist"));

        // ===== 🟢 必备属性 =====
        FlowPane pg = new FlowPane(14, 14);
        pg.getChildren().addAll(
                metric("性别", gender.isEmpty() ? "—" : gender, "", GREEN),
                metric("年龄", age > 0 ? age + " 岁" : "—", "", GREEN),
                metric("身高", height > 0 ? f1(height) + " cm" : "—", "", GREEN),
                metric("体重", weight > 0 ? f1(weight) + " kg" : "—", "注册录入/每日更新", GREEN),
                metric("腰围", waist > 0 ? f1(waist) + " cm" : "未填", "选填", GREEN),
                metric("活动水平", activity.isEmpty() ? "—" : activity, "", GREEN)
        );
        sectionGreen.getChildren().add(pg);

        // ===== 🟡 计算属性 =====
        if (height > 0 && weight > 0) {
            double bmi = HealthCalculator.calcBMI(weight, height);
            double bmr = HealthCalculator.calcAvgBMR(weight, height, age, gender);
            double tdee = HealthCalculator.calcTDEE(bmr, activity);
            double[] std = HealthCalculator.calcStandardWeightRange(height);
            // 体脂率：仪器优先，否则按 BMI 估算
            double bf = latest != null ? toDouble(latest.get("body_fat")) : 0;
            boolean estimated = bf <= 0;
            if (estimated) bf = HealthCalculator.estimateBodyFatFromBMI(bmi, age, gender);
            double lean = HealthCalculator.calcLeanMass(weight, bf);
            double fat = HealthCalculator.calcFatMass(weight, bf);
            String obesity = HealthCalculator.classifyObesityLevel(bmi);
            String bodyType = HealthCalculator.classifyBodyType(bmi, bf, gender);

            FlowPane py = new FlowPane(14, 14);
            py.getChildren().addAll(
                    metric("BMI", f2(bmi), obesity, YELLOW),
                    metric("基础代谢 BMR", f0(bmr) + " kcal", "性别+年龄+身高+体重", YELLOW),
                    metric("每日总消耗 TDEE", f0(tdee) + " kcal", "BMR × 活动系数", YELLOW),
                    metric("标准体重范围", f1(std[0]) + " ~ " + f1(std[1]) + " kg", "按身高推算", YELLOW),
                    metric("去脂体重", f1(lean) + " kg", estimated ? "按BMI估算" : "仪器实测", YELLOW),
                    metric("脂肪重量", f1(fat) + " kg", estimated ? "按BMI估算" : "仪器实测", YELLOW),
                    metric("肥胖等级(粗略)", obesity, "由BMI判定", YELLOW),
                    metric("体质/体型(粗略)", bodyType, estimated ? "体脂率按BMI估算" : "含仪器体脂率", YELLOW)
            );
            sectionYellow.getChildren().add(py);
        } else {
            Label tip = new Label("暂无身高/体重数据，无法计算。请先在「注册」或「数据录入」中填写体重。");
            tip.getStyleClass().add("text-muted");
            tip.setWrapText(true);
            sectionYellow.getChildren().add(tip);
        }

        // ===== 🔴 仪器测量属性 =====
        if (latest != null && !latest.isEmpty()) {
            FlowPane pr = new FlowPane(14, 14);
            pr.getChildren().addAll(
                    metric("体脂率", f1(toDouble(latest.get("body_fat"))) + " %", "生物电阻抗/DEXA", RED),
                    metric("水分率", f1(toDouble(latest.get("water_rate"))) + " %", "生物电阻抗", RED),
                    metric("蛋白质率", f1(toDouble(latest.get("protein_rate"))) + " %", "生物电阻抗", RED),
                    metric("骨量", f1(toDouble(latest.get("bone_muscle"))) + " kg", "生物电阻抗", RED),
                    metric("肌肉量", f1(toDouble(latest.get("muscle_rate"))) + " %", "生物电阻抗", RED),
                    metric("内脏脂肪等级", String.valueOf(toInt(latest.get("visceral_fat"))), assess(latest), RED),
                    metric("皮下脂肪率", f1(toDouble(latest.get("subcutaneous_fat"))) + " %", "卡尺/生物电阻抗", RED),
                    metric("代谢年龄", String.valueOf(toInt(latest.get("body_age"))) + " 岁", "由BMR推算", RED),
                    metric("身体评分", String.valueOf(toInt(latest.get("body_score"))), scoreLevel(toInt(latest.get("body_score"))), RED),
                    metric("肥胖等级(精确)", HealthCalculator.classifyBodyType(
                            toDouble(latest.get("bmi")), toDouble(latest.get("body_fat")), gender), "含体脂率", RED),
                    metric("体质(精确)", HealthCalculator.classifyBodyType(
                            toDouble(latest.get("bmi")), toDouble(latest.get("body_fat")), gender), "含体脂率", RED)
            );
            sectionRed.getChildren().add(pr);
        } else {
            Label tip = new Label("暂无仪器测量数据。请到「数据录入」用体脂秤/手环读数填写（体脂率、水分率、肌肉率、内脏脂肪、骨肌量、腰围等）。");
            tip.getStyleClass().add("text-muted");
            tip.setWrapText(true);
            sectionRed.getChildren().add(tip);
        }
    }

    /** 单个指标卡片 */
    private VBox metric(String title, String value, String note, String color) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setPrefSize(200, 104);
        card.getStyleClass().add("card");
        card.setStyle("-fx-border-color: " + color + " transparent transparent " + color + "; -fx-border-width: 0 0 0 4;");
        Label t = new Label(title);
        t.getStyleClass().add("sub-title");
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        VBox box = new VBox(4, t, v);
        if (note != null && !note.isEmpty()) {
            Label n = new Label(note);
            n.setStyle("-fx-font-size: 11px; -fx-text-fill: #8A97A3;");
            box.getChildren().add(n);
        }
        card.getChildren().add(box);
        return card;
    }

    private String assess(Map<String, Object> rec) {
        int vf = toInt(rec.get("visceral_fat"));
        if (vf <= 4) return "正常";
        if (vf <= 8) return "偏高";
        if (vf == 0) return "未测";
        return "过高";
    }

    private static String str(Object v, String fallback) {
        if (v == null) return fallback;
        String s = v.toString().trim();
        return s.isEmpty() ? fallback : s;
    }
    private static double toDouble(Object v) {
        if (v instanceof Number) return ((Number) v).doubleValue();
        if (v instanceof String) { try { return Double.parseDouble((String) v); } catch (Exception e) { return 0; } }
        return 0;
    }
    private static int toInt(Object v) {
        if (v instanceof Number) return ((Number) v).intValue();
        if (v instanceof String) { try { return Integer.parseInt((String) v); } catch (Exception e) { return 0; } }
        return 0;
    }
    private static String f0(double v) { return String.format("%.0f", v); }
    private static String f1(double v) { return String.format("%.1f", v); }
    private static String f2(double v) { return String.format("%.2f", v); }
    private static String scoreLevel(int s) {
        if (s >= 90) return "优秀";
        if (s >= 75) return "良好";
        if (s >= 60) return "及格";
        if (s == 0) return "未评";
        return "需改善";
    }
}
