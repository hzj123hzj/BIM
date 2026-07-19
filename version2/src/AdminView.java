import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Arrays;
import java.util.List;

/**
 * 管理员后台主视图（JavaFX 8 重写 version1 AdminMainFrame）
 * BorderPane 顶部栏 + 左侧 SideNav 导航（11 个面板，与用户端形式统一）。
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

        // 左侧导航 + 内容区（替代 TabPane，规避 JavaFX 左侧 Tab 文字旋转缺陷）
        List<Tab> tabs = Arrays.asList(
                tab("用户管理", new UserManagePanel()),
                tab("食物数据库", new FoodManagePanel()),
                tab("运动库", new ExerciseManagePanel()),
                tab("健康文章", new ArticleManagePanel()),
                tab("AI问答记录", new AIChatRecordPanel()),
                tab("AI饮食记录", new AIDietRecordPanel()),
                tab("AI菜谱记录", new AICookbookRecordPanel()),
                tab("Prompt模板", new AITemplatePanel()),
                tab("API配置", new ApiConfigPanel()),
                tab("AI使用统计", new AIUsagePanel()),
                tab("数据监控", new DataMonitorPanel())
        );
        SideNav nav = new SideNav("管理后台", tabs);
        root.setLeft(nav.getSidebar());
        root.setCenter(nav.getContent());
    }

    private static Tab tab(String name, javafx.scene.Parent content) {
        return new Tab(name, content);
    }

    public BorderPane getRoot() {
        return root;
    }
}
