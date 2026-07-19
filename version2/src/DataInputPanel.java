import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Map;

public class DataInputPanel extends VBox {
    private final TextField tfWeight = new TextField();
    private final TextField tfBodyFat = new TextField();
    private final TextField tfWater = new TextField();
    private final TextField tfMuscle = new TextField();
    private final TextField tfVisceral = new TextField();
    private final TextField tfBone = new TextField();
    private final TextField tfWaist = new TextField();
    private final Label lblLatest = new Label();

    private final ComboBox<String> cbExerciseType = new ComboBox<>();
    private final TextField tfDuration = new TextField();
    private final ComboBox<String> cbIntensity = new ComboBox<>();
    private final TextField tfCalories = new TextField();
    private final TableView<String[]> exerciseTable = new TableView<>();
    private final ObservableList<String[]> exerciseData = FXCollections.observableArrayList();

    public DataInputPanel() {
        setSpacing(14);
        setPadding(new Insets(18));
        setStyle("-fx-background-color: transparent;");

        VBox healthCard = buildHealthCard();
        VBox exerciseCard = buildExerciseCard();
        HBox.setHgrow(healthCard, Priority.ALWAYS);
        HBox.setHgrow(exerciseCard, Priority.ALWAYS);
        healthCard.setMaxWidth(Double.MAX_VALUE);
        exerciseCard.setMaxWidth(Double.MAX_VALUE);

        HBox topRow = new HBox(14);
        topRow.getChildren().addAll(healthCard, exerciseCard);

        VBox latestCard = new VBox(8);
        latestCard.getStyleClass().add("card");
        Label t2 = new Label("最近一次健康记录");
        t2.getStyleClass().add("card-title");
        lblLatest.getStyleClass().add("sub-title");
        latestCard.getChildren().addAll(t2, lblLatest);

        VBox todayExCard = new VBox(10);
        todayExCard.getStyleClass().add("card");
        VBox.setVgrow(todayExCard, Priority.ALWAYS);
        Label t3 = new Label("今日运动记录");
        t3.getStyleClass().add("card-title");
        buildExerciseTable();
        VBox.setVgrow(exerciseTable, Priority.ALWAYS);
        todayExCard.getChildren().addAll(t3, exerciseTable);

        HBox.setHgrow(latestCard, Priority.ALWAYS);
        HBox.setHgrow(todayExCard, Priority.ALWAYS);
        latestCard.setMaxWidth(Double.MAX_VALUE);
        todayExCard.setMaxWidth(Double.MAX_VALUE);
        todayExCard.setMaxHeight(Double.MAX_VALUE);
        HBox bottomRow = new HBox(14);
        bottomRow.getChildren().addAll(latestCard, todayExCard);

        getChildren().addAll(topRow, bottomRow);
        VBox.setVgrow(bottomRow, Priority.ALWAYS);

        refreshLatest();
        refreshExercise();
    }

    private VBox buildHealthCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        Label title = new Label("今日健康数据录入");
        title.getStyleClass().add("card-title");

        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(10);
        g.setPadding(new Insets(6, 2, 2, 2));
        int r = 0;
        g.add(new Label("体重(kg):"), 0, r);
        g.add(tfWeight, 1, r++);
        g.add(new Label("体脂率(%):"), 0, r);
        g.add(tfBodyFat, 1, r++);
        g.add(new Label("水分率(%):"), 0, r);
        g.add(tfWater, 1, r++);
        g.add(new Label("肌肉率(%):"), 0, r);
        g.add(tfMuscle, 1, r++);
        g.add(new Label("内脏脂肪等级:"), 0, r);
        g.add(tfVisceral, 1, r++);
        g.add(new Label("骨肌量(kg):"), 0, r);
        g.add(tfBone, 1, r++);
        g.add(new Label("腰围(cm):"), 0, r);
        g.add(tfWaist, 1, r++);

        HBox btns = new HBox(10);
        Button btnSave = new Button("保存记录");
        btnSave.getStyleClass().add("button-primary");
        Button btnScale = new Button("模拟称重");
        btnScale.getStyleClass().add("button-ghost");
        btns.getChildren().addAll(btnSave, btnScale);

        card.getChildren().addAll(title, g, btns);

        btnSave.setOnAction(e -> saveHealth());
        btnScale.setOnAction(e -> simulateScale());
        return card;
    }

    private VBox buildExerciseCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        Label title = new Label("今日运动录入");
        title.getStyleClass().add("card-title");

        cbExerciseType.getItems().addAll("跑步", "快走", "游泳", "骑行", "跳绳", "力量训练", "瑜伽", "其他");
        cbExerciseType.setValue("跑步");
        cbIntensity.getItems().addAll("低", "中", "高");
        cbIntensity.setValue("中");
        tfDuration.setPromptText("分钟");
        tfCalories.setPromptText("kcal");

        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(10);
        g.setPadding(new Insets(6, 2, 2, 2));
        int r = 0;
        g.add(new Label("运动类型:"), 0, r);
        g.add(cbExerciseType, 1, r++);
        g.add(new Label("时长(分钟):"), 0, r);
        g.add(tfDuration, 1, r++);
        g.add(new Label("强度:"), 0, r);
        g.add(cbIntensity, 1, r++);
        g.add(new Label("消耗热量:"), 0, r);
        g.add(tfCalories, 1, r++);

        HBox btns = new HBox(10);
        Button btnEstimate = new Button("估算热量");
        btnEstimate.getStyleClass().add("button-ghost");
        Button btnSaveEx = new Button("保存运动");
        btnSaveEx.getStyleClass().add("button-accent");
        btns.getChildren().addAll(btnEstimate, btnSaveEx);

        card.getChildren().addAll(title, g, btns);

        btnEstimate.setOnAction(e -> estimateCalories());
        btnSaveEx.setOnAction(e -> saveExercise());
        return card;
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
    }

    private void refreshExercise() {
        exerciseData.setAll(DBUtil.getTodayExerciseList());
    }

    private void saveHealth() {
        try {
            double w = Double.parseDouble(tfWeight.getText().trim());
            double bf = Double.parseDouble(tfBodyFat.getText().trim());
            double wr = Double.parseDouble(tfWater.getText().trim());
            double mr = Double.parseDouble(tfMuscle.getText().trim());
            int vf = Integer.parseInt(tfVisceral.getText().trim());
            double bm = Double.parseDouble(tfBone.getText().trim());
            double wa = Double.parseDouble(tfWaist.getText().trim());
            if (DBUtil.saveHealthRecord(w, bf, wr, mr, vf, bm, wa)) {
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

    private void simulateScale() {
        double base = 55 + Math.random() * 35;
        tfWeight.setText(String.format("%.1f", base));
        tfBodyFat.setText(String.format("%.1f", 15 + Math.random() * 15));
        tfWater.setText(String.format("%.1f", 50 + Math.random() * 10));
        tfMuscle.setText(String.format("%.1f", 30 + Math.random() * 15));
        tfVisceral.setText(String.valueOf(5 + (int) (Math.random() * 8)));
        tfBone.setText(String.format("%.1f", 2 + Math.random() * 2));
        tfWaist.setText(String.format("%.1f", 70 + Math.random() * 20));
    }

    private void refreshLatest() {
        Map<String, Object> rec = DBUtil.getLatestHealthRecord();
        if (rec == null || rec.isEmpty()) {
            lblLatest.setText("暂无记录");
            return;
        }
        lblLatest.setText("体重: " + rec.get("weight") + " kg   BMI: " + rec.get("bmi")
                + "   体脂率: " + rec.get("body_fat") + "%");
    }

    private void clearHealth() {
        tfWeight.clear();
        tfBodyFat.clear();
        tfWater.clear();
        tfMuscle.clear();
        tfVisceral.clear();
        tfBone.clear();
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
