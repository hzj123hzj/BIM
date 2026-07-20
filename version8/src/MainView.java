import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Arrays;
import java.util.List;

public class MainView {
    private BorderPane root = new BorderPane();

    public MainView() {
        // 顶栏
        HBox top = new HBox(12);
        top.setPadding(new Insets(10, 14, 10, 14));
        top.setAlignment(Pos.CENTER_LEFT);
        top.setStyle("-fx-background-color: linear-gradient(to right, #1E6478, #2D8CA0 60%, #1E6478); -fx-border-color: transparent transparent #154B5A transparent; -fx-border-width: 0 0 2 0; -fx-effect: dropshadow(gaussian, rgba(33,80,100,0.25), 10, 0.2, 0, 2);");
        Label user = new Label("欢迎, " + DBUtil.currentUsername);
        user.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;");
        Button btnMsg = new Button("消息中心");
        btnMsg.getStyleClass().add("button-primary");
        Button btnLogout = new Button("退出登录");
        btnLogout.getStyleClass().add("button-accent");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        top.getChildren().addAll(user, spacer, btnMsg, btnLogout);
        root.setTop(top);

        btnMsg.setOnAction(e -> NotificationCenter.show(DBUtil.currentUsername, false));
        btnLogout.setOnAction(e -> {
            DBUtil.currentUsername = "";
            App.showLogin();
        });

        // 左侧导航 + 内容区（替代 TabPane，规避 JavaFX 左侧 Tab 文字旋转缺陷）
        List<Tab> tabs = Arrays.asList(
                tab("数据大屏", new UserDashboardPanel()),
                tab("身体属性", new AttributeCenterPanel()),
                tab("数据录入", new DataInputPanel()),
                tab("历史趋势", new HistoryTrendPanel()),
                tab("分析评估", new AnalysisPanel()),
                tab("预测分析", new PredictionPanel()),
                tab("目标计划", new GoalPlanPanel()),
                tab("饮食管理", new DietPanel()),
                tab("AI 问答", new AIChatPanel()),
                tab("AI 饮食推荐", new AIDietPanel()),
                tab("AI 菜谱生成", new AICookbookPanel()),
                tab("成就徽章", new AchievementPanel()),
                tab("健康资讯", new HealthArticlePanel())
        );
        SideNav nav = new SideNav("健康管理系统", tabs);
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
