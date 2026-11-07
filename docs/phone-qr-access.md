# Phone QR access

The QR of a member carnet opens a small web page served by the reception
computer. Scanning it with the camera of any phone registers the entry or the
exit and shows a full-screen answer (green entry, blue exit, red denied). The
kiosk screen of NextFit reacts at the same time.

## How it works

1. `Main` starts `AccessServer` (JDK `com.sun.net.httpserver`) on port 8765, or the next free one.
2. The carnet QR contains `http://<ip of the computer>:8765/a/<CODE>`.
3. `CODE` is `HMAC-SHA256(secret key, document)` cut to 10 letters and digits (`AccessTokens`). It is not the document number and it is not stored anywhere.
4. `PhoneAccess.scan` finds the member that owns the code and calls `AccessService.processMember`, which applies the same rules as the USB reader and the typed document.
5. If the same code arrives twice in 10 seconds, the first answer is returned and nothing is toggled.

## Routes

| Route | What it does |
|---|---|
| `/ping` | Connection test: open it from the phone to check that it reaches the computer |
| `/a/CODE` | Registers the entry or exit and answers with the result page |
| `/photo/CODE` | Photo of the member for the result page, only when the code is valid |

## Requirements

- The phone and the computer must be in the same Wi-Fi network. If the network isolates devices, turn on the hotspot of the phone and connect the computer to it.
- The first time, Windows asks if Java may receive connections: allow it on private networks.
- The secret key is in `data/access.key` (created automatically, not committed). Deleting it invalidates every printed carnet.

## Limits

The server is only for the local network. It is not meant to be exposed to the internet.
A cloud server would be the next step.
