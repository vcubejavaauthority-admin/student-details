package com.Vcube_AdminPortal.studentDetails.excelSheetCodes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.Vcube_AdminPortal.studentDetails.model.MockModel;
import com.Vcube_AdminPortal.studentDetails.model.StudentModel;
import com.Vcube_AdminPortal.studentDetails.utlity.GoogleSheetsUtil;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

@Service
public class MockApp {

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

	public String setMockByRollNoSet(MockModel atd, String batch) {
		boolean mockMarked = false;
		DateTimeFormatter inFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		DateTimeFormatter outFmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		String date = LocalDate.parse(atd.getDate(), inFmt).format(outFmt);

		try {
			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			String spreadsheetId = BatchsListApp.getSheetIdbyBatchNo(batch);
			String sheetName = "Mock"; // change if needed

			int dateColIndex = GoogleSheetsUtil.findOrInsertDateColumn(sheetsService, spreadsheetId, sheetName, date, 2);

			String headerRange = sheetName + "!A1:ZZ1";
			ValueRange headerResponse = sheetsService.spreadsheets().values().get(spreadsheetId, headerRange).execute();
			List<List<Object>> headerValues = headerResponse.getValues();
			if (headerValues == null || headerValues.isEmpty()) {
				System.out.println("No header row found");
				return null;
			}
			List<Object> headerRow = headerValues.get(0);

			// 2) Read all data rows (A2:D and date column etc.)
			String dataRange = sheetName + "!A2:Z1000"; // adjust max rows/columns
			ValueRange dataResponse = sheetsService.spreadsheets().values().get(spreadsheetId, dataRange).execute();
			List<List<Object>> rows = dataResponse.getValues();
			if (rows == null) {
				rows = new ArrayList<>();
			}

			String input = atd.getRollNoSetWithMarks();
			Map<String, String> rollMarksMap = new HashMap<>();

			if (input != null && !input.isBlank()) {
				String[] pairs = input.split(",");
				for (String pair : pairs) {
					String[] parts = pair.trim().split("=");
					if (parts.length == 2) {
						String key = parts[0].trim().replaceFirst("^0+(?!$)", ""); // "84" OR "B57-3"
						String marks = parts[1].trim();
						rollMarksMap.put(key, marks);
					}
				}
			}

			int aCount = 0;
			mockMarked = false;
			// 3) Modify in-memory rows list
			for (int i = 0; i < rows.size(); i++) {
				List<Object> row = rows.get(i);
				if (row.isEmpty()) {
					continue;
				}

				String fullRoll = row.size() > 0 ? row.get(0).toString().trim() : "";
				if (fullRoll.isBlank()) {
					continue;
				}

				if (!fullRoll.contains("JFS-B")) {
					continue;
				}

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
				} else if (rollNos.length > 3 && rollNos[1].equals(batch)) {
					rollNo = rollNos[2].substring(1) + "-" + rollNos[rollNos.length - 1].replaceFirst("^0+(?!$)", "");
				}

				rollNo = rollNo.replaceFirst("^0+(?!$)", ""); // remove leading zeros

				if (rollMarksMap.containsKey(rollNo)) {
					// Ensure row has enough columns
					while (row.size() <= dateColIndex) {
						row.add("");
					}

					try {
						double marksValue = Double.parseDouble(rollMarksMap.get(rollNo));
						row.set(dateColIndex, marksValue);// ✅ numeric cell
					} catch (NumberFormatException e) {
						row.set(dateColIndex, rollMarksMap.get(rollNo));// fallback to text
					}

					aCount++;
					mockMarked = true;
					System.out.println("Mock marks added for roll: " + fullRoll);
				}
			}

			// 4) Write updated data rows back
			ValueRange body = new ValueRange().setValues(rows);
			sheetsService.spreadsheets().values().update(spreadsheetId, dataRange, body).setValueInputOption("RAW")
					.execute();

			System.out.println("Mock added finished. total : " + aCount);

		} catch (Exception e) {
			e.printStackTrace();
		}

		if (mockMarked) {
			return "success";
		} else {
			return null;
		}
	}

	public List<MockModel> getStudentsMockDetailsByBatchNo(String batch) {
		List<MockModel> mocks = new ArrayList<>();

		try {
			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			String spreadsheetId = BatchsListApp.getSheetIdbyBatchNo(batch);
			String sheetName = "Mock";

			String dataRange = sheetName + "!A1:Z1000";
			ValueRange dataResponse = sheetsService.spreadsheets().values().get(spreadsheetId, dataRange).execute();
			List<List<Object>> rows = dataResponse.getValues();
			if (rows == null) {
				rows = new ArrayList<>();
			}

			int totalMocks = rows.get(0).size() - 2;

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
				int studentTotalMocks = 0;
				int studentTotalExamsMarks = 0;
				// columns C (index 4) mocks
				for (int c = 2; c < row.size(); c++) {
					Object v = row.get(c);
					if (v == null) {
						continue;
					}
					String val = v.toString().trim();
					if (val.isEmpty()) {
						continue;
					}
					try {
						studentTotalExamsMarks += Double.parseDouble(val);
					} catch (NumberFormatException ex) {
						studentTotalExamsMarks += 0;
					}
					studentTotalMocks++;
				}

				MockModel mock = new MockModel();
				mock.setBatch(batch);
				mock.setStudent(stm);
				mock.setStudentTotalMocks(studentTotalMocks);
				mock.setStudentTotalMocksMarks(studentTotalExamsMarks);
				if (studentTotalExamsMarks > 0) {
					double percent = (studentTotalExamsMarks * 100.0) / (totalMocks * 5);
					percent = Math.round(percent * 100.0) / 100.0;
					mock.setStudentPercentage(percent);
				} else {
					mock.setStudentPercentage(0.0);
				}
				mocks.add(mock);

			}

			for (MockModel m : mocks) {
				m.setTotalMocks(totalMocks);
				m.setTotalMocksMarks(totalMocks * 5);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return mocks;
	}

	public MockModel getStudentAllMocksWithStatusByBatchRollNo(String batch, String rollNo) {
		MockModel exam = new MockModel();

		try {

			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			String spreadsheetId = BatchsListApp.getSheetIdbyBatchNo(batch);
			String sheetName = "Mock";

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
				return exam;
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
				return exam;
			}

			exam.setRollNo(
					studentRow.get(0).toString().trim().contains("JFS") ? studentRow.get(0).toString().trim() : "");

			List<String> dates = new ArrayList<>();
			List<String> marks = new ArrayList<>();
			int totalExams = 0;
			int studentTotalExams = 0;
			int studentTotalExamsMarks = 0;

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
				totalExams++;
				if (!status.isEmpty()) {
					marks.add(status.trim());
					studentTotalExams++;
					try {
						studentTotalExamsMarks += Double.parseDouble(status);
					} catch (NumberFormatException ex) {
						studentTotalExamsMarks += 0;
					}
				} else {
					marks.add("Not-Given");
				}

			}

			if (studentTotalExamsMarks > 0) {
				double percent = (studentTotalExamsMarks * 100.0) / (totalExams * 5);
				percent = Math.round(percent * 100.0) / 100.0;
				exam.setStudentPercentage(percent);
			} else {
				exam.setStudentPercentage(0.0);
			}

			exam.setDates(dates);
			exam.setMarks(marks);
			exam.setStudentTotalMocks(studentTotalExams);
			exam.setStudentTotalMocksMarks(studentTotalExamsMarks);
			exam.setTotalMocks(totalExams);
			exam.setTotalMocksMarks(totalExams * 5);

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return exam;
	}

}
