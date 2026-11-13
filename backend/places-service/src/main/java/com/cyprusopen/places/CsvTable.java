package com.cyprusopen.places;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A small comma-separated UTF-8 file with a header line: double quotes, doubled quotes and line breaks inside quoted
 * fields, and an optional byte order mark. Values are stripped; blank lines are skipped.
 */
public final class CsvTable {
    private CsvTable() {}

    /** Rows keyed by header name; throws {@link IllegalArgumentException} when a required column is missing. */
    public static List<Map<String, String>> read(byte[] csv, List<String> requiredColumns) {
        var records = records(new String(csv, StandardCharsets.UTF_8).replace("﻿", ""));
        if (records.isEmpty()) throw new IllegalArgumentException("Empty CSV file");
        var header = records.getFirst().stream().map(String::strip).toList();
        for (var column : requiredColumns) {
            if (!header.contains(column)) throw new IllegalArgumentException("CSV without column " + column);
        }
        var rows = new ArrayList<Map<String, String>>();
        for (var record : records.subList(1, records.size())) {
            if (record.stream().allMatch(String::isBlank)) continue;
            var row = new HashMap<String, String>();
            for (int c = 0; c < Math.min(header.size(), record.size()); c++) row.put(header.get(c), record.get(c).strip());
            rows.add(row);
        }
        return rows;
    }

    private static List<List<String>> records(String text) {
        var records = new ArrayList<List<String>>();
        var record = new ArrayList<String>();
        var field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                record.add(field.toString());
                field.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                record.add(field.toString());
                field.setLength(0);
                records.add(record);
                record = new ArrayList<>();
            } else {
                field.append(c);
            }
        }
        if (field.length() > 0 || !record.isEmpty()) {
            record.add(field.toString());
            records.add(record);
        }
        return records;
    }
}
