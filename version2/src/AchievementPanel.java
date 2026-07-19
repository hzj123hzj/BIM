import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.*;

/** 成就徽章面板 — 检查并展示已获得的成就徽章 */
public class AchievementPanel extends VBox {
    private static final String[][] ALL_BADGES = {
            {"毅力之星", "连续打卡 7 天"},
            {"坚持达人", "连续打卡 30 天"},
            {"目标达成者", "达成阶段目标"},
            {"美食家", "记录饮食 30 天"},
            {"运动健将", "记录运动 20 次"},
            {"健康标兵", "健康评分达到 90"},
            {"蜕变之星", "体脂率降至正常"}
    };

    private final FlowPane badgePane = new FlowPane();
    private final Label lblCount = new Label();

    public AchievementPanel() {
        setSpacing(12);
        setPadding(new Insets(14));
        setStyle("-fx-background-color: #F0F6F9;");

        HBox ctrl = new HBox(10);
        ctrl.setAlignment(Pos.CENTER_LEFT);
        Button btnRefresh = new Button("刷新徽章");
        btnRefresh.getStyleClass().add("button-primary");
        lblCount.getStyleClass().add("sub-title");
        ctrl.getChildren().addAll(btnRefresh, lblCount);

        VBox ctrlCard = new VBox(10);
        ctrlCard.getStyleClass().add("card");
        Label t1 = new Label("成就徽章");
        t1.getStyleClass().add("card-title");
        ctrlCard.getChildren().addAll(t1, ctrl);

        badgePane.setHgap(12);
        badgePane.setVgap(12);
        badgePane.setPadding(new Insets(4));

        ScrollPane sp = new ScrollPane(badgePane);
        sp.setFitToWidth(true);
        sp.setPrefHeight(420);
        VBox badgeCard = new VBox(10);
        badgeCard.getStyleClass().add("card");
        Label t2 = new Label("徽章墙");
        t2.getStyleClass().add("card-title");
        badgeCard.getChildren().addAll(t2, sp);

        getChildren().addAll(ctrlCard, badgeCard);
        btnRefresh.setOnAction(e -> refresh());
        refresh();
    }

    private void refresh() {
        DBUtil.checkAndGrantAchievements();
        List<String[]> earned = DBUtil.getAchievements();
        Set<String> earnedNames = new HashSet<>();
        Map<String, String> earnedDate = new HashMap<>();
        for (String[] b : earned) {
            earnedNames.add(b[0]);
            earnedDate.put(b[0], b[1]);
        }

        badgePane.getChildren().clear();
        for (String[] badge : ALL_BADGES) {
            boolean has = earnedNames.contains(badge[0]);
            VBox chip = new VBox(6);
            chip.setPadding(new Insets(14));
            chip.setPrefSize(200, 110);
            chip.getStyleClass().add("card");
            if (has) chip.setStyle("-fx-background-color: " + Theme.hex(Theme.ACCENT) + "; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(30,60,80,0.12), 10, 0, 0, 4);");
            else chip.setStyle("-fx-background-color: #EDEFF1; -fx-background-radius: 12;");

            Label icon = new Label(has ? "★" : "☆");
            icon.setStyle("-fx-font-size: 26px; -fx-text-fill: " + (has ? "white" : "#9AA4AD") + ";");
            Label name = new Label(badge[0]);
            name.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + (has ? "white" : "#646E78") + ";");
            Label desc = new Label(badge[1]);
            desc.setWrapText(true);
            desc.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (has ? "#FFF4E8" : "#8A929B") + ";");
            chip.getChildren().addAll(icon, name, desc);
            if (has && earnedDate.containsKey(badge[0])) {
                Label dt = new Label("📅 " + earnedDate.get(badge[0]));
                dt.setStyle("-fx-font-size: 11px; -fx-text-fill: #FFF4E8;");
                chip.getChildren().add(dt);
            }
            badgePane.getChildren().add(chip);
        }
        lblCount.setText("已获得: " + earnedNames.size() + "/" + ALL_BADGES.length + " 个徽章");
    }
}
