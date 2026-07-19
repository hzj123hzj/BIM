import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;

import java.util.Map;

public class DataInputPanel extends VBox {
    private final TextField tfWeight = new TextField();
    private final TextField tfBodyFat = new TextField();
    private final TextField tfWater = new TextField();
    private final TextField tfMuscle = new TextField();
    private final TextField tfVisceral = new TextField();
    private final TextField tfBone = new TextField();
    private final TextField tfWaist = new TextField();
    private final Label lblLatest = new Label();

    public DataInputPanel() {
        setSpacing(12);
        setPadding(new Insets(14));
        setStyle("-fx-background-color: #F0F6F9;");

        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setMaxWidth(520);
        Label title = new Label("今日健康数据录入");
        title.getStyleClass().add("card-title");
        card.getChildren().add(title);

        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(10);
        g.setPadding(new Insets(6, 2, 2, 2));
        int r = 0;
        g.add(new Label("体重(kg):"), 0, r);
        g.add(tfWeight, 1, r++);
        g.add(new Label("体脂率(%):"), 0, r);
        g.add(tfBodyFat, 1, r++);
        g.add(new Label("水分率(%):"), 0, r);
        g.add(tfWater, 1, r++);
        g.add(new Label("肌肉率(%):"), 0, r);
        g.add(tfMuscle, 1, r++);
        g.add(new Label("内脏脂肪等级:"), 0, r);
        g.add(tfVisceral, 1, r++);
        g.add(new Label("骨肌量(kg):"), 0, r);
        g.add(tfBone, 1, r++);
        g.add(new Label("腰围(cm):"), 0, r);
        g.add(tfWaist, 1, r++);
        card.getChildren().add(g);

        HBox btns = new HBox(10);
        Button btnSave = new Button("保存记录");
        btnSave.getStyleClass().add("button-primary");
        Button btnScale = new Button("模拟称重");
        btnScale.getStyleClass().add("button-ghost");
        btns.getChildren().addAll(btnSave, btnScale);
        card.getChildren().add(btns);
        getChildren().add(card);

        VBox card2 = new VBox(8);
        card2.getStyleClass().add("card");
        card2.setMaxWidth(520);
        Label t2 = new Label("最近一次记录");
        t2.getStyleClass().add("card-title");
        lblLatest.getStyleClass().add("sub-title");
        card2.getChildren().addAll(t2, lblLatest);
        getChildren().add(card2);

        btnSave.setOnAction(e -> save());
        btnScale.setOnAction(e -> simulateScale());
        refreshLatest();
    }

    private void save() {
        try {
            double w = Double.parseDouble(tfWeight.getText().trim());
            double bf = Double.parseDouble(tfBodyFat.getText().trim());
            double wr = Double.parseDouble(tfWater.getText().trim());
            double mr = Double.parseDouble(tfMuscle.getText().trim());
            int vf = Integer.parseInt(tfVisceral.getText().trim());
            double bm = Double.parseDouble(tfBone.getText().trim());
            double wa = Double.parseDouble(tfWaist.getText().trim());
            if (DBUtil.saveHealthRecord(w, bf, wr, mr, vf, bm, wa)) {
                alert("保存成功");
                refreshLatest();
                clear();
            } else {
                alert("保存失败，请检查输入");
            }
        } catch (Exception ex) {
            alert("请输入正确的数值");
        }
    }

    private void simulateScale() {
        double base = 55 + Math.random() * 35;
        tfWeight.setText(String.format("%.1f", base));
        tfBodyFat.setText(String.format("%.1f", 15 + Math.random() * 15));
        tfWater.setText(String.format("%.1f", 50 + Math.random() * 10));
        tfMuscle.setText(String.format("%.1f", 30 + Math.random() * 15));
        tfVisceral.setText(String.valueOf(5 + (int) (Math.random() * 8)));
        tfBone.setText(String.format("%.1f", 2 + Math.random() * 2));
        tfWaist.setText(String.format("%.1f", 70 + Math.random() * 20));
    }

    private void refreshLatest() {
        Map<String, Object> rec = DBUtil.getLatestHealthRecord();
        if (rec == null || rec.isEmpty()) {
            lblLatest.setText("暂无记录");
            return;
        }
        lblLatest.setText("体重: " + rec.get("weight") + " kg   BMI: " + rec.get("bmi")
                + "   体脂率: " + rec.get("body_fat") + "%");
    }

    private void clear() {
        tfWeight.clear();
        tfBodyFat.clear();
        tfWater.clear();
        tfMuscle.clear();
        tfVisceral.clear();
        tfBone.clear();
        tfWaist.clear();
    }

    private void alert(String m) {
        new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait();
    }
}
