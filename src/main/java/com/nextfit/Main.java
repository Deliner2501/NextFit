package com.nextfit;


import com.nextfit.persistence.CsvAttendanceRepository;
import com.nextfit.persistence.CsvHealthProfileRepository;
import com.nextfit.persistence.CsvMeasurementRepository;
import com.nextfit.persistence.CsvMembershipPlanRepository;
import com.nextfit.persistence.CsvSubscriptionRepository;
import com.nextfit.persistence.CsvPaymentRepository;
import com.nextfit.service.AttendanceService;
import com.nextfit.service.HealthService;
import com.nextfit.service.MembershipPlanService;
import com.nextfit.service.SubscriptionService;
import com.nextfit.service.PaymentService;
import com.nextfit.service.DashboardService;
import com.nextfit.service.UserService;
import com.nextfit.validation.HealthValidator;
import com.nextfit.validation.MembershipPlanValidator;
import com.nextfit.validation.PaymentValidator;
import com.nextfit.validation.UserValidator;
import java.time.Clock;
import com.nextfit.persistence.MemberRepository;
import com.nextfit.ui.members.MembersPanel;
import com.nextfit.util.PhotoStorage;
import com.nextfit.persistence.CsvUserRepository;
import com.nextfit.persistence.UserRepository;
import com.nextfit.service.AuthService;
import com.nextfit.service.Session;
import com.nextfit.ui.LoginFrame;
import com.nextfit.ui.MainWindow;
import com.nextfit.ui.ModulePanel;
import com.nextfit.ui.Theme;
import com.nextfit.ui.billing.BillingPanel;
import com.nextfit.ui.dashboard.DashboardPanel;
import com.nextfit.ui.users.UsersPanel;
import com.nextfit.ui.BannerModule;
import com.nextfit.persistence.CsvMemberRepository;
import com.nextfit.service.MemberService;
import com.nextfit.ui.training.TrainingPanel;
import com.nextfit.validation.MemberValidator;

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

        // First run only: fills an empty installation with example data.
        DemoData.seedIfEmpty(DATA_DIR);
        
        // Storage. To use a database, replace these repositories only.
        UserRepository users = new CsvUserRepository(DATA_DIR.resolve("users.csv"));

        // Services
        AuthService auth = new AuthService(users);
        auth.ensureDefaultAdmin();
        Session session = new Session();
        UserService userService = new UserService(users, new UserValidator());
        
        // Members module
        MemberRepository memberRepository = new CsvMemberRepository(DATA_DIR.resolve("members.csv"));
        MemberService memberService = new MemberService(memberRepository, new MemberValidator());
        PhotoStorage photoStorage = new PhotoStorage(DATA_DIR.resolve("photos"));

        // Health, memberships and attendance (used by the member record)
        HealthService healthService = new HealthService(memberService,
                new CsvHealthProfileRepository(DATA_DIR.resolve("health_profiles.csv")),
                new CsvMeasurementRepository(DATA_DIR.resolve("measurements.csv")),
                new HealthValidator());
        MembershipPlanService planService = new MembershipPlanService(
                new CsvMembershipPlanRepository(DATA_DIR.resolve("plans.csv")),
                new MembershipPlanValidator());
        SubscriptionService subscriptionService = new SubscriptionService(
                new CsvSubscriptionRepository(DATA_DIR.resolve("subscriptions.csv")),
                planService, memberService, Clock.systemDefaultZone());
        PaymentService paymentService = new PaymentService(
                new CsvPaymentRepository(DATA_DIR.resolve("payments.csv")),
                subscriptionService, planService, new PaymentValidator(), Clock.systemDefaultZone());
        AttendanceService attendanceService = new AttendanceService(memberService,
                new CsvAttendanceRepository(DATA_DIR.resolve("attendance.csv")),
                subscriptionService);
        DashboardService dashboardService = new DashboardService(
                subscriptionService, paymentService, attendanceService, Clock.systemDefaultZone());
        
        // Training module (exercises, routines, assignments and sessions)
        TrainingModule training = new TrainingModule(DATA_DIR, memberService, subscriptionService);
        
        // Screens. Each owner replaces their placeholder with the real panel.
        List<ModulePanel> modules = List.of(
                new DashboardPanel(dashboardService, session),

                new BannerModule(new MembersPanel(memberService, photoStorage, healthService, attendanceService),
                        "Socios", "Registra, consulta y da seguimiento a cada socio",
                        "/images/banners/members.jpg", 0.30),

                new BannerModule(new BillingPanel(planService, subscriptionService, paymentService, memberService, session),
                        "Membresías y pagos", "Planes, ventas y comprobantes",
                        "/images/banners/billing.jpg", 0.30),

                new BannerModule(new TrainingPanel(training, DATA_DIR),
                        "Entrenamiento", "Ejercicios, rutinas y sesiones",
                        "/images/banners/training.jpg", 0.55),

                new UsersPanel(userService, session));

        SwingUtilities.invokeLater(() -> showLogin(auth, session, modules));
    }

    private static void showLogin(AuthService auth, Session session, List<ModulePanel> modules) {
        new LoginFrame(auth, session, () -> showMain(auth, session, modules)).setVisible(true);
    }

    private static void showMain(AuthService auth, Session session, List<ModulePanel> modules) {
        new MainWindow(session, modules, () -> showLogin(auth, session, modules)).setVisible(true);
    }
}
