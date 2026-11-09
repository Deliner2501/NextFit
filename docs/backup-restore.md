# Backup and restore

The **Respaldo** module is only for the administrator. It protects the data of the gym against a mistake, a broken
file or a computer that stops working.

## What a backup holds

One zip file with:

- every `.csv` file of the `data/` folder (members, payments, users, routines...);
- `access.key`, the secret of the carnet QR codes (without it, the printed carnets stop working);
- the pictures of the exercises in `data/images/`.

Anything else that happens to be in the folder is left out.

## Automatic backup

When NextFit starts it makes **one backup per day** (if none was made yet today) and keeps the **last 14**. The files
are named `nextfit-respaldo-YYYYMMDD-HHmmss.zip` and live in the `backups/` folder next to `data/`. If the backup
fails, the application still opens and writes the reason in the console.

## In the screen

| Button | What it does |
|---|---|
| **+ Crear respaldo ahora** | Makes a backup right now. |
| **Restaurar seleccionado** | Restores the backup chosen in the table. |
| **Restaurar desde archivo** | Restores a zip from anywhere (a flash drive, an e-mail attachment). |
| **Guardar copia en…** | Copies the chosen backup to another folder or drive. |

To restore, NextFit asks for confirmation, saves a **safety copy of the current data**
(`nextfit-antes-de-restaurar-…zip`, shown in the same table) and then replaces the files. When it finishes it asks you
to **close and open the application again**, so every screen reads the restored data.

Every backup made by hand and every restore is written in the **audit log** (see [audit-and-security.md](audit-and-security.md)),
with the name of the administrator who did it.

## Safety rules

The restore refuses a zip when:

- it is not a zip, or it does not contain `users.csv` (so it is not a NextFit backup);
- it holds a file that a backup never contains, or a path that goes outside the data folder (for example `../x.csv`);
- it is larger than 1 GB when unpacked.

The files are first unpacked to a temporary folder and only then moved into `data/`, so a failure in the middle leaves
the safety copy to go back to.

## Good practice

- Copy a backup to a flash drive or the cloud at the end of the week: the `backups/` folder is on the same computer as
  the data.
- Do not share a backup: it has the personal data of the members and the access key.
- `backups/` is ignored by Git and must never be committed.

## How it is built

| Part | Class | Job |
|---|---|---|
| Rules | `BackupService` | Creates, lists, prunes and restores backups. It only works with folders and files, and tells the audit log what was done. |
| Data | `BackupInfo` | One backup file: path, date and size. |
| Screen | `BackupModule` | Buttons, table and messages. |
| Start | `Main` | Calls `backupIfNoneToday(14)` before building the application. |

The date of a backup is read from its file name (`nextfit-respaldo-YYYYMMDD-HHmmss.zip`), so copying the file does not
change it. The tests are in `BackupServiceTest`: contents of a backup, restoring, zip-slip, invalid zips, one backup per
day, the date taken from the name and pruning.

## When the CSV files are replaced by a database

The service will keep the same idea but export the database instead of copying files. This document will be updated
then.
