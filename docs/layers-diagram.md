# Layers and Package Diagram

NextFit follows a layered architecture. Each arrow means "depends on" (it comes from the real `import` statements of the code). Dependencies only go downwards, towards the domain, with two small exceptions that are shown in the second diagram.

## Package diagram by layers

```mermaid
graph TD
    subgraph ROOT["com.nextfit - Composition root"]
        R["<b>nextfit</b><br/>DemoData<br/>Main<br/>TrainingModule"]
    end

    subgraph PRES["Presentation layer (Swing + FlatLaf)"]
        UI["<b>ui</b><br/>BannerModule<br/>Formats<br/>LoginFrame<br/>MainWindow<br/>ModulePanel<br/>PlaceholderPanel<br/>Sidebar<br/>Theme<br/>TopBar<br/>UserChip"]
        UIC["<b>ui.components</b><br/>Avatar<br/>BarChart<br/>BmiGauge<br/>BrandPanel<br/>Branding<br/>CoverPhoto<br/>DonutChart<br/>GymArt<br/>HeroBanner<br/>Icons<br/>ImageCard<br/>Images<br/>LineChart<br/>MemberAvatar<br/>MuscleArt<br/>PictureSource<br/>StatCard"]
        UIM["<b>ui.members</b><br/>AttendanceTab<br/>Forms<br/>HealthTab<br/>MeasurementsTab<br/>MemberDetailPanel<br/>MemberFormDialog<br/>MembersPanel"]
        UIB["<b>ui.billing</b><br/>BillingPanel<br/>PaymentListView<br/>PlanFormDialog<br/>PlanListView<br/>ReceiptDialog<br/>ReceiptText<br/>SaleView<br/>SubscriptionListView"]
        UIT["<b>ui.training</b><br/>AssignmentView<br/>ExerciseCatalogView<br/>ExerciseFormDialog<br/>MemberPickerPanel<br/>RoutineFormDialog<br/>RoutineListView<br/>SessionView<br/>TrainerFormDialog<br/>TrainerListView<br/>TrainingPanel"]
        UID["<b>ui.dashboard</b><br/>DashboardPanel"]
        UIU["<b>ui.users</b><br/>UserFormDialog<br/>UsersPanel"]
    end

    subgraph BUS["Business layer"]
        SV["<b>service</b><br/>AttendanceService<br/>AuthService<br/>DashboardService<br/>ExerciseService<br/>HealthService<br/>MemberService<br/>MembershipPlanService<br/>MembershipSale<br/>PaymentService<br/>RoutineAssignmentService<br/>RoutineService<br/>Session<br/>SubscriptionChecker<br/>SubscriptionService<br/>TrainerService<br/>TrainingSessionService<br/>UserService"]
        VA["<b>validation</b><br/>ExerciseValidator<br/>HealthValidator<br/>MemberValidator<br/>MembershipPlanValidator<br/>PaymentValidator<br/>RoutineValidator<br/>TrainerValidator<br/>TrainingSessionValidator<br/>UserValidator"]
    end

    subgraph DATA["Persistence layer (CSV files)"]
        PE["<b>persistence</b><br/>AssignmentCsvMapper<br/>AssignmentRepository<br/>AttendanceCsvMapper<br/>AttendanceRepository<br/>CsvAssignmentRepository<br/>CsvAttendanceRepository<br/>CsvExerciseRepository<br/>CsvHealthProfileRepository<br/>CsvMeasurementRepository<br/>CsvMemberRepository<br/>CsvMembershipPlanRepository<br/>CsvPaymentRepository<br/>CsvRoutineRepository<br/>CsvStore<br/>CsvSubscriptionRepository<br/>CsvTrainerRepository<br/>CsvTrainingSessionRepository<br/>CsvUserRepository<br/>ExerciseCsvMapper<br/>ExerciseRepository<br/>HealthProfileCsvMapper<br/>HealthProfileRepository<br/>MeasurementCsvMapper<br/>MeasurementRepository<br/>MemberCsvMapper<br/>MemberRepository<br/>MembershipPlanCsvMapper<br/>MembershipPlanRepository<br/>PaymentCsvMapper<br/>PaymentRepository<br/>RoutineCsvMapper<br/>RoutineRepository<br/>SubscriptionCsvMapper<br/>SubscriptionRepository<br/>TrainerCsvMapper<br/>TrainerRepository<br/>TrainingSessionCsvMapper<br/>TrainingSessionRepository<br/>UserCsvMapper<br/>UserRepository"]
    end

    subgraph DOM["Domain layer"]
        MO["<b>model</b><br/>AssignmentStatus<br/>AttendanceRecord<br/>BmiCategory<br/>Difficulty<br/>Discount<br/>Exercise<br/>FixedDiscount<br/>HealthProfile<br/>Measurement<br/>Member<br/>MembershipPlan<br/>MuscleGroup<br/>NoDiscount<br/>Payment<br/>PaymentMethod<br/>PercentageDiscount<br/>Person<br/>Role<br/>Routine<br/>RoutineAssignment<br/>RoutineDay<br/>RoutineExercise<br/>Subscription<br/>SubscriptionStatus<br/>Trainer<br/>TrainingSession<br/>User"]
    end

    subgraph CROSS["Cross-cutting"]
        EX["<b>exceptions</b><br/>BusinessRuleException<br/>InvalidDataException<br/>NextFitException<br/>PersistenceException<br/>ResourceNotFoundException"]
        UT["<b>util</b><br/>PasswordHasher<br/>PhotoStorage"]
    end

    R --> UI
    R --> UIM
    R --> UIB
    R --> UIT
    R --> UID
    R --> UIU
    R --> SV
    R --> PE
    R --> VA

    UI --> UIC
    UIM --> UIC
    UIT --> UIC
    UID --> UIC
    UIM --> UI
    UIB --> UI
    UIT --> UI
    UID --> UI
    UIU --> UI

    UI --> SV
    UIM --> SV
    UIB --> SV
    UIT --> SV
    UID --> SV
    UIU --> SV

    SV --> VA
    SV --> PE
    SV --> MO
    VA --> MO
    PE --> MO
    UI --> MO

    SV --> UT
    UIM --> UT
    SV --> EX
    PE --> EX
    VA --> EX
    MO --> EX
```

## Package dependencies (simplified)

Same dependencies with one node per package. The two dashed arrows are the known exceptions to a strict layering: `ui.components` reads the shared `Theme` from `ui`, and `ui.billing` reuses `MemberPickerPanel` from `ui.training`.

```mermaid
graph LR
    root["nextfit (root)"]
    ui
    comp["ui.components"]
    members["ui.members"]
    billing["ui.billing"]
    training["ui.training"]
    dashboard["ui.dashboard"]
    users["ui.users"]
    service
    validation
    persistence
    model
    exceptions
    util

    root --> ui & members & billing & training & dashboard & users
    root --> service & validation & persistence
    ui & members & billing & training & dashboard & users --> service
    ui & members & training & dashboard --> comp
    members & billing & training & dashboard & users --> ui
    service --> validation & persistence & util
    service & validation & persistence & ui --> model
    members --> util
    service & validation & persistence & model --> exceptions
    util --> exceptions
    comp -.-> ui
    billing -.-> training
```
