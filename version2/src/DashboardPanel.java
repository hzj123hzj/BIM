import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.*;

/** 数据大屏面板 — 关键指标卡片布局 (getGlobalStats) */
public class DashboardPanel extends VBox {
    private final TilePane grid = new TilePane();

    public DashboardPanel() {
        setSpacing(12);
        setPadding(new Insets(14));
        setStyle("-fx-background-color: #F0F6F9;");

        HBox ctrl = new HBox(10);
        ctrl.setAlignment(Pos.CENTER_LEFT);
        Button btnRefresh = new Button("刷新大屏");
        btnRefresh.getStyleClass().add("button-primary");
        Label hint = new Label("实时展示平台核心健康指标");
        hint.getStyleClass().add("hint");
        ctrl.getChildren().addAll(btnRefresh, hint);

        VBox ctrlCard = new VBox(10);
        ctrlCard.getStyleClass().add("card");
        Label t1 = new Label("健康数据大屏");
        t1.getStyleClass().add("card-title");
        ctrlCard.getChildren().addAll(t1, ctrl);

        grid.setHgap(14);
        grid.setVgap(14);
        grid.setPrefColumns(4);
        grid.setPadding(new Insets(4));

        ScrollPane sp = new ScrollPane(grid);
        sp.setFitToWidth(true);
        VBox gridCard = new VBox(10);
        gridCard.getStyleClass().add("card");
        Label t2 = new Label("核心指标");
        t2.getStyleClass().add("card-title");
        gridCard.getChildren().addAll(t2, sp);

        getChildren().addAll(ctrlCard, gridCard);
        btnRefresh.setOnAction(e -> refresh());
        refresh();
    }

    private void refresh() {
        grid.getChildren().clear();
        Map<String, Object> stats = DBUtil.getGlobalStats();
        if (stats == null || stats.isEmpty()) {
            grid.getChildren().add(createCard("暂无数据", "统计不可用", "#9AA4AD"));
            return;
        }

        String blue = Theme.hex(Theme.PRIMARY);
        String accent = Theme.hex(Theme.ACCENT);
        String teal = Theme.hex(Theme.SUCCESS);
        String purple = "#8050A0";
        String red = Theme.hex(Theme.DANGER);

        grid.getChildren().add(createCard("总用户数", str(stats, "total_users"), blue));
        grid.getChildren().add(createCard("7 日活跃用户", str(stats, "active_users_7d"), teal));
        grid.getChildren().add(createCard("今日打卡", str(stats, "today_checkin"), accent));
        grid.getChildren().add(createCard("异常用户", str(stats, "abnormal_users"), red));

        grid.getChildren().add(createCard("平均 BMI", f2(num(stats, "avg_bmi")), blue));
        grid.getChildren().add(createCard("平均体脂率", f1(num(stats, "avg_body_fat")) + "%", accent));
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

    private double num(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (v instanceof Number) return ((Number) v).doubleValue();
        if (v instanceof String) { try { return Double.parseDouble((String) v); } catch (Exception e) { return 0; } }
        return 0;
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? "0" : v.toString();
    }

    private static String f1(double v) { return String.format("%.1f", v); }
    private static String f2(double v) { return String.format("%.2f", v); }
}
