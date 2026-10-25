package com.nextfit;

import com.nextfit.model.Role;
import com.nextfit.persistence.CsvMemberRepository;
import com.nextfit.persistence.MemberRepository;
import com.nextfit.service.MemberService;
import com.nextfit.ui.members.MembersPanel;
import com.nextfit.util.PhotoStorage;
import com.nextfit.validation.MemberValidator;
import com.nextfit.persistence.CsvUserRepository;
import com.nextfit.persistence.UserRepository;
import com.nextfit.service.AuthService;
import com.nextfit.service.Session;
import com.nextfit.ui.LoginFrame;
import com.nextfit.ui.MainWindow;
import com.nextfit.ui.ModulePanel;
import com.nextfit.ui.PlaceholderPanel;
import com.nextfit.ui.Theme;

import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.util.List;

/**
 * Application entry point. Repositories, services and screens are wired here
 * and only here, so switching the CSV files for a database later means
 * changing the lines of this class that create the repositories.
 */
public final class Main {

    private static final Path DATA_DIR = Path.of("data");

    private Main() {
    }

    /**
     * Starts the application.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        Theme.install();

        // Storage. To use a database, replace these repositories only.
        UserRepository users = new CsvUserRepository(DATA_DIR.resolve("users.csv"));

        // Services
        AuthService auth = new AuthService(users);
        auth.ensureDefaultAdmin();
        Session session = new Session();

        // Members module
        MemberRepository memberRepository = new CsvMemberRepository(DATA_DIR.resolve("members.csv"));
        MemberService memberService = new MemberService(memberRepository, new MemberValidator());
        PhotoStorage photoStorage = new PhotoStorage(DATA_DIR.resolve("photos"));

        // Screens. Each owner replaces their placeholder with the real panel.
        List<ModulePanel> modules = List.of(
                new PlaceholderPanel("dashboard", "Panel", "dashboard",
                        "Indicadores y gráficas del gimnasio."),

                new MembersPanel(memberService, photoStorage),

                new PlaceholderPanel("billing", "Membresías y pagos", "payments",
                        "Planes, suscripciones y pagos.", Role.ADMIN, Role.RECEPTION),

                new PlaceholderPanel("training", "Entrenamiento", "training",
                        "Ejercicios, rutinas y sesiones.", Role.ADMIN, Role.TRAINER),

                new PlaceholderPanel("users", "Usuarios", "shield",
                        "Cuentas y roles del sistema.", Role.ADMIN));

        SwingUtilities.invokeLater(() -> showLogin(auth, session, modules));
    }

    private static void showLogin(AuthService auth, Session session, List<ModulePanel> modules) {
        new LoginFrame(auth, session, () -> showMain(auth, session, modules)).setVisible(true);
    }

    private static void showMain(AuthService auth, Session session, List<ModulePanel> modules) {
        new MainWindow(session, modules, () -> showLogin(auth, session, modules)).setVisible(true);
    }
}