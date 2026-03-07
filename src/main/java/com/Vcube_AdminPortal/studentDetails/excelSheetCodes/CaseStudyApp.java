package com.Vcube_AdminPortal.studentDetails.excelSheetCodes;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import com.Vcube_AdminPortal.studentDetails.model.CaseStudyModel;
import com.Vcube_AdminPortal.studentDetails.model.StudentModel;
import com.Vcube_AdminPortal.studentDetails.utlity.GoogleSheetsUtil;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

@Service
public class CaseStudyApp {

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

	public String setCaseStudyByRollNoSet(CaseStudyModel atd, String batch) {

		try {
			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			String spreadsheetId = BatchsListApp.getSheetIdbyBatchNo(batch);
			String sheetName = "Case-Study"; // change if needed

			// 1) Read header row (A1:Z1 for example)
			String headerRange = sheetName + "!A1:ZZ1";
			ValueRange headerResponse = sheetsService.spreadsheets().values().get(spreadsheetId, headerRange).execute();
			List<List<Object>> headerValues = headerResponse.getValues();
			if (headerValues == null || headerValues.isEmpty()) {
				System.out.println("No header row found");
				return "No header row found";
			}

			List<Object> headerRow = headerValues.get(0);

			String today = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
			int dateColIndex = -1;
			for (int i = 0; i < headerRow.size(); i++) {
				Object v = headerRow.get(i);
				if (v != null && today.equals(v.toString().trim())) {
					dateColIndex = i;
					break;
				}
			}

			// If today's column not present, append it
			if (dateColIndex == -1) {
				dateColIndex = headerRow.size();
				headerRow.add(today);

				ValueRange updateHeader = new ValueRange().setRange(headerRange)
						.setValues(Collections.singletonList(headerRow));
				sheetsService.spreadsheets().values().update(spreadsheetId, headerRange, updateHeader)
						.setValueInputOption("RAW").execute();

				System.out.println("Added new date column at index " + dateColIndex);
			} else {
				System.out.println("Found today's column at index " + dateColIndex);
			}

			// 2) Read all data rows (A2:D and date column etc.)
			String dataRange = sheetName + "!A2:Z1000"; // adjust max rows/columns
			ValueRange dataResponse = sheetsService.spreadsheets().values().get(spreadsheetId, dataRange).execute();
			List<List<Object>> rows = dataResponse.getValues();
			if (rows == null) {
				rows = new ArrayList<>();
			}

			// Parse roll numbers input
			String input = atd.getRollNoSet();
			String[] rollNumbers = input.split(",");
			List<String> rollSet = new ArrayList<>();
			List<String> invalidRollSet = new ArrayList<>();
			for (String r : rollNumbers) {
				if (!rollSet.contains(r.trim().replaceFirst("^0+(?!$)", ""))) {
					rollSet.add(r.trim().replaceFirst("^0+(?!$)", ""));
					invalidRollSet.add(r.trim().replaceFirst("^0+(?!$)", ""));
				}
			}
			// 3) Modify in-memory rows list
			for (int i = 0; i < rows.size(); i++) {
				List<Object> row = rows.get(i);
				if (row.isEmpty()) {
					continue;
				}

				String fullRoll = row.get(0).toString().trim();
				if (fullRoll.isBlank() || !fullRoll.contains("JFS-B"))
					continue;

				System.out.println(fullRoll);

				String[] rollNos = fullRoll.split("-");
				String rollNo = "";
				if (rollNos.length == 3) {
					if (rollNos[1].equals(batch)) {
						rollNo = rollNos[2].replaceFirst("^0+(?!$)", "");
					} else {
						rollNo = rollNos[1].substring(1) + "-"
								+ rollNos[rollNos.length - 1].replaceFirst("^0+(?!$)", "");
					}
					rollNo = rollNo.replaceFirst("^0+(?!$)", "");
					System.out.println(rollNo);
				} else if (rollNos.length > 3 && rollNos[1].equals(batch)) {
					rollNo = rollNos[2].substring(1) + "-" + rollNos[rollNos.length - 1].replaceFirst("^0+(?!$)", "");
					System.out.println(rollNo);
				}

				rollNo = rollNo.replaceFirst("^0+(?!$)", "");

				if (rollSet.contains(rollNo)) {
					// Ensure row has enough columns
					while (row.size() <= dateColIndex) {
						row.add("");
					}
					row.set(dateColIndex, "S");
					invalidRollSet.remove(rollNo);

				}
			}

			// 4) Write updated data rows back
			ValueRange body = new ValueRange().setValues(rows);
			sheetsService.spreadsheets().values().update(spreadsheetId, dataRange, body).setValueInputOption("RAW")
					.execute();
			return "success";

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public List<CaseStudyModel> getStudentsCaseStudyDetailsByBatchNo(String batch) {
		List<CaseStudyModel> caseStudies = new ArrayList<>();

		try {
			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			String spreadsheetId = BatchsListApp.getSheetIdbyBatchNo(batch);
			String sheetName = "Case-Study";

			String dataRange = sheetName + "!A1:Z1000";
			ValueRange dataResponse = sheetsService.spreadsheets().values().get(spreadsheetId, dataRange).execute();
			List<List<Object>> rows = dataResponse.getValues();
			if (rows == null) {
				rows = new ArrayList<>();
			}

			int totalCaseStudies = rows.get(0).size() - 2;

			for (List<Object> row : rows) {
				if (row == null || row.isEmpty() || !row.get(0).toString().trim().contains("JFS")) {
					continue;
				}

				String stRollNo = row.size() > 0 ? row.get(0).toString().trim() : "";
				String stName = row.size() > 1 ? row.get(1).toString().trim() : "";

				if (stRollNo.isBlank() || stName.isBlank()) {
					continue;
				}

				// Student info
				StudentModel stm = new StudentModel();
				stm.setRollNo(stRollNo);
				stm.setName(stName);
				// Mock counts
				int studentTotalCaseStudies = 0;
				// columns C (index 2) case-study
				for (int c = 2; c < row.size(); c++) {
					Object v = row.get(c);
					if (v == null) {
						continue;
					}
					String val = v.toString().trim();
					if (val.isEmpty()) {
						continue;
					}
					studentTotalCaseStudies++;
				}

				CaseStudyModel exam = new CaseStudyModel();
				exam.setBatch(batch);
				exam.setStudent(stm);
				exam.setStudentTotalCaseStudies(studentTotalCaseStudies);

				caseStudies.add(exam);

			}

			for (CaseStudyModel m : caseStudies) {
				m.setTotalCaseStudies(totalCaseStudies);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return caseStudies;
	}

	public CaseStudyModel getStudentAllCaseStudiessWithStatusByBatchRollNo(String batch, String rollNo) {
		CaseStudyModel caseStudy = new CaseStudyModel();

		try {

			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			String spreadsheetId = BatchsListApp.getSheetIdbyBatchNo(batch);
			String sheetName = "Case-Study";

			// 1) Read header row (wide range)
			String headerRange = sheetName + "!A1:ZZ1";
			ValueRange headerResponse = sheetsService.spreadsheets().values().get(spreadsheetId, headerRange).execute();
			List<List<Object>> headerValues = headerResponse.getValues();
			if (headerValues == null || headerValues.isEmpty()) {
				System.out.println("No header row found");
				return null;
			}

			List<Object> headerRow = headerValues.get(0);
			// ✅ FIXED: Dynamic range based on header size + buffer
			int maxCols = Math.max(headerRow.size() + 10, 50); // 50 columns minimum
			String dataRange = getSafeDataRange(sheetName, maxCols);

			// 2) Read all data rows
			ValueRange dataResponse = sheetsService.spreadsheets().values().get(spreadsheetId, dataRange).execute();
			List<List<Object>> rows = dataResponse.getValues();
			if (rows == null || rows.isEmpty()) {
				return caseStudy;
			}

			// 2. Find student row by rollNo (column 0)
			List<Object> studentRow = null;
			for (int i = 1; i < rows.size(); i++) {
				List<Object> row = rows.get(i);
				if (row != null && !row.isEmpty() && row.get(0) != null
						&& rollNo.equalsIgnoreCase(row.get(0).toString().trim())) {
					studentRow = row;
					break;
				}
			}

			if (studentRow == null) {
				System.out.println("Student " + rollNo + " not found");
				return caseStudy;
			}

			caseStudy.setRollNo(
					studentRow.get(0).toString().trim().contains("JFS") ? studentRow.get(0).toString().trim() : "");

			List<String> dates = new ArrayList<>();
			List<String> caseStudyStatus = new ArrayList<>();
			int totalCaseStudy = 0, studentTotalCaseStudies = 0;

			// Process attendance columns (starting from column 4 onwards)
			for (int col = 2; col < Math.max(headerRow.size(), studentRow.size()); col++) {
				String dateHeader = (col < headerRow.size() && headerRow.get(col) != null)
						? headerRow.get(col).toString().trim()
						: "";
				String status = (col < studentRow.size() && studentRow.get(col) != null)
						? studentRow.get(col).toString().trim()
						: "";

				if (dateHeader.isEmpty())
					continue;

//				System.out.println(dateHeader + " " + status);
				dates.add(dateHeader);
				totalCaseStudy++;
				if (!status.isEmpty()) {
					caseStudyStatus.add(status.trim());
					studentTotalCaseStudies++;

				} else {
					caseStudyStatus.add("Not-Given");
				}

			}

			caseStudy.setDates(dates);
			caseStudy.setCaseStudyStatus(caseStudyStatus);
			caseStudy.setStudentTotalCaseStudies(studentTotalCaseStudies);
			caseStudy.setTotalCaseStudies(totalCaseStudy);

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return caseStudy;
	}

}
