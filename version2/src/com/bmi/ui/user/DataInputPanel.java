package com.bmi.ui.user;

import com.bmi.db.DBUtil;

import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

/**
 * 数据录入面板 — 独立 TitledPane 折叠式布局（可同时展开多个）。
 *
 * 原 4 张卡全摊一屏，挤且杂乱；后改为 Accordion 互斥展开，但「运动录入」与
 * 「今日运动记录」被「最近健康记录」隔开、且表压在最底，录完运动看不到结果。
 *
 * 现改为 4 个独立 TitledPane 顺序为：今日运动记录 → 健康录入 → 运动录入
 * → 最近健康记录；今日运动记录置顶，保存运动后自动展开记录表，数据即时可见。
 * 字段排成两列减少展开高度。
 */
public class DataInputPanel extends VBox {
    private final TextField tfWeight = new TextField();
    private final TextField tfBodyFat = new TextField();
    private final TextField tfWater = new TextField();
    private final TextField tfProtein = new TextField();
    private final TextField tfMuscle = new TextField();
    private final TextField tfVisceral = new TextField();
    private final TextField tfBone = new TextField();
    private final TextField tfBoneMass = new TextField();
    private final TextField tfWaist = new TextField();
    private final Label lblLatest = new Label();

    private final ComboBox<String> cbExerciseType = new ComboBox<>();
    private final TextField tfDuration = new TextField();
    private final ComboBox<String> cbIntensity = new ComboBox<>();
    private final TextField tfCalories = new TextField();
    private final TableView<String[]> exerciseTable = new TableView<>();
    private final ObservableList<String[]> exerciseData = FXCollections.observableArrayList();

    // 折叠面板提为字段，便于保存运动后自动展开记录表
    private final TitledPane tpHealth = new TitledPane("今日健康数据录入", buildHealthContent());
    private final TitledPane tpExercise = new TitledPane("今日运动录入", buildExerciseContent());
    private final TitledPane tpTodayEx = new TitledPane("今日运动记录", buildTodayExContent());
    private final TitledPane tpLatest = new TitledPane("最近一次健康记录", buildLatestContent());

    public DataInputPanel() {
        setSpacing(14);
        setPadding(new Insets(18));
        setStyle("-fx-background-color: transparent;");

        // 今日运动记录置顶并默认展开；健康录入、运动录入也默认展开
        tpTodayEx.setExpanded(true);
        tpHealth.setExpanded(true);
        tpExercise.setExpanded(true);
        tpLatest.setExpanded(false);

        getChildren().addAll(tpTodayEx, tpHealth, tpExercise, tpLatest);

        refreshLatest();
        refreshExercise();
    }

    private VBox buildHealthContent() {
        GridPane g = new GridPane();
        g.setHgap(14);
        g.setVgap(10);
        g.setPadding(new Insets(4, 4, 4, 4));
        // 两列布局，减少展开高度
        g.add(new Label("体重(kg):"), 0, 0); g.add(tfWeight, 1, 0);
        g.add(new Label("体脂率(%):"), 2, 0); g.add(tfBodyFat, 3, 0);
        g.add(new Label("水分率(%):"), 0, 1); g.add(tfWater, 1, 1);
        g.add(new Label("蛋白质率(%):"), 2, 1); g.add(tfProtein, 3, 1);
        g.add(new Label("肌肉率(%):"), 0, 2); g.add(tfMuscle, 1, 2);
        g.add(new Label("内脏脂肪:"), 2, 2); g.add(tfVisceral, 3, 2);
        g.add(new Label("骨肌量(kg):"), 0, 3); g.add(tfBone, 1, 3);
        g.add(new Label("骨量(kg):"), 2, 3); g.add(tfBoneMass, 3, 3);
        g.add(new Label("腰围(cm):"), 0, 4); g.add(tfWaist, 1, 4);

        HBox btns = new HBox(10);
        Button btnSave = new Button("保存记录");
        btnSave.getStyleClass().add("button-primary");
        Button btnScale = new Button("模拟称重");
        btnScale.getStyleClass().add("button-ghost");
        Button btnImport = new Button("从设备导入");
        btnImport.getStyleClass().add("button-ghost");
        btns.getChildren().addAll(btnSave, btnScale, btnImport);

        btnSave.setOnAction(e -> saveHealth());
        btnScale.setOnAction(e -> simulateScale());
        btnImport.setOnAction(e -> importFromDevice());

        VBox box = new VBox(12, g, btns);
        box.setPadding(new Insets(4, 8, 10, 8));
        return box;
    }

    private VBox buildExerciseContent() {
        cbExerciseType.getItems().addAll("跑步", "快走", "游泳", "骑行", "跳绳", "力量训练", "瑜伽", "其他");
        cbExerciseType.setValue("跑步");
        cbIntensity.getItems().addAll("低", "中", "高");
        cbIntensity.setValue("中");
        tfDuration.setPromptText("分钟");
        tfCalories.setPromptText("kcal");

        GridPane g = new GridPane();
        g.setHgap(14);
        g.setVgap(10);
        g.setPadding(new Insets(4, 4, 4, 4));
        g.add(new Label("运动类型:"), 0, 0); g.add(cbExerciseType, 1, 0);
        g.add(new Label("时长(分钟):"), 2, 0); g.add(tfDuration, 3, 0);
        g.add(new Label("强度:"), 0, 1); g.add(cbIntensity, 1, 1);
        g.add(new Label("消耗热量:"), 2, 1); g.add(tfCalories, 3, 1);

        HBox btns = new HBox(10);
        Button btnEstimate = new Button("估算热量");
        btnEstimate.getStyleClass().add("button-ghost");
        Button btnSaveEx = new Button("保存运动");
        btnSaveEx.getStyleClass().add("button-accent");
        btns.getChildren().addAll(btnEstimate, btnSaveEx);

        btnEstimate.setOnAction(e -> estimateCalories());
        btnSaveEx.setOnAction(e -> saveExercise());

        VBox box = new VBox(12, g, btns);
        box.setPadding(new Insets(4, 8, 10, 8));
        return box;
    }

    private VBox buildLatestContent() {
        VBox box = new VBox(8);
        lblLatest.getStyleClass().add("sub-title");
        lblLatest.setWrapText(true);
        box.getChildren().add(lblLatest);
        box.setPadding(new Insets(4, 8, 10, 8));
        return box;
    }

    private VBox buildTodayExContent() {
        VBox box = new VBox(8);
        buildExerciseTable();
        box.getChildren().add(exerciseTable);
        box.setPadding(new Insets(4, 8, 10, 8));
        return box;
    }

    private void buildExerciseTable() {
        String[] cols = {"运动类型", "时长", "强度", "消耗"};
        for (int i = 0; i < cols.length; i++) {
            final int idx = i;
            TableColumn<String[], String> c = new TableColumn<>(cols[i]);
            c.setCellValueFactory(cb -> new ReadOnlyStringWrapper(cb.getValue()[idx]));
            exerciseTable.getColumns().add(c);
        }
        exerciseTable.setItems(exerciseData);
        // 限制展开高度，超出滚动，避免长表把页面撑爆
        exerciseTable.setPrefHeight(180);
        exerciseTable.setMaxHeight(240);
    }

    private void refreshExercise() {
        exerciseData.setAll(DBUtil.getTodayExerciseList());
    }

    private void saveHealth() {
        try {
            double w = Double.parseDouble(tfWeight.getText().trim());
            double bf = Double.parseDouble(tfBodyFat.getText().trim());
            double wr = Double.parseDouble(tfWater.getText().trim());
            double pr = tfProtein.getText().trim().isEmpty() ? 0 : Double.parseDouble(tfProtein.getText().trim());
            double mr = Double.parseDouble(tfMuscle.getText().trim());
            int vf = Integer.parseInt(tfVisceral.getText().trim());
            double bm = Double.parseDouble(tfBone.getText().trim());
            double bma = tfBoneMass.getText().trim().isEmpty() ? 0 : Double.parseDouble(tfBoneMass.getText().trim());
            double wa = Double.parseDouble(tfWaist.getText().trim());
            if (DBUtil.saveHealthRecord(w, bf, wr, pr, mr, vf, bm, bma, wa)) {
                alert("保存成功");
                refreshLatest();
                clearHealth();
            } else {
                alert("保存失败，请检查输入");
            }
        } catch (Exception ex) {
            alert("请输入正确的数值");
        }
    }

    private void saveExercise() {
        try {
            String type = cbExerciseType.getValue();
            int duration = Integer.parseInt(tfDuration.getText().trim());
            String intensity = cbIntensity.getValue();
            int calories = Integer.parseInt(tfCalories.getText().trim());
            if (duration <= 0 || calories < 0) {
                alert("时长和热量必须为正数");
                return;
            }
            if (DBUtil.saveExerciseRecord(type, duration, intensity, calories)) {
                alert("运动记录保存成功");
                refreshExercise();
                // 保存后自动展开记录表，刚录入的数据即时可见
                tpTodayEx.setExpanded(true);
                clearExercise();
            } else {
                alert("保存失败");
            }
        } catch (Exception ex) {
            alert("请输入正确的数值");
        }
    }

    private void estimateCalories() {
        try {
            int duration = Integer.parseInt(tfDuration.getText().trim());
            String type = cbExerciseType.getValue();
            double weight = getCurrentWeight();
            double met = metOf(type);
            int cal = (int) Math.round(met * weight * duration / 60.0);
            tfCalories.setText(String.valueOf(cal));
        } catch (Exception ex) {
            alert("请先输入有效时长");
        }
    }

    private double getCurrentWeight() {
        Map<String, Object> rec = DBUtil.getLatestHealthRecord();
        if (rec != null && !rec.isEmpty()) {
            Object w = rec.get("weight");
            if (w instanceof Number) return ((Number) w).doubleValue();
        }
        return 60.0;
    }

    private double metOf(String type) {
        switch (type) {
            case "跑步": return 8.0;
            case "快走": return 4.0;
            case "游泳": return 7.0;
            case "骑行": return 6.0;
            case "跳绳": return 10.0;
            case "力量训练": return 5.0;
            case "瑜伽": return 2.5;
            default: return 4.0;
        }
    }

    /**
     * 平替 Tier 1：模拟称重。
     * 以最近一条记录为基线做 ±1.2kg 小幅抖动，保证连续多次测量数据有连续性，
     * 而非每次全局随机跳变。无历史记录时回落到 65.0kg 默认基线。
     */
    private void simulateScale() {
        Map<String, Object> last = DBUtil.getLatestHealthRecord();
        double baseW = (last != null && last.get("weight") instanceof Number)
                ? ((Number) last.get("weight")).doubleValue() : 65.0;
        double w = baseW + (Math.random() * 2.4 - 1.2);
        tfWeight.setText(String.format("%.1f", w));
        tfBodyFat.setText(String.format("%.1f", 15 + Math.random() * 15));
        tfWater.setText(String.format("%.1f", 50 + Math.random() * 10));
        tfProtein.setText(String.format("%.1f", 16 + Math.random() * 6));
        tfMuscle.setText(String.format("%.1f", 30 + Math.random() * 15));
        tfVisceral.setText(String.valueOf(5 + (int) (Math.random() * 8)));
        tfBone.setText(String.format("%.1f", 2 + Math.random() * 2));
        tfBoneMass.setText(String.format("%.1f", 2.5 + Math.random() * 1.5));
        tfWaist.setText(String.format("%.1f", 70 + Math.random() * 20));
    }

    /**
     * 平替 Tier 2（实训演示用桩，非真实电子秤对接）：
     * 因实训项目无法取得企业电子秤的接入授权，本功能以「读文件」模拟秤 App 的数据导出，
     * 用于演示"称重数据如何进入系统"的完整链路，并非未完成的真实集成。
     * 若将来实际接入电子秤，应新增串口/HTTP 等真实数据源实现，本方法仅作占位与演示。
     * 约定文件格式（CSV，9 列，可带表头）：
     *   weight,body_fat,water,protein,muscle,visceral,bone,bone_mass,waist
     *   e.g. 68.5,22.3,55.1,18.0,38.2,7,2.3,2.8,82.0
     * 读入后填充各字段，由用户核对后点「保存记录」持久化。
     */
    private void importFromDevice() {
        Window owner = getScene() != null ? getScene().getWindow() : null;
        FileChooser fc = new FileChooser();
        fc.setTitle("选择电子秤导出文件");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV 文件", "*.csv"),
                new FileChooser.ExtensionFilter("所有文件", "*.*"));
        File file = fc.showOpenDialog(owner);
        if (file == null) return;

        try {
            double[] v = parseDeviceFile(file.toPath());
            tfWeight.setText(String.format("%.1f", v[0]));
            tfBodyFat.setText(String.format("%.1f", v[1]));
            tfWater.setText(String.format("%.1f", v[2]));
            tfProtein.setText(String.format("%.1f", v[3]));
            tfMuscle.setText(String.format("%.1f", v[4]));
            tfVisceral.setText(String.valueOf((int) Math.round(v[5])));
            tfBone.setText(String.format("%.1f", v[6]));
            tfBoneMass.setText(String.format("%.1f", v[7]));
            tfWaist.setText(String.format("%.1f", v[8]));
            alert("已从设备文件导入，请核对后点「保存记录」");
        } catch (Exception ex) {
            alert("导入失败：" + ex.getMessage() + "\n请确认文件为 9 列 CSV（weight,body_fat,water,protein,muscle,visceral,bone,bone_mass,waist）");
        }
    }

    /**
     * 解析演示用设备文件，返回 9 个数值。自动跳过表头行，取首个可解析的数据行。
     * 仅用于实训演示桩，不代表真实电子秤的解析逻辑。
     */
    private double[] parseDeviceFile(java.nio.file.Path path) throws Exception {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            // 表头行（含字母表头）跳过
            if (line.toLowerCase().contains("weight") || line.toLowerCase().contains("body_fat")) continue;
            String[] parts = line.split("[,\\s]+");
            if (parts.length < 9) continue;
            double[] v = new double[9];
            for (int i = 0; i < 9; i++) v[i] = Double.parseDouble(parts[i].trim());
            return v;
        }
        throw new Exception("未找到有效数据行");
    }

    private void refreshLatest() {
        Map<String, Object> rec = DBUtil.getLatestHealthRecord();
        if (rec == null || rec.isEmpty()) {
            lblLatest.setText("暂无记录");
            return;
        }
        lblLatest.setText("体重: " + rec.get("weight") + " kg   BMI: " + rec.get("bmi")
                + "   体脂率: " + rec.get("body_fat") + "%   蛋白质率: " + rec.get("protein_rate")
                + "%   骨量: " + rec.get("bone_mass") + " kg");
    }

    private void clearHealth() {
        tfWeight.clear();
        tfBodyFat.clear();
        tfWater.clear();
        tfProtein.clear();
        tfMuscle.clear();
        tfVisceral.clear();
        tfBone.clear();
        tfBoneMass.clear();
        tfWaist.clear();
    }

    private void clearExercise() {
        tfDuration.clear();
        tfCalories.clear();
        cbExerciseType.setValue("跑步");
        cbIntensity.setValue("中");
    }

    private void alert(String m) {
        new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait();
    }
}
