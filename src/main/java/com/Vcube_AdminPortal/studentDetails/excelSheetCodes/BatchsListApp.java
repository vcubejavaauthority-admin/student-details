package com.Vcube_AdminPortal.studentDetails.excelSheetCodes;

import java.util.ArrayList;
import java.util.List;

import com.Vcube_AdminPortal.studentDetails.utlity.GoogleSheetsUtil;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

public class BatchsListApp {

	private static final String SPREADSHEET_ID = "1AnZbMJ_C5uyQ0q0Awjg10PTP0-bjhmXZ90_SLVHqYi0"; // Master sheet ID
	private static final String SHEET_NAME = "Batchs-List"; // Batch list tab name

	@SuppressWarnings("unused")
	public static List<String> getAllBatchesList() {
		List<String> allBatchesList = new ArrayList<>();

		try {
			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();

			// A1:C → Batch Name (A), ? (B), Sheet ID (C)
			String range = SHEET_NAME + "!A1:C";
			ValueRange response = sheetsService.spreadsheets().values().get(SPREADSHEET_ID, range).execute();

			List<List<Object>> values = response.getValues();
			if (values == null || values.isEmpty()) {
				return allBatchesList;
			}

			// A1 header skip chesi data rows search
			for (int i = 1; i < values.size(); i++) {
				List<Object> row = values.get(i);
				if (row != null && row.size() >= 2) {
					String sheetBatch = row.get(0).toString().trim(); // A column
					if (sheetBatch.startsWith("B") && !sheetBatch.isBlank()) {
						allBatchesList.add(sheetBatch);
					}
				}
			}

			if (allBatchesList != null) {
				return allBatchesList;
			}

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return null;
	}

	public static String getSheetIdbyBatchNo(String batchNo) {
		if (batchNo == null || batchNo.isBlank()) {
			return null;
		}
		
		String targetBatch = batchNo.trim();
		System.out.println("Processing Batch lookup for: [" + targetBatch + "]");
		try {
			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();

			// A1:C → Batch Name (A), Description (B), Sheet ID (C)
			String range = SHEET_NAME + "!A1:C";
			ValueRange response = sheetsService.spreadsheets().values().get(SPREADSHEET_ID, range).execute();

			List<List<Object>> values = response.getValues();
			if (values == null || values.isEmpty()) {
				System.err.println("Error: No data found in Batchs-List master sheet!");
				return null;
			}

			// A1 header skip chesi data rows search
			for (int i = 1; i < values.size(); i++) {
				List<Object> row = values.get(i);
				if (row != null && row.size() > 0) {
					String sheetBatch = row.get(0).toString().trim(); // A column
					if (targetBatch.equalsIgnoreCase(sheetBatch)) {
						
						// 1. Try index 2 (Column C) first as per structure comment
						if (row.size() > 2 && row.get(2) != null) {
							String sid = row.get(2).toString().trim();
							// Simple heuristic: Spreadsheet IDs are usually long alphanumeric strings
							if (!sid.isBlank() && sid.length() > 20) {
								System.out.println("Success: Resolved Batch [" + targetBatch + "] to ID [" + sid + "]");
								return sid;
							}
						}
						
						// 2. Fallback to index 1 (Column B)
						if (row.size() > 1 && row.get(1) != null) {
							String sid = row.get(1).toString().trim();
							if (!sid.isBlank()) {
								System.out.println("Success: Resolved Batch [" + targetBatch + "] to ID (fallback) [" + sid + "]");
								return sid;
							}
						}
					}
				}
			}
			System.err.println("Error: Batch [" + targetBatch + "] not found in master sheet.");
			return null; // batch not found

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
