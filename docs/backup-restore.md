# Backup and restore

The **Respaldo** module is only for the administrator. It protects the data of the gym against a mistake, a broken
file or a computer that stops working.

## What a backup holds

One zip file with:

- the active data in CSV format, including members, payments, users, routines, member password hashes and face templates
- `access.key`, the signing key for the carnet QR codes
- the local exercise images in `data/images/` and member photos in `data/photos/`

In Oracle mode, NextFit exports the database records from a single transaction and includes the shared carnet key. In CSV mode, it copies the existing files in `data/`.

## Automatic backup

When NextFit starts it makes **one backup per day per storage mode** and keeps the **last 14 for that mode**. The files live in `backups/`. A CSV backup is named `nextfit-respaldo-YYYYMMDD-HHmmss.zip` and an Oracle export is named `nextfit-respaldo-oracle-YYYYMMDD-HHmmss.zip`. If the backup fails, the application still opens and writes the reason in the console.

## In the screen

| Button | What it does |
|---|---|
| **+ Crear respaldo ahora** | Makes a backup right now. |
| **Restaurar seleccionado** | Restores the backup chosen in the table. |
| **Restaurar desde archivo** | Restores a zip from anywhere (a flash drive, an e-mail attachment). |
| **Guardar copia en…** | Copies the chosen backup to another folder or drive. |

To recover either ZIP as local files, close NextFit, set `db.enabled=false` in `nextfit.properties`, reopen the app and choose **Restaurar desde archivo**. Use the local administrator account to sign in. The restore buttons are available in CSV mode only.

NextFit asks for confirmation, saves a **safety copy of the current data**
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
| Rules | `BackupService` | Creates, lists and prunes ZIPs, restores local files, and records backup events in the audit log. |
| Export | `JdbcCsvBackup`, `CsvSnapshot` | Reads the database from one transaction and writes a set of CSV files for recovery. |
| Data | `BackupInfo` | One backup file: path, date and size. |
| Screen | `BackupModule` | Buttons, table and messages. |
| Start | `Main` | Calls `backupIfNoneToday(14)` before building the application. |

The date of a backup is read from its file name (`nextfit-respaldo-YYYYMMDD-HHmmss.zip`), so copying the file does not
change it. The tests are in `BackupServiceTest`: contents of a backup, restoring, zip-slip, invalid zips, one backup per
day, the date taken from the name and pruning.

## Recovering an Oracle database

To recover the application records into a new, empty Oracle schema, restore the ZIP in CSV mode, close NextFit and follow the initial data load in the [Oracle setup guide](oracle-setup.md). Use the restored `data/access.key` for the first online startup. The CSV upload merges rows by identifier and is intended for a new schema when recovering a backup. For point-in-time recovery of an existing Oracle database, use the database's recovery tools.
