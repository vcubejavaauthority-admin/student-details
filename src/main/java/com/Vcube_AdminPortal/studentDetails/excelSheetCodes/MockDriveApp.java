package com.Vcube_AdminPortal.studentDetails.excelSheetCodes;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.Vcube_AdminPortal.studentDetails.model.MockDriveModel;
import com.Vcube_AdminPortal.studentDetails.utlity.GoogleSheetsUtil;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.ValueRange;

@Service
public class MockDriveApp {

    private static final String SPREADSHEET_ID = "144a1L3p0lKLOQh1p9nYcxI8PEf57MOdxy57ccINyK_g";
    private static final String SHEET_NAME = "Mock-Drive";

    public boolean saveFeedback(MockDriveModel model) {
        try {
            Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
            
            // 1) Get current row count to determine S/No
            String range = SHEET_NAME + "!A:A";
            ValueRange response = sheetsService.spreadsheets().values()
                    .get(SPREADSHEET_ID, range)
                    .execute();
            
            List<List<Object>> values = response.getValues();
            int nextSNo = 1;
            if (values != null && values.size() > 1) {
                nextSNo = values.size(); // Headers are at row 1 & 2 in the image
                // Adjusting based on image: row 1 is Rounds, row 2 is Headers.
                // If there are 2 rows (headers), next student is S/No 1 at row 3.
                // If there are 3 rows (headers + 1 student), next student is S/No 2 at row 4.
                nextSNo = values.size() - 1; // Assuming 2 header rows
            }

            // 2) Prepare row data
            List<Object> row = new ArrayList<>();
            row.add(String.valueOf(nextSNo));
            row.add(model.getStudentName());
            row.add(model.getBatchNoRollNo());
            row.add(model.getEmailId());
            row.add(model.getAptitudeRound1());
            row.add(model.getTechnicalRound1());
            row.add(model.getJamRatingRound2());
            row.add(model.getTechnicalInterviewScoreRound3());
            row.add(model.getTechnicalInterviewFeedbackRound3());
            row.add(model.getHrInterviewScoreRound4());
            row.add(model.getHrInterviewFeedbackRound4());
            row.add(model.getOverall());
            row.add(""); // Notify-Email column (Column M)

            // 3) Append to sheet
            List<List<Object>> valuesToAppend = new ArrayList<>();
            valuesToAppend.add(row);

            ValueRange body = new ValueRange().setValues(valuesToAppend);
            sheetsService.spreadsheets().values()
                    .append(SPREADSHEET_ID, SHEET_NAME + "!A3", body)
                    .setValueInputOption("RAW")
                    .setInsertDataOption("INSERT_ROWS")
                    .execute();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public List<String> getSheetNames() {
        List<String> sheetNames = new ArrayList<>();
        try {
            Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
            Spreadsheet spreadsheet = sheetsService.spreadsheets().get(SPREADSHEET_ID).execute();
            List<Sheet> sheets = spreadsheet.getSheets();
            for (Sheet sheet : sheets) {
                sheetNames.add(sheet.getProperties().getTitle());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sheetNames;
    }

    public List<MockDriveModel> getSheetData(String sheetName) {
        List<MockDriveModel> data = new ArrayList<>();
        try {
            Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
            // Assuming headers are at rows 1 & 2, data starts at row 3
            String range = sheetName + "!A3:M"; 
            ValueRange response = sheetsService.spreadsheets().values()
                    .get(SPREADSHEET_ID, range)
                    .execute();
            List<List<Object>> values = response.getValues();

            if (values != null) {
                for (List<Object> row : values) {
                    MockDriveModel model = new MockDriveModel();
                    model.setSNo(getVal(row, 0));
                    model.setStudentName(getVal(row, 1));
                    model.setBatchNoRollNo(getVal(row, 2));
                    model.setEmailId(getVal(row, 3));
                    model.setAptitudeRound1(getVal(row, 4));
                    model.setTechnicalRound1(getVal(row, 5));
                    model.setJamRatingRound2(getVal(row, 6));
                    model.setTechnicalInterviewScoreRound3(getVal(row, 7));
                    model.setTechnicalInterviewFeedbackRound3(getVal(row, 8));
                    model.setHrInterviewScoreRound4(getVal(row, 9));
                    model.setHrInterviewFeedbackRound4(getVal(row, 10));
                    model.setOverall(getVal(row, 11));
                    model.setNotifyEmail(getVal(row, 12));
                    data.add(model);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    public boolean updateNotifyStatus(String sheetName, String emailId, String status) {
        try {
            Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
            
            // 1) Find the row index for the given email
            String range = sheetName + "!D:D"; // Email column
            ValueRange response = sheetsService.spreadsheets().values()
                    .get(SPREADSHEET_ID, range)
                    .execute();
            List<List<Object>> values = response.getValues();
            
            int rowIndex = -1;
            if (values != null) {
                for (int i = 0; i < values.size(); i++) {
                    if (!values.get(i).isEmpty() && values.get(i).get(0).toString().equalsIgnoreCase(emailId)) {
                        rowIndex = i + 1; // 1-indexed
                        // break; // Continue to find the last one if duplicates? Usually first one is enough or specific row is better.
                    }
                }
            }

            if (rowIndex != -1) {
                // 2) Update column M (Notify-Email) at the found row
                List<Object> updateRow = new ArrayList<>();
                updateRow.add(status);
                List<List<Object>> updateValues = new ArrayList<>();
                updateValues.add(updateRow);
                
                ValueRange body = new ValueRange().setValues(updateValues);
                sheetsService.spreadsheets().values()
                        .update(SPREADSHEET_ID, sheetName + "!M" + rowIndex, body)
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
        return (index < row.size() && row.get(index) != null) ? row.get(index).toString() : "";
    }
}
