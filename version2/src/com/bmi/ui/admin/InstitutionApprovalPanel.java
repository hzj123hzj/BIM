package com.bmi.ui.admin;

import com.bmi.db.DBUtil;

import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 机构入驻审批面板。
 * 列出待审批(pending)的入驻申请, 管理员可「通过」(生成机构编码+初始密码)或「拒绝」。
 * 通过后编码与密码以可复制对话框展示, 由管理员线下下发给机构。
 */
public class InstitutionApprovalPanel extends VBox {
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private final TableView<Map<String, Object>> table = new TableView<>();

    public InstitutionApprovalPanel() {
        setSpacing(16);
        setPadding(new Insets(18));
        setStyle("-fx-background-color: transparent;");

        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        Label title = new Label("机构入驻审批");
        title.getStyleClass().add("card-title");
        Label sub = new Label("待审批申请将由管理员审核, 通过后系统自动生成机构编码与初始密码");
        sub.getStyleClass().add("muted");

        HBox ctrl = new HBox(8);
        ctrl.setAlignment(Pos.CENTER_LEFT);
        Button btnRefresh = new Button("刷新");
        btnRefresh.getStyleClass().add("button-ghost");
        Button btnApprove = new Button("通过");
        btnApprove.getStyleClass().add("button-primary");
        Button btnReject = new Button("拒绝");
        btnReject.getStyleClass().add("button-accent");
        ctrl.getChildren().addAll(btnRefresh, btnApprove, btnReject);

        card.getChildren().addAll(title, sub, ctrl);

        buildTable();
        card.getChildren().add(table);
        getChildren().add(card);

        btnRefresh.setOnAction(e -> refresh());
        btnApprove.setOnAction(e -> approveSelected());
        btnReject.setOnAction(e -> rejectSelected());
        refresh();
    }

    private void buildTable() {
        String[] cols = {"ID", "机构名称", "联系人", "电话", "申请说明", "提交时间", "状态"};
        String[] keys = {"id", "org_name", "contact", "phone", "note", "created_at", "status"};
        for (int i = 0; i < cols.length; i++) {
            final int idx = i;
            TableColumn<Map<String, Object>, String> c = new TableColumn<>(cols[i]);
            c.setCellValueFactory(cb -> {
                Object v = cb.getValue().get(keys[idx]);
                String s = v == null ? "--" : v.toString();
                if ("created_at".equals(keys[idx]) && v instanceof Date)
                    s = sdf.format((Date) v);
                return new javafx.beans.property.ReadOnlyStringWrapper(s);
            });
            table.getColumns().add(c);
        }
    }

    private void refresh() {
        table.getItems().setAll(DBUtil.getInstitutionRequests("pending"));
    }

    private void approveSelected() {
        Map<String, Object> row = table.getSelectionModel().getSelectedItem();
        if (row == null) { alert(Alert.AlertType.WARNING, "提示", "请先选择一条待审批申请"); return; }
        int id = ((Number) row.get("id")).intValue();
        String[] cred = DBUtil.approveInstitutionRequest(id, DBUtil.currentUsername);
        if (cred == null) {
            alert(Alert.AlertType.ERROR, "失败", "审批失败 (申请可能已被处理或编号冲突)");
            refresh();
            return;
        }
        showCredentials((String) row.get("org_name"), cred[0], cred[1]);
        refresh();
    }

    private void rejectSelected() {
        Map<String, Object> row = table.getSelectionModel().getSelectedItem();
        if (row == null) { alert(Alert.AlertType.WARNING, "提示", "请先选择一条待审批申请"); return; }
        int id = ((Number) row.get("id")).intValue();
        TextInputDialog d = new TextInputDialog();
        d.setTitle("拒绝申请");
        d.setHeaderText("拒绝机构: " + row.get("org_name"));
        d.setContentText("拒绝原因 (可选):");
        Optional<String> note = d.showAndWait();
        if (DBUtil.rejectInstitutionRequest(id, DBUtil.currentUsername, note.orElse(""))) {
            alert(Alert.AlertType.INFORMATION, "已拒绝", "已拒绝该入驻申请");
        } else {
            alert(Alert.AlertType.ERROR, "失败", "操作失败, 请重试");
        }
        refresh();
    }

    private void showCredentials(String orgName, String code, String password) {
        Dialog<Void> d = new Dialog<>();
        d.setTitle("审批通过 — 机构凭证");
        d.setHeaderText("机构「" + orgName + "」已创建, 请线下将以下凭证发给机构");
        TextArea ta = new TextArea("机构名称: " + orgName + "\n机构编码: " + code + "\n初始密码: " + password);
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setPrefSize(420, 120);
        VBox box = new VBox(ta);
        box.setPadding(new Insets(10));
        d.getDialogPane().setContent(box);
        d.getDialogPane().setPrefSize(460, 180);
        d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        d.showAndWait();
    }

    private void alert(Alert.AlertType t, String title, String msg) {
        Alert a = new Alert(t, msg, ButtonType.OK);
        a.setTitle(title);
        a.showAndWait();
    }
}
