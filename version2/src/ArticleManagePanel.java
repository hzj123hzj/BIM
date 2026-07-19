import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.beans.property.ReadOnlyStringWrapper;

import java.util.List;

/**
 * 健康文章管理面板（JavaFX 8 重写 version1 ContentManagementPanel 文章部分）
 * 列表、新增/编辑/删除。status 字段只读展示（DBUtil.saveHealthArticle 接口不接收 status）。
 * getHealthArticles() 返回 String[]{id,title,category,status,published_at}。
 */
public class ArticleManagePanel extends VBox {
    private final TableView<String[]> table = new TableView<>();

    public ArticleManagePanel() {
        setSpacing(12);
        setPadding(new Insets(14));
        setStyle("-fx-background-color: #F0F6F9;");

        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        Label title = new Label("健康文章");
        title.getStyleClass().add("card-title");
        card.getChildren().add(title);

        HBox ctrl = new HBox(8);
        ctrl.setAlignment(Pos.CENTER_LEFT);
        Button btnAdd = new Button("新增");
        Button btnEdit = new Button("编辑");
        btnAdd.getStyleClass().add("button-primary");
        btnEdit.getStyleClass().add("button-primary");
        ctrl.getChildren().addAll(btnAdd, btnEdit);
        card.getChildren().add(ctrl);

        table.getColumns().addAll(
                colA("ID", 0, 60),
                colA("标题", 1, 240),
                colA("分类", 2, 120),
                colA("状态", 3, 90),
                colA("发布时间", 4, 160)
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        card.getChildren().add(table);
        getChildren().add(card);

        btnAdd.setOnAction(e -> editArticle(0));
        btnEdit.setOnAction(e -> {
            int id = getSelectedId();
            if (id > 0) editArticle(id);
        });

        loadArticles();
    }

    private TableColumn<String[], String> colA(String name, int idx, double w) {
        TableColumn<String[], String> c = new TableColumn<>(name);
        c.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue()[idx]));
        c.setPrefWidth(w);
        return c;
    }

    private void loadArticles() {
        table.getItems().clear();
        for (String[] row : DBUtil.getHealthArticles()) {
            table.getItems().add(row);
        }
    }

    private int getSelectedId() {
        String[] sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            warn("请先选择一行");
            return -1;
        }
        return Integer.parseInt(sel[0]);
    }

    private void editArticle(int id) {
        String title = "", category = "健康科普", content = "";
        if (id > 0) {
            String[] row = table.getSelectionModel().getSelectedItem();
            title = row[1];
            category = row[2];
        }
        TextField tfTitle = new TextField(title);
        TextField tfCategory = new TextField(category);
        TextArea taContent = new TextArea(content);
        taContent.setPrefRowCount(6);
        taContent.setWrapText(true);
        tfTitle.getStyleClass().add("text-field");
        tfCategory.getStyleClass().add("text-field");
        taContent.getStyleClass().add("text-field");

        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(8);
        g.addRow(0, new Label("标题"), tfTitle);
        g.addRow(1, new Label("分类"), tfCategory);
        g.add(new Label("内容"), 0, 2);
        g.add(taContent, 1, 2);

        if (confirmDialog(id > 0 ? "编辑文章" : "新增文章", g)) {
            if (DBUtil.saveHealthArticle(id, tfTitle.getText().trim(),
                    taContent.getText(), tfCategory.getText().trim())) {
                DBUtil.logAction("ADMIN", DBUtil.currentUsername, "保存文章", tfTitle.getText());
                loadArticles();
            } else {
                err("保存失败");
            }
        }
    }

    private boolean confirmDialog(String title, GridPane content) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "", ButtonType.OK, ButtonType.CANCEL);
        a.setTitle(title);
        a.getDialogPane().setContent(content);
        return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    private void warn(String m) {
        new Alert(Alert.AlertType.WARNING, m, ButtonType.OK).showAndWait();
    }

    private void err(String m) {
        new Alert(Alert.AlertType.ERROR, m, ButtonType.OK).showAndWait();
    }

    private boolean confirm(String title, String m) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, m, ButtonType.YES, ButtonType.NO);
        a.setTitle(title);
        return a.showAndWait().filter(b -> b == ButtonType.YES).isPresent();
    }
}
