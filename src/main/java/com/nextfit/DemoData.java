package com.nextfit;

import com.nextfit.exceptions.NextFitException;
import com.nextfit.model.AssignmentStatus;
import com.nextfit.model.Difficulty;
import com.nextfit.model.Discount;
import com.nextfit.model.Exercise;
import com.nextfit.model.FixedDiscount;
import com.nextfit.model.HealthProfile;
import com.nextfit.model.Member;
import com.nextfit.model.Measurement;
import com.nextfit.model.MembershipPlan;
import com.nextfit.model.MuscleGroup;
import com.nextfit.model.NoDiscount;
import com.nextfit.model.PaymentMethod;
import com.nextfit.model.PercentageDiscount;
import com.nextfit.model.Role;
import com.nextfit.model.Routine;
import com.nextfit.model.RoutineAssignment;
import com.nextfit.model.RoutineDay;
import com.nextfit.model.RoutineExercise;
import com.nextfit.model.Trainer;
import com.nextfit.model.TrainingSession;
import com.nextfit.persistence.CsvAssignmentRepository;
import com.nextfit.persistence.CsvAttendanceRepository;
import com.nextfit.persistence.CsvExerciseRepository;
import com.nextfit.persistence.CsvTrainerRepository;
import com.nextfit.persistence.CsvHealthProfileRepository;
import com.nextfit.persistence.CsvMeasurementRepository;
import com.nextfit.persistence.CsvMemberRepository;
import com.nextfit.persistence.CsvMembershipPlanRepository;
import com.nextfit.persistence.CsvPaymentRepository;
import com.nextfit.persistence.CsvSubscriptionRepository;
import com.nextfit.persistence.CsvUserRepository;
import com.nextfit.persistence.AssignmentRepository;
import com.nextfit.persistence.AttendanceRepository;
import com.nextfit.persistence.PaymentRepository;
import com.nextfit.persistence.SubscriptionRepository;
import com.nextfit.persistence.UserRepository;
import com.nextfit.service.AttendanceService;
import com.nextfit.service.AuthService;
import com.nextfit.service.HealthService;
import com.nextfit.service.MemberService;
import com.nextfit.service.MembershipPlanService;
import com.nextfit.service.PaymentService;
import com.nextfit.service.SubscriptionService;
import com.nextfit.service.UserService;
import com.nextfit.validation.HealthValidator;
import com.nextfit.validation.MemberValidator;
import com.nextfit.validation.MembershipPlanValidator;
import com.nextfit.validation.PaymentValidator;
import com.nextfit.validation.UserValidator;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Fills an empty installation with example data so the application can be shown
 * working: members, plans, memberships sold over the last months, attendance,
 * health records, exercises, routines and training sessions.
 *
 * <p>Everything goes through the same services the screens use, so the example
 * data obeys the same rules as real data. The past is reproduced by giving each
 * service a clock fixed on the day of the event, which keeps the rules (for
 * example "a membership must be valid to enter") true on that day. Nothing is
 * created when the members file already has members.
 */
public final class DemoData {

    /** Sign-in name of the example reception user. */
    public static final String RECEPTION_USER = "recepcion";
    /** Password of the example reception user. */
    public static final String RECEPTION_PASSWORD = "recepcion123";
    /** Sign-in name of the example trainer user. */
    public static final String TRAINER_USER = "entrenador";
    /** Password of the example trainer user. */
    public static final String TRAINER_PASSWORD = "entrenador123";

    private static final int HISTORY_DAYS = 105;
    private static final int ATTENDANCE_DAYS = 60;
    private static final double[] WEEKDAY_CHANCE = {0.62, 0.52, 0.58, 0.48, 0.52, 0.36, 0.14};
    private static final String[] NAMES = {
        "Valentina Rojas", "Santiago Mejía", "Camila Torres", "Andrés Pardo", "Laura Ospino",
        "Juan Pablo Cuello", "Mariana Daza", "Felipe Maestre", "Daniela Pinto", "Sebastián Fuentes",
        "Isabela Cabrera", "Mateo Arzuaga", "Sofía Barros", "Nicolás Orozco", "Paula Villazón",
        "Esteban Molina"};
    private static final String[] BLOOD = {"O+", "A+", "B+", "O-", "A-", "AB+", "O+", "A+"};

    /** One sale of the example history. */
    private record Sale(int member, int daysAgo, String plan, PaymentMethod method, Discount discount) {
    }

    /** The services of one moment of the history, all sharing the same fixed clock. */
    private record Moment(SubscriptionService subscriptions, PaymentService payments, AttendanceService attendance) {
    }

    private final ZoneId zone = ZoneId.systemDefault();
    private final Random random = new Random(2026);
    private final LocalDate today = LocalDate.now();
    private final Path dir;
    private final MemberService members;
    private final MembershipPlanService plans;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final AttendanceRepository attendanceRepository;
    private final AssignmentRepository assignmentRepository;
    private final HealthService health;
    private final List<Member> people = new ArrayList<>();
    private final Map<String, MembershipPlan> planByKey = new java.util.HashMap<>();

    private DemoData(Path dir) {
        this.dir = dir;
        this.members = new MemberService(new CsvMemberRepository(dir.resolve("members.csv")), new MemberValidator());
        this.plans = new MembershipPlanService(new CsvMembershipPlanRepository(dir.resolve("plans.csv")),
                new MembershipPlanValidator());
        this.subscriptionRepository = new CsvSubscriptionRepository(dir.resolve("subscriptions.csv"));
        this.paymentRepository = new CsvPaymentRepository(dir.resolve("payments.csv"));
        this.attendanceRepository = new CsvAttendanceRepository(dir.resolve("attendance.csv"));
        this.assignmentRepository = new CsvAssignmentRepository(dir.resolve("assignments.csv"));
        this.health = new HealthService(members, new CsvHealthProfileRepository(dir.resolve("health_profiles.csv")),
                new CsvMeasurementRepository(dir.resolve("measurements.csv")), new HealthValidator());
    }

    /**
     * Creates the example data when the installation has no members yet. Call it
     * before the application builds its own repositories.
     *
     * @param dataDir the folder with the CSV files
     * @return true when the example data was created, false when there was already data
     */
    public static boolean seedIfEmpty(Path dataDir) {
        DemoData demo = new DemoData(dataDir);
        if (!demo.isEmptyInstallation()) {
            return false;
        }
        try {
            demo.seedUsers();
            demo.seedPlans();
            List<Sale> sales = demo.planSales();
            demo.registerMembers(sales);
            demo.runHistory(sales);
            demo.seedHealth();
            demo.seedTraining();
            demo.finish();
            return true;
        } catch (NextFitException e) {
            throw new IllegalStateException("No se pudieron crear los datos de ejemplo: " + e.getMessage(), e);
        }
    }

    // The example data is only created when nothing of it exists yet, so real data is never mixed with it.
    private boolean isEmptyInstallation() {
        return members.findAll().isEmpty()
                && plans.findAll().isEmpty()
                && new CsvTrainerRepository(dir.resolve("trainers.csv")).findAll().isEmpty()
                && new CsvExerciseRepository(dir.resolve("exercises.csv")).findAll().isEmpty();
    }

    // ---- users, plans and members -------------------------------------------------------------

    private void seedUsers() {
        UserRepository users = new CsvUserRepository(dir.resolve("users.csv"));
        new AuthService(users).ensureDefaultAdmin();
        UserService service = new UserService(users, new UserValidator());
        createIfMissing(users, service, RECEPTION_USER, "Laura Gómez (Recepción)", RECEPTION_PASSWORD, Role.RECEPTION);
        createIfMissing(users, service, TRAINER_USER, "Carlos Ríos (Entrenador)", TRAINER_PASSWORD, Role.TRAINER);
    }

    private void createIfMissing(UserRepository users, UserService service, String username, String fullName,
                                 String password, Role role) {
        if (users.findByUsername(username).isEmpty()) {
            service.create(username, fullName, password, role);
        }
    }

    private void seedPlans() {
        addPlan("DAY", "Pase diario", "Entrada por un día a todas las zonas", 1, 10_000);
        addPlan("MONTH", "Mensual", "Acceso total durante 30 días", 30, 80_000);
        addPlan("QUARTER", "Trimestral", "Acceso total durante 3 meses, con ahorro", 90, 210_000);
        addPlan("SEMESTER", "Semestral", "Acceso total durante 6 meses", 180, 380_000);
        MembershipPlan launch = plans.create(new MembershipPlan(0, "Promo lanzamiento",
                "Oferta de apertura, ya no está disponible", 30, 60_000, true));
        plans.deactivate(launch.getId());
    }

    private void addPlan(String key, String name, String description, int days, long price) {
        planByKey.put(key, plans.create(new MembershipPlan(0, name, description, days, price, true)));
    }

    /** Defines who buys what and when. Newer sales have a smaller "daysAgo". */
    private List<Sale> planSales() {
        List<Sale> sales = new ArrayList<>();
        PaymentMethod[] methods = PaymentMethod.values();
        int counter = 0;
        // 0-5: regular monthly members who renew every month.
        for (int m = 0; m <= 5; m++) {
            for (int ago : new int[]{105 - m * 2, 75 - m * 2, 45 - m * 2, 15 - m * 2}) {
                counter++;
                sales.add(new Sale(m, Math.max(ago, 1), "MONTH", methods[counter % methods.length], discountFor(counter)));
            }
        }
        // 6-7: quarterly, the membership ends in a few days.
        sales.add(new Sale(6, 86, "QUARTER", PaymentMethod.TRANSFER, new PercentageDiscount(10)));
        sales.add(new Sale(7, 85, "QUARTER", PaymentMethod.CARD, new NoDiscount()));
        // 8-9: monthly, they end in about four days.
        sales.add(new Sale(8, 26, "MONTH", PaymentMethod.CASH, new NoDiscount()));
        sales.add(new Sale(9, 27, "MONTH", PaymentMethod.CARD, new FixedDiscount(5_000)));
        // 10-11: memberships that already ended.
        sales.add(new Sale(10, 70, "MONTH", PaymentMethod.CASH, new NoDiscount()));
        sales.add(new Sale(10, 40, "MONTH", PaymentMethod.CASH, new NoDiscount()));
        sales.add(new Sale(11, 62, "MONTH", PaymentMethod.TRANSFER, new NoDiscount()));
        // 12: a renewal bought today that starts when the current one ends.
        sales.add(new Sale(12, 20, "MONTH", PaymentMethod.CARD, new NoDiscount()));
        sales.add(new Sale(12, 0, "MONTH", PaymentMethod.CARD, new NoDiscount()));
        // 13: semester.
        sales.add(new Sale(13, 100, "SEMESTER", PaymentMethod.TRANSFER, new PercentageDiscount(15)));
        // 14: daily passes.
        sales.add(new Sale(14, 3, "DAY", PaymentMethod.CASH, new NoDiscount()));
        sales.add(new Sale(14, 0, "DAY", PaymentMethod.CASH, new NoDiscount()));
        // 15: left the gym (deactivated at the end).
        sales.add(new Sale(15, 50, "MONTH", PaymentMethod.CARD, new NoDiscount()));
        sales.sort((a, b) -> Integer.compare(b.daysAgo(), a.daysAgo()));
        return sales;
    }

    private Discount discountFor(int counter) {
        if (counter % 7 == 0) {
            return new FixedDiscount(5_000);
        }
        return counter % 5 == 0 ? new PercentageDiscount(10) : new NoDiscount();
    }

    private void registerMembers(List<Sale> sales) {
        for (int i = 0; i < NAMES.length; i++) {
            int firstSale = sales.stream().filter(s -> s.member() == people.size()).mapToInt(Sale::daysAgo).max().orElse(10);
            LocalDate joined = today.minusDays(firstSale + random.nextInt(3));
            String name = NAMES[i];
            String slug = name.toLowerCase().replace(" ", ".").replaceAll("[áä]", "a").replaceAll("[éë]", "e")
                    .replaceAll("[íï]", "i").replaceAll("[óö]", "o").replaceAll("[úü]", "u");
            Member saved = members.register(new Member(0, name, String.valueOf(1_065_400_000L + i * 7_919L),
                    "3" + (100_000_000 + i * 3_700_113), slug + "@correo.com",
                    LocalDate.of(1988 + (i * 3) % 16, 1 + (i * 5) % 12, 1 + (i * 7) % 27), joined, "",
                    "Familiar de " + name.split(" ")[0] + " · 3" + (200_000_000 + i * 1_511_777), true));
            people.add(saved);
        }
    }

    // ---- history of sales and attendance -------------------------------------------------------

    private Moment at(LocalDateTime when) {
        Clock clock = Clock.fixed(when.atZone(zone).toInstant(), zone);
        SubscriptionService subscriptions = new SubscriptionService(subscriptionRepository, plans, members, clock);
        return new Moment(subscriptions,
                new PaymentService(paymentRepository, subscriptions, plans, new PaymentValidator(), clock),
                new AttendanceService(members, attendanceRepository, subscriptions, clock));
    }

    private void runHistory(List<Sale> sales) {
        for (int ago = HISTORY_DAYS; ago >= 0; ago--) {
            LocalDate day = today.minusDays(ago);
            for (Sale sale : sales) {
                if (sale.daysAgo() == ago) {
                    LocalDateTime when = ago == 0 ? LocalDateTime.now() : day.atTime(10, random.nextInt(50));
                    at(when).payments().sellMembership(people.get(sale.member()).getId(),
                            planByKey.get(sale.plan()).getId(), null, sale.method(), sale.discount(), RECEPTION_USER);
                }
            }
            if (ago >= 1 && ago <= ATTENDANCE_DAYS) {
                registerAttendance(day);
            }
        }
        letSomeoneBeInside();
    }

    private void registerAttendance(LocalDate day) {
        double chance = WEEKDAY_CHANCE[day.getDayOfWeek().getValue() - 1];
        for (Member member : people) {
            if (random.nextDouble() > chance) {
                continue;
            }
            LocalDateTime enters = day.atTime(LocalTime.of(6 + random.nextInt(14), random.nextInt(60)));
            try {
                at(enters).attendance().checkIn(member.getId());
                at(enters.plusMinutes(45 + random.nextInt(70))).attendance().checkOut(member.getId());
            } catch (NextFitException e) {
                // The member has no valid membership that day: nobody entered, which is the expected result.
            }
        }
    }

    private void letSomeoneBeInside() {
        AttendanceService now = at(LocalDateTime.now()).attendance();
        int inside = 0;
        for (int i = 0; i < 6 && inside < 3; i++) {
            try {
                now.checkIn(people.get(i).getId());
                inside++;
            } catch (NextFitException e) {
                // That member cannot enter today; the next one is tried.
            }
        }
    }

    // ---- health ------------------------------------------------------------------------------

    private void seedHealth() {
        int[] daysAgo = {100, 75, 50, 25, 3};
        for (int i = 0; i < 12; i++) {
            Member member = people.get(i);
            double height = 155 + random.nextInt(35);
            health.saveProfile(new HealthProfile(member.getId(), height, BLOOD[i % BLOOD.length],
                    i % 5 == 0 ? "Asma leve" : "", i % 7 == 0 ? "Penicilina" : ""));
            double weight = 58 + random.nextInt(32);
            double trend = i % 3 == 0 ? 0.9 : i % 3 == 1 ? -1.4 : -0.3;
            for (int k = 0; k < daysAgo.length; k++) {
                LocalDate date = today.minusDays(daysAgo[k]);
                if (date.isBefore(member.getJoinDate())) {
                    continue;
                }
                double w = Math.round((weight + trend * k) * 10) / 10.0;
                health.addMeasurement(new Measurement(0, member.getId(), date, w,
                        Math.round((w * 1.1 + 12 - k * 0.8) * 10) / 10.0, Math.round((w * 1.2 + 24) * 10) / 10.0,
                        Math.round((w * 1.15 + 22) * 10) / 10.0));
            }
        }
    }

    // ---- training ----------------------------------------------------------------------------

    private void seedTraining() {
        SubscriptionService realSubscriptions = at(LocalDateTime.now()).subscriptions();
        TrainingModule training = new TrainingModule(dir, members, realSubscriptions);

        Trainer carlos = training.trainers().register(new Trainer(0, "Carlos Ríos", "1065111222", "3004567890",
                "carlos.rios@nextfit.com", LocalDate.of(1990, 3, 14), "Hipertrofia y fuerza", "", true));
        Trainer ana = training.trainers().register(new Trainer(0, "Ana Lucía Ferrer", "1065333444", "3015556677",
                "ana.ferrer@nextfit.com", LocalDate.of(1993, 8, 2), "Pérdida de grasa", "", true));
        training.trainers().register(new Trainer(0, "Julián Camargo", "1065555666", "3126667788",
                "julian.camargo@nextfit.com", LocalDate.of(1988, 11, 21), "Acondicionamiento", "", true));

        Map<String, Exercise> ex = new java.util.LinkedHashMap<>();
        exercise(training, ex, "Press de banca", "Acostado en banco plano, baja la barra al pecho y empuja hacia arriba.", MuscleGroup.CHEST, Difficulty.INTERMEDIATE);
        exercise(training, ex, "Flexiones", "Cuerpo recto, baja el pecho al suelo y sube controlando.", MuscleGroup.CHEST, Difficulty.BEGINNER);
        exercise(training, ex, "Aperturas con mancuernas", "En banco plano, abre los brazos con codos suaves y cierra sobre el pecho.", MuscleGroup.CHEST, Difficulty.INTERMEDIATE);
        exercise(training, ex, "Dominadas", "Cuélgate de la barra y sube hasta pasar la barbilla.", MuscleGroup.BACK, Difficulty.ADVANCED);
        exercise(training, ex, "Remo con barra", "Torso inclinado, lleva la barra al abdomen sin balancearte.", MuscleGroup.BACK, Difficulty.INTERMEDIATE);
        exercise(training, ex, "Jalón al pecho", "En polea alta, baja la barra hasta la clavícula.", MuscleGroup.BACK, Difficulty.BEGINNER);
        exercise(training, ex, "Press militar", "De pie, empuja la barra sobre la cabeza sin arquear la espalda.", MuscleGroup.SHOULDERS, Difficulty.INTERMEDIATE);
        exercise(training, ex, "Elevaciones laterales", "Sube las mancuernas hasta la altura de los hombros.", MuscleGroup.SHOULDERS, Difficulty.BEGINNER);
        exercise(training, ex, "Curl de bíceps", "Sube la barra con los codos pegados al cuerpo.", MuscleGroup.ARMS, Difficulty.BEGINNER);
        exercise(training, ex, "Fondos en paralelas", "Baja el cuerpo flexionando los codos y empuja para subir.", MuscleGroup.ARMS, Difficulty.ADVANCED);
        exercise(training, ex, "Extensión de tríceps en polea", "Empuja la cuerda hacia abajo hasta extender los codos.", MuscleGroup.ARMS, Difficulty.BEGINNER);
        exercise(training, ex, "Plancha", "Apoya antebrazos y puntas de los pies, mantén el cuerpo recto.", MuscleGroup.CORE, Difficulty.BEGINNER);
        exercise(training, ex, "Abdominales en polea", "De rodillas, flexiona el tronco llevando los codos a los muslos.", MuscleGroup.CORE, Difficulty.INTERMEDIATE);
        exercise(training, ex, "Sentadilla", "Baja con la espalda recta hasta que los muslos queden paralelos al suelo.", MuscleGroup.LEGS, Difficulty.INTERMEDIATE);
        exercise(training, ex, "Prensa de piernas", "Empuja la plataforma sin bloquear las rodillas.", MuscleGroup.LEGS, Difficulty.BEGINNER);
        exercise(training, ex, "Zancadas", "Da un paso largo y baja la rodilla trasera casi al suelo.", MuscleGroup.LEGS, Difficulty.INTERMEDIATE);
        exercise(training, ex, "Hip thrust", "Con la espalda en un banco, sube la cadera apretando los glúteos.", MuscleGroup.GLUTES, Difficulty.INTERMEDIATE);
        exercise(training, ex, "Puente de glúteos", "Acostado, sube la cadera y mantén un segundo arriba.", MuscleGroup.GLUTES, Difficulty.BEGINNER);
        exercise(training, ex, "Peso muerto", "Levanta la barra del suelo con la espalda neutra hasta quedar de pie.", MuscleGroup.FULL_BODY, Difficulty.ADVANCED);
        exercise(training, ex, "Burpees", "Baja al suelo, haz una flexión y salta con los brazos arriba.", MuscleGroup.FULL_BODY, Difficulty.INTERMEDIATE);
        exercise(training, ex, "Trote en cinta", "Mantén un ritmo constante que te permita hablar.", MuscleGroup.CARDIO, Difficulty.BEGINNER);
        exercise(training, ex, "Bicicleta estática", "Pedalea a ritmo medio con resistencia moderada.", MuscleGroup.CARDIO, Difficulty.BEGINNER);

        Routine fullBody = routine(training, "Full body principiante", "Adaptación y base de fuerza", carlos,
                day("Día 1 - Empuje", ex, "Press de banca", 3, 10, 60, "Press militar", 3, 10, 60, "Extensión de tríceps en polea", 3, 12, 45),
                day("Día 2 - Tirón", ex, "Jalón al pecho", 3, 10, 60, "Remo con barra", 3, 10, 60, "Curl de bíceps", 3, 12, 45),
                day("Día 3 - Piernas", ex, "Sentadilla", 3, 10, 90, "Prensa de piernas", 3, 12, 60, "Plancha", 3, 30, 45));
        Routine hypertrophy = routine(training, "Hipertrofia 4 días", "Ganar masa muscular", carlos,
                day("Día 1 - Pecho y tríceps", ex, "Press de banca", 4, 8, 90, "Aperturas con mancuernas", 3, 12, 60, "Fondos en paralelas", 3, 8, 90),
                day("Día 2 - Espalda y bíceps", ex, "Dominadas", 4, 6, 90, "Remo con barra", 4, 8, 90, "Curl de bíceps", 3, 10, 60),
                day("Día 3 - Hombros y core", ex, "Press militar", 4, 8, 90, "Elevaciones laterales", 3, 15, 45, "Abdominales en polea", 3, 15, 45),
                day("Día 4 - Piernas", ex, "Sentadilla", 4, 8, 120, "Zancadas", 3, 12, 75, "Peso muerto", 3, 6, 120));
        Routine fatLoss = routine(training, "Definición y cardio", "Perder grasa y mejorar resistencia", ana,
                day("Día 1 - Circuito", ex, "Burpees", 4, 12, 30, "Flexiones", 4, 15, 30, "Zancadas", 4, 12, 30),
                day("Día 2 - Cardio", ex, "Trote en cinta", 1, 30, 60, "Bicicleta estática", 1, 20, 60, "Plancha", 3, 40, 30),
                day("Día 3 - Glúteos y piernas", ex, "Hip thrust", 4, 12, 60, "Puente de glúteos", 3, 15, 45, "Sentadilla", 3, 15, 60));
        routine(training, "Fuerza tren inferior", "Aumentar la fuerza de piernas y glúteos", ana,
                day("Día 1 - Sentadilla", ex, "Sentadilla", 5, 5, 150, "Prensa de piernas", 3, 10, 90),
                day("Día 2 - Cadena posterior", ex, "Peso muerto", 4, 5, 150, "Hip thrust", 4, 8, 90));

        // Assignments are stored with a past date, then the sessions that followed are logged.
        assign(training, fullBody, 0, 1, 2);
        assign(training, hypertrophy, 3, 4, 5);
        assign(training, fatLoss, 13, 9);
    }

    private void exercise(TrainingModule training, Map<String, Exercise> byName, String name, String description,
                          MuscleGroup group, Difficulty difficulty) {
        byName.put(name, training.exercises().create(new Exercise(0, name, description, group, difficulty, "")));
    }

    private RoutineDay day(String name, Map<String, Exercise> ex, Object... values) {
        List<RoutineExercise> lines = new ArrayList<>();
        for (int i = 0; i < values.length; i += 4) {
            lines.add(new RoutineExercise(ex.get((String) values[i]).getId(), (Integer) values[i + 1],
                    (Integer) values[i + 2], (Integer) values[i + 3]));
        }
        return new RoutineDay(name, lines);
    }

    private Routine routine(TrainingModule training, String name, String objective, Trainer trainer, RoutineDay... days) {
        return training.routines().create(new Routine(0, name, objective, trainer.getId(), today.minusDays(60), List.of(days)));
    }

    private void assign(TrainingModule training, Routine routine, int... memberIndexes) {
        for (int index : memberIndexes) {
            Member member = people.get(index);
            RoutineAssignment assignment = assignmentRepository.save(new RoutineAssignment(0, member.getId(),
                    routine.getId(), today.minusDays(40), AssignmentStatus.ACTIVE));
            List<RoutineDay> days = routine.getDays();
            int session = 0;
            for (int ago = 38; ago >= 1; ago -= 3) {
                String dayName = days.get(session % days.size()).getName();
                session++;
                if (random.nextInt(4) == 0) {
                    continue;
                }
                training.sessions().log(new TrainingSession(0, member.getId(), assignment.getId(), dayName,
                        today.minusDays(ago), 45 + random.nextInt(40), session % 4 == 0 ? "Buen ritmo, subir el peso" : ""));
            }
        }
    }

    private void finish() {
        // The member who left the gym is deactivated last, after buying his membership.
        members.deactivate(people.get(15).getId());
    }
}
