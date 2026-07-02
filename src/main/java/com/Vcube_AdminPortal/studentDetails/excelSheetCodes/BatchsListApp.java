package com.Vcube_AdminPortal.studentDetails.excelSheetCodes;

import java.util.ArrayList;
import java.util.List;

import com.Vcube_AdminPortal.studentDetails.utlity.GoogleSheetsUtil;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BatchsListApp {

	private static String SPREADSHEET_ID;

	@Value("${google.sheet.master-id}")
	public void setSpreadsheetId(String spreadsheetId) {
		SPREADSHEET_ID = spreadsheetId;
	}

	private static final String SHEET_NAME = "Batchs-List"; // Batch list tab name


	// In-memory cache for batch list queries
	private static List<String> cachedActiveBatches = null;
	private static List<String> cachedInactiveBatches = null;
	private static List<String> cachedAllBatchesRaw = null;
	private static final java.util.Map<String, String> cachedSheetIds = new java.util.concurrent.ConcurrentHashMap<>();
	private static long lastCacheTime = 0;
	private static final long CACHE_DURATION = 10 * 60 * 1000; // 10 minutes cache

	public static List<String> getAllBatchesList() {
		return getBatchesByStatus("active");
	}

	public static List<String> getActiveBatchesList() {
		return getBatchesByStatus("active");
	}

	public static List<String> getInactiveBatchesList() {
		return getBatchesByStatus("in-active");
	}

	private static synchronized void refreshCache() {
		long now = System.currentTimeMillis();
		if (now - lastCacheTime <= CACHE_DURATION && cachedActiveBatches != null) {
			return; // already refreshed recently
		}

		List<String> active = new ArrayList<>();
		List<String> inactive = new ArrayList<>();
		List<String> allRaw = new ArrayList<>();

		try {
			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			String range = SHEET_NAME + "!A1:C";
			ValueRange response = sheetsService.spreadsheets().values().get(SPREADSHEET_ID, range).execute();

			List<List<Object>> values = response.getValues();
			if (values != null && !values.isEmpty()) {
				for (int i = 1; i < values.size(); i++) {
					List<Object> row = values.get(i);
					if (row != null && row.size() >= 2) {
						String sheetBatch = row.get(0).toString().trim(); // A column
						String status = row.size() > 2 ? row.get(2).toString().trim() : "active";
						if (status.isEmpty()) {
							status = "active";
						}

						if (sheetBatch.startsWith("B") && !sheetBatch.isBlank()) {
							allRaw.add(sheetBatch);
							if (status.equalsIgnoreCase("in-active")) {
								inactive.add(sheetBatch);
							} else {
								active.add(sheetBatch);
							}
						}
					}
				}
			}
			cachedActiveBatches = active;
			cachedInactiveBatches = inactive;
			cachedAllBatchesRaw = allRaw;
			cachedSheetIds.clear(); // Clear resolved sheet IDs to ensure update consistency
			lastCacheTime = System.currentTimeMillis();
			System.out.println("Batch list cache refreshed successfully at " + new java.util.Date());
		} catch (Exception e) {
			e.printStackTrace();
			// Keep old lists if they exist, otherwise initialize empty
			if (cachedActiveBatches == null) cachedActiveBatches = new ArrayList<>();
			if (cachedInactiveBatches == null) cachedInactiveBatches = new ArrayList<>();
			if (cachedAllBatchesRaw == null) cachedAllBatchesRaw = new ArrayList<>();
		}
	}

	public static List<String> getBatchesByStatus(String targetStatus) {
		long now = System.currentTimeMillis();
		if (now - lastCacheTime > CACHE_DURATION || cachedActiveBatches == null || cachedInactiveBatches == null) {
			refreshCache();
		}

		if ("in-active".equalsIgnoreCase(targetStatus)) {
			return new ArrayList<>(cachedInactiveBatches);
		} else {
			return new ArrayList<>(cachedActiveBatches);
		}
	}

	public static List<String> getAllBatchesListRaw() {
		long now = System.currentTimeMillis();
		if (now - lastCacheTime > CACHE_DURATION || cachedAllBatchesRaw == null) {
			refreshCache();
		}
		return new ArrayList<>(cachedAllBatchesRaw);
	}

	public static String getSheetIdbyBatchNo(String batchNo) {
		if (batchNo == null || batchNo.isBlank()) {
			return null;
		}
		
		String targetBatch = batchNo.trim();
		String cachedId = cachedSheetIds.get(targetBatch);
		if (cachedId != null) {
			return cachedId;
		}

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
								cachedSheetIds.put(targetBatch, sid);
								return sid;
							}
						}
						
						// 2. Fallback to index 1 (Column B)
						if (row.size() > 1 && row.get(1) != null) {
							String sid = row.get(1).toString().trim();
							if (!sid.isBlank()) {
								System.out.println("Success: Resolved Batch [" + targetBatch + "] to ID (fallback) [" + sid + "]");
								cachedSheetIds.put(targetBatch, sid);
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
