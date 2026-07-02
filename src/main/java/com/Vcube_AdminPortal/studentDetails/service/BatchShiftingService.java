package com.Vcube_AdminPortal.studentDetails.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.BatchsListApp;
import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.StudentDetails;
import com.Vcube_AdminPortal.studentDetails.model.StudentModel;
import com.Vcube_AdminPortal.studentDetails.utlity.GoogleSheetsUtil;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

@Service
public class BatchShiftingService {

	@Autowired
	private StudentDetails studentDetails;

	public boolean shiftStudent(String presentBatch, String rollNo, String targetBatch) {
		try {
			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			
			String presentSpreadsheetId = BatchsListApp.getSheetIdbyBatchNo(presentBatch);
			String targetSpreadsheetId = BatchsListApp.getSheetIdbyBatchNo(targetBatch);

			if (presentSpreadsheetId == null || targetSpreadsheetId == null) {
				System.err.println("Invalid batch IDs.");
				return false;
			}

			// 1. Fetch full details from present batch
			StudentModel stm = studentDetails.getStudentsDetailsByBatchNoAndRollNo(presentBatch, rollNo);
			if (stm == null || stm.getRollNo() == null || stm.getRollNo().isBlank()) {
				System.err.println("Student not found in present batch.");
				return false;
			}

			// The Photo URL returned by getStudentsDetailsByBatchNoAndRollNo includes google domains.
			// Let's extract the raw ID if we want, or just store the full link. Wait, StudentDetails appends the prefix.
			// Let's fetch raw photo url from Student-Info directly to be safe, or just append what we have.
			// Let's get the raw data from Student-Info to copy exactly.
			String sheetName = "Student-Info";
			String dataRange = sheetName + "!A2:Z1000";
			ValueRange dataResponse = sheetsService.spreadsheets().values().get(presentSpreadsheetId, dataRange).execute();
			List<List<Object>> rows = dataResponse.getValues();
			
			int rowIndex = -1;
			List<Object> targetRow = null;
			if (rows != null) {
				for (int i = 0; i < rows.size(); i++) {
					List<Object> row = rows.get(i);
					if (row != null && !row.isEmpty() && row.get(0).toString().trim().equalsIgnoreCase(rollNo)) {
						targetRow = row;
						rowIndex = i + 2; // +2 because range starts at A2
						break;
					}
				}
			}

			if (targetRow == null) {
				System.err.println("Student not found in Student-Info.");
				return false;
			}

			String rawPhotoUrl = targetRow.size() > 2 && targetRow.get(2) != null ? targetRow.get(2).toString().trim() : "";
			String mobileStr = targetRow.size() > 3 && targetRow.get(3) != null ? targetRow.get(3).toString().trim() : "";
			String emailStr = targetRow.size() > 4 && targetRow.get(4) != null ? targetRow.get(4).toString().trim() : "";
			String nameStr = stm.getName();

			// 2. Update status in present batch Student-Info to "shifted"
			if (rowIndex != -1) {
				String statusCellRange = sheetName + "!F" + rowIndex; // Column F is Status
				ValueRange statusBody = new ValueRange().setValues(Collections.singletonList(Collections.singletonList("shifted")));
				sheetsService.spreadsheets().values().update(presentSpreadsheetId, statusCellRange, statusBody)
						.setValueInputOption("USER_ENTERED").execute();
			}

			// 3. Append to target batch sheets
			
			// Full-Details: [Roll No, Name, Photo-URL, Mobile, "", E-mail]
			appendRow(sheetsService, targetSpreadsheetId, "Full-Details", 
					Arrays.asList(rollNo, nameStr, rawPhotoUrl, mobileStr, "", emailStr));
					
			// Student-Info: [Roll No, Name, Photo-URL, Mobile, E-mail, "add-on"]
			appendRow(sheetsService, targetSpreadsheetId, "Student-Info", 
					Arrays.asList(rollNo, nameStr, rawPhotoUrl, mobileStr, emailStr, "add-on"));
					
			// Attendance: [Roll No, Name, Mobile, E-mail]
			appendRow(sheetsService, targetSpreadsheetId, "Attendance", 
					Arrays.asList(rollNo, nameStr, mobileStr, emailStr));
			
			// Rest: [Roll No, Name]
			List<String> simpleSheets = Arrays.asList("Exam", "Mock", "Case-Study", "Projects", "Mock-Exams", "GUT-Exams", "Placement-Info");
			for (String sName : simpleSheets) {
				appendRow(sheetsService, targetSpreadsheetId, sName, Arrays.asList(rollNo, nameStr));
			}

			return true;
		} catch (IOException | GeneralSecurityException e) {
			e.printStackTrace();
			return false;
		}
	}

	private void appendRow(Sheets sheetsService, String spreadsheetId, String sheetName, List<Object> values) {
		try {
			String range = sheetName + "!A:A"; // Append to the bottom of the sheet
			ValueRange body = new ValueRange().setValues(Collections.singletonList(values));
			sheetsService.spreadsheets().values().append(spreadsheetId, range, body)
					.setValueInputOption("USER_ENTERED")
					.setInsertDataOption("INSERT_ROWS")
					.execute();
			System.out.println("Appended to " + sheetName + " successfully.");
		} catch (IOException e) {
			System.err.println("Failed to append to " + sheetName + ": " + e.getMessage());
		}
	}
}
