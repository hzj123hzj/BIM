import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
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
 * 用法：
 *   SideNav nav = new SideNav("标题", tabs);   // tabs 为 List<Tab>
 *   borderPane.setLeft(nav.getSidebar());
 *   borderPane.setCenter(nav.getContent());
 */
public class SideNav {
    private final VBox sidebar = new VBox();
    private final StackPane content = new StackPane();
    private Button activeBtn;
    private final List<Tab> tabs;

    public SideNav(String brandTitle, List<Tab> tabs) {
        this.tabs = tabs;
        content.setPadding(new Insets(10));
        content.setStyle("-fx-background-color: #F0F6F9;");

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
        if (panel instanceof Region) {
            ((Region) panel).setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        }
        StackPane.setAlignment(panel, Pos.TOP_LEFT);
        content.getChildren().setAll(panel);
    }

    public VBox getSidebar() {
        return sidebar;
    }

    public StackPane getContent() {
        return content;
    }
}
