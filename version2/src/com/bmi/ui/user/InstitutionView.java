package com.bmi.ui.user;

import com.bmi.App;
import com.bmi.db.DBUtil;
import com.bmi.util.HealthCalculator;
import com.bmi.util.Theme;

import javafx.geometry.*;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.text.SimpleDateFormat;

import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

/** 医疗机构端 — 病人列表 + 批量导入(CSV) + 逐人分析 */
public class InstitutionView extends VBox {
    private final TableView<Map<String, Object>> patientTable = new TableView<>();
    private Map<String, Object> selectedProfile = null;

    public InstitutionView() {
        setSpacing(14);
        setPadding(new Insets(18));
        setStyle("-fx-background-color: transparent;");

        // 顶部栏
        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("医疗机构工作台 — " + DBUtil.currentInstitutionName);
        title.getStyleClass().add("card-title");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button btnUpload = new Button("上传CSV批量导入");
        btnUpload.getStyleClass().add("button-accent");
        Button btnAnalyze = new Button("查看分析");
        btnAnalyze.getStyleClass().add("button-primary");
        Button btnRefresh = new Button("刷新");
        btnRefresh.getStyleClass().add("button-ghost");
        Button btnLogout = new Button("退出");
        btnLogout.getStyleClass().add("button-ghost");
        top.getChildren().addAll(title, sp, btnUpload, btnAnalyze, btnRefresh, btnLogout);

        buildTable();
        refresh();

        btnUpload.setOnAction(e -> uploadCsv());
        btnAnalyze.setOnAction(e -> analyzeSelected());
        btnRefresh.setOnAction(e -> refresh());
        btnLogout.setOnAction(e -> App.showLogin());

        getChildren().addAll(top, patientTable);
    }

    private void buildTable() {
        String[] cols = {"用户名", "性别", "年龄", "身高(cm)", "最新体重(kg)", "BMI", "体质类型", "最近记录"};
        String[] keys = {"username", "gender", "age", "height", "weight", "bmi", "body_type", "last_date"};
        for (int i = 0; i < cols.length; i++) {
            final int idx = i;
            TableColumn<Map<String, Object>, String> c = new TableColumn<>(cols[i]);
            c.setCellValueFactory(cb -> {
                Object v = cb.getValue().get(keys[idx]);
                return new javafx.beans.property.ReadOnlyStringWrapper(v == null ? "--" : v.toString());
            });
            patientTable.getColumns().add(c);
        }
        patientTable.setRowFactory(tv -> {
            TableRow<Map<String, Object>> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (!row.isEmpty()) selectedProfile = row.getItem();
            });
            return row;
        });
    }

    private void refresh() {
        patientTable.getItems().setAll(DBUtil.getInstitutionPatients(DBUtil.currentInstitutionId));
    }

    private void uploadCsv() {
        FileChooser fc = new FileChooser();
        fc.setTitle("选择病人健康数据 CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV 文件", "*.csv"));
        File file = fc.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (file == null) return;
        List<Map<String, Object>> rows = parseCsv(file);
        if (rows.isEmpty()) {
            alert("CSV 无有效数据行 (需表头: username,record_date,weight,...)");
            return;
        }
        DBUtil.ImportResult res = DBUtil.importInstitutionRecords(DBUtil.currentInstitutionId, rows);
        StringBuilder sb = new StringBuilder();
        sb.append("导入完成: 成功 ").append(res.success).append(" 条");
        if (!res.errors.isEmpty()) {
            sb.append("\n跳过 ").append(res.errors.size()).append(" 条:\n");
            for (String err : res.errors) sb.append("  - ").append(err).append("\n");
        }
        alert(sb.toString());
        refresh();
    }

    private List<Map<String, Object>> parseCsv(File file) {
        List<Map<String, Object>> rows = new ArrayList<>();
        SimpleDateFormat[] fmts = {new SimpleDateFormat("yyyy-MM-dd"), new SimpleDateFormat("yyyy/MM/dd")};
        try (BufferedReader br = Files.newBufferedReader(file.toPath())) {
            String header = br.readLine();
            if (header == null) return rows;
            String[] headers = header.split(",");
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] vals = line.split(",");
                if (vals.length < headers.length) continue;
                Map<String, Object> m = new HashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    String h = headers[i].trim();
                    String v = vals[i].trim();
                    if ("username".equals(h)) m.put("username", v);
                    else if ("record_date".equals(h)) {
                        Date d = null;
                        for (SimpleDateFormat f : fmts) {
                            try { d = f.parse(v); break; } catch (Exception ignore) {}
                        }
                        if (d != null) m.put("record_date", d);
                    } else {
                        try { m.put(h, Double.parseDouble(v)); } catch (Exception ignore) { m.put(h, 0.0); }
                    }
                }
                if (m.get("username") != null) rows.add(m);
            }
        } catch (Exception e) {
            DBUtil.logError("InstitutionView.parseCsv", e);
        }
        return rows;
    }

    private void analyzeSelected() {
        Map<String, Object> row = patientTable.getSelectionModel().getSelectedItem();
        if (row == null) { alert("请先选择一位病人"); return; }
        String username = (String) row.get("username");
        String gender = (String) row.get("gender");
        int age = ((Number) row.get("age")).intValue();
        double height = ((Number) row.get("height")).doubleValue();
        Map<String, Object> rec = DBUtil.getLatestHealthRecord(username);
        if (rec == null) { alert("该病人暂无健康记录"); return; }

        List<Map<String, Object>> history = DBUtil.getHealthRecords(username, 200);
        showAnalysisDialog(username, gender, age, height, rec, history);
    }

    private void showAnalysisDialog(String username, String gender, int age, double height,
                                    Map<String, Object> rec, List<Map<String, Object>> history) {
        double weight = ((Number) rec.get("weight")).doubleValue();
        double bmi = HealthCalculator.calcBMI(weight, height);
        double bodyFat = ((Number) rec.get("body_fat")).doubleValue();
        int visceral = ((Number) rec.get("visceral_fat")).intValue();
        double muscleRate = ((Number) rec.get("muscle_rate")).doubleValue();
        double waterRate = ((Number) rec.get("water_rate")).doubleValue();
        double waist = ((Number) rec.get("waist")).doubleValue();
        double boneMuscle = ((Number) rec.get("bone_muscle")).doubleValue();

        int healthScore = HealthCalculator.calcHealthScore(bmi, bodyFat, visceral, muscleRate, waterRate, gender);
        String scoreLevel = HealthCalculator.scoreLevel(healthScore);

        VBox root = new VBox(14);
        root.setPadding(new Insets(14));
        root.setStyle("-fx-background-color: transparent;");

        // —— 顶部: 标题 + 综合评分 ——
        HBox head = new HBox(12);
        head.setAlignment(Pos.CENTER_LEFT);
        Label t = new Label("病人分析 — " + username + "（" + gender + "，" + age + "岁）");
        t.getStyleClass().add("card-title");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label score = new Label("健康评分 " + healthScore + " / 100  ·  " + scoreLevel);
        score.getStyleClass().add("chip");
        head.getChildren().addAll(t, sp, score);

        // —— 关键指标网格 ——
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(8);
        String[][] items = {
                {"BMI", String.format("%.1f", bmi) + " (" + HealthCalculator.classifyBMI(bmi) + ")"},
                {"体脂率", String.format("%.1f", bodyFat) + "%"},
                {"内脏脂肪", visceral + " (" + HealthCalculator.assessVisceralFat(visceral) + ")"},
                {"肌肉量评级", HealthCalculator.assessMuscle(boneMuscle, weight, gender)},
                {"水分率", String.format("%.1f", waterRate) + "%"},
                {"腰围", String.format("%.1f", waist) + " cm"},
                {"腰高比", HealthCalculator.assessWHtR(waist, height)},
                {"体质类型", String.valueOf(rec.get("body_type"))},
        };
        for (int i = 0; i < items.length; i++) {
            Label k = new Label(items[i][0]); k.getStyleClass().add("muted");
            Label v = new Label(items[i][1]); v.getStyleClass().add("metric");
            grid.add(k, (i % 2) * 2, i / 2);
            grid.add(v, (i % 2) * 2 + 1, i / 2);
        }

        // —— 风险标记 ——
        List<String> risks = buildRiskFlags(gender, age, bmi, bodyFat, visceral, muscleRate, waist, height);

        // —— 趋势图 ——
        LineChart<String, Number> chart = buildTrendChart(history);

        // —— 智能建议 + 趋势预测 ——
        Map<String, Object> rec2 = new HashMap<>(rec);
        rec2.put("username", username);
        Map<String, Object> rcm = HealthCalculator.recommendGoal(rec2, gender, age, height);

        StringBuilder sb = new StringBuilder();
        sb.append("【智能建议】\n").append(rcm.get("reason")).append("\n");
        sb.append("推荐目标: ").append(rcm.get("goalType")).append(" / ")
                .append(String.format("%.1f", (double) rcm.get("targetWeight"))).append(" kg\n\n");

        if (history.size() >= 3) {
            List<Date> dates = new ArrayList<>();
            List<Double> weights = new ArrayList<>();
            for (Map<String, Object> m : history) {
                dates.add((Date) m.get("record_date"));
                weights.add(((Number) m.get("weight")).doubleValue());
            }
            double predW = HealthCalculator.predictTrend(dates, weights, 30);
            double predBMI = HealthCalculator.calcBMI(predW, height);
            sb.append("【趋势预测】\n");
            sb.append("体重趋势: ").append(HealthCalculator.trendDirection(dates, weights)).append("\n");
            sb.append("30天后预测体重: ").append(Double.isNaN(predW) ? "数据不足" : String.format("%.1f", predW) + " kg")
                    .append(" (预测BMI ").append(String.format("%.1f", predBMI)).append(")\n");
            sb.append(HealthCalculator.assessRisk(predBMI)).append("\n");
        } else {
            sb.append("【趋势预测】\n记录不足3条, 暂无法预测趋势, 建议该病人持续打卡。\n");
        }
        if (!risks.isEmpty()) {
            sb.append("\n【风险提示】\n");
            for (String r : risks) sb.append("  ⚠ ").append(r).append("\n");
        }

        TextArea ta = new TextArea(sb.toString());
        ta.setEditable(false); ta.setWrapText(true); ta.setPrefSize(560, 200);

        root.getChildren().addAll(head, grid, chart, ta);

        Dialog<Void> d = new Dialog<>();
        d.setTitle("病人分析 — " + username);
        d.setHeaderText(null);
        d.getDialogPane().setContent(root);
        d.getDialogPane().setPrefSize(620, 640);
        d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        d.showAndWait();
    }

    private List<String> buildRiskFlags(String gender, int age, double bmi, double bodyFat,
                                        int visceral, double muscleRate, double waist, double height) {
        List<String> risks = new ArrayList<>();
        boolean male = "男".equals(gender);
        if (bmi >= 28) risks.add("BMI 已达肥胖区间 (" + String.format("%.1f", bmi) + ")，心血管与代谢疾病风险升高");
        else if (bmi >= 24) risks.add("BMI 超重 (" + String.format("%.1f", bmi) + ")，建议控制体重");
        else if (bmi < 18.5) risks.add("BMI 偏瘦 (" + String.format("%.1f", bmi) + ")，需关注营养摄入");
        double fatHigh = male ? 25 : 32;
        double fatLow = male ? 12 : 20;
        if (bodyFat > fatHigh) risks.add("体脂率过高 (" + String.format("%.1f", bodyFat) + "%)，建议减脂");
        else if (bodyFat < fatLow) risks.add("体脂率偏低 (" + String.format("%.1f", bodyFat) + "%)");
        if (visceral > 8) risks.add("内脏脂肪过高 (" + visceral + ")，中心性肥胖，代谢综合征风险");
        double stdMuscle = male ? 40.0 : 35.0;
        if (muscleRate > 0 && muscleRate < stdMuscle * 0.9) risks.add("肌肉量偏低 (" + String.format("%.1f", muscleRate) + "%)");
        if (waist > 0) {
            String whr = HealthCalculator.assessWHtR(waist, height);
            if (whr.startsWith("偏高") || whr.startsWith("高")) risks.add("腰高比" + whr + "，腹部脂肪堆积");
        }
        int bodyAge = HealthCalculator.calcBodyAge(age, bodyFat, muscleRate, visceral, gender);
        if (bodyAge - age >= 3) risks.add("身体年龄 " + bodyAge + " 岁，比实际年龄大 " + (bodyAge - age) + " 岁");
        return risks;
    }

    private LineChart<String, Number> buildTrendChart(List<Map<String, Object>> history) {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        x.setLabel("记录时间"); y.setLabel("数值");
        LineChart<String, Number> chart = new LineChart<>(x, y);
        chart.setTitle("健康指标趋势 (" + history.size() + " 条记录)");
        chart.setPrefSize(580, 240);
        chart.setLegendVisible(true);
        chart.setCreateSymbols(true);

        XYChart.Series<String, Number> sWeight = new XYChart.Series<>();
        sWeight.setName("体重(kg)");
        XYChart.Series<String, Number> sBmi = new XYChart.Series<>();
        sBmi.setName("BMI");
        XYChart.Series<String, Number> sFat = new XYChart.Series<>();
        sFat.setName("体脂率(%)");

        Set<String> seen = new HashSet<>();
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd");
        for (Map<String, Object> m : history) {
            Date d = (Date) m.get("record_date");
            String label = sdf.format(d);
            while (seen.contains(label)) label += "*";
            seen.add(label);
            sWeight.getData().add(new XYChart.Data<>(label, ((Number) m.get("weight")).doubleValue()));
            sBmi.getData().add(new XYChart.Data<>(label, ((Number) m.get("bmi")).doubleValue()));
            sFat.getData().add(new XYChart.Data<>(label, ((Number) m.get("body_fat")).doubleValue()));
        }
        chart.getData().addAll(sWeight, sBmi, sFat);
        return chart;
    }

    private void alert(String m) {
        new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait();
    }

    public VBox getRoot() { return this; }
}
