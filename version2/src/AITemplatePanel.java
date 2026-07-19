import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.beans.property.ReadOnlyStringWrapper;

import java.util.List;
import java.util.Map;

/**
 * Prompt 模板管理面板（JavaFX 8 重写 version1 AISystemPanel 模板部分）
 * 列表、新增/编辑/删除，双击查看模板详情。
 * getAITemplates() 返回 String[]{id,name,type,status,updated_at}。
 */
public class AITemplatePanel extends VBox {
    private final TableView<String[]> table = new TableView<>();

    public AITemplatePanel() {
        setSpacing(16);
        setPadding(new Insets(18));
        setStyle("-fx-background-color: transparent;");

        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        Label title = new Label("Prompt 模板");
        title.getStyleClass().add("card-title");
        card.getChildren().add(title);

        HBox ctrl = new HBox(8);
        ctrl.setAlignment(Pos.CENTER_LEFT);
        Button btnAdd = new Button("新增模板");
        Button btnEdit = new Button("编辑");
        btnAdd.getStyleClass().add("button-primary");
        btnEdit.getStyleClass().add("button-primary");
        ctrl.getChildren().addAll(btnAdd, btnEdit);
        card.getChildren().add(ctrl);

        table.getColumns().addAll(
                colA("ID", 0, 60),
                colA("名称", 1, 160),
                colA("类型", 2, 110),
                colA("状态", 3, 90),
                colA("更新时间", 4, 170)
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String[] sel = table.getSelectionModel().getSelectedItem();
                if (sel != null) viewTemplate(Integer.parseInt(sel[0]));
            }
        });
        card.getChildren().add(table);
        getChildren().add(card);

        btnAdd.setOnAction(e -> editTemplate(0));
        btnEdit.setOnAction(e -> {
            int id = getSelectedId();
            if (id > 0) editTemplate(id);
        });

        loadTemplates();
    }

    private TableColumn<String[], String> colA(String name, int idx, double w) {
        TableColumn<String[], String> c = new TableColumn<>(name);
        c.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue()[idx]));
        c.setPrefWidth(w);
        return c;
    }

    private void loadTemplates() {
        table.getItems().clear();
        for (String[] row : DBUtil.getAITemplates()) {
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

    private void viewTemplate(int id) {
        Map<String, String> t = DBUtil.getAITemplateById(id);
        if (t == null || t.isEmpty()) {
            warn("未找到模板内容");
            return;
        }
        GridPane info = new GridPane();
        info.setHgap(10);
        info.setVgap(6);
        info.addRow(0, new Label("模板名称："), new Label(t.getOrDefault("template_name", "")));
        info.addRow(1, new Label("模板类型："), new Label(t.getOrDefault("template_type", "")));
        info.addRow(2, new Label("当前状态："), new Label(t.getOrDefault("status", "")));
        info.addRow(3, new Label("更新时间："), new Label(t.getOrDefault("updated_at", "-")));

        TextArea ta = new TextArea(t.getOrDefault("prompt_text", ""));
        ta.setEditable(false);
        ta.setPrefRowCount(12);
        ta.setPrefColumnCount(50);
        ta.setWrapText(true);
        ta.getStyleClass().add("text-field");

        VBox box = new VBox(10, info, ta);
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Prompt 模板详情");
        a.getDialogPane().setContent(box);
        a.setResizable(true);
        a.showAndWait();
    }

    private void editTemplate(int id) {
        String name = "", type = "建议", content = "";
        if (id > 0) {
            String[] row = table.getSelectionModel().getSelectedItem();
            name = row[1];
            type = row[2];
            Map<String, String> t = DBUtil.getAITemplateById(id);
            if (t != null && !t.isEmpty()) content = t.getOrDefault("prompt_text", "");
        }
        TextField tfName = new TextField(name);
        ComboBox<String> cbType = new ComboBox<>();
        cbType.getItems().addAll("建议", "周报", "饮食推荐");
        cbType.setValue(type);
        TextArea taContent = new TextArea(content);
        taContent.setPrefRowCount(8);
        taContent.setPrefColumnCount(40);
        taContent.setWrapText(true);
        tfName.getStyleClass().add("text-field");
        taContent.getStyleClass().add("text-field");

        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(8);
        g.addRow(0, new Label("名称"), tfName);
        g.addRow(1, new Label("类型"), cbType);
        g.add(new Label("Prompt内容"), 0, 2);
        g.add(taContent, 1, 2);

        if (confirmDialog(id > 0 ? "编辑模板" : "新增模板", g)) {
            if (DBUtil.saveAITemplate(id, tfName.getText().trim(), cbType.getValue(), taContent.getText())) {
                DBUtil.logAction("ADMIN", DBUtil.currentUsername, "保存AI模板", tfName.getText());
                loadTemplates();
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
}
