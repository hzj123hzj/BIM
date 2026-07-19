import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class MainView {
    private BorderPane root = new BorderPane();

    public MainView() {
        // 顶栏
        HBox top = new HBox(12);
        top.setPadding(new Insets(10, 14, 10, 14));
        top.setAlignment(Pos.CENTER_LEFT);
        top.setStyle("-fx-background-color: linear-gradient(to right, #E1F0F5, #F0F6F9); -fx-border-color: #D2DCE4; -fx-border-width: 0 0 1 0;");
        Label user = new Label("欢迎, " + DBUtil.currentUsername);
        user.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1E6478;");
        Button btnMsg = new Button("消息中心");
        btnMsg.getStyleClass().add("button-primary");
        Button btnLogout = new Button("退出登录");
        btnLogout.getStyleClass().add("button-accent");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        top.getChildren().addAll(user, spacer, btnMsg, btnLogout);
        root.setTop(top);

        btnMsg.setOnAction(e -> alert("消息中心模块迁移中"));
        btnLogout.setOnAction(e -> {
            DBUtil.currentUsername = "";
            App.showLogin();
        });

        // 中部 Tab
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
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
                tab("数据大屏", new DashboardPanel()),
                tab("健康资讯", new HealthArticlePanel())
        );
        tabs.setPadding(new Insets(8));
        root.setCenter(tabs);
        root.setStyle("-fx-background-color: #F0F6F9;");
    }

    private static Tab tab(String name, javafx.scene.Parent content) {
        Tab t = new Tab(name, content);
        return t;
    }

    private void alert(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }

    public BorderPane getRoot() {
        return root;
    }
}
