# Layers and Package Diagram

NextFit follows a layered architecture. Each arrow means "depends on" (it comes from the real `import` statements of the code). Dependencies only go downwards, towards the domain, with two small exceptions that are shown in the second diagram.

## Package diagram by layers

```mermaid
graph TD
    subgraph ROOT["com.nextfit - Composition root"]
        R["<b>nextfit</b><br/>DemoData<br/>Main<br/>TrainingModule"]
    end

    subgraph PRES["Presentation layer (JavaFX + AtlantaFX)"]
        UI["<b>ui</b><br/>Formats"]
        UIB["<b>ui.billing</b><br/>ReceiptText"]
        UIF["<b>ui.fx</b><br/>FxApp<br/>ShellView<br/>FxModule<br/>GlobalSearch<br/>SearchPalette<br/>one module and its panes per area<br/>(69 classes)"]
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

    R --> UIF
    R --> SV
    R --> PE
    R --> VA

    UIF --> UI
    UIF --> UIB
    UIB --> UI

    UIF --> SV

    SV --> VA
    SV --> PE
    SV --> MO
    VA --> MO
    PE --> MO
    UIF --> MO
    UIB --> MO

    SV --> UT
    UIF --> UT
    SV --> EX
    PE --> EX
    VA --> EX
    MO --> EX
```

## Package dependencies (simplified)

Same dependencies with one node per package. The Swing packages were removed; `ui.fx` is the only interface.

```mermaid
graph LR
    root["nextfit (root)"]
    fx["ui.fx"]
    ui
    billing["ui.billing"]
    service
    validation
    persistence
    model
    exceptions
    util

    root --> fx & service & validation & persistence
    fx --> service & ui & billing & util
    billing --> ui
    service --> validation & persistence & util
    service & validation & persistence & ui & billing & fx --> model
    service & validation & persistence & model --> exceptions
    util --> exceptions
```
