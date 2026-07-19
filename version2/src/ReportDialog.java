import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

/**
 * 统一的「高级报告弹窗」。
 *
 * 解决原先各面板用裸 TextArea 直接铺满、文字界面单调的问题：
 *   - 顶部渐变标题带（report-hero），给文字界面一个视觉锚点
 *   - 正文使用 Text 节点（受控行距/留白/宽度），比 TextArea 更具阅读舒适度
 *   - 整体为白色圆角卡片 + 柔和投影（report-card）
 *
 * 用法：
 *   ReportDialog.showText("健康分析评估报告", reportContent);   // 纯文本报告
 *   ReportDialog.show("核心指标详情", someNode);                // 任意内容节点
 */
public class ReportDialog {

    /** 纯文本报告：按行解析渲染，【小节标题】加粗品牌色、ASCII 装饰线剔除，破除文本墙塑料感 */
    public static void showText(String title, String content) {
        VBox bodyWrap = new VBox(3);
        bodyWrap.getStyleClass().add("report-body");
        bodyWrap.setMaxWidth(Double.MAX_VALUE);

        String text = content == null ? "" : content;
        for (String line : text.split("\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                Region gap = new Region();
                gap.setPrefHeight(5);
                bodyWrap.getChildren().add(gap);
                continue;
            }
            // 剔除 ═══ ─── ==== 这类 ASCII 装饰线，改由 CSS 分隔
            if (trimmed.matches("^[═─=\\-_*~#]{3,}$")) continue;
            Text t = new Text(line);
            t.setWrappingWidth(720);
            if (trimmed.startsWith("【") && trimmed.endsWith("】")) {
                t.getStyleClass().add("report-section-title");
            } else {
                t.getStyleClass().add("report-text");
            }
            bodyWrap.getChildren().add(t);
        }

        ScrollPane sp = new ScrollPane(bodyWrap);
        sp.setFitToWidth(true);
        sp.setFitToHeight(false);
        sp.getStyleClass().add("report-scroll");

        show(title, sp);
    }

    /** 任意内容节点（如指标卡片网格） */
    public static void show(String title, Node content) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setResizable(true);

        VBox root = new VBox();
        root.getStyleClass().add("report-card");
        root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        Label hero = new Label(title);
        hero.getStyleClass().add("report-hero");

        Node inner = content;
        if (!(content instanceof ScrollPane)) {
            ScrollPane sp = new ScrollPane(content);
            sp.setFitToWidth(true);
            sp.setFitToHeight(false);
            sp.getStyleClass().add("report-scroll");
            inner = sp;
        }

        root.getChildren().addAll(hero, inner);
        VBox.setVgrow(inner, Priority.ALWAYS);

        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().setPrefSize(840, 620);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.show();
    }
}
