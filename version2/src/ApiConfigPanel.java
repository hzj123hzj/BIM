import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;

import java.util.Map;

/**
 * AI API 配置面板（JavaFX 8 重写 version1 AISystemPanel API 配置部分）
 * apiKey / model / endpoint 表单，调用 getAIApiConfig / saveAIApiConfig。
 */
public class ApiConfigPanel extends VBox {
    private final TextField tfApiKey = new TextField();
    private final TextField tfModel = new TextField("glm-4.7-flash");
    private final TextField tfEndpoint = new TextField("https://open.bigmodel.cn/api/paas/v4");

    public ApiConfigPanel() {
        setSpacing(12);
        setPadding(new Insets(14));
        setStyle("-fx-background-color: #F0F6F9;");

        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        Label title = new Label("AI API 配置");
        title.getStyleClass().add("card-title");
        card.getChildren().add(title);

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.setPadding(new Insets(8, 4, 8, 4));
        for (TextField t : new TextField[]{tfApiKey, tfModel, tfEndpoint}) t.getStyleClass().add("text-field");
        tfApiKey.setPromptText("如 glm-4.7-flash 的 API Key");
        form.addRow(0, new Label("API Key:"), tfApiKey);
        form.addRow(1, new Label("模型名称:"), tfModel);
        form.addRow(2, new Label("接口地址:"), tfEndpoint);
        Label hint = new Label("配置智谱 GLM-4.7-Flash（OpenAI 兼容）API 信息（保存到数据库）");
        hint.getStyleClass().add("hint");
        form.add(hint, 0, 3, 2, 1);
        card.getChildren().add(form);

        Button btnSave = new Button("保存配置");
        btnSave.getStyleClass().add("button-primary");
        btnSave.setOnAction(e -> save());
        HBox btnBox = new HBox(btnSave);
        btnBox.setAlignment(Pos.CENTER);
        btnBox.setPadding(new Insets(6, 0, 0, 0));
        card.getChildren().add(btnBox);
        getChildren().add(card);

        loadConfig();
    }

    private void loadConfig() {
        Map<String, String> cfg = DBUtil.getAIApiConfig();
        if (cfg != null) {
            tfApiKey.setText(cfg.getOrDefault("api_key", ""));
            tfModel.setText(cfg.getOrDefault("model_name", "glm-4.7-flash"));
            tfEndpoint.setText(cfg.getOrDefault("endpoint_url", "https://open.bigmodel.cn/api/paas/v4"));
        }
    }

    private void save() {
        String apiKey = tfApiKey.getText().trim();
        String modelName = tfModel.getText().trim();
        String endpoint = tfEndpoint.getText().trim();
        if (modelName.isEmpty() || endpoint.isEmpty()) {
            warn("模型名称和接口地址不能为空");
            return;
        }
        if (DBUtil.saveAIApiConfig(apiKey, modelName, endpoint)) {
            DBUtil.logAction("ADMIN", DBUtil.currentUsername, "更新API配置", modelName);
            info("API 配置已保存到数据库\n\n后续可在 Prompt 模板或健康顾问模块中调用此配置生成 AI 建议。");
        } else {
            err("保存失败，请检查数据库连接");
        }
    }

    private void info(String m) {
        new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait();
    }

    private void warn(String m) {
        new Alert(Alert.AlertType.WARNING, m, ButtonType.OK).showAndWait();
    }

    private void err(String m) {
        new Alert(Alert.AlertType.ERROR, m, ButtonType.OK).showAndWait();
    }
}
