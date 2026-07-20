package com.bmi.ui.admin;

import com.bmi.db.DBUtil;
import com.bmi.util.ExcelUtil;

import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

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
        Button btnEdit = new Button("编辑");
        Button btnDel = new Button("删除");
        Button btnImport = new Button("批量导入");
        btnAdd.getStyleClass().add("button-primary");
        btnEdit.getStyleClass().add("button-primary");
        btnDel.getStyleClass().add("button-ghost");
        btnImport.getStyleClass().add("button-primary");
        ctrl.getChildren().addAll(btnAdd, btnEdit, btnDel, btnImport);
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
        btnEdit.setOnAction(e -> {
            int id = getSelectedId();
            if (id > 0) editFood(id);
        });
        btnDel.setOnAction(e -> deleteFood());
        btnImport.setOnAction(e -> importFromExcel());

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

    private void importFromExcel() {
        FileChooser fc = new FileChooser();
        fc.setTitle("选择食物 Excel 文件");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Excel 文件 (*.xlsx, *.xls)", "*.xlsx", "*.xls"),
                new FileChooser.ExtensionFilter("所有文件", "*.*"));
        File file = fc.showOpenDialog(getScene().getWindow());
        if (file == null) return;

        List<String[]> rows;
        try {
            rows = ExcelUtil.readFoods(file);
        } catch (Exception ex) {
            ex.printStackTrace();
            err("读取 Excel 失败: " + ex.getMessage());
            return;
        }
        if (rows.isEmpty()) {
            warn("该文件中没有可导入的数据（请检查列：名称、热量、蛋白质、碳水、脂肪）");
            return;
        }
        showImportPreview(rows, file.getName());
    }

    private void showImportPreview(List<String[]> rows, String fileName) {
        TableView<String[]> preview = new TableView<>();
        preview.getColumns().addAll(
                colA("名称", 1, 160),
                colA("热量", 2, 90),
                colA("蛋白质", 3, 90),
                colA("碳水", 4, 90),
                colA("脂肪", 5, 90)
        );
        preview.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        preview.setPrefHeight(360);
        preview.getItems().addAll(rows);

        Label info = new Label("文件: " + fileName + "    共 " + rows.size() + " 行（按名称同步：已存在则更新，不存在则新增）");
        info.setWrapText(true);

        VBox box = new VBox(10, info, preview);
        box.setPadding(new Insets(6));

        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "", ButtonType.OK, ButtonType.CANCEL);
        a.setTitle("确认导入食物数据");
        a.getDialogPane().setContent(box);
        if (a.showAndWait().filter(b -> b == ButtonType.OK).isPresent()) {
            try {
                String result = DBUtil.importFoods(rows);
                DBUtil.logAction("ADMIN", DBUtil.currentUsername, "批量导入食物", fileName + " -> " + result);
                loadFoods();
                new Alert(Alert.AlertType.INFORMATION, "导入完成: " + result, ButtonType.OK).showAndWait();
            } catch (Exception ex) {
                ex.printStackTrace();
                err("导入失败: " + ex.getMessage());
            }
        }
    }
}
