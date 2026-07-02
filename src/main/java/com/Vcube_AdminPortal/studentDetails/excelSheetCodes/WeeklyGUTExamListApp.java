package com.Vcube_AdminPortal.studentDetails.excelSheetCodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.Vcube_AdminPortal.studentDetails.model.WeeklyGUTExamRecord;
import com.Vcube_AdminPortal.studentDetails.utlity.GoogleSheetsUtil;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.springframework.beans.factory.annotation.Value;

@Service
public class WeeklyGUTExamListApp {

    @Value("${google.sheet.weekly-gut-exam-id}")
    private String masterSpreadsheetId;

    private static final String MASTER_SHEET_NAME = "GUT-Exam-List";

    public Map<String, String> getYearToSheetIdMap() {
        Map<String, String> yearMap = new LinkedHashMap<>();
        try {
            Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
            String range = MASTER_SHEET_NAME + "!A1:C";
            ValueRange response = sheetsService.spreadsheets().values().get(masterSpreadsheetId, range).execute();


            List<List<Object>> values = response.getValues();
            if (values == null || values.isEmpty()) {
                return yearMap;
            }

            for (int i = 1; i < values.size(); i++) {
                List<Object> row = values.get(i);
                if (row != null && row.size() >= 2) {
                    String year = row.get(0).toString().trim();
                    String sheetId = row.get(1).toString().trim();
                    String status = (row.size() > 2 && row.get(2) != null) ? row.get(2).toString().trim() : "active";
                    
                    if (!year.isBlank() && !sheetId.isBlank() && status.equalsIgnoreCase("active")) {
                        yearMap.put(year, sheetId);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return yearMap;
    }

    public List<String> getSheetNamesBySpreadsheetId(String spreadsheetId) {
        List<String> sheetNames = new ArrayList<>();
        try {
            Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
            Spreadsheet spreadsheet = sheetsService.spreadsheets().get(spreadsheetId).execute();
            for (Sheet sheet : spreadsheet.getSheets()) {
                sheetNames.add(sheet.getProperties().getTitle());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sheetNames;
    }

    public List<WeeklyGUTExamRecord> getWeeklyGUTExamListData(String spreadsheetId, String sheetName) {
        List<WeeklyGUTExamRecord> records = new ArrayList<>();
        try {
            Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
            String range = "'" + sheetName + "'!A1:ZZ1000";
            ValueRange response = sheetsService.spreadsheets().values().get(spreadsheetId, range).execute();
            List<List<Object>> values = response.getValues();
            if (values == null || values.isEmpty()) {
                return records;
            }

            // Dynamically discover indices based on headers in row 0
            List<Object> headers = values.get(0);
            int snoIdx = 0;
            int batchRollIdx = 1;
            int nameIdx = 2;
            int emailIdx = 3;
            int marksIdx = 4;
            int statusIdx = 5;
            int notifyStatusIdx = 6;

            boolean foundNotifyStatus = false;
            for (int i = 0; i < headers.size(); i++) {
                String h = headers.get(i).toString().trim().toLowerCase();
                if (h.contains("s/n") || h.contains("s.n") || h.equals("sno")) {
                    snoIdx = i;
                } else if (h.contains("batch") && h.contains("roll")) {
                    batchRollIdx = i;
                } else if (h.equals("name")) {
                    nameIdx = i;
                } else if (h.equals("email")) {
                    emailIdx = i;
                } else if (h.contains("marks")) {
                    marksIdx = i;
                } else if (h.equals("status")) {
                    statusIdx = i;
                } else if (h.contains("notify") && h.contains("status")) {
                    notifyStatusIdx = i;
                    foundNotifyStatus = true;
                }
            }

            if (!foundNotifyStatus) {
                notifyStatusIdx = Math.max(statusIdx + 1, headers.size());
            }

            for (int r = 1; r < values.size(); r++) {
                List<Object> row = values.get(r);
                if (row == null || row.isEmpty()) {
                    continue;
                }
                
                String sno = getVal(row, snoIdx);
                String batchRoll = getVal(row, batchRollIdx);
                String name = getVal(row, nameIdx);
                String email = getVal(row, emailIdx);
                String marks = getVal(row, marksIdx);
                String statusVal = getVal(row, statusIdx);
                String notifyStatus = getVal(row, notifyStatusIdx);

                if (notifyStatus.isEmpty()) {
                    notifyStatus = "Pending";
                }
                if (statusVal.isEmpty()) {
                    statusVal = "not-shortlist";
                }

                if (!batchRoll.isBlank() || !name.isBlank()) {
                    WeeklyGUTExamRecord record = new WeeklyGUTExamRecord();
                    record.setSerialNo(sno);
                    record.setBatchAndRollNo(batchRoll);
                    record.setName(name);
                    record.setEmail(email);
                    record.setMarks(marks);
                    record.setStarRating(calculateStars(marks));
                    record.setStatus(statusVal);
                    record.setNotificationStatus(notifyStatus);
                    records.add(record);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return records;
    }

    public boolean updateGUTExamNotifyStatus(String spreadsheetId, String sheetName, String email, String status) {
        try {
            Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
            
            // 1) Find the header row to discover columns and indices
            String headerRange = "'" + sheetName + "'!A1:ZZ1";
            ValueRange headerResponse = sheetsService.spreadsheets().values().get(spreadsheetId, headerRange).execute();
            List<List<Object>> headerValues = headerResponse.getValues();
            
            int emailIdx = 3; // default column D (index 3)
            int statusIdx = 5; // default column F (index 5)
            int notifyStatusIdx = 6; // default column G (index 6)
            
            if (headerValues != null && !headerValues.isEmpty()) {
                List<Object> headers = headerValues.get(0);
                boolean foundNotify = false;
                for (int i = 0; i < headers.size(); i++) {
                    String h = headers.get(i).toString().trim().toLowerCase();
                    if (h.equals("email")) {
                        emailIdx = i;
                    } else if (h.equals("status")) {
                        statusIdx = i;
                    } else if (h.contains("notify") && h.contains("status")) {
                        notifyStatusIdx = i;
                        foundNotify = true;
                    }
                }
                
                // If notify status column is not found, use statusIdx + 1 or headers.size()
                if (!foundNotify) {
                    notifyStatusIdx = Math.max(statusIdx + 1, headers.size());
                }
            }
            
            // 2) Find the row index for the given email
            String emailColLetter = GoogleSheetsUtil.indexToColumn(emailIdx);
            String range = "'" + sheetName + "'!" + emailColLetter + ":" + emailColLetter;
            ValueRange response = sheetsService.spreadsheets().values()
                    .get(spreadsheetId, range)
                    .execute();
            List<List<Object>> values = response.getValues();
            
            int rowIndex = -1;
            if (values != null) {
                for (int i = 0; i < values.size(); i++) {
                    if (!values.get(i).isEmpty() && values.get(i).get(0).toString().equalsIgnoreCase(email)) {
                        rowIndex = i + 1; // 1-indexed
                        break;
                    }
                }
            }

            if (rowIndex != -1) {
                String notifyStatusColLetter = GoogleSheetsUtil.indexToColumn(notifyStatusIdx);
                
                // If column index is out of bounds for current headers, write "Notify Status" header first
                if (headerValues != null && !headerValues.isEmpty() && notifyStatusIdx >= headerValues.get(0).size()) {
                    ValueRange headerBody = new ValueRange().setValues(Collections.singletonList(Collections.singletonList("Notify Status")));
                    sheetsService.spreadsheets().values().update(spreadsheetId, "'" + sheetName + "'!" + notifyStatusColLetter + "1", headerBody)
                            .setValueInputOption("RAW").execute();
                }

                // Update column notifyStatusIdx at the found row
                List<Object> updateRow = new ArrayList<>();
                updateRow.add(status);
                List<List<Object>> updateValues = new ArrayList<>();
                updateValues.add(updateRow);
                
                ValueRange body = new ValueRange().setValues(updateValues);
                sheetsService.spreadsheets().values()
                        .update(spreadsheetId, "'" + sheetName + "'!" + notifyStatusColLetter + rowIndex, body)
                        .setValueInputOption("RAW")
                        .execute();
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private String getVal(List<Object> row, int index) {
        return (index < row.size() && row.get(index) != null) ? row.get(index).toString().trim() : "";
    }

    public String calculateStars(String marksStr) {
        if (marksStr == null || marksStr.trim().isEmpty()) {
            return "☆☆☆☆☆";
        }
        try {
            double marks = Double.parseDouble(marksStr.trim());
            int filled = (int) Math.round((marks / 50.0) * 5.0);
            if (filled > 5) filled = 5;
            if (filled < 0) filled = 0;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 5; i++) {
                if (i < filled) {
                    sb.append("⭐");
                } else {
                    sb.append("☆");
                }
            }
            return sb.toString();
        } catch (NumberFormatException e) {
            return "☆☆☆☆☆";
        }
    }
}
