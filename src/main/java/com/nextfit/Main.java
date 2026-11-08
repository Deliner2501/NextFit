package com.nextfit;

import com.nextfit.ui.fx.BackupModule;
import com.nextfit.service.BackupService;
import com.nextfit.persistence.AccessKeyStore;
import com.nextfit.persistence.CsvAttendanceRepository;
import com.nextfit.persistence.CsvHealthProfileRepository;
import com.nextfit.persistence.CsvMeasurementRepository;
import com.nextfit.persistence.CsvMembershipPlanRepository;
import com.nextfit.persistence.CsvSubscriptionRepository;
import com.nextfit.persistence.CsvPaymentRepository;
import com.nextfit.service.AccessService;
import com.nextfit.service.AccessTokens;
import com.nextfit.service.AnalyticsService;
import com.nextfit.service.AttendanceService;
import com.nextfit.service.HealthService;
import com.nextfit.service.MembershipPlanService;
import com.nextfit.service.PhoneAccess;
import com.nextfit.service.SubscriptionService;
import com.nextfit.service.PaymentService;
import com.nextfit.service.ReportService;
import com.nextfit.service.DashboardService;
import com.nextfit.service.UserService;
import com.nextfit.service.suggest.BmiRule;
import com.nextfit.service.suggest.MedicalConditionRule;
import com.nextfit.service.suggest.SuggestionRule;
import com.nextfit.service.suggest.SuggestionService;
import com.nextfit.validation.HealthValidator;
import com.nextfit.validation.MembershipPlanValidator;
import com.nextfit.validation.PaymentValidator;
import com.nextfit.validation.UserValidator;
import java.time.Clock;
import com.nextfit.persistence.MemberRepository;
import com.nextfit.persistence.CsvUserRepository;
import com.nextfit.persistence.UserRepository;
import com.nextfit.service.AuthService;
import com.nextfit.service.CashCloseService;
import com.nextfit.service.Session;
import com.nextfit.model.Role;
import com.nextfit.model.Subscription;
import com.nextfit.ui.fx.AccessModule;
import com.nextfit.ui.fx.AnalyticsModule;
import com.nextfit.ui.fx.BillingModule;
import com.nextfit.ui.fx.DashboardModule;
import com.nextfit.ui.fx.FxApp;
import com.nextfit.ui.fx.FxIcons;
import com.nextfit.ui.fx.PendingModule;
import com.nextfit.ui.fx.ReportsModule;
import com.nextfit.ui.fx.TrainingFxModule;
import com.nextfit.ui.fx.UsersModule;
import com.nextfit.persistence.CsvMemberRepository;
import com.nextfit.service.MemberService;
import com.nextfit.validation.MemberValidator;
import com.nextfit.service.occupancy.OccupancyService;
import com.nextfit.ui.fx.OccupancyModule;
import com.nextfit.service.security.InMemoryAttemptLimiter;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import com.nextfit.web.AccessServer;
import com.nextfit.web.LanAddress;
import java.io.IOException;
import java.util.function.Supplier;

/**
 * Application entry point. Repositories, services and screens are wired here
 * and only here, so switching the CSV files for a database later means
 * changing the lines of this class that create the repositories.
 */
public final class Main {

    private static final Path DATA_DIR = Path.of("data");
    /** First port of the web server that answers the carnet scans of a phone. */
    private static final int ACCESS_PORT = 8765;

    private Main() {
    }

    /**
     * Starts the application with the JavaFX interface.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        // First run only: fills an empty installation with example data.
        DemoData.seedIfEmpty(DATA_DIR);

        BackupService backups = new BackupService(DATA_DIR, Path.of("backups"), Clock.systemDefaultZone());
        try {
            backups.backupIfNoneToday(14);
          } catch (RuntimeException e) {
           System.err.println("No se pudo hacer el respaldo automático: " + e.getMessage());
         }
        
        // Storage. To use a database, replace these repositories only.
        UserRepository users = new CsvUserRepository(DATA_DIR.resolve("users.csv"));

        // Services
        AuthService auth = new AuthService(users, new InMemoryAttemptLimiter());
        auth.ensureDefaultAdmin();
        Session session = new Session();
        UserService userService = new UserService(users, new UserValidator());
        
        // Members module
        MemberRepository memberRepository = new CsvMemberRepository(DATA_DIR.resolve("members.csv"));
        MemberService memberService = new MemberService(memberRepository, new MemberValidator());

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
        AccessService accessService = new AccessService(memberService, attendanceService,
                memberId -> subscriptionService.findCurrentFor(memberId).map(Subscription::getEndDate),
                Clock.systemDefaultZone());
        PhoneAccess phoneAccess = new PhoneAccess(memberService, accessService,
                new AccessTokens(AccessKeyStore.loadOrCreate(DATA_DIR.resolve("access.key"))),
                Clock.systemDefaultZone());
        CashCloseService cashCloseService = new CashCloseService(paymentService, memberService);
        ReportService reportService = new ReportService(cashCloseService, memberService, subscriptionService,
                attendanceService);
        AnalyticsService analyticsService = new AnalyticsService(attendanceService, subscriptionService,
                Clock.systemDefaultZone());
        DashboardService dashboardService = new DashboardService(
                subscriptionService, paymentService, attendanceService, Clock.systemDefaultZone());
        
        // Training module (exercises, routines, assignments and sessions)
        TrainingModule training = new TrainingModule(DATA_DIR, memberService, subscriptionService);
        // The suggestion service and its rules are wired here (composition root).
        SuggestionService suggestionService = new SuggestionService(training.exercises(), healthService,
                List.<SuggestionRule>of(new BmiRule(), new MedicalConditionRule()), Clock.systemDefaultZone());
        
                OccupancyService occupancyService = new OccupancyService(attendanceService, memberService,
                Clock.systemDefaultZone());

        // Phones that scan a carnet QR talk to this small server of the reception computer.
        AccessServer accessServer = new AccessServer(phoneAccess, ACCESS_PORT);
        try {
            accessServer.start();
        } catch (IOException e) {
            System.err.println("No se pudo abrir el servidor del QR: " + e.getMessage());
        }
        Supplier<String> phoneUrl = () -> "http://" + LanAddress.find() + ":" + accessServer.port();

        // The modules are built by a supplier because JavaFX controls can only be created after the toolkit starts.
        // Each owner replaces their PendingModule with the real screen.
        FxApp.run(auth, session, () -> List.of(
                new DashboardModule(dashboardService, session),
                new PendingModule("members", "Socios", FxIcons.MEMBERS,
                        Set.of(Role.ADMIN, Role.RECEPTION, Role.TRAINER)),
                new BillingModule(planService, subscriptionService, paymentService, memberService, session),
                new TrainingFxModule(training, suggestionService,
                        DATA_DIR.resolve("images").resolve("exercises")),
                new AccessModule(accessService, attendanceService, memberService, subscriptionService,
                        phoneAccess, phoneUrl),
                new AnalyticsModule(analyticsService, memberService),
                new OccupancyModule(occupancyService),
                new ReportsModule(cashCloseService, reportService),
                new UsersModule(userService, session),
                new BackupModule(backups)));
        accessServer.stop();
    }
}
