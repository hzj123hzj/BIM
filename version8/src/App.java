import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
    public static String currentUser = "";
    public static String currentRole = "user"; // "user" | "admin"
    private static Stage stage;

    @Override
    public void start(Stage s) {
        stage = s;
        s.setTitle("BMI 体质评估与预测系统 (JavaFX)");
        s.setMinWidth(1040);
        s.setMinHeight(700);
        showLogin();
        s.show();
    }

    public static void showLogin() {
        LoginView v = new LoginView();
        Scene sc = new Scene(v.getRoot(), 1100, 720);
        Theme.styleScene(sc);
        stage.setScene(sc);
    }

    public static void showMain() {
        MainView v = new MainView();
        Scene sc = new Scene(v.getRoot(), 1100, 720);
        Theme.styleScene(sc);
        stage.setScene(sc);
    }

    public static void showAdmin() {
        AdminView v = new AdminView();
        Scene sc = new Scene(v.getRoot(), 1200, 760);
        Theme.styleScene(sc);
        stage.setScene(sc);
    }

    public static void main(String[] args) { launch(args); }
}
