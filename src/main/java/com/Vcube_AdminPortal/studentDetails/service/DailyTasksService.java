package com.Vcube_AdminPortal.studentDetails.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.SubjectSheetMapping;
import com.Vcube_AdminPortal.studentDetails.utlity.GoogleSheetsUtil;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;

@Service
public class DailyTasksService {

    /**
     * Fetches all sheet titles (topics) for a given subject.
     */
    public List<String> getTopics(String subject) throws IOException, GeneralSecurityException {
        String spreadsheetId = SubjectSheetMapping.getSpreadsheetId(subject);
        if (spreadsheetId == null || spreadsheetId.isBlank()) {
            return new ArrayList<>();
        }

        Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
        Spreadsheet spreadsheet = sheetsService.spreadsheets().get(spreadsheetId).execute();
        
        return spreadsheet.getSheets().stream()
                .map(s -> s.getProperties().getTitle())
                .collect(Collectors.toList());
    }

    /**
     * Fetches questions for a specific topic (sheet) and level (column).
     * Level mapping: Basics (Col A), Intermediate (Col B), Advanced (Col C)
     */
    public List<String> getQuestions(String subject, String topic, String level) throws IOException, GeneralSecurityException {
        String spreadsheetId = SubjectSheetMapping.getSpreadsheetId(subject);
        if (spreadsheetId == null || spreadsheetId.isBlank()) {
            return new ArrayList<>();
        }

        Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
        
        // We fetch A1:E1000 to cover all levels and potential extra columns
        String range = topic + "!A1:E1000";
        ValueRange response = sheetsService.spreadsheets().values().get(spreadsheetId, range).execute();
        List<List<Object>> values = response.getValues();

        List<String> questions = new ArrayList<>();
        if (values == null || values.isEmpty()) {
            return questions;
        }

        // Determine column index based on level
        int colIndex = -1; 
        List<Object> headers = values.get(0);
        for (int i = 0; i < headers.size(); i++) {
            Object header = headers.get(i);
            if (header != null && isLevelMatch(header.toString().trim(), level)) {
                colIndex = i;
                break;
            }
        }

        // If no match found via header, use defaults
        if (colIndex == -1) {
            if (level.equalsIgnoreCase("Basics")) colIndex = 0;
            else if (level.equalsIgnoreCase("Intermediate")) colIndex = 1;
            else if (level.equalsIgnoreCase("Advanced")) colIndex = 2;
            else colIndex = 0; // Final fallback
        }

        // Collect questions from that column (skipping header)
        for (int i = 1; i < values.size(); i++) {
            List<Object> row = values.get(i);
            if (row != null && row.size() > colIndex) {
                Object cellValue = row.get(colIndex);
                if (cellValue != null && !cellValue.toString().isBlank()) {
                    questions.add(cellValue.toString().trim());
                }
            }
        }

        return questions;
    }

    /**
     * Checks if a sheet header matches the requested level, accounting for common variations and typos.
     */
    private boolean isLevelMatch(String header, String requestedLevel) {
        header = header.toLowerCase();
        requestedLevel = requestedLevel.toLowerCase();

        if (requestedLevel.contains("basic")) {
            return header.contains("basic");
        }
        if (requestedLevel.contains("intermediate")) {
            return header.contains("intermediate") || header.contains("intermidiate");
        }
        if (requestedLevel.contains("advanced")) {
            return header.contains("advance"); // Matches both "Advance" and "Advanced"
        }
        return header.equalsIgnoreCase(requestedLevel);
    }
}
