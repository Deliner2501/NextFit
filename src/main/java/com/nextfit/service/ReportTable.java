package com.nextfit.service;

import java.util.List;

/**
 * A report as plain data: a title, the columns and the rows. The services build
 * it and the exporters (Excel, PDF) draw it, so adding a report never touches a
 * file format and adding a format never touches a report.
 *
 * @param title   the title of the report, also the name of the Excel sheet
 * @param headers the column names
 * @param types   how each column is shown, one per header
 * @param rows    the rows; each cell is a {@link String} or a {@link Long} matching its column type
 */
public record ReportTable(String title, List<String> headers, List<ColumnType> types, List<List<Object>> rows) {

    /** Checks that every row has one cell per column. */
    public ReportTable {
        if (headers.size() != types.size()) {
            throw new IllegalArgumentException("Cada columna necesita su tipo");
        }
        for (List<Object> row : rows) {
            if (row.size() != headers.size()) {
                throw new IllegalArgumentException("Cada fila debe tener " + headers.size() + " celdas");
            }
        }
    }
}
