# Phone QR access

The QR of a member carnet opens a small web page served by the reception
computer. Scanning it with the camera of any phone registers the entry or the
exit and shows a NextFit-branded answer (green entry, blue exit, red denied) with the photo of the member, the end of the membership and a warning when it ends in 7 days or less. The
kiosk screen of NextFit reacts at the same time.

## How it works

1. `Main` starts `AccessServer` (JDK `com.sun.net.httpserver`) on port 8765, or the next free one.
2. The carnet QR (printed, in the PDF or on the carnet screen) contains `http://<ip of the computer>:8765/c/<FIXED CODE>`. It does **not** let anybody in: it only opens the **digital carnet** of the member.
3. The digital carnet shows a second QR that the page renews every 10 seconds. It contains `http://<ip>:8765/a/<LIVE CODE>`. The live code is `HMAC-SHA256(secret key, document + time window)` cut to 10 letters and digits (`AccessTokens.liveTokenFor`); the window is 30 seconds long, and the server accepts the current and the previous one.
4. Reception scans the live QR (a phone camera or any scanner that opens the address). `PhoneAccess.scan` finds the member that owns the code and calls `AccessService.processMember`, which applies the same rules as the typed document.
5. A live code works once. If the same code arrives twice in 10 seconds (a phone may send a scan twice) the first answer is returned; later, the same code is refused ("ya se usó"). The fixed code of a printed carnet is refused when it is used to enter.

Why: a fixed QR can be photographed or forwarded and works forever. A screenshot of the live code stops working in less than a minute and can be used only once.

## Routes

| Route | What it does |
|---|---|
| `/ping` | Connection test: open it from the phone to check that it reaches the computer |
| `/c/FIXED` | The digital carnet: name, live QR and a link to the portal |
| `/q/FIXED` | The picture (SVG) of the live QR of this moment; the digital carnet asks for it every 10 seconds |
| `/a/LIVE` | Registers the entry or exit and answers with the result page |
| `/m/FIXED` | The portal of the member (routine, progress, best hours) |
| `/logo.png` | NextFit logo for the result page |
| `/photo/FIXED` | Photo of the member for the result page, only when the code is valid |

## Your day and your summary

After a granted scan the result page adds a block under the answer (`VisitSummaryService`, drawn by `VisitSummaryHtml`):

- **Entry, "Tu día"**: the day of the routine to train with its exercises, the membership and its days left, and the quietest hours to train.
- **Exit, "Tu resumen"**: how long the member stayed (from the attendance record), the days trained, the streak in weeks, the day of the routine that comes next and the membership.

Nothing is shown when the access is denied. The data comes from the member portal (`MemberPortal`), so the portal and the summary never disagree.

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
4. Show the carnet again (or save the PDF again) and scan that one, then open the digital carnet again.

The QR of a carnet contains the address of the computer, so a carnet printed on another network has to be printed
again after changing network. Typing the document in the access screen always works, with or without network.

## Limits

The server is only for the local network. It is not meant to be exposed to the internet.
A cloud server would be the next step.
