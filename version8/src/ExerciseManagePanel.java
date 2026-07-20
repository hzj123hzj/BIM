import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.beans.property.ReadOnlyStringWrapper;

import java.util.List;
import java.util.ArrayList;

/**
 * 运动库管理面板（JavaFX 8 重写 version1 ContentManagementPanel 运动部分）
 * 列表、新增/编辑/删除。getExerciseLibrary() 返回 String[]{id,name,type,cal,intensity,desc}。
 */
public class ExerciseManagePanel extends VBox {
    private final TableView<String[]> table = new TableView<>();

    public ExerciseManagePanel() {
        setSpacing(16);
        setPadding(new Insets(18));
        setStyle("-fx-background-color: transparent;");

        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        Label title = new Label("运动库");
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
                colA("类型", 2, 90),
                colA("热量/小时", 3, 100),
                colA("强度", 4, 90),
                colA("说明", 5, 220)
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        card.getChildren().add(table);
        getChildren().add(card);

        btnAdd.setOnAction(e -> editExercise(0));
        btnBatch.setOnAction(e -> batchImportExercises());
        btnEdit.setOnAction(e -> {
            int id = getSelectedId();
            if (id > 0) editExercise(id);
        });
        btnDel.setOnAction(e -> deleteExercise());

        loadExercises();
    }

    private TableColumn<String[], String> colA(String name, int idx, double w) {
        TableColumn<String[], String> c = new TableColumn<>(name);
        c.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue()[idx]));
        c.setPrefWidth(w);
        return c;
    }

    private void loadExercises() {
        table.getItems().clear();
        for (String[] row : DBUtil.getExerciseLibrary()) {
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

    private void editExercise(int id) {
        String name = "", type = "有氧", cal = "0", intensity = "中", desc = "";
        if (id > 0) {
            String[] row = table.getSelectionModel().getSelectedItem();
            name = row[1];
            type = row[2];
            cal = row[3];
            intensity = row[4];
            desc = row[5];
        }
        TextField tfName = new TextField(name);
        ComboBox<String> cbType = new ComboBox<>();
        cbType.getItems().addAll("有氧", "力量");
        cbType.setValue(type);
        TextField tfCal = new TextField(cal);
        ComboBox<String> cbInt = new ComboBox<>();
        cbInt.getItems().addAll("低", "中", "高");
        cbInt.setValue(intensity);
        TextField tfDesc = new TextField(desc);
        for (TextField t : new TextField[]{tfName, tfCal, tfDesc}) t.getStyleClass().add("text-field");

        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(8);
        g.addRow(0, new Label("名称"), tfName);
        g.addRow(1, new Label("类型"), cbType);
        g.addRow(2, new Label("热量/小时"), tfCal);
        g.addRow(3, new Label("强度"), cbInt);
        g.addRow(4, new Label("说明"), tfDesc);

        if (confirmDialog(id > 0 ? "编辑运动" : "新增运动", g)) {
            try {
                if (DBUtil.saveExerciseLibrary(id, tfName.getText().trim(),
                        cbType.getValue(), Integer.parseInt(tfCal.getText().trim()),
                        cbInt.getValue(), tfDesc.getText().trim())) {
                    DBUtil.logAction("ADMIN", DBUtil.currentUsername, "保存运动", tfName.getText());
                    loadExercises();
                } else {
                    err("保存失败");
                }
            } catch (Exception ex) {
                err("输入格式错误");
            }
        }
    }

    private void deleteExercise() {
        int id = getSelectedId();
        if (id < 0) return;
        if (!confirm("确认", "确定删除该运动吗？")) return;
        if (DBUtil.deleteExerciseLibrary(id)) {
            DBUtil.logAction("ADMIN", DBUtil.currentUsername, "删除运动", "ID=" + id);
            loadExercises();
        }
    }

    private void batchImportExercises() {
        List<String[]> rows = ExcelImportDialog.show("批量导入运动",
                "请选择 Excel（.xlsx）文件。列顺序：名称, 类型, 热量/小时, 强度, 说明。\n" +
                "类型填 有氧/力量，强度填 低/中/高；首行可设为表头（自动跳过）。");
        if (rows == null || rows.isEmpty()) return;

        List<String[]> cleaned = new ArrayList<>();
        for (String[] r : rows) {
            if (r.length < 5) continue;
            for (int i = 0; i < r.length; i++) r[i] = (r[i] == null ? "" : r[i].trim());
            String type = r[1];
            if (!"有氧".equals(type) && !"力量".equals(type)) type = "有氧";
            String intensity = r[3];
            if (!"低".equals(intensity) && !"中".equals(intensity) && !"高".equals(intensity)) intensity = "中";
            cleaned.add(new String[]{r[0], type, r[2], intensity, r[4]});
        }
        if (cleaned.isEmpty()) {
            warn("未解析到有效数据，请检查列数（需至少 5 列：名称/类型/热量/强度/说明）");
            return;
        }
        try {
            int[] result = DBUtil.batchInsertExercises(cleaned);
            info("导入完成：成功 " + result[0] + " 条，跳过/失败 " + result[1] + " 条");
            DBUtil.logAction("ADMIN", DBUtil.currentUsername, "批量导入运动", "成功 " + result[0] + " 条");
            loadExercises();
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
