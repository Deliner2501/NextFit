# User manual

How to use NextFit, screen by screen. The names in quotes are the ones shown in the application.

## Start

1. Open NextFit and sign in. On a new installation the users are `admin / admin123`, `recepcion / recepcion123` and
   `entrenador / entrenador123`: **change the passwords** from **Usuarios** before using it for real.
2. The **menu** on the left shows only what your role can use (see the table at the end).
3. The **☰** button next to the title collapses the menu to icons; click it again to open it. NextFit remembers the choice.
4. The **magnifier** button (or **Ctrl+K**) opens the global search: type a member's name, document or phone, or the name
   of a section, and press Enter to open the first result (arrows move the selection, Esc closes). Searches ignore
   capitals and accents.
5. The **sun / moon** button switches between the dark and the light theme. The exit button signs you out.
6. After 15 minutes without activity the session closes by itself, and after 5 wrong passwords a user is blocked for
   5 minutes.

## Panel (dashboard)

The numbers of the day: active memberships, memberships about to expire (7 days or less), today's entries and the income
of the month. The green or red chip beside a number tells whether it is higher or lower than before (income against the
same days of last month, entries against yesterday); rest the mouse on it to see the value it is compared with.
The charts show memberships by status, attendance by weekday and income by month ("Últimos 6 meses" can be changed).

## Socios

A table with photo, document, phone and status. Type in the search box to find a member by name, document or phone.

- **+ Nuevo socio** (administrator and reception): fill in the form, choose a photo with **Elegir foto** and save. The data
  is validated and the message tells what to fix.
- Select a member and press **Editar** to change the record or **Dar de baja** to deactivate it.
- Trainers can look at the list but cannot change it.
- **Carnets** (administrators and reception): choose a member to see the carnet. **Guardar carnet como imagen** saves a PNG, **Guardar carnet en PDF** saves the front and back at credit-card size, and **PDF de todos los socios activos** makes one file with every active member (four per A4 page) to print and cut.

## Membresías y pagos

Four tabs:

- **Venta**: choose the member, plan, start date and payment method. Renewal benefits appear only for eligible returning members. The largest benefit is selected by default. Choose **Sin beneficio** to pay the full price or select another eligible benefit. A sale can use one benefit.
- Birthdays award 50% on the date of birth, once per calendar year. Attendance achievements award 10% to 40% depending on difficulty, once per achievement. The confirmed payment records which benefit was used.
- Only administrators can create, edit, retire and reactivate plans or cancel memberships. Reception can sell existing plans and consult subscriptions and payments.
- **Suscripciones**: every membership with its state (próxima, activa, vencida, cancelada) and the days left; a valid one can be cancelled.
- **Pagos**: history of payments and receipts. The **Correo** column shows whether the receipt email was sent (*Enviado*, *Fallido*, *Pendiente* or *Sin intento*); select a payment and choose **Reintentar correo** to send it again, only after checking the sender's Sent folder. Email setup is in [automatic-email.md](automatic-email.md).
- **Planes**: create, edit and retire membership plans.

## Entrenamiento

- In **Asignación**, filter members with or without an active routine. In **Sesiones**, filter members with or without a session in their history.

Five tabs: **Ejercicios** (catalog with photo, muscle group and difficulty; filters and search), **Rutinas** (days and
exercises with sets, repetitions and rest), **Asignación** (give a routine to a member with a valid membership),
**Sesiones** (log of each training) and **Entrenadores**. In **Rutinas**, **Sugerir rutina** proposes a routine from the member data.

## Control de acceso

- **Dentro ahora** shows current visitors, capacity and available spots. Administrators can change the limit with **Configurar aforo** beside the **Dentro ahora** heading. Visitor cards are ordered from the earliest arrival to the latest. The default is 60. The current setting is saved locally in `data/gym-capacity.txt`. The running reception instance serializes simultaneous admissions. Use a single reception instance to enforce a single gym-wide limit; separate installations do not share the setting or lock. Exits can always be recorded when the gym is full.

- **Dentro ahora**: the list of the members who are inside the gym, with the hour of the entry. The members enter and leave by themselves, with the face (tab **Rostro**) or with the document and a password (tab **Contraseña**). If a member forgot to mark the exit, choose him in the list and press **Registrar salida**; the screen asks for confirmation.
- In the lists of the tabs **Rostro** and **Contraseña**, each member shows a label **Rostro** and/or **Contraseña** when he already registered that way to enter; a member without labels cannot enter by himself yet.
- **Contraseña**: enter with the document and a password the member created himself ([details](member-password.md)). The first time, choose the member on the right, press **Crear contraseña** and let the member type it twice (the staff must not look). To enter or leave, the member types the document and the password on the left. If he forgets it, press **Restablecer contraseña** and he creates a new one. After 5 wrong passwords the document is blocked for 5 minutes.
- **Rostro**: enter with the face, without carnet ([details](face-recognition.md)). To register a member choose them on the right, press **Registrar rostro**, read the consent aloud and accept; the camera takes three pictures looking forward. To let someone in press **Iniciar entrada con rostro**: the member looks at the camera, turns the head to the left and to the right, looks forward and the entry is registered. **Eliminar rostro** deletes the stored face. The first time the model files must be copied to `data/models`.

## Alertas

The alerts of the day, grouped by type: memberships that end in the next days (*Por vencer*), members who pay but stopped coming (*Inactivos*) and birthdays (*Cumpleaños*). Choose an alert to read the message prepared for that member; **Abrir WhatsApp** opens the chat with the text ready and **Copiar mensaje** copies it. If the phone of the member is not valid, the screen says so and you can fix it in the member file.

## Analítica

Peak hour, average stay, renewal rate and members at risk. The heat map shows the busiest hours by weekday (violet is few
people, green is many), and the list at the bottom names the members who stopped coming.

## Reportes

- **Cierre de caja**: pick a day and see the total collected, the payments by method and the cash that should be in the
  drawer. Type the cash you counted to see whether it matches. **Exportar PDF** and **Excel** save the report.
- **Reportes**: choose *Pagos*, *Asistencia* or *Socios*, the dates, and export. **Excel con todo** puts the three in one
  workbook ([details](reports-cash-close.md)).

## Usuarios (administrator)

Create users, edit them, **Restablecer contraseña** and **Desactivar**. You cannot deactivate yourself, and there must
always be one active administrator.

## Auditoría (administrator)

Who signed in, who failed, who was blocked, which users were changed and which backups were made or restored. Use the
filter to look at one kind of event ([details](audit-and-security.md)).

## Respaldo (administrator)

NextFit makes a backup every day. Here you can make one now, restore one, restore from a file or save a copy to a flash
drive ([details](backup-restore.md)). After restoring, close and open NextFit again.

## What each role can open

| Screen | Administrator | Reception | Trainer |
|---|:--:|:--:|:--:|
| Panel | ✅ | ✅ | ❌ |
| Socios | ✅ edit | ✅ edit | 👁️ read |
| Membresías y pagos | ✅ | ✅ | ❌ |
| Entrenamiento | ✅ | ❌ | ✅ |
| Control de acceso | ✅ | ✅ | ❌ |
| Alertas | ✅ | ✅ | ❌ |
| Analítica | ✅ | ✅ | ❌ |
| Reportes | ✅ | ✅ | ❌ |
| Usuarios | ✅ | ❌ | ❌ |
| Auditoría | ✅ | ❌ | ❌ |
| Respaldo | ✅ | ❌ | ❌ |

## Frequent questions

- **I cannot sign in.** Check the user and the password. After 5 failures wait 5 minutes or ask the administrator.
- **A member cannot enter.** The membership may be expired or the member inactive; the screen says the reason.
- **I deleted something by mistake.** Ask the administrator to restore the last backup from **Respaldo**.
- **The phone does not open the QR.** The phone and the computer must be on the same Wi-Fi and the network must allow devices to talk to each other. Public and university networks often do not: use the hotspot of the phone and restart NextFit (see [phone-qr-access.md](phone-qr-access.md)).
