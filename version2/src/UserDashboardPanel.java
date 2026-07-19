import javafx.geometry.*;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.*;

/** 用户端数据大屏 — 展示当前用户自己的健康数据 */
public class UserDashboardPanel extends VBox {

    private final TableView<String[]> recentTable = new TableView<>();
    private final ObservableList<String[]> recentData = FXCollections.observableArrayList();

    public UserDashboardPanel() {
        setSpacing(12);
        setPadding(new Insets(14));
        setStyle("-fx-background-color: transparent;");

        HBox ctrl = new HBox(10);
        ctrl.setAlignment(Pos.CENTER_LEFT);
        Button btnRefresh = new Button("刷新大屏");
        btnRefresh.getStyleClass().add("button-primary");
        Button btnMetrics = new Button("查看核心指标");
        btnMetrics.getStyleClass().add("button-accent");
        btnMetrics.setOnAction(e -> showCoreMetricsDialog());
        Label hint = new Label("实时展示您的个人健康数据");
        hint.getStyleClass().add("hint");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        ctrl.getChildren().addAll(btnRefresh, btnMetrics, sp, hint);

        VBox ctrlCard = new VBox(10);
        ctrlCard.getStyleClass().add("card");
        Label t1 = new Label("我的健康数据大屏");
        t1.getStyleClass().add("card-title");
        ctrlCard.getChildren().addAll(t1, ctrl);

        // 基本信息
        VBox profileCard = new VBox(10);
        profileCard.getStyleClass().add("card");
        Label tProfile = new Label("基本信息");
        tProfile.getStyleClass().add("card-title");
        GridPane gridProfile = new GridPane();
        gridProfile.setHgap(24);
        gridProfile.setVgap(10);
        gridProfile.addRow(0, kv("用户名", DBUtil.currentUsername), kv("性别", DBUtil.currentGender));
        gridProfile.addRow(1, kv("年龄", DBUtil.currentAge + " 岁"), kv("身高", DBUtil.currentHeight + " cm"));
        gridProfile.addRow(2, kv("活动等级", DBUtil.currentActivityLevel), new Label(""));
        profileCard.getChildren().addAll(tProfile, gridProfile);

        // 最近体重趋势
        VBox trendCard = new VBox(10);
        trendCard.getStyleClass().add("card");
        Label t3 = new Label("最近体重趋势");
        t3.getStyleClass().add("card-title");
        LineChart<String, Number> trendChart = buildTrendChart();
        trendChart.setMinHeight(260);
        trendCard.getChildren().addAll(t3, trendChart);

        // 最近健康记录
        VBox recentCard = new VBox(10);
        recentCard.getStyleClass().add("card");
        Label tRecent = new Label("最近健康记录");
        tRecent.getStyleClass().add("card-title");
        buildRecentTable();
        VBox.setVgrow(recentTable, Priority.ALWAYS);
        recentCard.getChildren().addAll(tRecent, recentTable);

        getChildren().addAll(ctrlCard, profileCard, trendCard, recentCard);
        VBox.setVgrow(recentCard, Priority.ALWAYS);
        btnRefresh.setOnAction(e -> refresh());
        refresh();
    }

    private void buildRecentTable() {
        String[] cols = {"日期", "体重(kg)", "BMI", "体脂率(%)", "内脏脂肪", "身体年龄"};
        for (int i = 0; i < cols.length; i++) {
            final int idx = i;
            TableColumn<String[], String> c = new TableColumn<>(cols[i]);
            c.setCellValueFactory(cb -> new ReadOnlyStringWrapper(cb.getValue()[idx]));
            recentTable.getColumns().add(c);
        }
        recentTable.setItems(recentData);
    }

    private void refresh() {
        recentData.clear();
        List<Map<String, Object>> records = DBUtil.getHealthRecords(10);
        if (records != null) {
            for (Map<String, Object> r : records) {
                recentData.add(new String[]{
                        fmt(r.get("record_date")),
                        f1(r.get("weight")),
                        f2(r.get("bmi")),
                        f1(r.get("body_fat")),
                        String.valueOf(toInt(r.get("visceral_fat"))),
                        String.valueOf(toInt(r.get("body_age")))
                });
            }
        }
    }

    private void showCoreMetricsDialog() {
        Map<String, Object> latest = DBUtil.getLatestHealthRecord();
        List<String[]> badges = DBUtil.getAchievements();
        List<Map<String, Object>> records = DBUtil.getHealthRecords(30);

        int totalRecords = records == null ? 0 : records.size();
        int badgeCount = badges == null ? 0 : badges.size();

        String blue = Theme.hex(Theme.PRIMARY);
        String accent = Theme.hex(Theme.ACCENT);
        String teal = Theme.hex(Theme.SUCCESS);
        String purple = "#8050A0";
        String red = Theme.hex(Theme.DANGER);

        TilePane pane = new TilePane();
        pane.setHgap(16);
        pane.setVgap(16);
        pane.setPrefColumns(4);
        pane.setPadding(new Insets(10));
        pane.getChildren().addAll(
                metricCard("累计打卡", String.valueOf(totalRecords), blue),
                metricCard("获得徽章", String.valueOf(badgeCount), purple),
                metricCard("健康评分", latest == null ? "--" : String.valueOf(calcScore(latest)), teal),
                metricCard("当前体重", latest == null ? "--" : f1(latest.get("weight")) + " kg", accent),
                metricCard("BMI", latest == null ? "--" : f2(latest.get("bmi")), blue),
                metricCard("体脂率", latest == null ? "--" : f1(latest.get("body_fat")) + "%", accent),
                metricCard("内脏脂肪", latest == null ? "--" : String.valueOf(latest.get("visceral_fat")), red),
                metricCard("身体年龄", latest == null ? "--" : String.valueOf(latest.get("body_age")), teal)
        );

        ScrollPane sp = new ScrollPane(pane);
        sp.setFitToWidth(true);
        VBox box = new VBox(sp);
        VBox.setVgrow(sp, Priority.ALWAYS);
        box.setPrefSize(800, 600);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("核心指标详情");
        dialog.setHeaderText(null);
        dialog.setResizable(true);
        ReportDialog.show("核心指标详情", pane);
    }

    private VBox metricCard(String title, String value, String colorHex) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(20));
        card.setPrefSize(170, 110);
        card.getStyleClass().add("card");
        card.setStyle("-fx-border-color: " + colorHex + "; -fx-border-width: 0 0 0 5; -fx-border-radius: 12;");
        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("sub-title");
        Label lblValue = new Label(value);
        lblValue.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: " + colorHex + ";");
        card.getChildren().addAll(lblTitle, lblValue);
        return card;
    }

    private LineChart<String, Number> buildTrendChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("日期");
        yAxis.setLabel("体重 (kg)");
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setTitle("近 7 次体重变化");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        List<Map<String, Object>> records = DBUtil.getHealthRecords(7);
        if (records != null) {
            List<Map<String, Object>> reversed = new ArrayList<>(records);
            Collections.reverse(reversed);
            for (Map<String, Object> r : reversed) {
                Object date = r.get("record_date");
                String label = date == null ? "" : date.toString().substring(5);
                double weight = toDouble(r.get("weight"));
                series.getData().add(new XYChart.Data<>(label, weight));
            }
        }
        chart.getData().add(series);
        series.getNode().setStyle("-fx-stroke: #2D8CA0; -fx-stroke-width: 2px;");
        return chart;
    }

    private HBox kv(String key, String value) {
        Label lk = new Label(key + "：");
        lk.getStyleClass().add("sub-title");
        Label lv = new Label(value);
        lv.setStyle("-fx-text-fill: #28323C; -fx-font-weight: bold; -fx-font-size: 14px;");
        HBox hb = new HBox(4, lk, lv);
        hb.setAlignment(Pos.CENTER_LEFT);
        return hb;
    }

    private int calcScore(Map<String, Object> latest) {
        try {
            return HealthCalculator.calcHealthScore(
                    toDouble(latest.get("bmi")),
                    toDouble(latest.get("body_fat")),
                    toInt(latest.get("visceral_fat")),
                    toDouble(latest.get("muscle_rate")),
                    toDouble(latest.get("water_rate")),
                    DBUtil.currentGender
            );
        } catch (Exception e) {
            return 0;
        }
    }

    private static String fmt(Object v) {
        if (v == null) return "-";
        String s = v.toString();
        return s.length() >= 10 ? s.substring(5, 10) : s;
    }

    private static String f1(Object v) { return String.format("%.1f", toDouble(v)); }
    private static String f2(Object v) { return String.format("%.2f", toDouble(v)); }

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
}
