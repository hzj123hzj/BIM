package com.bmi.ui.user;

import com.bmi.App;
import com.bmi.db.DBUtil;
import com.bmi.util.HealthCalculator;
import com.bmi.util.Theme;

import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

/** 医疗机构端 — 病人列表 + 批量导入(CSV) + 逐人分析 */
public class InstitutionView extends VBox {
    private final TableView<Map<String, Object>> patientTable = new TableView<>();
    private Map<String, Object> selectedProfile = null;

    public InstitutionView() {
        setSpacing(14);
        setPadding(new Insets(18));
        setStyle("-fx-background-color: transparent;");

        // 顶部栏
        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("医疗机构工作台 — " + DBUtil.currentInstitutionName);
        title.getStyleClass().add("card-title");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button btnUpload = new Button("上传CSV批量导入");
        btnUpload.getStyleClass().add("button-accent");
        Button btnAnalyze = new Button("查看分析");
        btnAnalyze.getStyleClass().add("button-primary");
        Button btnRefresh = new Button("刷新");
        btnRefresh.getStyleClass().add("button-ghost");
        Button btnLogout = new Button("退出");
        btnLogout.getStyleClass().add("button-ghost");
        top.getChildren().addAll(title, sp, btnUpload, btnAnalyze, btnRefresh, btnLogout);

        buildTable();
        refresh();

        btnUpload.setOnAction(e -> uploadCsv());
        btnAnalyze.setOnAction(e -> analyzeSelected());
        btnRefresh.setOnAction(e -> refresh());
        btnLogout.setOnAction(e -> App.showLogin());

        getChildren().addAll(top, patientTable);
    }

    private void buildTable() {
        String[] cols = {"用户名", "性别", "年龄", "身高(cm)", "最新体重(kg)", "BMI", "体质类型", "最近记录"};
        String[] keys = {"username", "gender", "age", "height", "weight", "bmi", "body_type", "last_date"};
        for (int i = 0; i < cols.length; i++) {
            final int idx = i;
            TableColumn<Map<String, Object>, String> c = new TableColumn<>(cols[i]);
            c.setCellValueFactory(cb -> {
                Object v = cb.getValue().get(keys[idx]);
                return new javafx.beans.property.ReadOnlyStringWrapper(v == null ? "--" : v.toString());
            });
            patientTable.getColumns().add(c);
        }
        patientTable.setRowFactory(tv -> {
            TableRow<Map<String, Object>> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (!row.isEmpty()) selectedProfile = row.getItem();
            });
            return row;
        });
    }

    private void refresh() {
        patientTable.getItems().setAll(DBUtil.getInstitutionPatients(DBUtil.currentInstitutionId));
    }

    private void uploadCsv() {
        FileChooser fc = new FileChooser();
        fc.setTitle("选择病人健康数据 CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV 文件", "*.csv"));
        File file = fc.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (file == null) return;
        List<Map<String, Object>> rows = parseCsv(file);
        if (rows.isEmpty()) {
            alert("CSV 无有效数据行 (需表头: username,record_date,weight,...)");
            return;
        }
        DBUtil.ImportResult res = DBUtil.importInstitutionRecords(DBUtil.currentInstitutionId, rows);
        StringBuilder sb = new StringBuilder();
        sb.append("导入完成: 成功 ").append(res.success).append(" 条");
        if (!res.errors.isEmpty()) {
            sb.append("\n跳过 ").append(res.errors.size()).append(" 条:\n");
            for (String err : res.errors) sb.append("  - ").append(err).append("\n");
        }
        alert(sb.toString());
        refresh();
    }

    private List<Map<String, Object>> parseCsv(File file) {
        List<Map<String, Object>> rows = new ArrayList<>();
        SimpleDateFormat[] fmts = {new SimpleDateFormat("yyyy-MM-dd"), new SimpleDateFormat("yyyy/MM/dd")};
        try (BufferedReader br = Files.newBufferedReader(file.toPath())) {
            String header = br.readLine();
            if (header == null) return rows;
            String[] headers = header.split(",");
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] vals = line.split(",");
                if (vals.length < headers.length) continue;
                Map<String, Object> m = new HashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    String h = headers[i].trim();
                    String v = vals[i].trim();
                    if ("username".equals(h)) m.put("username", v);
                    else if ("record_date".equals(h)) {
                        Date d = null;
                        for (SimpleDateFormat f : fmts) {
                            try { d = f.parse(v); break; } catch (Exception ignore) {}
                        }
                        if (d != null) m.put("record_date", d);
                    } else {
                        try { m.put(h, Double.parseDouble(v)); } catch (Exception ignore) { m.put(h, 0.0); }
                    }
                }
                if (m.get("username") != null) rows.add(m);
            }
        } catch (Exception e) {
            DBUtil.logError("InstitutionView.parseCsv", e);
        }
        return rows;
    }

    private void analyzeSelected() {
        Map<String, Object> row = patientTable.getSelectionModel().getSelectedItem();
        if (row == null) { alert("请先选择一位病人"); return; }
        String username = (String) row.get("username");
        String gender = (String) row.get("gender");
        int age = ((Number) row.get("age")).intValue();
        double height = ((Number) row.get("height")).doubleValue();
        Map<String, Object> rec = DBUtil.getLatestHealthRecord(username);
        if (rec == null) { alert("该病人暂无健康记录"); return; }

        double bmi = HealthCalculator.calcBMI((double) rec.get("weight"), height);
        StringBuilder sb = new StringBuilder();
        sb.append("═══════ 病人分析: ").append(username).append(" ═══════\n\n");
        sb.append("BMI: ").append(String.format("%.1f", bmi)).append(" (").append(HealthCalculator.classifyBMI(bmi)).append(")\n");
        sb.append("体脂率: ").append(String.format("%.1f", (double) rec.get("body_fat"))).append("%\n");
        sb.append("体质类型: ").append(rec.get("body_type")).append("\n\n");
        Map<String, Object> rec2 = new HashMap<>(rec);
        rec2.put("username", username);
        Map<String, Object> rcm = HealthCalculator.recommendGoal(rec2, gender, age, height);
        sb.append("【智能建议】\n").append(rcm.get("reason")).append("\n");
        sb.append("\n推荐目标: ").append(rcm.get("goalType")).append(" / ")
                .append(String.format("%.1f", (double) rcm.get("targetWeight"))).append(" kg");
        showText("病人分析 — " + username, sb.toString());
    }

    private void alert(String m) {
        new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait();
    }

    private void showText(String title, String content) {
        Dialog<Void> d = new Dialog<>();
        d.setTitle(title);
        d.setHeaderText(null);
        TextArea ta = new TextArea(content);
        ta.setEditable(false); ta.setWrapText(true); ta.setPrefSize(560, 360);
        VBox box = new VBox(ta); box.setPadding(new Insets(10));
        d.getDialogPane().setContent(box);
        d.getDialogPane().setPrefSize(580, 400);
        d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        d.showAndWait();
    }

    public VBox getRoot() { return this; }
}
