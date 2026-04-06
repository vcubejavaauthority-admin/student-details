package com.Vcube_AdminPortal.studentDetails.data;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.BatchsListApp;
import com.Vcube_AdminPortal.studentDetails.model.SheetModel;
import com.Vcube_AdminPortal.studentDetails.utlity.GoogleSheetsUtil;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

public class SheetData {

	// ✅ FIXED: Dynamic column range calculation
	private static String getSafeDataRange(String sheetName, int maxCols) {
		return sheetName + "!A2:" + indexToColumn(maxCols) + "1000";
	}

	/** 0 -> A, 1 -> B, 2 -> C ... */
	// ✅ Helper method for safe column conversion
	private static String indexToColumn(int index) {
		StringBuilder sb = new StringBuilder();
		int n = index;
		do {
			int rem = n % 26;
			sb.insert(0, (char) ('A' + rem));
			n = n / 26 - 1;
		} while (n >= 0);
		return sb.toString();
	}

	static SheetModel getFullSheetDataByBatchNoAndSheetName(String batch, String sheetName) {
		SheetModel sheet = new SheetModel();
		try {
			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();

			String spreadSheetId = BatchsListApp.getSheetIdbyBatchNo(batch);
			String spreadSheetName = sheetName;

			// 1) Read header row (wide range)
			String headerRange = spreadSheetName + "!A1:ZZ1";
			ValueRange headerResponse = sheetsService.spreadsheets().values().get(spreadSheetId, headerRange).execute();
			List<List<Object>> headerValues = headerResponse.getValues();

			List<Object> headerRow = headerValues.get(0);

			// ✅ FIXED: Dynamic range based on header size + buffer
			int maxCols = Math.max(headerRow.size() + 10, 50); // 50 columns minimum
			String dataRange = getSafeDataRange(spreadSheetName, maxCols);

			// 2) Read all data rows
			ValueRange dataResponse = sheetsService.spreadsheets().values().get(spreadSheetId, dataRange).execute();
			List<List<Object>> rows = dataResponse.getValues();

			if (headerRow != null && rows != null) {
				sheet.setHeaderRows(headerRow);
				sheet.setBodyRows(rows);
			}

			return sheet;

		} catch (IOException | GeneralSecurityException e) {

			e.printStackTrace();

			return sheet;
		}
	}

}
