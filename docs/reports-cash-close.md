# Reports and cash close

The **Reportes** module has two tabs. Both are for the administrator and the reception.

## Cash close

Pick a day (today by default) to see:

- the total collected, the number of payments, the discounts and the cash that should be in the drawer;
- the money of each payment method, with a bar that shows its share;
- a **cash count**: type the cash you counted and the screen tells you if the drawer is balanced, has a surplus or is short;
- the list of payments of the day.

**Exportar PDF** writes a one or two page document with the same numbers and two lines for the signatures of the
cashier and the administrator. **Excel** writes a workbook with a summary sheet and a sheet with the payments.
The cash you typed travels to both files. It is not stored in the data folder.

## Reports

Choose **Pagos**, **Asistencia** or **Socios**, set the days when it applies, check the table and export it as PDF or
Excel. **Excel con todo** puts the three reports in one workbook, each in its own sheet.

## How it is built

| Part | Class | Job |
|---|---|---|
| Data | `CashCloseService`, `ReportService` | Build `CashClose` and `ReportTable` from the payments, members and attendance. |
| Files | `PdfExporter`, `XlsxWriter` | Draw any table as PDF or Excel. They use the JDK only, so the project has no new dependency. |
| Screens | `CashClosePane`, `ReportsPane`, `FileExport` | Show the data, ask where to save and report the result. |

The PDF uses the standard Helvetica fonts, so accents and the letter ñ print correctly without embedding fonts.
