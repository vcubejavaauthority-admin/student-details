package com.Vcube_AdminPortal.studentDetails.excelSheetCodes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.Vcube_AdminPortal.studentDetails.utlity.GoogleSheetsUtil;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

public class StudentsRollNosApp {

	public static Map<String, String> getStudentsRollNosWithNameByBatchNo(String batch) {
		Map<String, String> rollNosWithNames = new LinkedHashMap<>();

		try {
			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			String spreadsheetId = BatchsListApp.getSheetIdbyBatchNo(batch);
			String sheetName = "Student-Info";

			String dataRange = sheetName + "!A2:B1000";
			ValueRange dataResponse = sheetsService.spreadsheets().values().get(spreadsheetId, dataRange).execute();
			List<List<Object>> rows = dataResponse.getValues();
			if (rows == null) {
				rows = new ArrayList<>();
			}

			for (List<Object> row : rows) {
				if (row == null || row.size() < 2) {
					continue;
				}
				rollNosWithNames.put(row.get(0).toString().trim(), row.get(1).toString().trim());
			}

			return rollNosWithNames;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return new LinkedHashMap<>();
	}

	public static Map<String, String> getStudentsStatusByBatchNo(String batch) {
		Map<String, String> rollNosWithStatus = new LinkedHashMap<>();

		try {
			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			String spreadsheetId = BatchsListApp.getSheetIdbyBatchNo(batch);
			String sheetName = "Student-Info";

			// Column F is index 5
			String dataRange = sheetName + "!A2:F1000";
			ValueRange dataResponse = sheetsService.spreadsheets().values().get(spreadsheetId, dataRange).execute();
			List<List<Object>> rows = dataResponse.getValues();
			if (rows == null) {
				rows = new ArrayList<>();
			}

			for (List<Object> row : rows) {
				if (row == null || row.isEmpty()) {
					continue;
				}
				String rollNo = row.get(0).toString().trim();
				String status = row.size() > 5 ? row.get(5).toString().trim() : "active";
				if (status.isEmpty())
					status = "active";
				rollNosWithStatus.put(rollNo, status);
			}

			return rollNosWithStatus;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return new LinkedHashMap<>();
	}

}
