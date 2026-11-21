package com.nextfit.service;

/** How a column of a report is shown by the exporters. */
public enum ColumnType {
    /** Plain text, aligned to the left. */
    TEXT,
    /** A whole number of pesos, shown with thousands separators and aligned to the right. */
    MONEY,
    /** A whole number (counts, minutes), aligned to the right. */
    NUMBER
}
