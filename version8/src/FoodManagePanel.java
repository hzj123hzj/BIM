import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.beans.property.ReadOnlyStringWrapper;

import java.util.List;
import java.util.ArrayList;

/**
 * 食物库管理面板（JavaFX 8 重写 version1 ContentManagementPanel 食物部分）
 * 列表、新增/编辑/删除。getFoods() 返回 String[]{id,name,cal,protein,carbs,fat}。
 */
public class FoodManagePanel extends VBox {
    private final TableView<String[]> table = new TableView<>();

    public FoodManagePanel() {
        setSpacing(16);
        setPadding(new Insets(18));
        setStyle("-fx-background-color: transparent;");

        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        Label title = new Label("食物数据库");
        title.getStyleClass().add("card-title");
        card.getChildren().add(title);

        HBox ctrl = new HBox(8);
        ctrl.setAlignment(Pos.CENTER_LEFT);
        Button btnAdd = new Button("新增");
        Button btnBatch = new Button("批量导入");
        Button btnEdit = new Button("编辑");
        Button btnDel = new Button("删除");
        btnAdd.getStyleClass().add("button-primary");
        btnBatch.getStyleClass().add("button-primary");
        btnEdit.getStyleClass().add("button-primary");
        btnDel.getStyleClass().add("button-ghost");
        ctrl.getChildren().addAll(btnAdd, btnBatch, btnEdit, btnDel);
        card.getChildren().add(ctrl);

        table.getColumns().addAll(
                colA("ID", 0, 60),
                colA("名称", 1, 160),
                colA("热量", 2, 90),
                colA("蛋白质", 3, 90),
                colA("碳水", 4, 90),
                colA("脂肪", 5, 90)
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        card.getChildren().add(table);
        getChildren().add(card);

        btnAdd.setOnAction(e -> editFood(0));
        btnBatch.setOnAction(e -> batchImportFoods());
        btnEdit.setOnAction(e -> {
            int id = getSelectedId();
            if (id > 0) editFood(id);
        });
        btnDel.setOnAction(e -> deleteFood());

        loadFoods();
    }

    private TableColumn<String[], String> colA(String name, int idx, double w) {
        TableColumn<String[], String> c = new TableColumn<>(name);
        c.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue()[idx]));
        c.setPrefWidth(w);
        return c;
    }

    private void loadFoods() {
        table.getItems().clear();
        for (String[] row : DBUtil.getFoods()) {
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

    private void editFood(int id) {
        String name = "", cal = "0", p = "0", c = "0", f = "0";
        if (id > 0) {
            String[] row = table.getSelectionModel().getSelectedItem();
            name = row[1];
            cal = row[2];
            p = row[3];
            c = row[4];
            f = row[5];
        }
        TextField tfName = new TextField(name);
        TextField tfCal = new TextField(cal);
        TextField tfP = new TextField(p);
        TextField tfC = new TextField(c);
        TextField tfF = new TextField(f);
        for (TextField t : new TextField[]{tfName, tfCal, tfP, tfC, tfF}) t.getStyleClass().add("text-field");

        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(8);
        g.addRow(0, new Label("名称"), tfName);
        g.addRow(1, new Label("热量"), tfCal);
        g.addRow(2, new Label("蛋白质"), tfP);
        g.addRow(3, new Label("碳水"), tfC);
        g.addRow(4, new Label("脂肪"), tfF);

        if (confirmDialog(id > 0 ? "编辑食物" : "新增食物", g)) {
            try {
                if (DBUtil.saveFood(id, tfName.getText().trim(),
                        Integer.parseInt(tfCal.getText().trim()),
                        Double.parseDouble(tfP.getText().trim()),
                        Double.parseDouble(tfC.getText().trim()),
                        Double.parseDouble(tfF.getText().trim()))) {
                    DBUtil.logAction("ADMIN", DBUtil.currentUsername, "保存食物", tfName.getText());
                    loadFoods();
                } else {
                    err("保存失败");
                }
            } catch (Exception ex) {
                err("输入格式错误");
            }
        }
    }

    private void deleteFood() {
        int id = getSelectedId();
        if (id < 0) return;
        if (!confirm("确认", "确定删除该食物吗？")) return;
        if (DBUtil.deleteFood(id)) {
            DBUtil.logAction("ADMIN", DBUtil.currentUsername, "删除食物", "ID=" + id);
            loadFoods();
        }
    }

    private void batchImportFoods() {
        List<String[]> rows = ExcelImportDialog.show("批量导入食物",
                "请选择 Excel（.xlsx）文件。列顺序：名称, 热量, 蛋白质, 碳水, 脂肪。\n" +
                "热量为整数，其余可为小数；首行可设为表头（自动跳过）。");
        if (rows == null || rows.isEmpty()) return;

        List<String[]> cleaned = new ArrayList<>();
        for (String[] r : rows) {
            if (r.length < 5) continue;
            for (int i = 0; i < r.length; i++) r[i] = (r[i] == null ? "" : r[i].trim());
            cleaned.add(r);
        }
        if (cleaned.isEmpty()) {
            warn("未解析到有效数据，请检查列数（需至少 5 列：名称/热量/蛋白质/碳水/脂肪）");
            return;
        }
        try {
            int[] result = DBUtil.batchInsertFoods(cleaned);
            info("导入完成：成功 " + result[0] + " 条，跳过/失败 " + result[1] + " 条");
            DBUtil.logAction("ADMIN", DBUtil.currentUsername, "批量导入食物", "成功 " + result[0] + " 条");
            loadFoods();
        } catch (Exception ex) {
            err("批量导入失败：" + ex.getMessage());
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

    private void info(String m) {
        new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait();
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
