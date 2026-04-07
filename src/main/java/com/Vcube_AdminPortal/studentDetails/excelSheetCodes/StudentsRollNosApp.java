package com.Vcube_AdminPortal.studentDetails.excelSheetCodes;

import java.util.ArrayList;
import java.util.List;

import com.Vcube_AdminPortal.studentDetails.utlity.GoogleSheetsUtil;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

public class StudentsRollNosApp {

	public static List<String> getStudentsRollNosByBatchNo(String batch) {
		List<String> rollNos = new ArrayList<>();

		try {
			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			String spreadsheetId = BatchsListApp.getSheetIdbyBatchNo(batch);
			String sheetName = "Student-Info";

			String dataRange = sheetName + "!A2:A1000";
			ValueRange dataResponse = sheetsService.spreadsheets().values().get(spreadsheetId, dataRange).execute();
			List<List<Object>> rows = dataResponse.getValues();
			if (rows == null) {
				rows = new ArrayList<>();
			}

			for (List<Object> row : rows) {
				if (row == null || row.isEmpty()) {
					continue;
				}
				rollNos.add(row.get(0).toString().trim());

			}

			if (rollNos != null) {
				return rollNos;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

}
