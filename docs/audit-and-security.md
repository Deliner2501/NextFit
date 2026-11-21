# Audit log and security

NextFit protects the access to the data in three ways: it blocks repeated wrong passwords, it closes forgotten sessions,
and it records who did what. The audit screen is only for the administrator.

## Login lockout

After **5 failed sign-ins** of the same user, that user is **blocked for 5 minutes**. While blocked, even the right
password is refused, and each attempt is recorded. The block is kept in memory (`InMemoryAttemptLimiter`), so it
disappears when the application closes. `AttemptLimiter` is an interface: a limiter that stores the attempts in the
database can replace it without touching `AuthService`.

## Session closed by inactivity

If nobody touches the keyboard or the mouse for **15 minutes**, the session is closed and the login screen shows a
message ("Cerramos tu sesión por inactividad"). `InactivityMonitor` holds the rule and the injected clock, and
`IdleWatcher` (JavaFX) checks every few seconds and listens to the activity of the window.

## Audit log

Every important event is stored in `data/audit.csv` with its date and time, the user and a short detail:

| Event | When |
|---|---|
| Inicio de sesión | A user signs in. |
| Inicio de sesión fallido | A wrong user or password. |
| Usuario bloqueado | An attempt while the user is blocked. |
| Cierre de sesión | The user signs out. |
| Sesión cerrada por inactividad | The 15 minutes pass. |
| Usuario creado / modificado / desactivado / reactivado | The administrator changes a user. |
| Contraseña restablecida | The administrator resets a password. |
| Respaldo creado / restaurado | A backup is made by hand or restored. |

The events that need a user use the one who is signed in; events before the sign-in or from the system use `sistema`.

### The Auditoría screen

- Three counters: **events of today**, **failed sign-ins** and **blocked users**.
- A table with date and time, user, event and detail, newest first.
- A filter by kind of event.

## How it is built

| Part | Class | Job |
|---|---|---|
| Rule | `AuditLog` (interface), `AuditService` | `record(user, action, detail)`, `findAll()`, `findByAction(...)`, `countToday(...)`. |
| Data | `AuditEntry`, `AuditAction` | One event and the list of kinds of events. |
| Storage | `AuditRepository`, `CsvAuditRepository` | Append-only CSV. |
| Callers | `AuthService`, `UserService`, `BackupService` | They receive an `AuditLog` and a supplier with the current user, so they do not depend on the session. |
| Screen | `AuditModule` | Counters, filter and table. |
| Security | `AttemptLimiter`, `InMemoryAttemptLimiter`, `InactivityMonitor`, `IdleWatcher` | Lockout and inactivity. |

## Good practice

- The log only stores the user name and a short detail, never a password.
- The application has no button to edit or delete events.
- It is included in the backups like any other CSV file.
