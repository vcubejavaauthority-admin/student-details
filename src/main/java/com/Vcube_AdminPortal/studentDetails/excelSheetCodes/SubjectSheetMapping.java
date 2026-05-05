package com.Vcube_AdminPortal.studentDetails.excelSheetCodes;

import java.util.HashMap;
import java.util.Map;

public class SubjectSheetMapping {

    private static final Map<String, String> SUBJECT_SHEET_MAP = new HashMap<>();

    static {
        // TODO: Replace with actual Spreadsheet IDs from the user
        SUBJECT_SHEET_MAP.put("Core Java", "1E8StE3mCKU_xjKMKMJ1ipk1Ese0FCYJF0LvGlyCpNPk"); // Example using Master
                                                                                            // sheet for now
        SUBJECT_SHEET_MAP.put("MySQL", "");
        SUBJECT_SHEET_MAP.put("HTML", "");
        SUBJECT_SHEET_MAP.put("CSS", "");
        SUBJECT_SHEET_MAP.put("JavaScript", "");
        SUBJECT_SHEET_MAP.put("Servlets", "");
        SUBJECT_SHEET_MAP.put("Spring", "");
        SUBJECT_SHEET_MAP.put("Spring Boot", "");
        SUBJECT_SHEET_MAP.put("Microservices", "");
        SUBJECT_SHEET_MAP.put("React", "");
    }

    public static String getSpreadsheetId(String subject) {
        return SUBJECT_SHEET_MAP.get(subject);
    }

    public static Map<String, String> getAllMappings() {
        return SUBJECT_SHEET_MAP;
    }
}
