import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.beans.property.ReadOnlyStringWrapper;

import java.util.List;
import java.util.Map;

/**
 * AI 饮食记录面板（JavaFX 8 重写 version1 AISystemPanel 饮食部分）
 * 列表、双击查看、标记有效/无效。
 * getAIDietRecords() 返回 String[]{id,username,query,status,time}。
 */
public class AIDietRecordPanel extends VBox {
    private final TableView<String[]> table = new TableView<>();

    public AIDietRecordPanel() {
        setSpacing(16);
        setPadding(new Insets(18));
        setStyle("-fx-background-color: transparent;");

        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        Label title = new Label("AI 饮食记录");
        title.getStyleClass().add("card-title");
        card.getChildren().add(title);

        HBox ctrl = new HBox(8);
        ctrl.setAlignment(Pos.CENTER_LEFT);
        Button btnRefresh = new Button("刷新");
        Button btnValid = new Button("标记有效");
        Button btnInvalid = new Button("标记无效");
        btnRefresh.getStyleClass().add("button-primary");
        btnValid.getStyleClass().add("button-primary");
        btnInvalid.getStyleClass().add("button-ghost");
        ctrl.getChildren().addAll(btnRefresh, btnValid, btnInvalid);
        card.getChildren().add(ctrl);

        table.getColumns().addAll(
                colA("ID", 0, 60),
                colA("用户", 1, 120),
                colA("饮食需求", 2, 340),
                colA("状态", 3, 90),
                colA("时间", 4, 160)
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String[] sel = table.getSelectionModel().getSelectedItem();
                if (sel != null) viewRecord(Integer.parseInt(sel[0]));
            }
        });
        card.getChildren().add(table);
        getChildren().add(card);

        btnRefresh.setOnAction(e -> load());
        btnValid.setOnAction(e -> updateStatus("有效"));
        btnInvalid.setOnAction(e -> updateStatus("无效"));

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
        for (String[] row : DBUtil.getAIDietRecords()) {
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

    private void viewRecord(int id) {
        Map<String, String> rec = DBUtil.getAIDietRecordById(id);
        if (rec == null || rec.isEmpty()) {
            warn("未找到该记录");
            return;
        }
        TextArea ta = new TextArea(rec.getOrDefault("result", ""));
        ta.setEditable(false);
        ta.setPrefRowCount(12);
        ta.setPrefColumnCount(50);
        ta.setWrapText(true);
        ta.getStyleClass().add("text-field");
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("AI 饮食推荐详情 - " + rec.getOrDefault("username", "") + " [" + rec.getOrDefault("status", "") + "]");
        a.getDialogPane().setContent(ta);
        a.setResizable(true);
        a.showAndWait();
    }

    private void updateStatus(String status) {
        int id = getSelectedId();
        if (id < 0) return;
        if (DBUtil.updateAIDietStatus(id, status)) {
            if ("无效".equals(status)) notifyRejected(id);
            DBUtil.logAction("ADMIN", DBUtil.currentUsername, "审核AI饮食", "ID=" + id + ", 状态=" + status);
            load();
        } else {
            warn("操作失败（可能已是该状态）");
        }
    }

    private void notifyRejected(int id) {
        Map<String, String> rec = DBUtil.getAIDietRecordById(id);
        if (rec == null) return;
        String username = rec.get("username");
        if (username != null && !username.isEmpty()) {
            DBUtil.saveNotification("系统", username, "AI 内容审核通知",
                    "您的一条 AI 饮食推荐记录（ID=" + id + "）被管理员判定为无效，已对您隐藏。" +
                            "如内容有误或需要更准确建议，请重新生成或重新提问。", "审核");
        }
    }

    private void warn(String m) {
        new Alert(Alert.AlertType.WARNING, m, ButtonType.OK).showAndWait();
    }
}
