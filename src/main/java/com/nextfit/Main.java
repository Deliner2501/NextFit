package com.nextfit;

import com.nextfit.ui.fx.BackupModule;
import com.nextfit.ui.fx.AuditModule;
import com.nextfit.service.BackupService;
import com.nextfit.persistence.AccessKeyStore;
import com.nextfit.persistence.Storage;
import com.nextfit.persistence.MemberCredentialRepository;
import com.nextfit.persistence.FaceTemplateRepository;
import com.nextfit.service.AccessService;
import com.nextfit.service.MemberPasswordService;
import com.nextfit.service.SelfAccessService;
import com.nextfit.service.face.FaceAccessService;
import com.nextfit.service.face.FaceEnrollmentService;
import com.nextfit.service.face.MemberLookup;
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
import com.nextfit.service.alert.AlertMessages;
import com.nextfit.service.alert.AlertService;
import com.nextfit.service.alert.WhatsAppLink;
import com.nextfit.service.gamification.GamificationService;
import com.nextfit.service.suggest.BmiRule;
import com.nextfit.service.suggest.MedicalConditionRule;
import com.nextfit.service.suggest.RoutineDraftChecker;
import com.nextfit.service.suggest.SuggestionRule;
import com.nextfit.service.suggest.SuggestionService;
import com.nextfit.validation.HealthValidator;
import com.nextfit.validation.MembershipPlanValidator;
import com.nextfit.validation.PaymentValidator;
import com.nextfit.validation.UserValidator;
import java.time.Clock;
import com.nextfit.persistence.MemberRepository;
import com.nextfit.persistence.UserRepository;
import com.nextfit.service.AuthService;
import com.nextfit.service.CashCloseService;
import com.nextfit.service.Session;
import com.nextfit.model.Subscription;
import com.nextfit.model.User;
import com.nextfit.ui.fx.AccessModule;
import com.nextfit.ui.fx.AnalyticsModule;
import com.nextfit.ui.fx.BillingModule;
import com.nextfit.ui.fx.DashboardModule;
import com.nextfit.ui.fx.FxApp;
import com.nextfit.ui.fx.AlertsModule;
import com.nextfit.ui.fx.MembersModule;
import com.nextfit.ui.fx.ReportsModule;
import com.nextfit.ui.fx.TrainingFxModule;
import com.nextfit.ui.fx.UsersModule;
import com.nextfit.service.MemberService;
import com.nextfit.validation.MemberValidator;
import com.nextfit.util.PhotoStorage;
import com.nextfit.service.occupancy.OccupancyForecastService;
import com.nextfit.service.occupancy.OccupancyService;
import com.nextfit.ui.fx.OccupancyModule;
import com.nextfit.service.security.InMemoryAttemptLimiter;
import com.nextfit.service.audit.AuditService;
import com.nextfit.service.churn.ChurnService;
import com.nextfit.ui.fx.ChurnModule;
import java.nio.file.Path;
import java.util.List;
import com.nextfit.service.portal.BestTimesAdvisor;
import com.nextfit.service.portal.MemberPortal;
import com.nextfit.service.portal.ProgressReportService;
import com.nextfit.web.AccessServer;
import com.nextfit.web.LanAddress;
import com.nextfit.web.PortalChatEndpoint;
import java.io.IOException;
import java.util.function.Supplier;
import com.nextfit.ai.AiAssistant;
import com.nextfit.ai.AiRoutineService;
import com.nextfit.ai.AiSetup;
import com.nextfit.ai.AiTool;
import com.nextfit.ai.AiUser;
import com.nextfit.ai.AssistantChat;
import com.nextfit.ai.PortalChatService;
import com.nextfit.ai.tools.BestHoursTool;
import com.nextfit.ai.tools.GymAlertsTool;
import com.nextfit.ai.tools.MemberSummaryLookup;
import com.nextfit.ai.tools.MembersAtRiskTool;
import com.nextfit.ai.tools.MyMembershipTool;
import com.nextfit.ai.tools.MyNextWorkoutTool;
import com.nextfit.ai.tools.RevenueSummaryTool;
import com.nextfit.exceptions.AiException;
import com.nextfit.persistence.jdbc.DatabaseConfig;
import com.nextfit.ui.fx.AssistantModule;
import java.util.Optional;

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

        // Storage: Oracle when nextfit.properties says db.enabled=true and the database answers; CSV files otherwise.
        Storage storage = StorageSetup.create(DATA_DIR, Path.of(DatabaseConfig.FILE_NAME));
        System.out.println("Almacenamiento: " + storage.description());

        // Repositories come from the storage chosen at the start (CSV files or Oracle).
        UserRepository users = storage.users();

        // Services
        AuditService auditService = new AuditService(
                storage.audit(), Clock.systemDefaultZone());
        AuthService auth = new AuthService(users, new InMemoryAttemptLimiter(), auditService);
        auth.ensureDefaultAdmin();
        Session session = new Session();
        // Who is acting now, for the audit log ("" before anybody signs in).
        Supplier<String> actor = () -> session.getCurrentUser().map(User::getUsername).orElse("");
        UserService userService = new UserService(users, new UserValidator(), auditService, actor);

        BackupService backups = new BackupService(DATA_DIR, Path.of("backups"), Clock.systemDefaultZone(),
                auditService, actor);
        try {
            backups.backupIfNoneToday(14);
        } catch (RuntimeException e) {
            System.err.println("No se pudo hacer el respaldo automático: " + e.getMessage());
        }

        // Members module
        MemberRepository memberRepository = storage.members();
        MemberService memberService = new MemberService(memberRepository, new MemberValidator());

        // Health, memberships and attendance (used by the member record)
        HealthService healthService = new HealthService(memberService,
                storage.healthProfiles(),
                storage.measurements(),
                new HealthValidator());
        MembershipPlanService planService = new MembershipPlanService(
                storage.plans(),
                new MembershipPlanValidator());
        SubscriptionService subscriptionService = new SubscriptionService(
                storage.subscriptions(),
                planService, memberService, Clock.systemDefaultZone());
        PaymentService paymentService = new PaymentService(
                storage.payments(),
                subscriptionService, planService, new PaymentValidator(), Clock.systemDefaultZone());
        AttendanceService attendanceService = new AttendanceService(memberService,
                storage.attendance(),
                subscriptionService);
        AccessService accessService = new AccessService(memberService, attendanceService,
                memberId -> subscriptionService.findCurrentFor(memberId).map(Subscription::getEndDate),
                Clock.systemDefaultZone());
        PhoneAccess phoneAccess = new PhoneAccess(memberService,
                new AccessTokens(AccessKeyStore.loadOrCreate(DATA_DIR.resolve("access.key"))));
        // Face recognition (only a numeric code of the face is stored; see docs/face-recognition.md)
        FaceTemplateRepository faceTemplates = storage.faceTemplates();
        FaceEnrollmentService faceEnrollment = new FaceEnrollmentService(faceTemplates, auditService);
        FaceAccessService faceAccess = new FaceAccessService(faceTemplates, MemberLookup.of(memberService),
                accessService::processMember, accessService::deny, Clock.systemDefaultZone());
        // Members can also enter with the document and a password they create themselves
        MemberCredentialRepository memberCredentials =
                storage.memberCredentials();
        InMemoryAttemptLimiter memberLimiter = new InMemoryAttemptLimiter();
        MemberPasswordService memberPasswords = new MemberPasswordService(memberCredentials, memberLimiter, auditService);
        SelfAccessService selfAccess = new SelfAccessService(memberService::findByDocumentNumber, memberPasswords,
                memberLimiter, auditService, accessService::processMember, accessService::deny);
        // Faces of members who left are deleted automatically.
        faceEnrollment.purgeInactive(memberService.findAll());
        CashCloseService cashCloseService = new CashCloseService(paymentService, memberService);
        ReportService reportService = new ReportService(cashCloseService, memberService, subscriptionService,
                attendanceService);
        AnalyticsService analyticsService = new AnalyticsService(attendanceService, subscriptionService,
                Clock.systemDefaultZone());
        DashboardService dashboardService = new DashboardService(
                subscriptionService, paymentService, attendanceService, Clock.systemDefaultZone());

        // Training module (exercises, routines, assignments and sessions)
        TrainingModule training = new TrainingModule(storage, memberService, subscriptionService);
        GamificationService gamificationService = new GamificationService(memberService, attendanceService,
                Clock.systemDefaultZone());
        AlertService alertService = AlertService.withStandardRules(subscriptionService, attendanceService,
                memberService, Clock.systemDefaultZone());
        ChurnService churnService = ChurnService.withStandardRules(attendanceService, subscriptionService,
                memberService, Clock.systemDefaultZone());

        // The suggestion service and its rules are wired here (composition root).
        SuggestionService suggestionService = new SuggestionService(training.exercises(), healthService,
                List.<SuggestionRule>of(new BmiRule(), new MedicalConditionRule()), Clock.systemDefaultZone());

        OccupancyService occupancyService = new OccupancyService(attendanceService, memberService,
                Clock.systemDefaultZone());

        // Phones that scan a carnet QR talk to this small server of the reception computer.
        BestTimesAdvisor bestTimesAdvisor = new BestTimesAdvisor(
                new OccupancyForecastService(attendanceService, Clock.systemDefaultZone()),
                Clock.systemDefaultZone());
        MemberPortal memberPortal = new MemberPortal(phoneAccess, subscriptionService, planService,
                training.assignments(), training.routines(), training.exercises(), training.sessions(),
                gamificationService, bestTimesAdvisor);

        // The AI assistant. Without an ai.apiKey line in nextfit.properties it stays off and NextFit works as always.
        // EXPLICACIÓN: es UN solo asistente con las herramientas del personal y las del socio; cada quien solo ve
        // las de su rol (AiAudience), y el socio nunca puede pedir datos de otro porque sus herramientas no
        // reciben argumentos: el socio sale del código de su carnet.
        MemberSummaryLookup memberSummaries = id -> Optional.of(memberPortal.summaryOf(memberService.findById(id)));
        List<AiTool> aiTools = List.of(
                new MembersAtRiskTool(churnService),
                new RevenueSummaryTool(paymentService, Clock.systemDefaultZone()),
                new GymAlertsTool(alertService),
                new MyMembershipTool(memberSummaries),
                new MyNextWorkoutTool(memberSummaries),
                new BestHoursTool(memberSummaries));
        Optional<AiAssistant> assistant = AiSetup
                .create(Path.of(DatabaseConfig.FILE_NAME), aiTools, Clock.systemDefaultZone(), auditService);
        Optional<AssistantChat> assistantChat = assistant
                .map(ai -> new AssistantChat(ai, () -> session.getCurrentUser()
                        .map(user -> AiUser.staff(user.getRole(), user.getUsername()))
                        .orElseThrow(() -> new AiException("No hay una sesión iniciada"))));
        // EXPLICACIÓN: las rutinas con IA usan el mismo asistente (mismo límite diario); sin clave no existen y el botón no aparece
        Optional<AiRoutineService> aiRoutines = assistant
                .map(ai -> new AiRoutineService(ai, suggestionService, training.exercises(),
                        new RoutineDraftChecker(Clock.systemDefaultZone())));
        PortalChatEndpoint portalChat = assistant
                .map(ai -> new PortalChatService(ai, phoneAccess::findMember, Clock.systemDefaultZone()))
                .map(service -> new PortalChatEndpoint(service::ask))
                .orElse(null);

        AccessServer accessServer = new AccessServer(phoneAccess, memberPortal, portalChat,
                new ProgressReportService(training.assignments(), training.routines(), training.sessions(),
                        attendanceService, healthService), ACCESS_PORT);
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
                new MembersModule(memberService, healthService, attendanceService, gamificationService,
                        new PhotoStorage(DATA_DIR.resolve("photos")), session),
                new AlertsModule(alertService, new AlertMessages("NextFit"), new WhatsAppLink()),
                new ChurnModule(churnService),
                new AssistantModule(assistantChat),
                new BillingModule(planService, subscriptionService, paymentService, memberService, session),
                new TrainingFxModule(training, suggestionService,
                        DATA_DIR.resolve("images").resolve("exercises"), aiRoutines),
                new AccessModule(attendanceService, memberService, subscriptionService,
                        phoneAccess, phoneUrl, faceAccess, faceEnrollment, DATA_DIR, actor,
                        selfAccess, memberPasswords),
                new AnalyticsModule(analyticsService, memberService),
                new OccupancyModule(occupancyService),
                new ReportsModule(cashCloseService, reportService),
                new UsersModule(userService, session),
                new AuditModule(auditService),
                new BackupModule(backups)));
        accessServer.stop();
    }
}