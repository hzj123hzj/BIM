import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.*;

public class LoginView {
    private BorderPane root = new BorderPane();

    public LoginView() {
        // 左侧品牌渐变区
        StackPane left = new StackPane();
        left.getStyleClass().add("bg-gradient");
        left.setMinWidth(420);
        Label brand = new Label("BMI 体质评估\n与预测系统");
        brand.setStyle("-fx-text-fill: white; -fx-font-size: 30px; -fx-font-weight: bold; -fx-alignment: center; -fx-line-spacing: 6px;");
        brand.setWrapText(true);
        Label sub = new Label("健康数据 · 智能分析 · 科学管理");
        sub.setStyle("-fx-text-fill: #D6ECF2; -fx-font-size: 14px;");
        VBox leftBox = new VBox(14, brand, sub);
        leftBox.setAlignment(Pos.CENTER);
        left.getChildren().add(leftBox);
        root.setLeft(left);

        // 右侧表单卡片
        VBox card = new VBox(14);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(22));
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(380);
        card.setMaxHeight(520);

        TabPane tabPane = new TabPane();
        Tab tabLogin = new Tab("登录", createLoginPanel());
        Tab tabReg = new Tab("注册", createRegisterPanel());
        tabLogin.setClosable(false);
        tabReg.setClosable(false);
        tabPane.getTabs().addAll(tabLogin, tabReg);
        card.getChildren().add(tabPane);

        root.setCenter(card);
        BorderPane.setAlignment(card, Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #EAF3F7, #D6E6EC);");
    }

    private GridPane createLoginPanel() {
        GridPane p = new GridPane();
        p.setHgap(10);
        p.setVgap(10);
        p.setPadding(new Insets(10, 4, 4, 4));
        TextField tfUser = new TextField();
        tfUser.setPromptText("用户名");
        PasswordField pfPass = new PasswordField();
        pfPass.setPromptText("密码");
        Button btn = new Button("登 录");
        btn.getStyleClass().add("button-primary");
        btn.setMaxWidth(Double.MAX_VALUE);
        Label hint = new Label("请输入账号密码登录");
        hint.getStyleClass().add("hint");

        p.add(hint, 0, 0, 2, 1);
        p.add(new Label("用户名:"), 0, 1);
        p.add(tfUser, 1, 1);
        p.add(new Label("密码:"), 0, 2);
        p.add(pfPass, 1, 2);
        p.add(btn, 0, 3, 2, 1);

        btn.setOnAction(e -> doLogin(tfUser.getText(), pfPass.getText()));
        pfPass.setOnAction(e -> doLogin(tfUser.getText(), pfPass.getText()));
        tfUser.setOnAction(e -> doLogin(tfUser.getText(), pfPass.getText()));
        return p;
    }

    private void doLogin(String u, String p) {
        u = u == null ? "" : u.trim();
        if (u.isEmpty() || p == null || p.isEmpty()) {
            alert(Alert.AlertType.WARNING, "提示", "请输入用户名和密码");
            return;
        }
        if (DBUtil.loginAdmin(u, p)) {
            App.currentUser = DBUtil.currentUsername;
            App.currentRole = "admin";
            App.showAdmin();
        } else if (DBUtil.loginUser(u, p)) {
            App.currentUser = DBUtil.currentUsername;
            App.currentRole = "user";
            App.showMain();
        } else {
            alert(Alert.AlertType.ERROR, "登录失败", "用户名或密码错误");
        }
    }

    private GridPane createRegisterPanel() {
        GridPane p = new GridPane();
        p.setHgap(10);
        p.setVgap(8);
        p.setPadding(new Insets(10, 4, 4, 4));
        TextField tfUser = new TextField();
        tfUser.setPromptText("用户名");
        PasswordField pf1 = new PasswordField();
        pf1.setPromptText("密码");
        PasswordField pf2 = new PasswordField();
        pf2.setPromptText("确认密码");
        ComboBox<String> cbGender = new ComboBox<>();
        cbGender.getItems().addAll("男", "女");
        cbGender.setValue("男");
        Spinner<Integer> spAge = new Spinner<>(5, 120, 25);
        TextField tfHeight = new TextField("170");
        TextField tfWeight = new TextField("60");
        TextField tfWaist = new TextField();
        tfWaist.setPromptText("选填");
        ComboBox<String> cbAct = new ComboBox<>();
        cbAct.getItems().addAll("久坐", "轻度活动", "中度活动", "重度活动", "极重度活动");
        cbAct.setValue("久坐");
        Button btn = new Button("注 册");
        btn.getStyleClass().add("button-accent");
        btn.setMaxWidth(Double.MAX_VALUE);

        int r = 0;
        p.add(new Label("用户名:"), 0, r);
        p.add(tfUser, 1, r++);
        p.add(new Label("密码:"), 0, r);
        p.add(pf1, 1, r++);
        p.add(new Label("确认密码:"), 0, r);
        p.add(pf2, 1, r++);
        p.add(new Label("性别:"), 0, r);
        p.add(cbGender, 1, r++);
        p.add(new Label("年龄:"), 0, r);
        p.add(spAge, 1, r++);
        p.add(new Label("身高(cm):"), 0, r);
        p.add(tfHeight, 1, r++);
        p.add(new Label("体重(kg):"), 0, r);
        p.add(tfWeight, 1, r++);
        p.add(new Label("腰围(cm):"), 0, r);
        p.add(tfWaist, 1, r++);
        p.add(new Label("活动等级:"), 0, r);
        p.add(cbAct, 1, r++);
        p.add(btn, 0, r, 2, 1);

        btn.setOnAction(e -> doRegister(tfUser.getText(), pf1.getText(), pf2.getText(),
                cbGender.getValue(), spAge.getValue(), tfHeight.getText(), tfWeight.getText(), tfWaist.getText(), cbAct.getValue()));
        return p;
    }

    private void doRegister(String user, String p1, String p2, String gender, int age, String h, String w, String wa, String act) {
        user = user == null ? "" : user.trim();
        if (user.isEmpty() || p1 == null || p1.isEmpty()) {
            alert(Alert.AlertType.WARNING, "提示", "用户名和密码不能为空");
            return;
        }
        if (!p1.equals(p2)) {
            alert(Alert.AlertType.WARNING, "提示", "两次密码不一致");
            return;
        }
        double height;
        try {
            height = Double.parseDouble(h.trim());
            if (height < 100 || height > 250) {
                alert(Alert.AlertType.WARNING, "提示", "身高范围 100-250cm");
                return;
            }
        } catch (Exception ex) {
            alert(Alert.AlertType.WARNING, "提示", "身高请输入数字");
            return;
        }
        double weight;
        try {
            weight = Double.parseDouble(w.trim());
            if (weight < 20 || weight > 300) {
                alert(Alert.AlertType.WARNING, "提示", "体重范围 20-300kg");
                return;
            }
        } catch (Exception ex) {
            alert(Alert.AlertType.WARNING, "提示", "体重请输入数字");
            return;
        }
        double waist = 0;
        if (wa != null && !wa.trim().isEmpty()) {
            try {
                waist = Double.parseDouble(wa.trim());
                if (waist < 30 || waist > 200) {
                    alert(Alert.AlertType.WARNING, "提示", "腰围范围 30-200cm");
                    return;
                }
            } catch (Exception ex) {
                alert(Alert.AlertType.WARNING, "提示", "腰围请输入数字");
                return;
            }
        }
        if (DBUtil.registerUser(user, p1, gender, age, height, weight, waist, act)) {
            alert(Alert.AlertType.INFORMATION, "成功", "注册成功! 请登录");
        } else {
            alert(Alert.AlertType.ERROR, "失败", "注册失败 (用户名可能已存在)");
        }
    }

    private void alert(Alert.AlertType t, String title, String msg) {
        Alert a = new Alert(t, msg, ButtonType.OK);
        a.setTitle(title);
        a.showAndWait();
    }

    public BorderPane getRoot() {
        return root;
    }
}
