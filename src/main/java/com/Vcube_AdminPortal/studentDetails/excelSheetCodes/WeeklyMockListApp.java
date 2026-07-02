package com.Vcube_AdminPortal.studentDetails.excelSheetCodes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.Vcube_AdminPortal.studentDetails.model.WeeklyMockRecord;
import com.Vcube_AdminPortal.studentDetails.utlity.GoogleSheetsUtil;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.springframework.beans.factory.annotation.Value;

@Service
public class WeeklyMockListApp {

	@Value("${google.sheet.weekly-mock-id}")
	private String masterSpreadsheetId;

	private static final String MASTER_SHEET_NAME = "Mock-List"; // Or Java-Mock-List depending on actual tab name

	public Map<String, String> getYearToSheetIdMap() {
		Map<String, String> yearMap = new LinkedHashMap<>();

		try {
			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			// We read Year (A), Sheet-Id (B), Status (C)
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
					
					// Optional: Check status if column C exists
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

	public List<WeeklyMockRecord> getWeeklyMockListData(String spreadsheetId, String sheetName) {
		List<WeeklyMockRecord> records = new ArrayList<>();
		try {
			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			
			// Columns: A (S/No), B (Batch & Rollno), C (Name), D (Technical), E (Communication), F (Remarks)
			String range = "'" + sheetName + "'!A2:F";
			ValueRange response = sheetsService.spreadsheets().values().get(spreadsheetId, range).execute();
			
			List<List<Object>> values = response.getValues();
			if (values == null || values.isEmpty()) {
				return records;
			}
			
			for (List<Object> row : values) {
				if (row == null || row.isEmpty()) {
					continue;
				}
				String sno = row.size() > 0 && row.get(0) != null ? row.get(0).toString().trim() : "";
				String batchRoll = row.size() > 1 && row.get(1) != null ? row.get(1).toString().trim() : "";
				String name = row.size() > 2 && row.get(2) != null ? row.get(2).toString().trim() : "";
				String technical = row.size() > 3 && row.get(3) != null ? row.get(3).toString().trim() : "";
				String communication = row.size() > 4 && row.get(4) != null ? row.get(4).toString().trim() : "";
				String remarks = row.size() > 5 && row.get(5) != null ? row.get(5).toString().trim() : "";
				
				if (!batchRoll.isBlank() || !name.isBlank()) {
					WeeklyMockRecord record = new WeeklyMockRecord();
					record.setSerialNo(sno);
					record.setBatchAndRollNo(batchRoll);
					record.setName(name);
					
					// Set legacy marks as a combined string if available
					String combinedMarks = "";
					if (!technical.isEmpty() || !communication.isEmpty()) {
						combinedMarks = (technical.isEmpty() ? "0" : technical) + " / " + (communication.isEmpty() ? "0" : communication);
					}
					record.setMarks(combinedMarks);
					
					record.setTechnical(technical);
					record.setCommunication(communication);
					record.setRemarks(remarks);
					records.add(record);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return records;
	}
}
