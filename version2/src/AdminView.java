import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * 管理员后台主视图（JavaFX 8 重写 version1 AdminMainFrame）
 * BorderPane 顶部栏 + 中间 TabPane（11 个 tab，TabClosingPolicy.UNAVAILABLE）。
 */
public class AdminView {
    private BorderPane root = new BorderPane();

    public AdminView() {
        // 顶部栏
        HBox top = new HBox(12);
        top.setPadding(new Insets(10, 14, 10, 14));
        top.setAlignment(Pos.CENTER_LEFT);
        top.setStyle("-fx-background-color: linear-gradient(to right, #E1F0F5, #F0F6F9); -fx-border-color: #D2DCE4; -fx-border-width: 0 0 1 0;");
        Label t = new Label("管理后台");
        t.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1E6478;");
        Label admin = new Label("当前管理员: " + DBUtil.currentUsername);
        admin.setStyle("-fx-font-size: 13px; -fx-text-fill: #646E78;");
        Button btnMsg = new Button("消息中心");
        btnMsg.getStyleClass().add("button-primary");
        Button btnLogout = new Button("退出登录");
        btnLogout.getStyleClass().add("button-accent");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        top.getChildren().addAll(t, admin, sp, btnMsg, btnLogout);
        btnMsg.setOnAction(e -> NotificationCenter.show(DBUtil.currentUsername, true));
        root.setTop(top);
        btnLogout.setOnAction(e -> {
            DBUtil.logAction("ADMIN", DBUtil.currentUsername, "退出登录", "返回登录界面");
            App.showLogin();
        });

        // 中间 TabPane
        TabPane tp = new TabPane();
        tp.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tp.getTabs().addAll(
                new Tab("用户管理", new UserManagePanel()),
                new Tab("食物数据库", new FoodManagePanel()),
                new Tab("运动库", new ExerciseManagePanel()),
                new Tab("健康文章", new ArticleManagePanel()),
                new Tab("AI问答记录", new AIChatRecordPanel()),
                new Tab("AI饮食记录", new AIDietRecordPanel()),
                new Tab("AI菜谱记录", new AICookbookRecordPanel()),
                new Tab("Prompt模板", new AITemplatePanel()),
                new Tab("API配置", new ApiConfigPanel()),
                new Tab("AI使用统计", new AIUsagePanel()),
                new Tab("数据监控", new DataMonitorPanel())
        );
        tp.setPadding(new Insets(10));
        root.setCenter(tp);
        root.setStyle("-fx-background-color: #F0F6F9;");
    }

    public BorderPane getRoot() {
        return root;
    }
}
