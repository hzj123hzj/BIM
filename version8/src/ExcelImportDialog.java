import javafx.stage.FileChooser;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.*;
import javafx.stage.Window;
import javafx.event.ActionEvent;

import java.io.File;
import java.util.List;

/**
 * 通用 Excel 导入对话框：选择 .xlsx 文件，可勾选「首行作为表头」自动跳过首行。
 * 解析成功后返回 List<String[]>（每行一个字符串数组）；取消或解析失败返回 null。
 */
public class ExcelImportDialog {

    public static List<String[]> show(String title, String hint) {
        Alert dlg = new Alert(Alert.AlertType.CONFIRMATION, "", ButtonType.OK, ButtonType.CANCEL);
        dlg.setTitle(title);
        dlg.setHeaderText("导入 Excel 表格（.xlsx）");

        VBox root = new VBox(10);
        Label hintLbl = new Label(hint);
        hintLbl.setWrapText(true);
        hintLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        TextField tfPath = new TextField();
        tfPath.setEditable(false);
        tfPath.setPromptText("未选择文件");
        Button btnChoose = new Button("选择文件...");
        btnChoose.getStyleClass().add("button-primary");

        final File[] selected = {null};
        btnChoose.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("选择 Excel 文件");
            fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Excel 工作簿 (*.xlsx)", "*.xlsx"));
            Window w = btnChoose.getScene() != null ? btnChoose.getScene().getWindow() : null;
            File f = fc.showOpenDialog(w);
            if (f != null) {
                selected[0] = f;
                tfPath.setText(f.getAbsolutePath());
            }
        });

        HBox fileRow = new HBox(8, tfPath, btnChoose);
        HBox.setHgrow(tfPath, Priority.ALWAYS);

        CheckBox cbHeader = new CheckBox("首行是标题/说明（导入时跳过首行）");
        cbHeader.setSelected(false);

        Label status = new Label();
        status.setStyle("-fx-text-fill: #c0392b;");

        root.getChildren().addAll(hintLbl, fileRow, cbHeader, status);
        dlg.getDialogPane().setContent(root);
        dlg.getDialogPane().setPrefWidth(540);

        final List<String[]>[] result = new List[1];
        Button okBtn = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(ActionEvent.ACTION, ev -> {
            if (selected[0] == null) {
                status.setText("请先选择 Excel 文件");
                ev.consume();
                return;
            }
            if (!selected[0].getName().toLowerCase().endsWith(".xlsx")) {
                status.setText("仅支持 .xlsx 格式（旧版 .xls 不支持）");
                ev.consume();
                return;
            }
            try {
                List<String[]> rows = ExcelReader.readFirstSheet(selected[0]);
                if (cbHeader.isSelected() && !rows.isEmpty()) {
                    rows = rows.subList(1, rows.size());
                }
                if (rows.isEmpty()) {
                    status.setText("文件中没有可导入的数据");
                    ev.consume();
                    return;
                }
                result[0] = rows;
            } catch (Exception ex) {
                status.setText("读取失败：" + ex.getMessage());
                ev.consume();
            }
        });

        dlg.showAndWait();
        return result[0];
    }
}
