import javafx.geometry.*;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;

/** 用户端数据大屏 — 展示当前用户自己的健康数据 */
public class UserDashboardPanel extends VBox {

    private final TilePane grid = new TilePane();

    public UserDashboardPanel() {
        setSpacing(12);
        setPadding(new Insets(14));
        setStyle("-fx-background-color: #F0F6F9;");

        HBox ctrl = new HBox(10);
        ctrl.setAlignment(Pos.CENTER_LEFT);
        Button btnRefresh = new Button("刷新大屏");
        btnRefresh.getStyleClass().add("button-primary");
        Label hint = new Label("实时展示您的个人健康数据");
        hint.getStyleClass().add("hint");
        ctrl.getChildren().addAll(btnRefresh, hint);

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
        gridProfile.setVgap(8);
        gridProfile.addRow(0, kv("用户名", DBUtil.currentUsername), kv("性别", DBUtil.currentGender));
        gridProfile.addRow(1, kv("年龄", String.valueOf(DBUtil.currentAge) + " 岁"), kv("身高", String.valueOf(DBUtil.currentHeight) + " cm"));
        gridProfile.addRow(2, kv("活动等级", DBUtil.currentActivityLevel), new Label(""));
        profileCard.getChildren().addAll(tProfile, gridProfile);

        // 关键指标卡片
        grid.setHgap(14);
        grid.setVgap(14);
        grid.setPrefColumns(4);
        grid.setPadding(new Insets(4));

        ScrollPane spGrid = new ScrollPane(grid);
        spGrid.setFitToWidth(true);
        VBox gridCard = new VBox(10);
        gridCard.getStyleClass().add("card");
        Label t2 = new Label("核心指标");
        t2.getStyleClass().add("card-title");
        gridCard.getChildren().addAll(t2, spGrid);

        // 最近体重趋势
        VBox trendCard = new VBox(10);
        trendCard.getStyleClass().add("card");
        Label t3 = new Label("最近体重趋势");
        t3.getStyleClass().add("card-title");
        LineChart<String, Number> trendChart = buildTrendChart();
        trendChart.setMaxHeight(240);
        trendCard.getChildren().addAll(t3, trendChart);

        getChildren().addAll(ctrlCard, profileCard, gridCard, trendCard);
        btnRefresh.setOnAction(e -> refresh());
        refresh();
    }

    private void refresh() {
        grid.getChildren().clear();

        Map<String, Object> latest = DBUtil.getLatestHealthRecord();
        List<Map<String, Object>> records = DBUtil.getHealthRecords(30);
        List<String[]> badges = DBUtil.getAchievements();

        int totalRecords = records == null ? 0 : records.size();
        int badgeCount = badges == null ? 0 : badges.size();

        String blue = Theme.hex(Theme.PRIMARY);
        String accent = Theme.hex(Theme.ACCENT);
        String teal = Theme.hex(Theme.SUCCESS);
        String purple = "#8050A0";
        String red = Theme.hex(Theme.DANGER);

        grid.getChildren().add(createCard("累计打卡", String.valueOf(totalRecords), blue));
        grid.getChildren().add(createCard("获得徽章", String.valueOf(badgeCount), purple));
        grid.getChildren().add(createCard("健康评分", latest == null ? "--" : String.valueOf(calcScore(latest)), teal));
        grid.getChildren().add(createCard("当前体重", latest == null ? "--" : f1(latest.get("weight")) + " kg", accent));

        grid.getChildren().add(createCard("BMI", latest == null ? "--" : f2(latest.get("bmi")), blue));
        grid.getChildren().add(createCard("体脂率", latest == null ? "--" : f1(latest.get("body_fat")) + "%", accent));
        grid.getChildren().add(createCard("内脏脂肪", latest == null ? "--" : String.valueOf(latest.get("visceral_fat")), red));
        grid.getChildren().add(createCard("身体年龄", latest == null ? "--" : String.valueOf(latest.get("body_age")), teal));
    }

    private VBox createCard(String title, String value, String colorHex) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16));
        card.setPrefSize(220, 110);
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

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        List<Map<String, Object>> records = DBUtil.getHealthRecords(7);
        if (records != null) {
            // 数据库返回按日期 DESC，图表需要 ASC
            List<Map<String, Object>> reversed = new ArrayList<>(records);
            Collections.reverse(reversed);
            for (Map<String, Object> r : reversed) {
                Object date = r.get("record_date");
                String label = date == null ? "" : date.toString().substring(5); // MM-DD
                double weight = toDouble(r.get("weight"));
                series.getData().add(new XYChart.Data<>(label, weight));
            }
        }
        chart.getData().add(series);

        // 折线颜色
        series.getNode().setStyle("-fx-stroke: #2D8CA0; -fx-stroke-width: 2px;");
        return chart;
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

    private HBox kv(String key, String value) {
        Label lk = new Label(key + "：");
        lk.getStyleClass().add("sub-title");
        Label lv = new Label(value);
        lv.setStyle("-fx-text-fill: #28323C; -fx-font-weight: bold;");
        HBox hb = new HBox(4, lk, lv);
        hb.setAlignment(Pos.CENTER_LEFT);
        return hb;
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
