import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

/**
 * 消息中心弹窗 — 显示通知列表，支持查看内容、标记已读。
 */
public class NotificationCenter {

    public static void show(String username, boolean isAdmin) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("消息中心");
        dialog.setHeaderText(null);
        dialog.setResizable(true);

        VBox root = new VBox(10);
        root.setPadding(new Insets(12));
        root.setPrefSize(760, 520);

        // 顶部工具栏
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        Label lblCount = new Label();
        lblCount.getStyleClass().add("sub-title");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button btnView = new Button("查看内容");
        btnView.getStyleClass().add("button-primary");
        Button btnRead = new Button("标记已读");
        btnRead.getStyleClass().add("button-accent");
        Button btnRefresh = new Button("刷新");
        btnRefresh.getStyleClass().add("button-ghost");
        toolbar.getChildren().addAll(lblCount, sp, btnView, btnRead, btnRefresh);

        // 表格
        TableView<String[]> table = new TableView<>();
        ObservableList<String[]> data = FXCollections.observableArrayList();
        String[] cols = isAdmin
                ? new String[]{"ID", "发送者", "接收者", "标题", "类型", "状态", "时间"}
                : new String[]{"ID", "发送者", "标题", "类型", "状态", "时间"};
        for (int i = 0; i < cols.length; i++) {
            final int idx = i;
            TableColumn<String[], String> c = new TableColumn<>(cols[i]);
            c.setCellValueFactory(cb -> new ReadOnlyStringWrapper(cb.getValue()[idx]));
            c.setPrefWidth(i == 0 ? 50 : (i == cols.length - 1 ? 140 : 110));
            // 状态列加颜色
            if ("状态".equals(cols[i])) {
                c.setCellFactory(tc -> new TableCell<String[], String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText("");
                            setStyle("");
                        } else {
                            setText(item);
                            if ("未读".equals(item)) {
                                setStyle("-fx-text-fill: #E67E22; -fx-font-weight: bold;");
                            } else {
                                setStyle("-fx-text-fill: #27AE60;");
                            }
                        }
                    }
                });
            }
            table.getColumns().add(c);
        }
        table.setItems(data);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        Runnable load = () -> {
            data.clear();
            List<String[]> list = isAdmin ? DBUtil.getAllNotifications() : DBUtil.getMyNotifications(username);
            data.addAll(list);
            int unread = isAdmin ? countUnread(list, 5) : DBUtil.getUnreadNotificationCount(username);
            lblCount.setText("共 " + list.size() + " 条消息，未读 " + unread + " 条");
        };

        btnView.setOnAction(e -> {
            String[] sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) {
                info("请先选择一条消息");
                return;
            }
            int idCol = isAdmin ? 0 : 0;
            int id = Integer.parseInt(sel[idCol]);
            String content = DBUtil.getNotificationContent(id);
            showContent(sel, content, isAdmin);
        });

        btnRead.setOnAction(e -> {
            String[] sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) {
                info("请先选择一条消息");
                return;
            }
            int id = Integer.parseInt(sel[0]);
            DBUtil.updateNotificationStatus(id, "已读");
            load.run();
        });

        btnRefresh.setOnAction(e -> load.run());

        table.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String[] sel = table.getSelectionModel().getSelectedItem();
                if (sel != null) {
                    int id = Integer.parseInt(sel[0]);
                    String content = DBUtil.getNotificationContent(id);
                    showContent(sel, content, isAdmin);
                }
            }
        });

        root.getChildren().addAll(toolbar, table);
        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().setPrefSize(760, 520);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        load.run();
        dialog.show();
    }

    private static int countUnread(List<String[]> list, int statusIdx) {
        int c = 0;
        for (String[] row : list) {
            if ("未读".equals(row[statusIdx])) c++;
        }
        return c;
    }

    private static void showContent(String[] row, String content, boolean isAdmin) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("消息详情");
        a.setHeaderText(row[isAdmin ? 3 : 2]); // 标题
        TextArea ta = new TextArea(content);
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setPrefSize(520, 320);
        a.getDialogPane().setContent(ta);
        a.setResizable(true);
        a.showAndWait();
    }

    private static void info(String m) {
        new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait();
    }
}
