package com.Vcube_AdminPortal.studentDetails.excelSheetCodes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.Vcube_AdminPortal.studentDetails.model.GUTExamModel;
import com.Vcube_AdminPortal.studentDetails.model.StudentModel;
import com.Vcube_AdminPortal.studentDetails.utlity.GoogleSheetsUtil;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

@Service
public class GUTExamApp {

	// ✅ FIXED: Dynamic column range calculation
	private static String getSafeDataRange(String sheetName, int maxCols) {
		return sheetName + "!A2:" + indexToColumn(maxCols) + "1000";
	}

	// ✅ Helper method for safe column conversion
	private static String indexToColumn(int index) {
		return GoogleSheetsUtil.indexToColumn(index);
	}

	public String setGUTExamByRollNoSet(GUTExamModel atd, String batch) {
		boolean mockMarked = false;
		DateTimeFormatter inFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		DateTimeFormatter outFmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		String date = LocalDate.parse(atd.getDate(), inFmt).format(outFmt);

		try {
			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			String spreadsheetId = BatchsListApp.getSheetIdbyBatchNo(batch);
			String sheetName = "GUT-Exams"; // change if needed

			int dateColIndex = GoogleSheetsUtil.findOrInsertDateColumn(sheetsService, spreadsheetId, sheetName, date, 2);

			String headerRange = sheetName + "!A1:ZZ1";
			ValueRange headerResponse = sheetsService.spreadsheets().values().get(spreadsheetId, headerRange).execute();
			List<List<Object>> headerValues = headerResponse.getValues();
			if (headerValues == null || headerValues.isEmpty()) {
				System.out.println("No header row found");
				return null;
			}
			List<Object> headerRow = headerValues.get(0);

			// 2) Read all data rows
			int maxCols = Math.max(headerRow.size() + 10, 50);
			String dataRange = getSafeDataRange(sheetName, maxCols);
			ValueRange dataResponse = sheetsService.spreadsheets().values().get(spreadsheetId, dataRange).execute();
			List<List<Object>> rows = dataResponse.getValues();
			if (rows == null) {
				rows = new ArrayList<>();
			}

			// Example input: "84=30, B57-3=25, 91=28"
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

	public List<GUTExamModel> getStudentsGUTExamDetailsByBatchNo(String batch) {
		List<GUTExamModel> gutExams = new ArrayList<>();

		try {
			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			String spreadsheetId = BatchsListApp.getSheetIdbyBatchNo(batch);
			String sheetName = "GUT-Exams";

			int maxCols = 50; // Initial guess
			String initialRange = sheetName + "!A1:ZZ1";
			ValueRange headerResp = sheetsService.spreadsheets().values().get(spreadsheetId, initialRange).execute();
			List<List<Object>> headerValues = headerResp.getValues();
			if (headerValues != null && !headerValues.isEmpty()) {
				maxCols = Math.max(headerValues.get(0).size() + 10, 50);
			}
			String dataRange = getSafeDataRange(sheetName, maxCols);
			ValueRange dataResponse = sheetsService.spreadsheets().values().get(spreadsheetId, dataRange).execute();
			List<List<Object>> rows = dataResponse.getValues();
			if (rows == null) {
				rows = new ArrayList<>();
			}

			int totalGUTExams = rows.get(0).size() - 2;

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
				int studentTotalGUTExams = 0;
				int studentTotalGUTExamsMarks = 0;
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
						studentTotalGUTExamsMarks += Double.parseDouble(val);
					} catch (NumberFormatException ex) {
						studentTotalGUTExamsMarks += 0;
					}
					studentTotalGUTExams++;
				}

				GUTExamModel gutExam = new GUTExamModel();
				gutExam.setBatch(batch);
				gutExam.setStudent(stm);
				gutExam.setStudentTotalGUTExams(studentTotalGUTExams);
				gutExam.setStudentTotalGUTExamsMarks(studentTotalGUTExamsMarks);
				if (studentTotalGUTExamsMarks > 0) {
					double percent = (studentTotalGUTExamsMarks * 100.0) / (totalGUTExams * 40);
					percent = Math.round(percent * 100.0) / 100.0;
					gutExam.setStudentPercentage(percent);
				} else {
					gutExam.setStudentPercentage(0.0);
				}
				gutExams.add(gutExam);

			}

			for (GUTExamModel gut : gutExams) {
				gut.setTotalGUTExams(totalGUTExams);
				gut.setTotalGUTExamsMarks(totalGUTExams * 40);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return gutExams;
	}

	public GUTExamModel getStudentAllGUTExamsWithStatusByBatchRollNo(String batch, String rollNo) {
		GUTExamModel exam = new GUTExamModel();

		try {

			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			String spreadsheetId = BatchsListApp.getSheetIdbyBatchNo(batch);
			String sheetName = "GUT-Exams";

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
			int totalExams = 0, studentTotalExams = 0, studentTotalExamsMarks = 0;

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
				double percent = (studentTotalExamsMarks * 100.0) / (totalExams * 40);
				percent = Math.round(percent * 100.0) / 100.0;
				exam.setStudentPercentage(percent);
			} else {
				exam.setStudentPercentage(0.0);
			}

			exam.setDates(dates);
			exam.setMarks(marks);
			exam.setStudentTotalGUTExams(studentTotalExams);
			exam.setStudentTotalGUTExamsMarks(studentTotalExamsMarks);
			exam.setTotalGUTExams(totalExams);
			exam.setTotalGUTExamsMarks(totalExams * 40);

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return exam;
	}

	public List<StudentModel> getStudentsGUTExamListByBatchAndDate(String batch, String uDate) {
		List<StudentModel> students = new ArrayList<>();
		try {
			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			String spreadsheetId = BatchsListApp.getSheetIdbyBatchNo(batch);
			if (spreadsheetId == null || spreadsheetId.isBlank()) {
				return students;
			}

			String studentInfoSheetName = "Student-Info";
			String dataRangeInfo = studentInfoSheetName + "!A2:F1000";
			ValueRange responseInfo = sheetsService.spreadsheets().values().get(spreadsheetId, dataRangeInfo).execute();
			List<List<Object>> infoRows = responseInfo.getValues();
			if (infoRows == null) {
				infoRows = new ArrayList<>();
			}

			for (List<Object> row : infoRows) {
				if (row == null || row.isEmpty()) {
					continue;
				}
				String roll = row.size() > 0 ? row.get(0).toString().trim() : "";
				String name = row.size() > 1 ? row.get(1).toString().trim() : "";
				String status = row.size() > 5 ? row.get(5).toString().trim() : "Active";
				if (status.isBlank()) {
					status = "Active";
				}

				if (roll.isBlank() || name.isBlank()) {
					continue;
				}

				String normStatus = status.trim().toLowerCase();
				if (normStatus.equals("active") || normStatus.equals("online") || normStatus.equals("add-on")) {
					StudentModel st = new StudentModel();
					st.setRollNo(roll);
					st.setName(name);
					st.setStatus(status);
					st.setMark(""); 

					String mobile = row.size() > 3 ? row.get(3).toString().trim() : "";
					String email = row.size() > 4 ? row.get(4).toString().trim() : "";
					st.setEmail(email);
					if (!mobile.isBlank()) {
						try {
							st.setMobile(Long.parseLong(mobile.replaceAll("[^0-9]", "")));
						} catch (NumberFormatException e) {
							st.setMobile(0L);
						}
					}
					students.add(st);
				}
			}

			String gutSheetName = "GUT-Exams";
			DateTimeFormatter inFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			DateTimeFormatter outFmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
			String dateStr = LocalDate.parse(uDate, inFmt).format(outFmt);
			System.out.println("DEBUG getStudentsGUTExamListByBatchAndDate: batch=" + batch + ", dateStr=" + dateStr);

			String headerRange = gutSheetName + "!A1:ZZ1";
			ValueRange headerResponse = sheetsService.spreadsheets().values().get(spreadsheetId, headerRange).execute();
			List<List<Object>> headerValues = headerResponse.getValues();

			int dateColIndex = -1;
			if (headerValues != null && !headerValues.isEmpty()) {
				List<Object> headerRow = headerValues.get(0);
				for (int i = 2; i < headerRow.size(); i++) {
					Object v = headerRow.get(i);
					if (v != null && dateStr.equals(v.toString().trim())) {
						dateColIndex = i;
						break;
					}
				}
			}
			System.out.println("DEBUG getStudentsGUTExamListByBatchAndDate: dateColIndex=" + dateColIndex);

			if (dateColIndex != -1) {
				int maxCols = dateColIndex + 10;
				String gutRange = gutSheetName + "!A2:" + indexToColumn(maxCols) + "1000";
				ValueRange gutResponse = sheetsService.spreadsheets().values().get(spreadsheetId, gutRange).execute();
				List<List<Object>> gutRows = gutResponse.getValues();
				if (gutRows != null) {
					Map<String, String> gutMarksMap = new HashMap<>();
					for (List<Object> row : gutRows) {
						if (row == null || row.isEmpty()) {
							continue;
						}
						String roll = row.get(0).toString().trim();
						if (!roll.isEmpty()) {
							String mark = row.size() > dateColIndex && row.get(dateColIndex) != null
									? row.get(dateColIndex).toString().trim()
									: "";
							gutMarksMap.put(roll.toLowerCase(), mark);
						}
					}

					for (StudentModel st : students) {
						String mark = gutMarksMap.get(st.getRollNo().toLowerCase());
						if (mark != null) {
							st.setMark(mark);
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return students;
	}

	public String saveGUTExamMarks(String batch, String uDate, List<String> rollNos, List<String> marks) {
		DateTimeFormatter inFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		DateTimeFormatter outFmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		String dateStr = LocalDate.parse(uDate, inFmt).format(outFmt);
		System.out.println("DEBUG saveGUTExamMarks: batch=" + batch + ", dateStr=" + dateStr);
		System.out.println("DEBUG saveGUTExamMarks: rollNos=" + rollNos);
		System.out.println("DEBUG saveGUTExamMarks: marks=" + marks);

		try {
			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			String spreadsheetId = BatchsListApp.getSheetIdbyBatchNo(batch);
			String sheetName = "GUT-Exams";

			int dateColIndex = GoogleSheetsUtil.findOrInsertDateColumn(sheetsService, spreadsheetId, sheetName, dateStr, 2);
			System.out.println("DEBUG saveGUTExamMarks: dateColIndex=" + dateColIndex);

			String headerRange = sheetName + "!A1:ZZ1";
			ValueRange headerResponse = sheetsService.spreadsheets().values().get(spreadsheetId, headerRange).execute();
			List<List<Object>> headerValues = headerResponse.getValues();
			int maxCols = 50;
			if (headerValues != null && !headerValues.isEmpty()) {
				maxCols = Math.max(headerValues.get(0).size() + 10, 50);
			}
			String dataRange = getSafeDataRange(sheetName, maxCols);
			ValueRange dataResponse = sheetsService.spreadsheets().values().get(spreadsheetId, dataRange).execute();
			List<List<Object>> rows = dataResponse.getValues();
			if (rows == null) {
				rows = new ArrayList<>();
			}

			Map<String, Integer> rollToIndexMap = new HashMap<>();
			for (int i = 0; i < rollNos.size(); i++) {
				rollToIndexMap.put(rollNos.get(i).trim().toLowerCase(), i);
			}

			java.util.Set<String> seenRolls = new java.util.HashSet<>();

			for (int i = 0; i < rows.size(); i++) {
				List<Object> row = rows.get(i);
				if (row == null || row.isEmpty()) {
					continue;
				}
				String roll = row.get(0).toString().trim();
				if (roll.isEmpty()) {
					continue;
				}

				String rollKey = roll.toLowerCase();
				Integer index = rollToIndexMap.get(rollKey);
				if (index != null) {
					seenRolls.add(rollKey);
					while (row.size() <= dateColIndex) {
						row.add("");
					}

					String mark = marks.size() > index && marks.get(index) != null ? marks.get(index).trim() : "";
					if (!mark.isEmpty()) {
						try {
							row.set(dateColIndex, Double.parseDouble(mark));
						} catch (NumberFormatException e) {
							row.set(dateColIndex, mark);
						}
					} else {
						row.set(dateColIndex, "");
					}
				}
			}

			Map<String, String> rollToNameMap = new HashMap<>();
			try {
				String studentInfoSheetName = "Student-Info";
				String dataRangeInfo = studentInfoSheetName + "!A2:B1000";
				ValueRange responseInfo = sheetsService.spreadsheets().values().get(spreadsheetId, dataRangeInfo).execute();
				List<List<Object>> infoRows = responseInfo.getValues();
				if (infoRows != null) {
					for (List<Object> r : infoRows) {
						if (r != null && r.size() > 1) {
							rollToNameMap.put(r.get(0).toString().trim().toLowerCase(), r.get(1).toString().trim());
						}
					}
				}
			} catch (Exception e) {
				System.err.println("Warning: Could not fetch names from Student-Info: " + e.getMessage());
			}

			for (String inputRoll : rollNos) {
				String rollKey = inputRoll.trim().toLowerCase();
				if (!seenRolls.contains(rollKey)) {
					List<Object> newRow = new ArrayList<>();
					newRow.add(inputRoll); 
					String name = rollToNameMap.getOrDefault(rollKey, "");
					newRow.add(name); 
					while (newRow.size() <= dateColIndex) {
						newRow.add("");
					}
					
					Integer index = rollToIndexMap.get(rollKey);
					String mark = (index != null && marks.size() > index && marks.get(index) != null) ? marks.get(index).trim() : "";
					if (!mark.isEmpty()) {
						try {
							newRow.set(dateColIndex, Double.parseDouble(mark));
						} catch (NumberFormatException e) {
							newRow.set(dateColIndex, mark);
						}
					} else {
						newRow.set(dateColIndex, "");
					}
					rows.add(newRow);
				}
			}

			ValueRange body = new ValueRange().setValues(rows);
			sheetsService.spreadsheets().values().update(spreadsheetId, dataRange, body).setValueInputOption("RAW")
					.execute();
			return "success";

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
