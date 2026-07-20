package com.bmi.ui.admin;

import com.bmi.db.DBUtil;

import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.beans.property.ReadOnlyStringWrapper;

import java.util.List;

/**
 * AI 使用统计面板（JavaFX 8 重写 version1 AISystemPanel 使用统计部分）
 * 表格：用户名 / AI问答次数 / AI饮食次数 / AI菜谱次数 / 最近使用时间。
 * getAIUsageStats() 返回 String[]{username,chat,diet,cookbook,last}。
 */
public class AIUsagePanel extends VBox {
    private final TableView<String[]> table = new TableView<>();

    public AIUsagePanel() {
        setSpacing(16);
        setPadding(new Insets(18));
        setStyle("-fx-background-color: transparent;");

        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        Label title = new Label("AI 使用统计");
        title.getStyleClass().add("card-title");
        card.getChildren().add(title);

        HBox ctrl = new HBox(8);
        ctrl.setAlignment(Pos.CENTER_LEFT);
        Button btnRefresh = new Button("刷新统计");
        btnRefresh.getStyleClass().add("button-primary");
        ctrl.getChildren().add(btnRefresh);
        card.getChildren().add(ctrl);

        table.getColumns().addAll(
                colA("用户名", 0, 160),
                colA("AI 问答", 1, 110),
                colA("AI 饮食", 2, 110),
                colA("AI 菜谱", 3, 110),
                colA("最后使用时间", 4, 180)
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        card.getChildren().add(table);
        getChildren().add(card);

        btnRefresh.setOnAction(e -> load());

        load();
    }

    private TableColumn<String[], String> colA(String name, int idx, double w) {
        TableColumn<String[], String> c = new TableColumn<>(name);
        c.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue()[idx]));
        c.setPrefWidth(w);
        return c;
    }

    private void load() {
        table.getItems().clear();
        for (String[] row : DBUtil.getAIUsageStats()) {
            table.getItems().add(row);
        }
    }
}
