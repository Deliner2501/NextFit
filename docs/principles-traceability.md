# Principles Traceability

Each row links a principle or pattern to the class or method where it is applied and explains why. Each team member adds their own rows in the same Pull Request where their code is created.

## Deiner: core and access

| Principle or pattern | Class or method | Justification |
|---|---|---|
| SRP | `PasswordHasher`, `UserValidator`, `UserCsvMapper`, `Session` | Each class has a single reason to change: hashing, validating, converting rows, or remembering the active user |
| GRASP Information Expert | `UserValidator` | The class that knows the user rules is the one that checks them |
| GRASP Information Expert | `AuthService.login` | Sign-in rules live in the service, not in the window |
| DIP | `AuthService` and `UserRepository` | The service depends on the interface, not on the CSV file |
| GRASP Protected Variations | `CsvStore` | The file format is isolated in one class; moving to a database does not touch the services |
| OCP | `ModulePanel` and `MainWindow` | A new module is added to the list without editing the main window |
| GRASP Controller and low coupling | `LoginFrame` | The window collects input and delegates to `AuthService`; it holds no business rules |
| GRASP Pure Fabrication | `PasswordHasher`, `Session` | They are not gym concepts, but they concentrate a technical task and reduce coupling |
| Encapsulation | `User`, `PasswordHasher` | `final` attributes; the hash is one-way and never exposes the password |
| ISP and DIP | `SubscriptionChecker` | Other modules depend on a one-method contract, not on the membership module |
| OCP and GRASP Polymorphism | `Discount`, `NoDiscount`, `PercentageDiscount`, `FixedDiscount` | A new kind of discount is a new subclass; the payment only calls `amountOff` |
| Template Method | `Discount.amountOff` | The final method fixes the rule "never negative, never above the price"; subclasses only supply `calculate` |
| GRASP Information Expert | `MembershipPlan.endDateFor`, `Subscription.statusOn`, `Subscription.daysLeftOn`, `Payment.getTotal` | The class that owns the data does the calculation |
| Encapsulation | `MembershipPlan`, `Subscription`, `Payment` | `final` attributes; changes produce copies (`withId`, `cancel`) |
| SRP | `MembershipPlanCsvMapper`, `SubscriptionCsvMapper`, `PaymentCsvMapper` and their `Csv...Repository` classes | Each mapper only converts rows; each repository decides when to read and save |
| DIP | `MembershipPlanRepository`, `SubscriptionRepository`, `PaymentRepository` | Services depend on the interfaces, not on the CSV files |
| SRP | `MembershipPlanValidator`, `PaymentValidator` | Validators only check data; business rules stay in the services |
| DIP | `SubscriptionService` implements `SubscriptionChecker` | Attendance and training ask the contract, not the membership module |
| DIP and testability | `Clock` injected in `SubscriptionService` and `PaymentService` | The rules can be tested on any date without waiting for real time |
| GRASP Controller (use case) | `PaymentService.sellMembership` | One operation coordinates subscription and payment, and cancels the subscription if the payment cannot be stored |
| GRASP Information Expert | `SubscriptionService.firstFreeDay`, `PaymentService.revenueByMonth` | The service that holds the data computes renewal dates and revenue totals |

## Ronald: members and health

| Principle or pattern | Class or method | Justification |
|---|---|---|
| Inheritance and polymorphism | `Person`, `Member`, `Person.getPersonType` | The shared personal data lives in the abstract class and each subclass says what kind of person it is; a trainer can extend `Person` without changing it |
| Encapsulation and immutability | `Member`, `HealthProfile`, `Measurement`, `AttendanceRecord` | `final` attributes; changes produce copies (`withId`, `withActive`, `withCheckOut`) |
| SRP | `MemberValidator`, `HealthValidator`, `MemberCsvMapper`, `HealthProfileCsvMapper`, `MeasurementCsvMapper`, `AttendanceCsvMapper` | Each class has one reason to change: checking data or converting rows |
| GRASP Information Expert | `MemberValidator`, `HealthValidator`, `AttendanceRecord.isOpen`, `BmiCategory.fromBmi`, `HealthService.calculateBmi` | The class that owns the data or the rule is the one that applies it |
| DIP | `MemberService`, `HealthService`, `AttendanceService` and their repositories | Services depend on the repository interfaces, not on the CSV files |
| GRASP Protected Variations | `MemberRepository`, `HealthProfileRepository`, `MeasurementRepository`, `AttendanceRepository` | Moving from CSV to a database changes only the implementation, never the services |
| ISP and DIP | `AttendanceService` and `SubscriptionChecker` | Attendance depends on a one-method contract, not on the membership module, and the tests use a fake one |
| GRASP Low coupling | `HealthService`, `AttendanceService` and `MemberService.findById` | They ask the member service whether a member exists instead of reading the members file |
| Dependency injection | `AttendanceService` constructor with `Clock` | The clock is received from outside so the tests can fix the time |
| Low coupling and reuse | `LineChart` | The chart receives plain `Point(label, value)` data and knows nothing about members or payments, so any screen can reuse it |
| SRP | `LineChart` | It only draws a chart; services provide the data and each screen decides what to show |
| GRASP High cohesion | `LineChart.drawGrid`, `drawLineAndArea`, `drawPoints`, `drawTooltip` | Each private method draws one part of the chart |
| GRASP Controller | `MembersPanel`, `MemberFormDialog` | The screens collect what the user types and delegate every rule to `MemberService`; they hold no business rules |
| Low coupling | `MembersPanel` and `MemberService` | The screen depends only on the service, never on repositories or CSV files |
| OCP | `MembersPanel` implements `ModulePanel` | The module joins the menu by replacing a placeholder in `Main`; the main window does not change |
| SRP | `PhotoStorage`, `MemberAvatar` | One only copies photos into the data folder, the other only draws a picture or initials |
| GRASP Pure Fabrication | `PhotoStorage` | It is not a gym concept, but it keeps file work out of the screens and the services |
| GRASP Controller | `MemberDetailPanel`, `HealthTab`, `MeasurementsTab`, `AttendanceTab` | Each tab collects input and delegates to `HealthService` or `AttendanceService`; none holds business rules |
| High cohesion | `HealthTab`, `MeasurementsTab`, `AttendanceTab` | The member record is split into one class per topic instead of one huge screen |
| Information Expert | `BmiGauge` | It knows how an index maps to a colored position and asks `BmiCategory` for the category |
| Low coupling and reuse | `MeasurementsTab` uses `LineChart` | The weight chart reuses the generic component without changing it |
| DIP | `Main` | Services are created with their CSV repositories in one place, so a database only changes those lines |

## Santiago: training

| Principle or pattern | Class or method | Justification |
|---|---|---|
| Encapsulation | `Exercise`, `Routine`, `RoutineDay`, `RoutineExercise`, `Trainer` | `final` attributes; changes produce copies (`withId`, `withActive`), and lists are copied with `List.copyOf` |
| SRP | `ExerciseValidator`, `RoutineValidator`, `TrainerValidator` | Each class only checks data; storing and business rules live elsewhere |
| SRP | `ExerciseCsvMapper`, `RoutineCsvMapper`, `TrainerCsvMapper` | Each class only converts objects to CSV rows and back |
| DIP | `TrainerService` and `TrainerRepository` | The service depends on the interface, not on the CSV file |
| GRASP Protected Variations | `ExerciseRepository`, `RoutineRepository`, `TrainerRepository` | Moving from CSV to Oracle means replacing the implementation only; the rest of the code does not change |
| GRASP Information Expert | `RoutineDay.getTotalSets`, `Routine.getTotalSets` | The class that holds the exercises does the calculation |
| GRASP Information Expert | `TrainerValidator`, `RoutineValidator` | The class that knows the rules is the one that applies them |
| GRASP Low coupling | `Routine` | It references exercises and the trainer by id, not by object |
| Inheritance (Liskov) | `Trainer extends Person` | A trainer can be used anywhere a `Person` is expected, and adds only its own data |
| SRP | `ExerciseService`, `RoutineService`, `RoutineAssignmentService` | Each service holds the business rules of one concept; validation and storage live in other classes |
| DIP | `RoutineAssignmentService` and `SubscriptionChecker`, `AssignmentRepository` | The service depends on interfaces, so the membership module and the CSV file can change or be faked in tests |
| GRASP Information Expert | `RoutineAssignmentService.findCurrentFor` | The service that owns the assignments knows which one is active |
| GRASP Information Expert | `ExerciseService.search` | The service that owns the catalog does the filtering, not the screen |
| GRASP Low coupling | `RoutineAssignment` | It references the member and the routine by id, not by object |
| Encapsulation | `RoutineAssignment` | `final` attributes; a change of state produces a copy (`withStatus`) |
| SRP | `TrainingSessionValidator`, `TrainingSessionService` | One class checks the data, the other applies the business rules |
| GRASP Information Expert | `TrainingSessionService.log` | The service that records sessions knows which rules a session must satisfy |
| DIP | `TrainingSessionService` and `TrainingSessionRepository`, `SubscriptionChecker` | The service depends on interfaces, not on CSV or the membership module |
| Encapsulation | `TrainingSession` | `final` attributes; the only copy allowed is `withId` |
| SRP and reuse | `PictureSource`, `Avatar`, `ImageCard` | Finding a picture, showing a person and showing an item are separate classes that any module can reuse |
| Testability (DIP) | `RoutineAssignmentServiceTest`, `TrainingSessionServiceTest` | `SubscriptionChecker` is replaced by a lambda in tests because the services depend on an interface, not on the membership module |
| GRASP Controller and low coupling | `ExerciseCatalogView`, `ExerciseFormDialog` | The screens collect input and delegate to `ExerciseService`; they hold no business rules |
| OCP | `TrainingPanel` | A new tab (trainers, routines, sessions) is added to the panel without changing the existing tabs |

