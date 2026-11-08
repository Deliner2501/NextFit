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
| SRP | `StatCard`, `DonutChart`, `BarChart` | Each class only draws; the screen decides which data to show and how to write it |
| Low coupling and reuse | `DonutChart.Slice`, `BarChart.Bar` | The charts receive plain records and know nothing about memberships or payments, so any screen can reuse them |
| Strategy (injected function) | `StatCard` constructor (`LongFunction`), `BarChart.setValueFormatter` | The way a number is written (pesos, count) is supplied from outside and the component does not change |
| GRASP High cohesion | `DonutChart.drawRing`, `drawCenter`, `drawLegend`; `BarChart.drawGrid`, `drawBars`, `drawLabels`, `drawTooltip` | Each private method draws one part of the chart |
| GRASP Information Expert | `BarChart.axisMax` | The chart owns the scale, so it rounds the axis to clean values itself |
| SRP and GRASP Pure Fabrication | `Formats` | It is not a gym concept, but it concentrates how pesos and dates are written and read, so no screen repeats that code |
| Testability | `FormatsTest` | The writing and reading rules are checked without opening any window |
| GRASP Controller | `PlanListView`, `PlanFormDialog` | The screens collect clicks and typed text and delegate every rule to `MembershipPlanService`; they hold no business rules |
| Low coupling | `PlanListView` and `MembershipPlanService` | The screen depends only on the service, never on repositories or CSV files |
| High cohesion | `BillingPanel` | It only groups the screens of the module in tabs; each tab is its own class |
| OCP | `BillingPanel` implements `ModulePanel` | The module replaces its placeholder in `Main`; the main window does not change |
| GRASP Controller (use case) | `SaleView` | The screen collects member, plan, method and discount and delegates the sale to `PaymentService.sellMembership`; it holds no business rules |
| OCP and polymorphism | `SaleView.buildDiscount` | The screen builds a `NoDiscount`, `PercentageDiscount` or `FixedDiscount` and passes it as a `Discount`; the service does not need to know which one |
| SRP and testability | `ReceiptText`, `ReceiptTextTest` | One class only builds the receipt text, without Swing, so the window, the clipboard and the tests use the same text |
| SRP | `ReceiptDialog` | It only shows the text and copies it; it never calculates amounts |
| Low coupling | `SaleView` and `PaymentService`, `SubscriptionService`, `MembershipPlanService` | The screen depends only on services, never on repositories or CSV files |
| Reuse | `MemberPickerPanel` in `SaleView` | The member search column built for training is reused instead of writing it again |
| GRASP Information Expert | `SubscriptionService.statusOf`, `daysLeftOf` | The service owns the clock, so "what state is it in today" is answered there and no screen reads the date itself |
| GRASP Information Expert | `PaymentService.findSale` | The service that owns payments, subscriptions and plans rebuilds the sale; the screen does not chain three lookups |
| GRASP Controller | `SubscriptionListView`, `PaymentListView` | The screens collect search text, filters and clicks and delegate to the services; they hold no business rules |
| Low coupling | `SubscriptionListView`, `PaymentListView` and the services | Each screen depends only on services, never on repositories or CSV files |
| Low coupling (callback) | `SaleView` `onSold` and `BillingPanel` | The sale screen only runs an action after selling; it does not know which other tabs need to refresh |
| Testability | `SubscriptionServiceTest.statusAndDaysLeftAreWorkedOutFromToday`, `PaymentServiceTest.findSaleRebuildsTheSaleFromItsPayment` | The new service methods are checked with a fixed `Clock` and in-memory repositories |
| GRASP Pure Fabrication | `DashboardService` | It is not a gym concept, but it gathers numbers from the membership, payment and attendance services so no screen does that math |
| Encapsulation | `DashboardService.Summary` | One immutable record carries every number of the dashboard from the service to the screen |
| Low coupling | `DashboardPanel` and `DashboardService` | The panel depends on one service and only draws; it knows nothing about repositories, CSV files or the other services |
| DIP and testability | `DashboardServiceTest` | The summary is checked with a fixed `Clock` and in-memory repositories, with memberships in every state |
| Reuse | `StatCard`, `DonutChart`, `BarChart`, `LineChart` in `DashboardPanel` | The components built with plain data are reused without any change |
| OCP | `DashboardPanel` implements `ModulePanel` | The module replaces its placeholder in `Main`; the main window does not change |
| Least privilege | `DashboardPanel.allowedFor` | The trainer role does not see the income numbers, because the screen is hidden from that role |
| SRP | `UserService`, `UserValidator` | The validator only checks data; the service holds the account rules |
| DIP | `UserService` and `UserRepository` | The service depends on the interface, not on the CSV file |
| GRASP Information Expert | `UserService.ensureAnotherActiveAdmin` | The service that sees every account is the one that protects the last administrator |
| OCP | `UsersPanel` implements `ModulePanel` | The module replaced its placeholder in `Main`; the main window did not change |
| GRASP Controller | `UsersPanel`, `UserFormDialog` | The screens collect clicks and delegate every rule to `UserService` |
| Security | `UserService.resetPassword`, `LoginFrame` | Only the hash is stored; the default credentials are no longer shown on screen |
| SRP | `GymArt`, `MuscleArt`, `HeroBanner` | One only draws equipment, one only draws muscle groups, one only paints a banner |
| Low coupling and reuse | `GymArt` | The login and the dashboard share the same drawings without knowing each other |
| GRASP Protected Variations | `PictureSource` with the `art:` path | Screens ask for a picture by path; whether it is a photo or a drawing is hidden |
| OCP | `MuscleArt` and `ImageCard` | The catalog gained illustrations without changing how `ImageCard` draws |
| SRP | `CoverPhoto` | It only fits a photo into an area and keeps the scaled copy; screens decide where to use it |
| Decorator | `BannerModule` | It adds a banner to any `ModulePanel` and forwards every call, so the modules stay unchanged |
| GRASP Protected Variations | `HeroBanner`, `MuscleArt.pathFor` | A missing photo falls back to the drawing, so the screens never break |
| DIP and reuse | `DemoData` | It creates the example data through the same services the screens use, so it follows the same rules and needs no special code path |
| DIP and testability | `DemoData.at` with `Clock.fixed` | Each past day is reproduced with its own fixed clock, so "a membership must be valid to enter" holds on that day |
| GRASP Protected Variations | `DemoData.isEmptyInstallation` | Example data is only created when nothing exists, so real data is never mixed with it |
| GRASP Information Expert and SRP | `Images.deviceScale` | The only place that knows how to read the screen density from a `Graphics2D`; `CoverPhoto` and `ImageCard` ask it instead of repeating the calculation |
| GRASP Protected Variations | `CoverPhoto.paint`, `ImageCard.paintPicture` | The photo is prepared at the real pixel size of the screen, so Windows scaling (125 %, 150 %) changes nothing for the callers, who keep drawing in logical pixels |
| SRP | `Images.scale` | One class owns the quality of resizing (progressive halving plus bicubic), so every screen gets the same sharpness |
| GRASP Protected Variations | `FxTheme` and `nextfit.css` | Dark and light modes change only the values of the style sheet; no screen writes a color in code, so a new mode does not touch any screen |
| DIP | `FxApp.run` | The JavaFX interface receives `AuthService`, `Session` and `DashboardService` from `Main` and never creates a repository; the interface never creates a repository, so the CSV files can be replaced by a database in `Main` only |
| SRP and GRASP Information Expert | `DashboardView` | It only draws the `Summary` that `DashboardService` calculates; no business rule is repeated in the view |
| OCP | `Main` (JavaFX only) | The interface was replaced without changing services, model or persistence |
| SRP | `LoginView`, `ShellView`, `FxIcons`, `FxImages` | One class per concern: sign-in screen, main window, vector icons, photo areas |
| OCP and polymorphism | `FxModule`, `DashboardModule`, `PendingModule`, `UsersModule` | `ShellView` hosts any module through one contract; a new screen appears in the menu without changing the shell |
| GRASP Pure Fabrication and DRY | `FxDialogs` | Forms and messages shared by every module, so no module builds its own window; the form stays open and shows the business error |
| SRP and DIP | `UsersModule` | It only collects clicks and shows data; every rule (last administrator, own account, password strength) stays in `UserService` |
| DRY and GRASP Pure Fabrication | `MemberPicker` | The member search column is one reusable class that any module (sales, training, members) can use instead of copying it |
| SRP and DIP | `SalePane`, `PlansPane` | They only collect data and show the total; the discount math is the `Discount` hierarchy and the sale rules are in `PaymentService` |
| OCP | `BillingModule` | The four tabs are added to one module; the subscriptions and payments tabs replace their placeholders without changing the module contract |
| DRY and GRASP Pure Fabrication | `FxTables` | Text columns and colored state columns are built in one place; `UsersModule`, `PlansPane`, `SubscriptionsPane` and `PaymentsPane` no longer repeat the cell code |
| SRP and DIP | `SubscriptionsPane`, `PaymentsPane` | They filter, search and show rows; the state of a membership (`statusOf`, `daysLeftOf`) and the receipt (`findSale`) come from the services |
| DRY | `ReceiptText` reused by `FxDialogs.document` | The receipt text is built once and shown by the JavaFX dialogs and by the tests |
| SRP | `CarnetCode`, `AccessService`, `AccessResult` | One class defines what the QR carries, one decides entry, exit or denial, and one carries the answer; none draws anything |
| DIP and ISP | `MembershipExpiry` | The access service asks a one-method interface when the membership ends, so it does not know the membership module and is tested with a lambda |
| GRASP Controller (use case) | `AccessService.process` | One operation turns a scan into an entry or an exit and reuses `AttendanceService` for the rules instead of repeating them |
| GRASP Protected Variations | `CarnetCode.decode` | The scanner format (prefix, case, spaces, typed document) is normalized in one place, so a different reader does not change the service |
| GRASP Low coupling | `AccessResult` | The kiosk shows a record with the outcome and the message; it never catches rule exceptions itself |
| GRASP Pure Fabrication | `QrImage`, `MemberAvatar` | Small helpers that draw the QR and the photo, shared by the kiosk and the carnet screen |
| Testability | `AccessServiceTest` | Entry, exit, denial, unknown code and the last day of the membership are tested with a fixed clock |
| SRP | `AccessTokens`, `AccessKeyStore` | One class signs and checks the access code; the other only keeps the secret key in a file |
| GRASP Protected Variations | `AccessTokens` | The code is derived from the document with a secret key, so the Member model and the CSV files do not change and a leaked carnet is revoked by changing the key |
| GRASP Controller (use case) | `PhoneAccess.scan` | One operation turns a phone scan into an entry or exit, and returns the previous answer when the same code arrives twice in 10 seconds |
| OCP and Observer | `AccessService.addListener` | The kiosk screen subscribes to results, so a new way to scan (phone, reader, typed) needs no change in the screen |
| SRP and Pure Fabrication | `AccessPage` | A pure function from a result to the phone page, tested without a server and with the member text escaped |
| DIP | `AccessServer` | The web server depends on `PhoneAccess`, not on members, attendance or files; the port and the clock are injected |
| Testability | `PhoneAccessTest`, `AccessServerTest`, `AccessPageTest`, `AccessTokensTest`, `AccessKeyStoreTest` | The server is tested for real on a free port with an HTTP client, like a phone would call it |
| SRP | `OccupancyHeatmap`, `AnalyticsService` | One class only counts entries by weekday and hour; the other only joins attendance and memberships into a report; neither draws anything |
| GRASP Information Expert | `OccupancyHeatmap` | The heatmap owns its counts, so it is the one that knows the maximum, the total and the peak slot |
| GRASP Low coupling | `AnalyticsService.Report` | The screen receives one immutable report and never reads attendance or memberships itself |
| GRASP Pure Fabrication | `OccupancyHeatmap` | It is not a gym concept, but it concentrates the counting so it can be tested without storage |
| Encapsulation and immutability | `Report`, `AtRisk` | Records returned by a read-only service; the screen cannot change the numbers |
| DIP and testability | `AnalyticsService` with `Clock` | Today is injected, so renewal and risk rules are tested with a fixed date |
| OCP | `AnalyticsModule` | The screen is added as one more module in the list of `Main`; the shell does not change |
| Testability | `AnalyticsServiceTest` | Heatmap window, average stay, renewal rate and at-risk members are tested with fake repositories |

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
| Testability (DIP) | `BillingFakeRepositories` | The billing services are tested with in-memory repositories because they depend on interfaces, not on CSV files |
| Testability (DIP) | `Clock.fixed` in `SubscriptionServiceTest`, `PaymentServiceTest` | The rules about dates (start, renewal, expiry, revenue by month) are checked on a known day |
| GRASP Information Expert | `MembershipPlanServiceTest.endDateCountsTheStartingDayAsTheFirstDay` | The plan owns the duration, so the test checks the end date on the plan itself |
| OCP and polymorphism | `PaymentServiceTest` with `PercentageDiscount`, `FixedDiscount` | The same sale code works with every kind of discount without changes |
| GRASP Controller (use case) | `PaymentServiceTest.ifThePaymentCannotBeStoredTheSubscriptionIsCancelled` | The test proves that selling is one operation: no subscription is left without its payment |
| OCP and polymorphism | `AlertRule`, `ExpiringMembershipRule`, `InactiveMemberRule`, `BirthdayRule` | Each kind of alert is one class that implements the same interface; a new kind of alert is a new class and `AlertService` does not change |
| SRP | `AlertService`, `AlertMessages`, `WhatsAppLink` | The service only gathers the alerts, `AlertMessages` only writes the text and `WhatsAppLink` only formats a phone and a text; none of them draws a screen or sends anything |
| GRASP Information Expert | `Alert`, `AlertType` | The alert is a plain value that carries what the screens need (type, member, days, date) and the type knows its own label and its order of importance |
| High cohesion | `ExpiringMembershipRule`, `InactiveMemberRule`, `BirthdayRule` | Each rule contains only the logic of its own alert, including its edge cases (renewed membership, new membership, 29 February) |
| DIP and dependency injection | `AlertService` constructor, `AlertRule`, `Clock` | The service receives its rules and the clock from outside, so tests choose the day and the alerts that run |
| GRASP Creator | `AlertService.withStandardRules` | The service knows which rules it uses by default and builds them, so `Main` and the screens do not repeat that list |
| Low coupling and reuse | `WhatsAppLink` | It receives a plain phone and a plain text and knows neither members nor alerts, so any screen can reuse it |
| Protected Variations | `AlertMessages`, `WhatsAppLink` | The wording and the way of contacting the member are isolated; changing to SMS or email would replace these classes only |
| Testability | `AlertServiceTest`, `AlertMessagesTest`, `WhatsAppLinkTest` | The rules are tested with a fixed clock and real CSV files in a temporary folder, including the edge cases of the dates |
| GRASP Controller | `MembersPane`, `MemberForm` | They collect clicks and typed data and delegate to `MemberService`; no business rule lives on the screen |
| OCP and polymorphism | `MembersModule` implements `FxModule` | The screen entered the menu by replacing one line in `Main`; `ShellView` did not change |
| Low coupling | `MembersPane` receives a `BooleanSupplier` | The pane does not know the session or the roles; the module tells it whether the user may edit |
| High cohesion | `MembersPane`, `MemberForm`, `MembersModule` | The list, the form and the module are separate classes with one job each |
| Reuse | `MembersPane` uses `FxTables`, `FxDialogs` and `MemberAvatar` | The table cells, the dialogs and the photo come from the shared components, so no code is repeated |
| GRASP Pure Fabrication | `MemberForm` and `PhotoStorage` | Copying the photo is delegated to `PhotoStorage`; the form only chooses the file |

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
| SRP | `RoutineFormDialog.DayPanel`, `RoutineFormDialog.LineRow` | Each nested class handles one level of the routine (a day, a line); the dialog only assembles the result and delegates saving to `RoutineService` |
| GRASP Information Expert | `AssignmentView` and `RoutineAssignmentService.findCurrentFor` | The screen asks the service which routine is current instead of filtering assignments itself |
| DRY and SRP | `MemberPickerPanel` | The member search column is one reusable class instead of being copied into each screen |
| SRP | `SuggestionRequest`, `SuggestionSettings`, `WeeklySplit` | One class only carries what was asked, one only holds the adjustable settings and one only decides the days; none of them builds a routine |
| OCP and ISP | `SuggestionRule`, `BmiRule`, `MedicalConditionRule` | A new rule is a new class that implements a one-method interface; `SuggestionService` does not change |
| DIP | `SuggestionService` and `SuggestionRule`, `ExerciseService`, `HealthService` | The service receives its rules and collaborators through the constructor and depends on the interface, not on concrete rules |
| GRASP Information Expert | `TrainingGoal` | Each goal knows its own sets, repetitions, rest and whether it needs cardio, so no chain of `if` is needed |
| GRASP Information Expert | `SuggestionSettings.allows` | The settings own the limits, so they decide whether an exercise can be suggested |
| Encapsulation and immutability | `SuggestionRequest`, `SuggestionSettings`, `SuggestedRoutine` | Records with copied collections; each rule returns a new copy instead of changing the previous one |
| GRASP Controller (use case) | `SuggestionService.suggest` | One operation coordinates the rules, the weekly split and the catalog to build the draft; it holds no screen code and saves nothing |
| GRASP Low coupling | `SuggestionRequest` | The rules read a BMI category and a text, not the member or the health service, so they are tested alone |
| GRASP Protected Variations | `MedicalConditionRule.normalize` | The free text typed by people is normalized (case and accents) in one place, so the key words match however they were written |
| GRASP Pure Fabrication | `WeeklySplit` | It is not a gym concept, but it concentrates the weekly layout so the service does not repeat it |
| DIP and testability | `SuggestionService` with `Clock` | The creation date is checked with a fixed clock |
| Testability | `SuggestionRulesTest`, `SuggestionServiceTest` | The rules are tested alone and the service with CSV files in a temporary folder |
| SRP | `ExercisesPane`, `ExerciseForm` | One class shows the gallery and filters; the other only collects the form fields; neither holds business rules |
| GRASP Controller | `ExercisesPane`, `ExerciseForm` | They collect the search text, the filters and the clicks, and delegate every rule to `ExerciseService` |
| GRASP Low coupling | `ExercisesPane` and `ExerciseService` | The screen depends only on the service, never on repositories or CSV files |
| GRASP Information Expert | `ExerciseService.search` used by `ExercisesPane` | The service owns the catalog, so it does the filtering; the screen only passes the text and the filters |
| OCP | `TrainingFxModule` implements `FxModule` | The module replaces its placeholder in `Main`; the main window does not change and each new tab is added without touching the others |
| GRASP Protected Variations | `ExercisesPane.pictureOf` | A missing or unreadable photo falls back to the picture of the muscle group, so the screen never breaks |
| Reuse | `FxDialogs.form` and `FxDialogs.labeled` in `ExerciseForm` | The form reuses the shared dialog, which shows the business errors inside the window without repeating that code |
| GRASP Protected Variations | `nf-exercise-card`, `nf-textarea` in `nextfit.css` | The colors live in the style sheet, so the screens only add style classes and work in light and dark mode |
| SRP | `RoutineLineRow`, `RoutineDayPane`, `RoutineForm` | Each class handles one level of the routine form: one exercise line, one day, the whole routine. |
| Information Expert | `RoutineDayPane` | Each day pane builds its own `RoutineDay` from the data it holds, so the form does not need to know its details. |
| DIP | `RoutinesPane`, `RoutineForm` | They depend on the service classes, not on repositories or files; all rules stay in `RoutineService`. |
| Low Coupling | `SuggestRoutineDialog` | It only asks `SuggestionService` for a draft and returns it; the form decides what to do with it. |
| OCP | `Main` | The suggestion service is built here with its list of rules, so a new rule is added without changing the service. |
| SRP | `AssignmentPane` | It only shows the member, the current routine and the history, and reports the clicks; it does not decide anything. |
| Information Expert | `RoutineAssignmentService` | The rules (active member, valid membership, one active routine) stay in the service that owns the assignment data; the screen only shows its error messages. |
| Low Coupling | `AssignmentPane`, `MemberPicker` | The pane reuses the shared member picker and only learns which member was chosen, so it does not know how the search works. |
| SRP | `SessionsPane` | It only collects the day, date, minutes and notes and shows the history; it does not decide what is a valid session. |
| Information Expert | `TrainingSessionService` | The rules for a session (active routine, valid data) stay in the service that owns the session data; the screen only shows its messages. |
| Low Coupling | `SessionsPane`, `MemberPicker` | The pane reuses the shared member picker and only learns which member was chosen, so it does not know how the search works. |
| SRP | `TrainersPane`, `TrainerForm` | The pane lists trainers and reports clicks; the form only collects the data of one trainer. Neither one decides what is valid. |
| Information Expert | `TrainerService` | The rules to register, update, deactivate and reactivate a trainer stay in the service that owns the trainer data; the screens only show its messages. |
| DIP | `TrainersPane`, `TrainerForm` | They depend on `TrainerService`, not on the CSV files, so the storage can change to the database without touching the screens. |
| SRP | `OccupancyService` | It only calculates the picture of the gym (people inside, level, entries by hour, best hour to come); it does not draw it and never changes the attendance data. |
| Information Expert | `OccupancyLevel` | Each level knows the percentage where it starts, so nobody else repeats those limits. |
| Pure Fabrication | `OccupancySnapshot` | An immutable picture of the gym with no drawing logic, created so the TV screen only reads data. |
| DIP | `OccupancyService` | It depends on `AttendanceService` and `MemberService`, and receives the capacity and the `Clock` by constructor, so tests can fix the time. |
| Low Coupling | `OccupancyService` | It reuses `OccupancyHeatmap` from the analytics instead of counting entries by weekday and hour again. |
| SRP | `OccupancyView`, `OccupancyModule` | The view only draws the picture it receives and refreshes itself; the module only hosts it and opens the full-screen window. The numbers come from `OccupancyService`. |
| DIP | `OccupancyView` | It depends on `OccupancyService`, not on the attendance files, so the data source can change without touching the screen. |
| Protected Variations | `nextfit.css` (`nf-level-*`) | The color of the occupancy bar changes with the level by a style class, so no color is written in code and the light and dark modes work without changes. |
| SRP | `CashCloseService`, `ReportService` | The first one only gathers the payments of a day into a close; the second only builds report tables. Neither one draws or writes files. |
| Information Expert | `CashClose` | The record that holds the lines and the totals also computes the expected cash and the difference with the cash counted, so no screen repeats that arithmetic. |
| OCP | `ReportTable`, `PdfExporter`, `XlsxWriter` | A new report is only a new table; the exporters draw any table, so adding a report does not touch them. |
| DIP | `PdfExporter`, `XlsxWriter` | The exporters depend on the plain `ReportTable` and `CashClose` values, not on services or repositories. |
| Low Coupling | `CashClosePane`, `ReportsPane`, `FileExport` | The panes only show data and call `FileExport`, which owns the "save as" dialog and the messages, so no pane repeats that code. |
| Protected Variations | `PdfDocument`, `PdfFonts` | The file formats are written with the JDK only. If a library such as POI or OpenPDF is used later, only the export package changes. |
| OCP | `Main`, `ReportsModule` | The module is added to the list of `FxModule`; the main window does not change. |
| SRP | `InMemoryAttemptLimiter`, `InactivityMonitor` | Each one only keeps the data of one rule (wrong attempts, last activity) and compares it with the clock; neither one knows screens, files or passwords. |
| OCP | `AttemptLimiter` | `AuthService` asks the interface, so a new way of limiting (for example saved in the database) is a new class and the sign-in rules do not change. |
| Polymorphism | `AttemptLimiter.NONE` | A limiter that never blocks lets `AuthService` work without a limit and keeps the older code unchanged, with no `if` to ask whether there is a limiter. |
| DIP | `AuthService`, `IdleWatcher` | `AuthService` depends on the `AttemptLimiter` interface and `IdleWatcher` on `InactivityMonitor`; both receive them by constructor and the clock is injected, so tests can move the time. |
| Controller | `FxApp` | It receives the "inactive for too long" event and decides to end the session and show the sign-in, so no screen has to know about the timer. |

