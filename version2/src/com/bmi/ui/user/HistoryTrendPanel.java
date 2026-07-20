package com.bmi.ui.user;

import com.bmi.db.DBUtil;
import com.bmi.util.Theme;

import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.chart.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.*;
import java.util.Map;
import java.util.List;
import java.text.SimpleDateFormat;

/** 历史趋势面板 — 指标选择 + 折线趋势图 + 历史记录明细表 */
public class HistoryTrendPanel extends VBox {
    private final ComboBox<String> cbMetric = new ComboBox<>();
    private final LineChart<Number, Number> chart;
    private final TableView<Map<String, Object>> table = new TableView<>();
    private final ObservableList<Map<String, Object>> tableData = FXCollections.observableArrayList();
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    public HistoryTrendPanel() {
        setSpacing(16);
        setPadding(new Insets(18));
        setStyle("-fx-background-color: transparent;");

        cbMetric.getItems().addAll("体重", "体脂率", "肌肉率", "BMI", "腰围", "蛋白质率", "骨量", "水分率", "内脏脂肪");
        cbMetric.setValue("体重");

        HBox ctrl = new HBox(10);
        ctrl.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label("趋势指标:");
        lbl.getStyleClass().add("sub-title");
        Button btnRefresh = new Button("刷新趋势");
        btnRefresh.getStyleClass().add("button-primary");
        ctrl.getChildren().addAll(lbl, cbMetric, btnRefresh);

        VBox ctrlCard = new VBox(10);
        ctrlCard.getStyleClass().add("card");
        Label t1 = new Label("趋势指标选择");
        t1.getStyleClass().add("card-title");
        ctrlCard.getChildren().addAll(t1, ctrl);

        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("记录序号");
        xAxis.setTickUnit(1);
        xAxis.setMinorTickVisible(false);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("数值");
        chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("历史趋势");
        chart.setPrefHeight(320);
        chart.setMinHeight(220);
        chart.setLegendVisible(false);
        chart.setCreateSymbols(true);
        chart.setStyle("-fx-series-color: " + Theme.hex(Theme.PRIMARY) + ";");

        VBox chartCard = new VBox(10);
        chartCard.getStyleClass().add("card");
        Label t2 = new Label("历史趋势图");
        t2.getStyleClass().add("card-title");
        chartCard.getChildren().addAll(t2, chart);

        buildTable();
        ScrollPane sp = new ScrollPane(table);
        sp.setFitToWidth(true);
        sp.setPrefHeight(240);
        VBox tableCard = new VBox(10);
        tableCard.getStyleClass().add("card");
        Label t3 = new Label("历史记录明细");
        t3.getStyleClass().add("card-title");
        tableCard.getChildren().addAll(t3, sp);

        getChildren().addAll(ctrlCard, chartCard, tableCard);

        cbMetric.setOnAction(e -> refresh());
        btnRefresh.setOnAction(e -> refresh());
        refresh();
    }

    private void buildTable() {
        addCol("日期", r -> fmtDate((Date) r.get("record_date")));
        addCol("体重", r -> f1(num(r, "weight")) + "kg");
        addCol("体脂率", r -> f1(num(r, "body_fat")) + "%");
        addCol("水分率", r -> f1(num(r, "water_rate")) + "%");
        addCol("肌肉率", r -> f1(num(r, "muscle_rate")) + "%");
        addCol("内脏脂肪", r -> r.get("visceral_fat") + "级");
        addCol("BMI", r -> f1(num(r, "bmi")));
        addCol("腰围", r -> f1(num(r, "waist")) + "cm");
        addCol("蛋白质率", r -> f1(num(r, "protein_rate")) + "%");
        addCol("骨量", r -> f1(num(r, "bone_mass")) + "kg");
        addCol("身体年龄", r -> r.get("body_age") + "岁");
        addCol("体质分类", r -> str(r, "body_type"));
        table.setItems(tableData);
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    }

    private void addCol(String name, java.util.function.Function<Map<String, Object>, String> f) {
        TableColumn<Map<String, Object>, String> c = new TableColumn<>(name);
        c.setCellValueFactory(cb -> new ReadOnlyStringWrapper(f.apply(cb.getValue())));
        table.getColumns().add(c);
    }

    private void refresh() {
        String metric = cbMetric.getValue();
        List<Map<String, Object>> records = DBUtil.getHealthRecords(60);

        // 反转成正序
        List<Date> dates = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        for (int i = records.size() - 1; i >= 0; i--) {
            Map<String, Object> r = records.get(i);
            dates.add((Date) r.get("record_date"));
            switch (metric) {
                case "体重": values.add(num(r, "weight")); break;
                case "体脂率": values.add(num(r, "body_fat")); break;
                case "肌肉率": values.add(num(r, "muscle_rate")); break;
                case "BMI": values.add(num(r, "bmi")); break;
                case "腰围": values.add(num(r, "waist")); break;
                case "蛋白质率": values.add(num(r, "protein_rate")); break;
                case "骨量": values.add(num(r, "bone_mass")); break;
                case "水分率": values.add(num(r, "water_rate")); break;
                case "内脏脂肪": values.add(num(r, "visceral_fat")); break;
                default: values.add(num(r, "weight"));
            }
        }

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName(metric + " 趋势");
        for (int i = 0; i < values.size(); i++) {
            XYChart.Data<Number, Number> d = new XYChart.Data<>(i + 1, values.get(i));
            final int idx = i;
            d.setNode(tooltipNode(fmtDate(dates.get(idx)) + "  " + metric + ": " + f1(values.get(idx))));
            series.getData().add(d);
        }
        chart.setTitle(metric + " 历史趋势");
        chart.getYAxis().setLabel(metric.equals("体重") ? "kg"
                : metric.equals("腰围") ? "cm"
                : metric.equals("内脏脂肪") ? "级"
                : metric.equals("BMI") ? "BMI" : "%");
        chart.getData().clear();
        chart.getData().add(series);

        tableData.setAll(records);
    }

    private Node tooltipNode(String text) {
        Label dot = new Label();
        dot.setStyle("-fx-background-color: #FFFFFF, #2D8CA0; -fx-background-insets: 0, 2px; -fx-background-radius: 7px; -fx-min-width: 12; -fx-min-height: 12; -fx-border-color: #2D8CA0; -fx-border-width: 2; -fx-border-radius: 7px;");
        Tooltip tp = new Tooltip(text);
        Tooltip.install(dot, tp);
        return dot;
    }

    private String fmtDate(Date d) {
        return d == null ? "-" : sdf.format(d);
    }

    private double num(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v instanceof Number ? ((Number) v).doubleValue() : 0.0;
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? "-" : v.toString();
    }

    private static String f1(double v) {
        return String.format("%.1f", v);
    }

    private void alert(String m) {
        new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait();
    }
}
