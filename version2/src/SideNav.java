import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;

import java.util.List;

/**
 * 左侧导航栏（自建，替代 JavaFX TabPane）。
 *
 * 为什么不用 TabPane + setSide(Side.LEFT)：
 *   JavaFX 8 在 Side.LEFT 时会把整个 Tab 头区域旋转 90°，
 *   tab-label 在屏幕上变成竖排，且窄边栏下 4~5 个汉字会被换行/截断，
 *   靠 setTabMinHeight 与 CSS 均无法根治。
 * 这里用普通 Button 列表实现侧边导航，文字天然横向渲染，无旋转缺陷。
 *
 * 内容区使用 ScrollPane：当面板内容高于可视区时自动出现滚动条，
 * 避免 StackPane 直接裁剪导致"无法下滑查看"的问题。
 *
 * 用法：
 *   SideNav nav = new SideNav("标题", tabs);   // tabs 为 List<Tab>
 *   borderPane.setLeft(nav.getSidebar());
 *   borderPane.setCenter(nav.getContent());
 */
public class SideNav {
    private final VBox sidebar = new VBox();
    private final ScrollPane content = new ScrollPane();
    private Button activeBtn;
    private final List<Tab> tabs;

    public SideNav(String brandTitle, List<Tab> tabs) {
        this.tabs = tabs;

        // 内容区：可滚动，宽度贴合视口，高度跟随内容（超出滚动）
        content.setFitToWidth(true);
        content.setFitToHeight(false);
        content.setPannable(false); // 关闭拖拽平移，恢复标准鼠标滚轮滚动（避免灵敏度忽快忽慢）
        content.setPadding(new Insets(10));
        content.setStyle("-fx-background-color: #F0F6F9; -fx-border-width: 0;");
        content.getStyleClass().add("content-scroll");
        // 标准化鼠标滚轮滚动：每次固定步长，避免灵敏度随鼠标硬件 delta 跳变
        content.addEventFilter(ScrollEvent.SCROLL, e -> {
            if (e.getDeltaY() != 0) {
                double viewportH = content.getViewportBounds().getHeight();
                Node c = content.getContent();
                double contentH = (c != null) ? c.getBoundsInLocal().getHeight() : 0;
                double scrollable = contentH - viewportH;
                if (scrollable > 0 && viewportH > 0) {
                    double sign = Math.signum(e.getDeltaY());
                    double step = 50.0; // 每次滚轮滚动 50px，舒适一致
                    double frac = sign * step / scrollable;
                    double newV = content.getVvalue() - frac;
                    content.setVvalue(Math.max(content.getVmin(), Math.min(content.getVmax(), newV)));
                    e.consume();
                }
            }
        });

        sidebar.getStyleClass().add("sidebar");

        Label brand = new Label(brandTitle);
        brand.getStyleClass().add("sidebar-brand");

        VBox nav = new VBox(4);
        nav.getStyleClass().add("sidebar-nav");

        ScrollPane scroll = new ScrollPane(nav);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.getStyleClass().add("sidebar-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        for (Tab tab : tabs) {
            Button btn = new Button(tab.getText());
            btn.getStyleClass().add("sidebar-btn");
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setAlignment(Pos.CENTER_LEFT);
            btn.setOnAction(e -> select(btn, tab));
            nav.getChildren().add(btn);
        }

        sidebar.getChildren().addAll(brand, scroll);

        if (!tabs.isEmpty()) {
            select((Button) nav.getChildren().get(0), tabs.get(0));
        }
    }

    private void select(Button btn, Tab tab) {
        if (activeBtn != null) {
            activeBtn.getStyleClass().remove("sidebar-btn-active");
        }
        btn.getStyleClass().add("sidebar-btn-active");
        activeBtn = btn;

        Node panel = tab.getContent();
        content.setContent(panel);
    }

    public VBox getSidebar() {
        return sidebar;
    }

    public ScrollPane getContent() {
        return content;
    }
}
