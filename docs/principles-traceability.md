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

## Ronald: members and health

Pending: Ronald adds his rows in his next Pull Request.

## Santiago: training

Pending: Santiago adds his rows in his next Pull Request.