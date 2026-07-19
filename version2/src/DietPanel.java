import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.chart.*;
import javafx.scene.layout.*;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.*;
import java.util.Map;

/** 饮食管理面板 — 录入饮食记录、今日营养汇总(饼图)、今日饮食记录 */
public class DietPanel extends VBox {
    private final ComboBox<String> cbMealType = new ComboBox<>();
    private final ComboBox<String> cbFood = new ComboBox<>();
    private final TextField tfGrams = new TextField("100");
    private final Label lblSummary = new Label("今日汇总: 暂无数据");
    private final PieChart pie = new PieChart();
    private final Map<String, String[]> foodData = new LinkedHashMap<>();
    private final TableView<String[]> todayTable = new TableView<>();
    private final ObservableList<String[]> todayData = FXCollections.observableArrayList();
    private String reportContent = "";

    public DietPanel() {
        setSpacing(12);
        setPadding(new Insets(14));
        setStyle("-fx-background-color: #F0F6F9;");

        cbMealType.getItems().addAll("早餐", "午餐", "晚餐", "加餐");
        cbMealType.setValue("早餐");
        tfGrams.setPrefWidth(80);

        HBox input = new HBox(10);
        input.setAlignment(Pos.CENTER_LEFT);
        Label l1 = new Label("餐次:"); l1.getStyleClass().add("sub-title");
        Label l2 = new Label("食物:"); l2.getStyleClass().add("sub-title");
        Label l3 = new Label("食用量(g):"); l3.getStyleClass().add("sub-title");
        Button btnAdd = new Button("记录饮食");
        btnAdd.getStyleClass().add("button-primary");
        input.getChildren().addAll(l1, cbMealType, l2, cbFood, l3, tfGrams, btnAdd);

        VBox inputCard = new VBox(10);
        inputCard.getStyleClass().add("card");
        Label t1 = new Label("饮食记录录入");
        t1.getStyleClass().add("card-title");
        inputCard.getChildren().addAll(t1, input);

        lblSummary.getStyleClass().add("card-title");
        pie.setTitle("营养素占比");
        pie.setLabelsVisible(true);
        pie.setPrefSize(320, 240);

        VBox left = new VBox(8, lblSummary, pie);
        left.setAlignment(Pos.TOP_LEFT);

        buildTodayTable();
        VBox right = new VBox(8, new Label("今日饮食记录") {{ getStyleClass().add("card-title"); }}, todayTable);
        VBox.setVgrow(todayTable, Priority.ALWAYS);
        right.setAlignment(Pos.TOP_LEFT);

        HBox top = new HBox(14);
        top.setAlignment(Pos.CENTER_LEFT);
        top.getChildren().addAll(left, right);
        HBox.setHgrow(right, Priority.ALWAYS);

        HBox summaryHeader = new HBox(10);
        summaryHeader.setAlignment(Pos.CENTER_LEFT);
        Label t2 = new Label("今日营养汇总");
        t2.getStyleClass().add("card-title");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button btnReport = new Button("查看每日营养报告");
        btnReport.getStyleClass().add("button-primary");
        btnReport.setOnAction(e -> showReportDialog("每日营养报告", reportContent));
        summaryHeader.getChildren().addAll(t2, sp, btnReport);

        VBox summaryCard = new VBox(10);
        summaryCard.getStyleClass().add("card");
        summaryCard.getChildren().addAll(summaryHeader, top);
        VBox.setVgrow(summaryCard, Priority.ALWAYS);

        getChildren().addAll(inputCard, summaryCard);
        VBox.setVgrow(summaryCard, Priority.ALWAYS);

        btnAdd.setOnAction(e -> addDietRecord());
        loadFoods();
        refreshSummary();
    }

    private void buildTodayTable() {
        String[] cols = {"餐次", "食物", "热量", "蛋白质", "碳水", "脂肪"};
        for (int i = 0; i < cols.length; i++) {
            final int idx = i;
            TableColumn<String[], String> c = new TableColumn<>(cols[i]);
            c.setCellValueFactory(cb -> new ReadOnlyStringWrapper(cb.getValue()[idx]));
            todayTable.getColumns().add(c);
        }
        todayTable.setItems(todayData);
    }

    private void loadFoods() {
        cbFood.getItems().clear();
        foodData.clear();
        for (String[] food : DBUtil.getAllFoods()) {
            String name = food[0];
            foodData.put(name, food);
            cbFood.getItems().add(name);
        }
        if (!cbFood.getItems().isEmpty()) cbFood.setValue(cbFood.getItems().get(0));
    }

    private void addDietRecord() {
        String mealType = cbMealType.getValue();
        String foodName = cbFood.getValue();
        if (foodName == null) { alert("请选择食物"); return; }
        try {
            double grams = Double.parseDouble(tfGrams.getText().trim());
            if (grams <= 0 || grams > 5000) { alert("食用量范围 1-5000g"); return; }
            String[] food = foodData.get(foodName);
            double ratio = grams / 100.0;
            int calories = (int) (Integer.parseInt(food[1]) * ratio);
            double protein = Double.parseDouble(food[2]) * ratio;
            double carbs = Double.parseDouble(food[3]) * ratio;
            double fat = Double.parseDouble(food[4]) * ratio;
            if (DBUtil.saveDietRecord(mealType, foodName + "(" + f0(grams) + "g)", calories, protein, carbs, fat)) {
                alert("饮食记录保存成功! 热量: " + calories + " kcal");
                refreshSummary();
            } else {
                alert("饮食记录保存失败");
            }
        } catch (NumberFormatException e) {
            alert("请输入有效数字");
        }
    }

    private void refreshSummary() {
        int[] diet = DBUtil.getTodayDietSummary();
        int totalCal = diet[0];
        double protein = diet[1] / 100.0;
        double carbs = diet[2] / 100.0;
        double fat = diet[3] / 100.0;

        Map<String, Object> latest = DBUtil.getLatestHealthRecord();
        double tdee = latest != null ? num(latest, "tdee") : 1800;
        double recProtein = latest != null ? num(latest, "weight") * 1.2 : 60;
        double recCarbs = tdee * 0.5 / 4;
        double recFat = tdee * 0.3 / 9;

        lblSummary.setText(String.format("今日汇总: 总热量 %d kcal / 推荐 %d kcal | 蛋白质 %.1fg | 碳水 %.1fg | 脂肪 %.1fg",
                totalCal, (int) tdee, protein, carbs, fat));

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                new PieChart.Data("蛋白质 " + f1(protein * 4), protein * 4),
                new PieChart.Data("碳水 " + f1(carbs * 4), carbs * 4),
                new PieChart.Data("脂肪 " + f1(fat * 9), fat * 9));
        pie.setData(pieData);

        todayData.setAll(DBUtil.getTodayDietRecords());

        StringBuilder sb = new StringBuilder();
        sb.append("═══ 每日营养报告 ═══\n\n");
        sb.append("热量:\n");
        sb.append(String.format("  摄入 %d kcal / 推荐 %d kcal", totalCal, (int) tdee));
        if (totalCal > tdee * 1.1) sb.append("  [超标]");
        else if (totalCal < tdee * 0.7) sb.append("  [不足]");
        sb.append("\n\n");

        sb.append("蛋白质:\n");
        sb.append(String.format("  摄入 %.1fg / 推荐 %.1fg", protein, recProtein));
        if (protein < recProtein * 0.8) sb.append("  [不足]");
        sb.append("\n\n");

        sb.append("碳水:\n");
        sb.append(String.format("  摄入 %.1fg / 推荐 %.1fg", carbs, recCarbs));
        if (carbs > recCarbs * 1.3) sb.append("  [超标]");
        sb.append("\n\n");

        sb.append("脂肪:\n");
        sb.append(String.format("  摄入 %.1fg / 推荐 %.1fg", fat, recFat));
        if (fat > recFat * 1.3) sb.append("  [超标]");
        sb.append("\n\n");

        double totalNutrientCal = protein * 4 + carbs * 4 + fat * 9;
        if (totalNutrientCal > 0) {
            sb.append("营养素占比:\n");
            sb.append(String.format("  蛋白质: %.1f%%\n", protein * 4 / totalNutrientCal * 100));
            sb.append(String.format("  碳水:   %.1f%%\n", carbs * 4 / totalNutrientCal * 100));
            sb.append(String.format("  脂肪:   %.1f%%\n", fat * 9 / totalNutrientCal * 100));
        }
        reportContent = sb.toString();
    }

    private double num(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v instanceof Number ? ((Number) v).doubleValue() : 0.0;
    }

    private static String f0(double v) { return String.format("%.0f", v); }
    private static String f1(double v) { return String.format("%.1f", v); }

    private void showReportDialog(String title, String content) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setResizable(true);
        TextArea area = new TextArea(content);
        area.setEditable(false);
        area.setWrapText(true);
        area.setStyle("-fx-font-family: 'Microsoft YaHei UI', 'Microsoft YaHei', sans-serif; -fx-font-size: 14px;");
        VBox box = new VBox(area);
        VBox.setVgrow(area, Priority.ALWAYS);
        box.setPrefSize(800, 600);
        dialog.getDialogPane().setContent(box);
        dialog.getDialogPane().setPrefSize(800, 600);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.show();
    }

    private void alert(String m) {
        new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait();
    }
}
