package com.nextfit.ui.fx;

import com.nextfit.model.Role;
import com.nextfit.service.CashCloseService;
import com.nextfit.service.ReportService;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;

/**
 * Reports module in JavaFX: the daily cash close and the exportable reports. It is for the administrator
 * and the reception, who handle the money.
 */
public final class ReportsModule implements FxModule {

    private final TabPane tabs = new TabPane();
    private final CashClosePane cashClose;
    private final ReportsPane reports;

    /**
     * @param cashCloseService the service that builds the close of a day
     * @param reportService    the service that builds the report tables
     */
    public ReportsModule(CashCloseService cashCloseService, ReportService reportService) {
        this.cashClose = new CashClosePane(cashCloseService, reportService);
        this.reports = new ReportsPane(reportService);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("nf-tabs");
        tabs.getTabs().addAll(tab("Cierre de caja", scrolling(cashClose)), tab("Reportes", reports));
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, old, now) -> refresh());
    }

    @Override
    public String id() {
        return "reports";
    }

    @Override
    public String title() {
        return "Reportes";
    }

    @Override
    public String icon() {
        return FxIcons.REPORTS;
    }

    @Override
    public Node view() {
        return tabs;
    }

    @Override
    public void onShow() {
        refresh();
    }

    /** Only the administrator and the reception can open this module. */
    @Override
    public boolean allowedFor(Role role) {
        return role == Role.ADMIN || role == Role.RECEPTION;
    }

    private void refresh() {
        cashClose.refresh();
        reports.refresh();
    }

    /**
     * The close of the day is taller than a short window, so it scrolls instead of being cut at the bottom.
     * On a tall window it still fills the whole height.
     */
    private static ScrollPane scrolling(Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.getStyleClass().add("nf-scroll");
        return scroll;
    }

    private static Tab tab(String title, Node content) {
        StackPane holder = new StackPane(content);
        holder.setPadding(new Insets(18, 0, 0, 0));
        Tab tab = new Tab(title, holder);
        tab.setClosable(false);
        return tab;
    }
}
