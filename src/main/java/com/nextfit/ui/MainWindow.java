package com.nextfit.ui;

import com.nextfit.model.User;
import com.nextfit.service.Session;
import com.nextfit.ui.components.Branding;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.util.List;

/**
 * Main window: side menu, top bar and the area where the selected module is
 * shown. It never knows the concrete modules; it only hosts every
 * {@link ModulePanel} it receives that the user's role allows.
 */
public class MainWindow extends JFrame {

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Sidebar sidebar;
    private final TopBar topBar;
    private final List<ModulePanel> modules;

    /**
     * Creates the window for the user of the current session.
     *
     * @param session    the session with the signed-in user
     * @param allModules every module of the application
     * @param onLogout   what to do after the session is closed (show the sign-in window)
     * @throws IllegalStateException when nobody is signed in
     */
    public MainWindow(Session session, List<ModulePanel> allModules, Runnable onLogout) {
        super("NextFit");
        User user = session.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("No hay una sesión iniciada"));
        this.modules = allModules.stream().filter(m -> m.allowedFor(user.getRole())).toList();

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.BACKGROUND);
        content.setBackground(Theme.BACKGROUND);

        sidebar = new Sidebar(modules, this::show);
        topBar = new TopBar(user, () -> {
            session.end();
            onLogout.run();
            dispose();
        });

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(Theme.BACKGROUND);
        center.add(topBar, BorderLayout.NORTH);
        center.add(content, BorderLayout.CENTER);

        for (ModulePanel module : modules) {
            content.add(module.view(), module.id());
        }
        add(sidebar, BorderLayout.WEST);
        add(center, BorderLayout.CENTER);

        setSize(1360, 840);
        setMinimumSize(new Dimension(1000, 640));
        setLocationRelativeTo(null);
        setIconImages(Branding.windowIcons());
        if (!modules.isEmpty()) {
            show(modules.get(0));
        }
    }

    private void show(ModulePanel module) {
        cards.show(content, module.id());
        sidebar.select(module);
        topBar.setTitle(module.title());
        module.onShow();
    }
}