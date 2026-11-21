# Entry with a password

A member can enter the gym in two ways that he chooses himself: with the **face** (see `face-recognition.md`) or with the **document number and a password** that he created. Every member can have a password, so it is also the way in when the camera fails.

## How it works

1. The first time, the member creates the password in the **Contraseña** tab of access control, with a member of the staff beside. The **member types it**; the staff does not see it. It needs at least 6 characters, at most 64, it cannot be the document number and it must be typed twice.
2. To enter or to leave, the member types the document and the password in the same tab (`SelfAccessService`). The same method is used for the entry and the exit.
3. If the member forgets it, the staff presses **Restablecer contraseña** (it is written in the audit log) and the member creates a new one.

## Security

- Only a salted hash is stored (`PasswordHasher`, PBKDF2 with HMAC-SHA256), in `data/member_credentials.csv` (or the Oracle table with the same columns). The text of the password is never stored nor written anywhere.
- A wrong document and a wrong password get the **same answer**, so nobody can find out which documents exist.
- After 5 wrong passwords a document is **blocked for 5 minutes** (`InMemoryAttemptLimiter`, the same limiter as the staff sign-in). A reset lifts the block.
- Created passwords, resets, failed attempts and blocks are written in the audit log (`MEMBER_PASSWORD_CREATED`, `MEMBER_PASSWORD_RESET`, `MEMBER_LOGIN_FAILED`, `MEMBER_LOCKOUT`).
- An inactive member cannot enter even with the right password.

## Code

| Class | Job |
|---|---|
| `MemberCredential`, `MemberCredentialRepository`, `CsvMemberCredentialRepository` | The hash of each member |
| `MemberPasswordService` | Creates, resets and checks passwords; writes the audit log |
| `SelfAccessService` | Turns a document and a password into an entry, an exit or a denial |
| `PasswordPane` | The screen |

Table for Oracle: `member_credentials(member_id, password_hash)`, one row per member.
