package com.bmi.ui.user;

import com.bmi.db.DBUtil;

import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.*;

/** 健康资讯面板 — 浏览已发布文章, 双击查看正文 */
public class HealthArticlePanel extends VBox {
    private final TableView<String[]> table = new TableView<>();
    private final ObservableList<String[]> items = FXCollections.observableArrayList();

    public HealthArticlePanel() {
        setSpacing(16);
        setPadding(new Insets(18));
        setStyle("-fx-background-color: transparent;");

        HBox ctrl = new HBox(10);
        ctrl.setAlignment(Pos.CENTER_LEFT);
        Button btnRefresh = new Button("刷新");
        btnRefresh.getStyleClass().add("button-primary");
        Label hint = new Label("点击文章行查看正文");
        hint.getStyleClass().add("hint");
        ctrl.getChildren().addAll(btnRefresh, hint);

        VBox ctrlCard = new VBox(10);
        ctrlCard.getStyleClass().add("card");
        Label t1 = new Label("健康资讯");
        t1.getStyleClass().add("card-title");
        ctrlCard.getChildren().addAll(t1, ctrl);

        String[] cols = {"ID", "标题", "分类", "发布时间"};
        for (int i = 0; i < cols.length; i++) {
            final int idx = i;
            TableColumn<String[], String> c = new TableColumn<>(cols[i]);
            c.setCellValueFactory(cb -> new ReadOnlyStringWrapper(cb.getValue()[idx]));
            table.getColumns().add(c);
        }
        table.setItems(items);
        table.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && table.getSelectionModel().getSelectedItem() != null) {
                viewArticle(table.getSelectionModel().getSelectedItem());
            }
        });

        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        Label t2 = new Label("已发布文章");
        t2.getStyleClass().add("card-title");
        card.getChildren().add(new ScrollPane(table){{setFitToWidth(true);}});

        getChildren().addAll(ctrlCard, card);
        btnRefresh.setOnAction(e -> loadArticles());
        loadArticles();
    }

    private void loadArticles() {
        items.setAll(DBUtil.getPublishedHealthArticles());
    }

    private void viewArticle(String[] row) {
        int id = Integer.parseInt(row[0]);
        Map<String, String> art = DBUtil.getHealthArticleById(id);
        if (art == null || art.isEmpty()) { alert("未找到该文章"); return; }

        Label lblTitle = new Label(art.get("title") == null ? "" : art.get("title"));
        lblTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1E6478;");
        Label lblMeta = new Label("分类：" + (art.get("category") == null ? "-" : art.get("category"))
                + "  ｜  发布：" + (art.get("published_at") == null ? "-" : art.get("published_at")));
        lblMeta.getStyleClass().add("hint");

        TextArea ta = new TextArea(art.get("content") == null ? "" : art.get("content"));
        ta.setWrapText(true);
        ta.setEditable(false);
        ta.setPrefSize(560, 360);
        ta.setStyle("-fx-font-family: 'Microsoft YaHei UI', 'Microsoft YaHei', sans-serif; -fx-font-size: 13px;");

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().addAll(lblTitle, lblMeta, ta);

        Alert dlg = new Alert(Alert.AlertType.INFORMATION);
        dlg.setTitle("健康资讯");
        dlg.setHeaderText(null);
        dlg.getDialogPane().setContent(content);
        dlg.setResizable(true);
        dlg.showAndWait();
    }

    private void alert(String m) {
        new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait();
    }
}
