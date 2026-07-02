package com.Vcube_AdminPortal.studentDetails.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;

import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.BatchsListApp;
import com.Vcube_AdminPortal.studentDetails.model.StudentPlacementModel;
import com.Vcube_AdminPortal.studentDetails.utlity.GoogleSheetsUtil;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

@Service
public class StudentPlacementService {

	public List<StudentPlacementModel> getPlacementDetailsByBatch(String batch) {
		List<StudentPlacementModel> list = new ArrayList<>();
		if (batch == null || batch.isBlank()) {
			return list;
		}

		try {
			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			String spreadsheetId = BatchsListApp.getSheetIdbyBatchNo(batch);
			if (spreadsheetId == null || spreadsheetId.isBlank()) {
				System.err.println("No spreadsheet ID found for batch: " + batch);
				return list;
			}
			
			String sheetName = "Full-Details";
			String dataRange = sheetName + "!A2:T1000";
			ValueRange dataResponse = sheetsService.spreadsheets().values().get(spreadsheetId, dataRange).execute();
			List<List<Object>> rows = dataResponse.getValues();
			if (rows == null || rows.isEmpty()) {
				return list;
			}

			for (List<Object> row : rows) {
				if (row == null || row.isEmpty()) {
					continue;
				}

				String rollNo = row.size() > 0 ? row.get(0).toString().trim() : "";
				String name = row.size() > 1 ? row.get(1).toString().trim() : "";

				if (rollNo.isBlank() || name.isBlank()) {
					continue;
				}

				StudentPlacementModel sp = new StudentPlacementModel();
				sp.setBatchNo(batch);
				sp.setRollNo(rollNo);
				sp.setName(name);

				String photoVal = row.size() > 2 ? row.get(2).toString().trim() : "";
				if (!photoVal.isBlank() && !photoVal.startsWith("http")) {
					sp.setPhotoURL("https://lh3.googleusercontent.com/d/" + photoVal + "=w300?authuser=0");
				} else {
					sp.setPhotoURL(photoVal);
				}

				sp.setMobile(row.size() > 3 ? row.get(3).toString().trim() : "");
				sp.setAlternateMobile(row.size() > 4 ? row.get(4).toString().trim() : "");
				sp.setEmail(row.size() > 5 ? row.get(5).toString().trim() : "");
				sp.setModeOfTraining(row.size() > 6 ? row.get(6).toString().trim() : "");
				sp.setPgQualification(row.size() > 7 ? row.get(7).toString().trim() : "");
				sp.setPgPassedOutYear(row.size() > 8 ? row.get(8).toString().trim() : "");
				sp.setPgPercentage(row.size() > 9 ? row.get(9).toString().trim() : "");
				sp.setUgQualification(row.size() > 10 ? row.get(10).toString().trim() : "");
				sp.setUgStream(row.size() > 11 ? row.get(11).toString().trim() : "");
				sp.setUgPassedOutYear(row.size() > 12 ? row.get(12).toString().trim() : "");
				sp.setUgPercentage(row.size() > 13 ? row.get(13).toString().trim() : "");
				sp.setUniversityName(row.size() > 14 ? row.get(14).toString().trim() : "");
				sp.setCollegeName(row.size() > 15 ? row.get(15).toString().trim() : "");
				sp.setWorkExperience(row.size() > 16 ? row.get(16).toString().trim() : "");
				sp.setPreviousJob(row.size() > 17 ? row.get(17).toString().trim() : "");
				sp.setPreviousJobRole(row.size() > 18 ? row.get(18).toString().trim() : "");
				sp.setPreviousCompanyName(row.size() > 19 ? row.get(19).toString().trim() : "");

				list.add(sp);
			}
		} catch (Exception e) {
			System.err.println("Error reading Full-Details for batch " + batch + ": " + e.getMessage());
		}
		return list;
	}

	public List<StudentPlacementModel> getPlacementDetails(String status, String batch) {
		List<StudentPlacementModel> all = new ArrayList<>();
		if (batch != null && !batch.isBlank()) {
			return getPlacementDetailsByBatch(batch);
		}

		List<String> batches = new ArrayList<>();
		if ("inactive".equalsIgnoreCase(status) || "in-active".equalsIgnoreCase(status)) {
			batches = BatchsListApp.getInactiveBatchesList();
		} else if ("all".equalsIgnoreCase(status)) {
			batches = BatchsListApp.getAllBatchesListRaw();
		} else {
			batches = BatchsListApp.getActiveBatchesList();
		}

		if (batches == null || batches.isEmpty()) {
			return all;
		}

		List<CompletableFuture<List<StudentPlacementModel>>> futures = batches.stream()
				.map(b -> CompletableFuture.supplyAsync(() -> getPlacementDetailsByBatch(b)))
				.toList();

		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

		for (CompletableFuture<List<StudentPlacementModel>> future : futures) {
			try {
				all.addAll(future.get());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return all;
	}
}
