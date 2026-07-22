package com.bmi.ui.user;

import com.bmi.db.DBUtil;
import com.bmi.util.Theme;

import javafx.animation.*;
import javafx.geometry.*;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

import java.util.*;

/** 成就徽章面板 — 检查并展示已获得的成就徽章（含概览、进度提示与解锁动效） */
public class AchievementPanel extends VBox {
    // {图标, 名称, 描述}
    private static final String[][] ALL_BADGES = {
            {"🔥", "毅力之星", "连续打卡 7 天"},
            {"🏆", "坚持达人", "连续打卡 30 天"},
            {"🎯", "目标达成者", "达成阶段目标"},
            {"🍱", "美食家", "记录饮食 30 天"},
            {"🏃", "运动健将", "记录运动 20 次"},
            {"💧", "补水达人", "累计记录饮水 7 天"},
            {"💪", "健康标兵", "健康评分达到 90"},
            {"🌟", "蜕变之星", "体脂率降至正常"}
    };

    /** 拖动滚动灵敏度：>1 表示更跟手（内容移动多于光标位移） */
    private static final double DRAG_SPEED = 1.6;

    private final FlowPane badgePane = new FlowPane();
    private final Label toast = new Label();
    private final StackPane ring = new StackPane();
    private final Label ringPct = new Label();
    private final VBox overviewInfo = new VBox(6);
    private final Set<String> prevEarned = new HashSet<>();
    private boolean firstLoad = true;

    public AchievementPanel() {
        setSpacing(16);
        setPadding(new Insets(18));
        setStyle("-fx-background-color: transparent;");

        // 顶部：标题 + 刷新按钮（「成就徽章」与「成绩概览」合并为一栏，刷新置于最右）
        Button btnRefresh = new Button("刷新徽章");
        btnRefresh.getStyleClass().add("button-primary");
        Label title = new Label("成就徽章");
        title.getStyleClass().add("card-title");
        Region headSpacer = new Region();
        HBox.setHgrow(headSpacer, Priority.ALWAYS);
        HBox header = new HBox(12, title, headSpacer, btnRefresh);
        header.setAlignment(Pos.CENTER_LEFT);

        // 概览（环形进度 + 文案）
        HBox overviewRow = new HBox(22);
        overviewRow.setAlignment(Pos.CENTER_LEFT);
        ringPct.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + Theme.hex(Theme.ACCENT) + ";");
        ring.getChildren().add(ringPct);
        overviewRow.getChildren().addAll(ring, overviewInfo);

        VBox mergedCard = new VBox(14);
        mergedCard.getStyleClass().add("card");
        mergedCard.getChildren().addAll(header, overviewRow);

        // 解锁提示 toast（默认隐藏）
        toast.getStyleClass().add("badge-toast");
        toast.setVisible(false);
        toast.setManaged(false);

        // 徽章墙
        badgePane.getStyleClass().add("badge-wall");
        ScrollPane sp = new ScrollPane(badgePane);
        sp.setFitToWidth(true);
        sp.setPrefHeight(500);
        enableWheelScroll(sp);
        enableDragToScroll(sp);

        VBox badgeCard = new VBox(10);
        badgeCard.getStyleClass().add("card");
        Label t2 = new Label("徽章墙");
        t2.getStyleClass().add("card-title");
        badgeCard.getChildren().addAll(t2, sp);

        getChildren().addAll(mergedCard, toast, badgeCard);
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
        Map<String, int[]> progress = DBUtil.getBadgeProgress();

        // 检测本次新解锁的徽章（首次加载不弹动效）
        Set<String> newly = new HashSet<>(earnedNames);
        newly.removeAll(prevEarned);
        prevEarned.clear();
        prevEarned.addAll(earnedNames);
        boolean animateNew = !firstLoad && !newly.isEmpty();
        firstLoad = false;

        updateOverview(earnedNames.size());

        badgePane.getChildren().clear();
        for (String[] badge : ALL_BADGES) {
            String icon = badge[0];
            String name = badge[1];
            String desc = badge[2];
            boolean has = earnedNames.contains(name);

            VBox card = new VBox(10);
            card.setAlignment(Pos.CENTER);
            card.getStyleClass().add("badge-card");
            card.getStyleClass().add(has ? "badge-card-earned" : "badge-card-locked");

            // 圆形徽章图标
            StackPane emblem = new StackPane();
            emblem.getStyleClass().add("badge-emblem");
            emblem.getStyleClass().add(has ? "badge-emblem-earned" : "badge-emblem-locked");
            Label iconLbl = new Label(has ? icon : "🔒");
            iconLbl.getStyleClass().add("badge-icon");
            iconLbl.getStyleClass().add(has ? "badge-icon-earned" : "badge-icon-locked");
            emblem.getChildren().add(iconLbl);

            Label nameLbl = new Label(name);
            nameLbl.getStyleClass().add("badge-name");
            nameLbl.getStyleClass().add(has ? "badge-name-earned" : "badge-name-locked");

            Label descLbl = new Label(desc);
            descLbl.setWrapText(true);
            descLbl.setAlignment(Pos.CENTER);
            descLbl.getStyleClass().add("badge-desc");
            descLbl.getStyleClass().add(has ? "badge-desc-earned" : "badge-desc-locked");

            Label status = new Label(has ? "已获得" : "未获得");
            status.getStyleClass().add("badge-status");
            status.getStyleClass().add(has ? "badge-status-earned" : "badge-status-locked");

            card.getChildren().addAll(emblem, nameLbl, descLbl, status);

            if (has) {
                if (earnedDate.containsKey(name)) {
                    Label dt = new Label("📅 " + earnedDate.get(name));
                    dt.getStyleClass().add("badge-date");
                    card.getChildren().add(dt);
                }
            } else {
                // 未解锁：展示进度条 + 进度提示
                int[] pr = progress.get(name);
                if (pr != null && pr[1] > 1) {
                    double ratio = Math.max(0, Math.min(1, (double) pr[0] / pr[1]));
                    ProgressBar pb = new ProgressBar(ratio);
                    pb.getStyleClass().add("badge-progress");
                    pb.setPrefWidth(160);
                    pb.setMaxWidth(160);
                    Label ptext = new Label(progressText(name, pr[0], pr[1]));
                    ptext.getStyleClass().add("badge-progress-text");
                    ptext.setAlignment(Pos.CENTER);
                    card.getChildren().addAll(pb, ptext);
                } else {
                    Label hint = new Label(progressText(name, pr == null ? 0 : pr[0], pr == null ? 1 : pr[1]));
                    hint.getStyleClass().add("badge-progress-text");
                    hint.setAlignment(Pos.CENTER);
                    card.getChildren().add(hint);
                }
            }

            // 悬停缩放微交互
            ScaleTransition hover = new ScaleTransition(Duration.millis(140), card);
            card.setOnMouseEntered(e -> { hover.setToX(1.04); hover.setToY(1.04); hover.playFromStart(); });
            card.setOnMouseExited(e -> { hover.setToX(1.0); hover.setToY(1.0); hover.playFromStart(); });

            badgePane.getChildren().add(card);

            if (animateNew && newly.contains(name)) playUnlockPulse(card);
        }

        if (animateNew) showToast(newly);
    }

    /** 更新顶部环形概览 */
    private void updateOverview(int earned) {
        int total = ALL_BADGES.length;
        double pct = total == 0 ? 0 : (double) earned / total;
        double r = 44, sw = 12;
        double circum = 2 * Math.PI * r;
        double len = Math.max(0, Math.min(1, pct)) * circum;

        Circle track = new Circle(r);
        track.setFill(null);
        track.setStroke(Color.web("#E2EBF0"));
        track.setStrokeWidth(sw);
        Circle fill = new Circle(r);
        fill.setFill(null);
        fill.setStroke(Theme.ACCENT);
        fill.setStrokeWidth(sw);
        fill.setStrokeLineCap(StrokeLineCap.ROUND);
        fill.getStrokeDashArray().setAll(len, circum - len);
        fill.setRotate(-90);

        ring.getChildren().setAll(track, fill, ringPct);
        ring.setPrefSize((r + sw / 2) * 2, (r + sw / 2) * 2);
        ring.setMaxSize((r + sw / 2) * 2, (r + sw / 2) * 2);
        ringPct.setText((int) (pct * 100) + "%");

        overviewInfo.getChildren().clear();
        Label big = new Label("已点亮 " + earned + " / " + total + " 枚徽章");
        big.getStyleClass().add("overview-title");
        int left = total - earned;
        Label sub = new Label(left > 0
                ? "继续加油，距离全部解锁还差 " + left + " 枚"
                : "太棒了，全部徽章已解锁！");
        sub.getStyleClass().add("overview-sub");
        overviewInfo.getChildren().addAll(big, sub);
    }

    /** 未解锁徽章的进度文案 */
    private String progressText(String name, int cur, int target) {
        switch (name) {
            case "毅力之星":
            case "坚持达人":   return "已连续打卡 " + cur + " / " + target + " 天";
            case "美食家":     return "已记录饮食 " + cur + " / " + target + " 天";
            case "运动健将":   return "已记录运动 " + cur + " / " + target + " 次";
            case "补水达人":   return "已记录饮水 " + cur + " / " + target + " 天";
            case "健康标兵":   return "健康评分 " + cur + " / " + target;
            case "蜕变之星":   return cur >= target ? "体脂已恢复正常" : "坚持锻炼降低体脂";
            case "目标达成者": return cur >= target ? "阶段目标已达成" : "完成阶段目标以解锁";
            default:          return "";
        }
    }

    /** 滚轮滚动徽章墙：固定步长、更快更跟手（不与主内容区抢占） */
    private void enableWheelScroll(ScrollPane sp) {
        sp.addEventFilter(ScrollEvent.SCROLL, e -> {
            if (e.getDeltaY() == 0) return;
            Node c = sp.getContent();
            double viewportH = sp.getViewportBounds().getHeight();
            double contentH = (c != null) ? c.getBoundsInLocal().getHeight() : 0;
            double scrollable = contentH - viewportH;
            if (scrollable > 0 && viewportH > 0) {
                double sign = Math.signum(e.getDeltaY());
                double step = 64.0; // 每格 64px，比主内容区更跟手
                double frac = sign * step / scrollable;
                sp.setVvalue(clamp01(sp.getVvalue() - frac));
                e.consume();
            }
        });
    }

    /** 鼠标拖动滚动徽章墙（按住拖动即可上下滑动） */
    private void enableDragToScroll(ScrollPane sp) {
        sp.setPannable(false); // 关闭原生拖拽平移，由下方自定义逻辑独占，避免与主内容区冲突
        final double[] last = {0, 0};
        sp.setCursor(Cursor.OPEN_HAND);
        sp.setOnMousePressed(e -> {
            last[0] = e.getX();
            last[1] = e.getY();
            sp.setCursor(Cursor.CLOSED_HAND);
            e.consume(); // 阻止事件冒泡到主内容区 ScrollPane
        });
        sp.setOnMouseReleased(e -> {
            sp.setCursor(Cursor.OPEN_HAND);
            e.consume();
        });
        sp.setOnMouseDragged(e -> {
            double dx = e.getX() - last[0];
            double dy = e.getY() - last[1];
            last[0] = e.getX();
            last[1] = e.getY();
            Node content = sp.getContent();
            if (content == null) return;
            double rangeX = content.getBoundsInLocal().getWidth() - sp.getViewportBounds().getWidth();
            double rangeY = content.getBoundsInLocal().getHeight() - sp.getViewportBounds().getHeight();
            if (rangeY > 0) sp.setVvalue(clamp01(sp.getVvalue() - DRAG_SPEED * dy / rangeY));
            if (rangeX > 0) sp.setHvalue(clamp01(sp.getHvalue() - DRAG_SPEED * dx / rangeX));
            e.consume();
        });
    }

    private static double clamp01(double v) {
        return Math.max(0, Math.min(1, v));
    }

    /** 新徽章解锁脉冲动效 */
    private void playUnlockPulse(VBox card) {
        ScaleTransition pulse = new ScaleTransition(Duration.millis(460), card);
        pulse.setFromX(0.55); pulse.setFromY(0.55);
        pulse.setToX(1.0);   pulse.setToY(1.0);
        pulse.setInterpolator(Interpolator.EASE_OUT);
        pulse.play();
    }

    /** 顶部 toast 提示新解锁徽章 */
    private void showToast(Set<String> names) {
        String msg;
        if (names.size() == 1) msg = "🎉 恭喜解锁「" + names.iterator().next() + "」徽章！";
        else msg = "🎉 恭喜解锁 " + names.size() + " 枚新徽章：" + String.join("、", names);

        toast.setText(msg);
        toast.setVisible(true);
        toast.setManaged(true);
        toast.setOpacity(0);
        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(toast.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(300), new KeyValue(toast.opacityProperty(), 1)),
                new KeyFrame(Duration.millis(2800), new KeyValue(toast.opacityProperty(), 1)),
                new KeyFrame(Duration.millis(3300), new KeyValue(toast.opacityProperty(), 0))
        );
        tl.setOnFinished(e -> { toast.setVisible(false); toast.setManaged(false); });
        tl.play();
    }
}
