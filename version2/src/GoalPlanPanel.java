import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.ZoneId;

/** 目标计划面板 — 目标设定、分阶段进度、计划详情、运动记录 */
public class GoalPlanPanel extends VBox {
    private final ComboBox<String> cbGoalType = new ComboBox<>();
    private final TextField tfTargetWeight = new TextField("60.0");
    private final ProgressBar[] progressBars = new ProgressBar[4];
    private final TableView<String[]> exerciseTable = new TableView<>();
    private final ObservableList<String[]> exerciseData = FXCollections.observableArrayList();
    private String planContent = "";

    private final Label lblCurrent = new Label("--");
    private final Label lblTarget = new Label("--");
    private final Label lblDiff = new Label("--");
    private final Label lblDays = new Label("--");
    private final Label lblPredict = new Label("--");

    public GoalPlanPanel() {
        setSpacing(16);
        setPadding(new Insets(18));
        setStyle("-fx-background-color: transparent;");

        cbGoalType.getItems().addAll("减脂", "减重", "增肌", "保持健康");
        cbGoalType.setValue("减脂");
        tfTargetWeight.setPrefWidth(90);

        HBox ctrl = new HBox(10);
        ctrl.setAlignment(Pos.CENTER_LEFT);
        Label lblG = new Label("目标类型:");
        Label lblW = new Label("目标体重(kg):");
        Button btnRecommend = new Button("推荐目标");
        btnRecommend.getStyleClass().add("button-accent");
        Button btnSet = new Button("设置目标");
        btnSet.getStyleClass().add("button-primary");
        Button btnRefresh = new Button("刷新");
        btnRefresh.getStyleClass().add("button-ghost");
        Button btnReport = new Button("查看目标计划");
        btnReport.getStyleClass().add("button-primary");
        btnReport.setOnAction(e -> showReportDialog("目标计划详情", planContent));
        ctrl.getChildren().addAll(lblG, cbGoalType, lblW, tfTargetWeight, btnRecommend, btnSet, btnRefresh, btnReport);

        VBox ctrlCard = new VBox(10);
        ctrlCard.getStyleClass().add("card");
        Label t1 = new Label("目标设定");
        t1.getStyleClass().add("card-title");
        ctrlCard.getChildren().addAll(t1, ctrl);

        GridPane progressGrid = new GridPane();
        progressGrid.setHgap(10);
        progressGrid.setVgap(8);
        for (int i = 0; i < 4; i++) {
            Label lbl = new Label("第" + (i + 1) + "周:");
            progressBars[i] = new ProgressBar(0);
            progressBars[i].setPrefWidth(400);
            progressBars[i].setStyle("-fx-accent: " + Theme.hex(Theme.PRIMARY) + ";");
            progressGrid.add(lbl, 0, i);
            progressGrid.add(progressBars[i], 1, i);
        }
        VBox progressCard = new VBox(10);
        progressCard.getStyleClass().add("card");
        Label t2 = new Label("分阶段进度");
        t2.getStyleClass().add("card-title");
        progressCard.getChildren().addAll(t2, progressGrid);

        VBox overviewCard = new VBox(10);
        overviewCard.getStyleClass().add("card");
        HBox overviewHeader = new HBox(10);
        overviewHeader.setAlignment(Pos.CENTER_LEFT);
        Label t3 = new Label("目标概览");
        t3.getStyleClass().add("card-title");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button btnExercises = new Button("查看运动记录");
        btnExercises.getStyleClass().add("button-accent");
        btnExercises.setOnAction(e -> showExerciseDialog());
        overviewHeader.getChildren().addAll(t3, sp, btnExercises);
        GridPane overviewGrid = new GridPane();
        overviewGrid.setHgap(24);
        overviewGrid.setVgap(14);
        overviewGrid.setPadding(new Insets(4));
        overviewGrid.addRow(0, goalBox("当前体重", lblCurrent, Theme.hex(Theme.PRIMARY)),
                                 goalBox("目标体重", lblTarget, Theme.hex(Theme.ACCENT)));
        overviewGrid.addRow(1, goalBox("距离目标", lblDiff, Theme.hex(Theme.DANGER)),
                                 goalBox("已进行天数", lblDays, Theme.hex(Theme.SUCCESS)));
        overviewGrid.addRow(2, goalBox("预测达成", lblPredict, "#8050A0"), new Label(""));
        overviewCard.getChildren().addAll(overviewHeader, overviewGrid);

        buildExerciseTable();

        getChildren().addAll(ctrlCard, progressCard, overviewCard);

        btnRecommend.setOnAction(e -> recommendTarget());
        btnSet.setOnAction(e -> setGoal());
        btnRefresh.setOnAction(e -> refresh());
        refresh();
    }

    private VBox goalBox(String title, Label valueLabel, String color) {
        VBox vb = new VBox(4);
        Label lt = new Label(title);
        lt.getStyleClass().add("sub-title");
        valueLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        vb.getChildren().addAll(lt, valueLabel);
        return vb;
    }

    private void buildExerciseTable() {
        String[] cols = {"日期", "运动类型", "时长", "强度", "消耗"};
        for (int i = 0; i < cols.length; i++) {
            final int idx = i;
            TableColumn<String[], String> c = new TableColumn<>(cols[i]);
            c.setCellValueFactory(cb -> new ReadOnlyStringWrapper(cb.getValue()[idx]));
            exerciseTable.getColumns().add(c);
        }
        exerciseTable.setItems(exerciseData);
    }

    private void refreshExercise() {
        exerciseData.setAll(DBUtil.getExerciseRecordsByUser(DBUtil.currentUsername, 50));
    }

    private void recommendTarget() {
        double ideal = HealthCalculator.calcIdealWeight(DBUtil.currentHeight);
        tfTargetWeight.setText(f1(ideal));
        alert("推荐目标体重: " + f1(ideal) + " kg (身高²×22)");
    }

    private void setGoal() {
        try {
            String goalType = cbGoalType.getValue();
            double target = Double.parseDouble(tfTargetWeight.getText().trim());
            if (target < 20 || target > 300) {
                alert("目标体重范围 20-300kg");
                return;
            }
            if (DBUtil.saveGoal(goalType, target)) {
                alert("目标设置成功!");
                refresh();
            } else {
                alert("目标设置失败");
            }
        } catch (NumberFormatException e) {
            alert("请输入有效数字");
        }
    }

    private void refresh() {
        Map<String, Object> latest = DBUtil.getLatestHealthRecord();
        Map<String, Object> goal = DBUtil.getGoal();

        if (latest == null) {
            planContent = "暂无健康记录, 请先录入数据";
            lblCurrent.setText("--");
            lblTarget.setText("--");
            lblDiff.setText("--");
            lblDays.setText("--");
            lblPredict.setText("--");
            for (ProgressBar pb : progressBars) pb.setProgress(0);
            return;
        }

        double currentWeight = num(latest, "weight");
        lblCurrent.setText(f1(currentWeight) + " kg");

        if (goal == null) {
            planContent = "尚未设置目标, 请选择目标类型和目标体重后点击「设置目标」\n\n推荐目标体重: " +
                    f1(HealthCalculator.calcIdealWeight(DBUtil.currentHeight)) + " kg";
            lblTarget.setText("未设置");
            lblDiff.setText("--");
            lblDays.setText("--");
            lblPredict.setText("请先设置目标");
            for (ProgressBar pb : progressBars) pb.setProgress(0);
            return;
        }

        String goalType = str(goal, "goal_type");
        double targetWeight = num(goal, "target_value");
        java.sql.Date startDateRaw = (java.sql.Date) goal.get("start_date");
        LocalDate startDate = startDateRaw != null ? startDateRaw.toLocalDate() : LocalDate.now();
        LocalDate today = LocalDate.now();
        long daysSinceStart = ChronoUnit.DAYS.between(startDate, today);
        int actualStage = (int) Math.min(4, Math.max(1, daysSinceStart / 7 + 1));
        DBUtil.updateGoalStage(actualStage);

        lblTarget.setText(f1(targetWeight) + " kg");
        lblDiff.setText(f1(Math.abs(currentWeight - targetWeight)) + " kg");
        lblDays.setText(daysSinceStart + " 天");

        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════\n");
        sb.append("              目标计划\n");
        sb.append("═══════════════════════════════════════════\n\n");
        sb.append("目标类型: ").append(goalType).append("\n");
        sb.append("当前体重: ").append(f1(currentWeight)).append(" kg\n");
        sb.append("目标体重: ").append(f1(targetWeight)).append(" kg\n");
        sb.append("需变化: ").append(f1(Math.abs(currentWeight - targetWeight))).append(" kg\n");
        sb.append("目标开始日期: ").append(startDate).append(" (已进行 ").append(daysSinceStart).append(" 天)\n\n");

        sb.append("【分阶段计划 (4周递进)】\n");
        double totalChange = targetWeight - currentWeight;
        double[] stageTargets = new double[4];
        double[] ratios = {0.2, 0.25, 0.275, 0.275};
        double cumRatio = 0;
        int[] weeklyTargets = getWeeklyTargets(goalType);
        for (int i = 0; i < 4; i++) {
            cumRatio += ratios[i];
            stageTargets[i] = currentWeight + totalChange * cumRatio;
            sb.append("  第").append(i + 1).append("周: ").append(f1(i == 0 ? currentWeight : stageTargets[i - 1]));
            sb.append(" → ").append(f1(stageTargets[i])).append(" kg\n");

            double progress;
            if (i + 1 < actualStage) {
                progress = 1.0;
            } else if (i + 1 > actualStage) {
                progress = 0.0;
            } else {
                LocalDate stageStart = startDate.plusWeeks(i);
                LocalDate stageEnd = startDate.plusWeeks(i + 1);
                Date s = Date.from(stageStart.atStartOfDay(ZoneId.systemDefault()).toInstant());
                Date e = Date.from(stageEnd.atStartOfDay(ZoneId.systemDefault()).toInstant());
                int[] stats = DBUtil.getExerciseStatsBetween(s, e);
                int targetTimes = weeklyTargets[0];
                int targetMinutes = weeklyTargets[1];
                double pTimes = targetTimes > 0 ? (double) stats[0] / targetTimes : 0;
                double pMinutes = targetMinutes > 0 ? (double) stats[1] / targetMinutes : 0;
                progress = Math.min(1.0, Math.max(pTimes, pMinutes));
                sb.append("    本周运动: ").append(stats[0]).append(" 次, ").append(stats[1]).append(" 分钟\n");
                sb.append("    阶段目标: ").append(targetTimes).append(" 次 / ").append(targetMinutes).append(" 分钟\n");
            }
            final int fi = i;
            progressBars[fi].setProgress(progress);
        }
        sb.append("\n");

        sb.append("【运动建议】\n");
        switch (goalType) {
            case "减脂":
                sb.append("  有氧运动: 跑步/游泳/跳绳/骑行\n  频率: 每周 4-5 次, 每次 40-60 分钟\n  力量训练: 每周 2 次, 每次 30 分钟\n  建议: 有氧为主燃烧脂肪, 力量训练维持肌肉量\n");
                break;
            case "减重":
                sb.append("  有氧运动: 快走/游泳/骑行\n  频率: 每周 5 次, 每次 45-60 分钟\n  力量训练: 每周 2 次\n  建议: 持续中低强度有氧, 配合饮食控制\n");
                break;
            case "增肌":
                sb.append("  力量训练: 深蹲/卧推/硬拉/划船\n  频率: 每周 4 次, 每次 60 分钟\n  有氧运动: 每周 2 次, 每次 20 分钟\n  建议: 大重量少次数, 蛋白质摄入 1.5-2g/kg体重\n");
                break;
            default:
                sb.append("  有氧 + 力量均衡\n  频率: 每周 3-4 次, 每次 30-45 分钟\n  建议: 保持规律运动, 注意拉伸和恢复\n");
        }
        sb.append("\n");

        sb.append("【预测达成日期】\n");
        double tdee = num(latest, "tdee");
        int exerciseCal = DBUtil.getTodayExerciseCalories();
        int[] diet = DBUtil.getTodayDietSummary();
        double dailyDeficit = tdee + exerciseCal - diet[0];
        int days = HealthCalculator.predictGoalDays(currentWeight, targetWeight, dailyDeficit, goalType);
        if (days == -2) {
            sb.append("  ⚠️ 当前进度过慢, 建议调整计划\n");
            lblPredict.setText("进度过慢，建议调整");
        } else if (days == -1) {
            sb.append("  ⚠️ 当前饮食超过消耗, 无法达成目标\n");
            lblPredict.setText("当前无法达成");
        } else if (days == 0) {
            sb.append("  ✅ 已达成目标!\n");
            lblPredict.setText("已达成目标");
        } else {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, days);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            sb.append("  预测达成日期: ").append(sdf.format(cal.getTime())).append(" (约 ").append(days).append(" 天)\n");
            sb.append("  每日热量差: ").append(f0(dailyDeficit)).append(" kcal\n");
            lblPredict.setText("约 " + days + " 天后 (" + sdf.format(cal.getTime()) + ")");
        }
        sb.append("\n═══════════════════════════════════════════\n");

        planContent = sb.toString();
    }

    private void showReportDialog(String title, String content) {
        ReportDialog.showText(title, content);
    }

    private void showExerciseDialog() {
        refreshExercise();
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("运动记录");
        dialog.setHeaderText(null);
        dialog.setResizable(true);
        VBox box = new VBox(10, exerciseTable);
        VBox.setVgrow(exerciseTable, Priority.ALWAYS);
        box.setPrefSize(800, 600);
        box.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(box);
        dialog.getDialogPane().setPrefSize(800, 600);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.show();
    }

    private int[] getWeeklyTargets(String goalType) {
        switch (goalType) {
            case "减脂": return new int[]{4, 180};
            case "减重": return new int[]{5, 225};
            case "增肌": return new int[]{4, 240};
            default: return new int[]{3, 120};
        }
    }

    private double num(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v instanceof Number ? ((Number) v).doubleValue() : 0.0;
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? "-" : v.toString();
    }

    private static String f0(double v) { return String.format("%.0f", v); }
    private static String f1(double v) { return String.format("%.1f", v); }

    private void alert(String m) {
        new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait();
    }
}
