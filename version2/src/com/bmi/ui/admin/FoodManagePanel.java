package com.bmi.ui.admin;

import com.bmi.db.DBUtil;
import com.bmi.util.ExcelUtil;
import com.bmi.util.ImageUtil;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 食物库管理面板（JavaFX 8 重写 version1 ContentManagementPanel 食物部分）
 * 列表（含图片）、新增/编辑/删除、批量导入、待确认草稿审核。
 */
public class FoodManagePanel extends VBox {
    private final TableView<DBUtil.FoodRow> table = new TableView<>();
    private final TableView<DBUtil.FoodRow> draftTable = new TableView<>();

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
                colStr("ID", fr -> String.valueOf(fr.id()), 60),
                colStr("名称", DBUtil.FoodRow::name, 160),
                colStr("热量", fr -> String.valueOf(fr.cal()), 90),
                colStr("蛋白质", fr -> DBUtil.df2.format(fr.protein()), 90),
                colStr("碳水", fr -> DBUtil.df2.format(fr.carbs()), 90),
                colStr("脂肪", fr -> DBUtil.df2.format(fr.fat()), 90),
                colImage()
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        card.getChildren().add(table);
        getChildren().add(card);

        // 待确认草稿审核区
        VBox draftCard = new VBox(10);
        draftCard.getStyleClass().add("card");
        Label dTitle = new Label("待确认食物（AI 识图新增，需审核）");
        dTitle.getStyleClass().add("card-title");
        draftCard.getChildren().add(dTitle);

        draftTable.getColumns().addAll(
                colStr("ID", fr -> String.valueOf(fr.id()), 60),
                colStr("名称", DBUtil.FoodRow::name, 160),
                colStr("热量", fr -> String.valueOf(fr.cal()), 90),
                colImage()
        );
        draftTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        draftTable.setPrefHeight(170);
        draftCard.getChildren().add(draftTable);

        HBox draftCtrl = new HBox(8);
        draftCtrl.setAlignment(Pos.CENTER_LEFT);
        Button btnApprove = new Button("通过(已发布)");
        Button btnReject = new Button("拒绝(删除)");
        btnApprove.getStyleClass().add("button-primary");
        btnReject.getStyleClass().add("button-ghost");
        draftCtrl.getChildren().addAll(btnApprove, btnReject);
        draftCard.getChildren().add(draftCtrl);
        getChildren().add(draftCard);

        btnAdd.setOnAction(e -> editFood(0));
        btnEdit.setOnAction(e -> {
            DBUtil.FoodRow sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) editFood(sel.id());
            else warn("请先选择一行");
        });
        btnDel.setOnAction(e -> deleteFood());
        btnImport.setOnAction(e -> importFromExcel());
        btnApprove.setOnAction(e -> {
            DBUtil.FoodRow sel = draftTable.getSelectionModel().getSelectedItem();
            if (sel == null) { warn("请先选择一条待确认食物"); return; }
            if (DBUtil.approveFood(sel.id())) {
                DBUtil.logAction("ADMIN", DBUtil.currentUsername, "通过食物草稿", sel.name());
                loadAll();
            }
        });
        btnReject.setOnAction(e -> {
            DBUtil.FoodRow sel = draftTable.getSelectionModel().getSelectedItem();
            if (sel == null) { warn("请先选择一条待确认食物"); return; }
            if (confirm("确认", "拒绝并删除该草稿？")) {
                if (DBUtil.rejectFood(sel.id())) {
                    DBUtil.logAction("ADMIN", DBUtil.currentUsername, "拒绝食物草稿", sel.name());
                    loadAll();
                }
            }
        });

        loadAll();
    }

    private void loadAll() {
        loadFoods();
        loadDrafts();
    }

    private void loadFoods() {
        table.getItems().clear();
        table.getItems().addAll(DBUtil.getFoodsWithImage());
    }

    private void loadDrafts() {
        draftTable.getItems().clear();
        draftTable.getItems().addAll(DBUtil.getDraftFoods());
    }

    private TableColumn<DBUtil.FoodRow, String> colStr(String name, Function<DBUtil.FoodRow, String> f, double w) {
        TableColumn<DBUtil.FoodRow, String> c = new TableColumn<>(name);
        c.setCellValueFactory(cb -> new ReadOnlyObjectWrapper<>(f.apply(cb.getValue())));
        c.setPrefWidth(w);
        return c;
    }

    private TableColumn<DBUtil.FoodRow, DBUtil.FoodRow> colImage() {
        TableColumn<DBUtil.FoodRow, DBUtil.FoodRow> c = new TableColumn<>("图片");
        c.setPrefWidth(70);
        c.setCellValueFactory(cb -> new ReadOnlyObjectWrapper<>(cb.getValue()));
        c.setCellFactory(tc -> new TableCell<DBUtil.FoodRow, DBUtil.FoodRow>() {
            private final ImageView iv = new ImageView();
            {
                iv.setFitWidth(56);
                iv.setFitHeight(56);
                iv.setPreserveRatio(true);
            }
            @Override
            protected void updateItem(DBUtil.FoodRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.image() == null || item.image().length == 0) {
                    setGraphic(placeholderNode(56));
                } else {
                    iv.setImage(ImageUtil.byteArrayToImage(item.image()));
                    setGraphic(iv);
                }
            }
        });
        return c;
    }

    private Node placeholderNode(int size) {
        StackPane ph = new StackPane(new Label("无图"));
        ph.setPrefSize(size, size);
        ph.setStyle("-fx-background-color:#E3E8EE; -fx-background-radius:6; "
                + "-fx-border-color:#C7D0DA; -fx-border-radius:6;");
        ph.getChildren().get(0).setStyle("-fx-text-fill:#8A97A5; -fx-font-size:10px;");
        return ph;
    }

    private void editFood(int id) {
        DBUtil.FoodRow existing = null;
        if (id > 0) {
            for (DBUtil.FoodRow fr : table.getItems()) {
                if (fr.id() == id) { existing = fr; break; }
            }
        }
        String name = existing != null ? existing.name() : "";
        String cal = existing != null ? String.valueOf(existing.cal()) : "0";
        String p = existing != null ? DBUtil.df2.format(existing.protein()) : "0";
        String c = existing != null ? DBUtil.df2.format(existing.carbs()) : "0";
        String f = existing != null ? DBUtil.df2.format(existing.fat()) : "0";
        TextField tfName = new TextField(name);
        TextField tfCal = new TextField(cal);
        TextField tfP = new TextField(p);
        TextField tfC = new TextField(c);
        TextField tfF = new TextField(f);
        for (TextField t : new TextField[]{tfName, tfCal, tfP, tfC, tfF}) t.getStyleClass().add("text-field");

        final byte[][] picked = { existing != null ? existing.image() : null };
        ImageView preview = new ImageView();
        preview.setFitWidth(80);
        preview.setFitHeight(80);
        preview.setPreserveRatio(true);
        if (picked[0] != null && picked[0].length > 0) preview.setImage(ImageUtil.byteArrayToImage(picked[0]));
        Button btnPick = new Button("选择图片");
        btnPick.getStyleClass().add("button-ghost");
        btnPick.setOnAction(ev -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("选择食物图片");
            fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("图片", "*.png", "*.jpg", "*.jpeg", "*.bmp"));
            Window owner = (getScene() != null) ? getScene().getWindow() : null;
            File ff = fc.showOpenDialog(owner);
            if (ff != null) {
                try {
                    picked[0] = Files.readAllBytes(ff.toPath());
                    preview.setImage(ImageUtil.byteArrayToImage(picked[0]));
                } catch (Exception ex) {
                    err("读取图片失败: " + ex.getMessage());
                }
            }
        });

        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(8);
        g.addRow(0, new Label("名称"), tfName);
        g.addRow(1, new Label("热量"), tfCal);
        g.addRow(2, new Label("蛋白质"), tfP);
        g.addRow(3, new Label("碳水"), tfC);
        g.addRow(4, new Label("脂肪"), tfF);
        g.addRow(5, new Label("图片"), btnPick);
        g.addRow(6, new Label(""), preview);

        if (confirmDialog(id > 0 ? "编辑食物" : "新增食物", g)) {
            try {
                // 编辑且未重新选图时传 null，保留原图；否则用所选（可为 null=无图）
                byte[] imgToSave = (id <= 0) ? picked[0]
                        : (existing != null && picked[0] != existing.image() ? picked[0] : null);
                boolean ok = DBUtil.saveFood(id, tfName.getText().trim(),
                        Integer.parseInt(tfCal.getText().trim()),
                        Double.parseDouble(tfP.getText().trim()),
                        Double.parseDouble(tfC.getText().trim()),
                        Double.parseDouble(tfF.getText().trim()),
                        imgToSave);
                if (ok) {
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
        DBUtil.FoodRow sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { warn("请先选择一行"); return; }
        if (!confirm("确认", "确定删除该食物吗？")) return;
        if (DBUtil.deleteFood(sel.id())) {
            DBUtil.logAction("ADMIN", DBUtil.currentUsername, "删除食物", "ID=" + sel.id());
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
        fc.setTitle("选择要导入的食物 Excel 文件（可多选）");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Excel 文件 (*.xlsx, *.xls)", "*.xlsx", "*.xls"),
                new FileChooser.ExtensionFilter("所有文件", "*.*"));
        Window owner = (getScene() != null) ? getScene().getWindow() : null;
        List<File> files = fc.showOpenMultipleDialog(owner);
        if (files == null || files.isEmpty()) return;

        List<String[]> rows = new ArrayList<>();
        StringBuilder failed = new StringBuilder();
        int okFiles = 0;
        for (File f : files) {
            try {
                List<String[]> r = ExcelUtil.readFoods(f);
                if (!r.isEmpty()) {
                    rows.addAll(r);
                    okFiles++;
                } else {
                    failed.append("\n• ").append(f.getName()).append("：无数据行");
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
                failed.append("\n• ").append(f.getName()).append("：")
                        .append(ex.getClass().getSimpleName()).append(" - ").append(ex.getMessage());
            }
        }
        if (rows.isEmpty()) {
            warn("所选文件中没有可导入的数据（请检查列：名称、热量、蛋白质、碳水、脂肪）"
                    + (failed.length() > 0 ? "\n\n以下文件读取失败：" + failed : ""));
            return;
        }

        String fileInfo = okFiles + "/" + files.size() + " 个文件，" + rows.size() + " 行"
                + (failed.length() > 0 ? "；" + countLines(failed) + " 个文件读取失败" : "");
        showImportPreview(rows, fileInfo);
    }

    private int countLines(StringBuilder sb) {
        int c = 0;
        for (int i = 0; i < sb.length(); i++) if (sb.charAt(i) == '\n') c++;
        return c;
    }

    private void showImportPreview(List<String[]> rows, String fileName) {
        TableView<String[]> preview = new TableView<>();
        preview.getColumns().addAll(
                colA("名称", 0, 160),
                colA("热量", 1, 90),
                colA("蛋白质", 2, 90),
                colA("碳水", 3, 90),
                colA("脂肪", 4, 90)
        );
        preview.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        preview.setPrefHeight(360);
        preview.getItems().addAll(rows);

        Label info = new Label("来源: " + fileName + "    共 " + rows.size() + " 行（按名称同步：已存在则更新，不存在则新增）");
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
                new Alert(Alert.AlertType.INFORMATION, "导入成功！\n" + result, ButtonType.OK).showAndWait();
            } catch (Throwable ex) {
                ex.printStackTrace();
                err("导入失败: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
            }
        }
    }

    /** String[] 预览表的文本列（仅批量导入预览用）。 */
    private TableColumn<String[], String> colA(String name, int idx, double w) {
        TableColumn<String[], String> c = new TableColumn<>(name);
        c.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()[idx]));
        c.setPrefWidth(w);
        return c;
    }
}
