# Phone QR: the personal portal

The QR of a member carnet opens the **personal portal** of the member, a small web page served by the
reception computer. The QR is **read-only**: scanning it never registers an entry or an exit. Entries and exits are
registered by face ([face-recognition.md](face-recognition.md)), by document and password
([member-password.md](member-password.md)) or by the staff typing the document in the access screen.

## How it works

1. `Main` starts `AccessServer` (JDK `com.sun.net.httpserver`) on port 8765, or the next free one.
2. The carnet QR (printed, in the PDF or on the carnet screen) contains `http://<ip of the computer>:8765/m/<CODE>`.
   The code is `HMAC-SHA256(secret key, document)` cut to 10 letters and digits (`AccessTokens.tokenFor`).
3. `PhoneAccess.findMember` finds the member that owns the code; `MemberPortal` builds the summary and
   `ProgressReportService` adds the complete progress report. `PortalPage` draws everything.
4. The page is never cached by the phone (`Cache-Control: no-store`).

## What the portal shows

- **Tu día**: the day of the routine to train today, with its exercises.
- **Tu próximos días**: the next three days of the routine, going around when it ends.
- **Membership**: plan, end date and days left, with a warning when it ends in 7 days or less.
- **Progress**: days trained, streak in weeks and badges.
- **Tus últimas visitas**: the last five visits with the hour and the time inside.
- **Tu cuerpo**: first and last weight, the change and the body mass index.
- **Best hours** to train and the chat with the assistant when it is on.

## Routes

| Route | What it does |
|---|---|
| `/ping` | Connection test: open it from the phone to check that it reaches the computer |
| `/m/CODE` | The portal of the member (read-only) |
| `POST /m/CODE/chat` | Question to the assistant of the portal |
| `/photo/CODE` | Photo of the member, only when the code is valid |
| `/logo.png` | NextFit logo |

Any other address answers 404. There is no route that registers an entry or an exit.

## Requirements

- The phone and the computer must be in the same Wi-Fi network. If the network isolates devices, turn on the hotspot of the phone and connect the computer to it.
- The first time, Windows asks if Java may receive connections: allow it on private networks.
- The secret key is in `data/access.key` (created automatically, not committed). Deleting it invalidates every printed carnet.

## Networks that isolate devices

Many public and university Wi-Fi networks do not let two devices talk to each other (client isolation). The phone then
shows `ERR_ADDRESS_UNREACHABLE` even when both are "on the same Wi-Fi". To solve it:

1. Turn on the hotspot of the phone and connect the computer to it.
2. Close NextFit and open it again: the address of the computer is read when the program starts.
3. Open `http://<address of the computer>:8765/ping` on the phone to check the connection.
4. Show the carnet again (or save the PDF again) and scan that one.

The QR of a carnet contains the address of the computer, so a carnet printed on another network has to be printed
again after changing network. Typing the document in the access screen always works, with or without network.

## Limits

The server is only for the local network. It is not meant to be exposed to the internet.
A cloud server would be the next step.
