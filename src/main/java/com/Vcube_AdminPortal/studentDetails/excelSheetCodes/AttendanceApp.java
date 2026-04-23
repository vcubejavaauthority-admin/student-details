package com.Vcube_AdminPortal.studentDetails.excelSheetCodes;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.Vcube_AdminPortal.studentDetails.model.AttendanceModel;
import com.Vcube_AdminPortal.studentDetails.model.BatchAttendanceReportModel;
import com.Vcube_AdminPortal.studentDetails.model.StudentModel;
import com.Vcube_AdminPortal.studentDetails.utlity.GoogleSheetsUtil;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.ClearValuesRequest;
import com.google.api.services.sheets.v4.model.DimensionRange;
import com.google.api.services.sheets.v4.model.InsertDimensionRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;

@Service
public class AttendanceApp {

	// ✅ FIXED: Dynamic column range calculation
	private static String getSafeDataRange(String sheetName, int maxCols) {
		return sheetName + "!A2:" + indexToColumn(maxCols) + "1000";
	}

	// ✅ Helper method for safe column conversion
	private static String indexToColumn(int index) {
		return GoogleSheetsUtil.indexToColumn(index);
	}

	/**
	 * Robustly extracts the numeric roll number from various formats like:
	 * JFS-B70-01 -> 1
	 * B70-5 -> 5
	 * JFS-70-P-10 -> 10
	 * Returns empty string if no numeric part found at end.
	 */
	private String extractShortRollNo(String fullRoll, String batch) {
		if (fullRoll == null || fullRoll.isBlank()) return "";
		
		// Split by hyphen or space
		String[] parts = fullRoll.split("[- ]");
		if (parts.length == 0) return "";
		
		// The numeric roll number is traditionally the last part
		String lastPart = parts[parts.length - 1].trim();
		
		// Clean leading zeros: 01 -> 1, 007 -> 7
		return lastPart.replaceFirst("^0+(?!$)", "");
	}

	public AttendanceModel setAttendanceByRollNoSet(String rollNoSet, String batch, String date) {
		List<String> onlineEmails = OnlineEmailsApp.getOnlineEmailsByBatchNoWithFilter(batch);
		AttendanceModel attendance = new AttendanceModel();
		List<String> emails = new ArrayList<>();
//		System.out.println(batch);
//		System.out.println(rollNoSet);
		try {
			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			String spreadsheetId = BatchsListApp.getSheetIdbyBatchNo(batch);
			String sheetName = "Attendance";

			String today = GoogleSheetsUtil.resolveDate(date);
			int dateColIndex = GoogleSheetsUtil.findOrInsertDateColumn(sheetsService, spreadsheetId, sheetName, today, 4);

			String headerRange = sheetName + "!A1:ZZ1";
			ValueRange headerResponse = sheetsService.spreadsheets().values().get(spreadsheetId, headerRange).execute();
			List<List<Object>> headerValues = headerResponse.getValues();
			if (headerValues == null || headerValues.isEmpty()) {
				return attendance;
			}
			List<Object> headerRow = headerValues.get(0);

			// ✅ FIXED: Dynamic range based on header size + buffer
			int maxCols = Math.max(headerRow.size() + 10, 50); // 50 columns minimum
			String dataRange = getSafeDataRange(sheetName, maxCols);

			// 2) Read all data rows
			ValueRange dataResponse = sheetsService.spreadsheets().values().get(spreadsheetId, dataRange).execute();
			List<List<Object>> rows = dataResponse.getValues();
			if (rows == null) {
				rows = new ArrayList<>();
			}

			// Parse roll numbers input
			String input = rollNoSet;
			String[] rollNumbers = input.split(",");
			List<String> rollSet = new ArrayList<>();
			List<String> invalidRollSet = new ArrayList<>();
			for (String r : rollNumbers) {
				if(!rollSet.contains(r.trim().replaceFirst("^0+(?!$)", ""))) {
					rollSet.add(r.trim().replaceFirst("^0+(?!$)", ""));
					invalidRollSet.add(r.trim().replaceFirst("^0+(?!$)", ""));
				}
			}

			int aCount = 0;

			// 3) Modify rows
			for (int i = 0; i < rows.size(); i++) {
				List<Object> row = rows.get(i);
				if (row.isEmpty())
					continue;

				String fullRoll = row.get(0).toString().trim();
				if (fullRoll.isBlank())
					continue;

				String rollNo = extractShortRollNo(fullRoll, batch);

				if (rollSet.contains(rollNo)) {
					// Ensure row has enough columns
					while (row.size() <= dateColIndex) {
						row.add("");
					}
					row.set(dateColIndex, "P");

					// Get email (column D, index 3)
					String email = "";
					if (row.size() > 3 && row.get(3) != null) {
						email = row.get(3).toString().trim();
					}
					if (!email.isBlank()) {
						emails.add(email);
					}
					aCount++;
					invalidRollSet.remove(rollNo);
				}
			}

			// 4) Write back (same safe range)
			ValueRange body = new ValueRange().setValues(rows);
			UpdateValuesResponse usp = sheetsService.spreadsheets().values().update(spreadsheetId, dataRange, body)
					.setValueInputOption("RAW").execute();

			if (usp != null) {
				attendance.setEmailsList(emails);
				attendance.setOnlineEmailsList(onlineEmails);
				attendance.setInvalidRollSet(invalidRollSet);
				attendance.setAttendanceCount(aCount);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return attendance;
	}

	public List<StudentModel> setAttendanceByRollNoSetWithCheckBox(String batch, String date,
			List<String> presentRolls) {
		List<StudentModel> students = new ArrayList<>();
		try {
			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			String spreadsheetId = BatchsListApp.getSheetIdbyBatchNo(batch);
			String sheetName = "Attendance";

			String today = GoogleSheetsUtil.resolveDate(date);
			int dateColIndex = GoogleSheetsUtil.findOrInsertDateColumn(sheetsService, spreadsheetId, sheetName, today, 4);

			String headerRange = sheetName + "!A1:ZZ1";
			ValueRange headerResponse = sheetsService.spreadsheets().values().get(spreadsheetId, headerRange).execute();
			List<List<Object>> headerValues = headerResponse.getValues();
			if (headerValues == null || headerValues.isEmpty()) {
				return students;
			}
			List<Object> headerRow = headerValues.get(0);

			// ✅ FIXED: Safe dynamic range
			int maxCols = Math.max(headerRow.size() + 10, 50);
			String dataRange = getSafeDataRange(sheetName, maxCols);

			ValueRange dataResponse = sheetsService.spreadsheets().values().get(spreadsheetId, dataRange).execute();
			List<List<Object>> rows = dataResponse.getValues();
			if (rows == null) {
				rows = new ArrayList<>();
			}

			int aCount = 0;
			for (int i = 0; i < rows.size(); i++) {
				List<Object> row = rows.get(i);
				if (row.isEmpty())
					continue;

				String fullRoll = row.size() > 0 ? row.get(0).toString().trim() : "";
				if (fullRoll.isBlank())
					continue;

				if (presentRolls.contains(fullRoll)) {
					while (row.size() <= dateColIndex) {
						row.add("");
					}
					row.set(dateColIndex, "P");

					// Create StudentModel
					String stRollNo = row.size() > 0 ? row.get(0).toString().trim() : "";
					String stName = row.size() > 1 ? row.get(1).toString().trim() : "";
					String stMobile = row.size() > 2 ? row.get(2).toString().trim() : "";
					String stEmail = row.size() > 3 ? row.get(3).toString().trim() : "";

					if (stRollNo.isBlank() || stName.isBlank())
						continue;

					StudentModel stm = new StudentModel();
					stm.setRollNo(stRollNo);
					stm.setName(stName);

					if (!stMobile.isBlank()) {
						try {
							stm.setMobile(Long.parseLong(stMobile));
						} catch (NumberFormatException ex) {
							stm.setMobile(0L);
						}
					}
					stm.setEmail(stEmail);
					stm.setMark(row.size() > dateColIndex ? row.get(dateColIndex).toString().trim() : "");
					stm.setOnline(true);
					students.add(stm);

					aCount++;
				}
			}

			// Write back
			ValueRange body = new ValueRange().setValues(rows);
			sheetsService.spreadsheets().values().update(spreadsheetId, dataRange, body).setValueInputOption("RAW")
					.execute();

			System.out.println("Attendance finished. Total attendees: " + aCount);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return students;
	}

	public String setEmailsForDailyAttendance(List<String> emails, String batch) {
		try {
			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			String spreadsheetId = BatchsListApp.getSheetIdbyBatchNo(batch);
			String sheetName = "DailyAttendance";

			String today = GoogleSheetsUtil.resolveDate(null); // Assuming daily email is always today unless parameterized

			// Use centralized logic for chronological date column insertion
			int todayColIndex = GoogleSheetsUtil.findOrInsertDateColumn(sheetsService, spreadsheetId, sheetName, today, 0);

			// 3) Prepare Set with existing emails in today's column
			String colLetter = indexToColumn(todayColIndex);
			String existingRange = sheetName + "!" + colLetter + "2:" + colLetter;

			ValueRange existingResp = sheetsService.spreadsheets().values().get(spreadsheetId, existingRange).execute();

			// LinkedHashSet -> unique + insertion order preserve
			List<String> emailSet = new ArrayList<>();

			List<List<Object>> existingValues = existingResp.getValues();
			if (existingValues != null) {
				for (List<Object> row : existingValues) {
					if (row != null && !row.isEmpty()) {
						Object v = row.get(0);
						if (v != null) {
							String mail = v.toString().trim();
							if (!mail.isEmpty()) {
								if (!emailSet.contains(mail)) {
									emailSet.add(mail);
								}
							}
						}
					}
				}
			}

			// 4) Merge batch-side emails (filtered)
			List<String> onlineEmails = OnlineEmailsApp.getOnlineEmailsByBatchNoWithFilter(batch);
			if (onlineEmails != null) {
				for (String mail : onlineEmails) {
					if (mail != null && !mail.isBlank()) {
						if (!emailSet.contains(mail)) {
							emailSet.add(mail);
						}
					}
				}
			}

			// 5) Merge new emails from UI parameter
			if (emails != null) {
				for (String mail : emails) {
					if (mail != null && !mail.isBlank()) {
						if (!emailSet.contains(mail)) {
							emailSet.add(mail);
						}
					}
				}
			}

			if (emailSet.isEmpty()) {
				return "no-emails";
			}

			// 6) Convert Set -> vertical values list
			List<List<Object>> values = new ArrayList<>();
			for (String mail : emailSet) {
				values.add(List.of(mail));
			}

			// 7) Clear old values in that column (rows 2..)
			String clearRange = sheetName + "!" + colLetter + "2:" + colLetter;
			ClearValuesRequest clearReq = new ClearValuesRequest();
			sheetsService.spreadsheets().values().clear(spreadsheetId, clearRange, clearReq).execute();

			// 8) Write merged unique emails from row 2
			String emailsRange = sheetName + "!" + colLetter + "2";
			ValueRange body = new ValueRange().setValues(values);
			sheetsService.spreadsheets().values().update(spreadsheetId, emailsRange, body).setValueInputOption("RAW")
					.execute();

			System.out.println(
					"DailyAttendance updated in column " + colLetter + " with " + values.size() + " unique emails.");
			return "success";

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public List<StudentModel> setTickMarksForCheckboxesByRollNos(List<StudentModel> students, String batch,
			String rollSet) {
		List<String> onlineEmails = OnlineEmailsApp.getOnlineEmailsByBatchNoWithFilter(batch);
		List<StudentModel> markedStudents = new ArrayList<>();

		List<String> rollNos = new ArrayList<>();
		if (students != null && rollSet != null) {
			String[] parts = rollSet.split(",");

			for (String part : parts) {
				if (part != null)
					if (!rollNos.contains(part.replaceFirst("^0+(?!$)", ""))) {
						rollNos.add(part.replaceFirst("^0+(?!$)", ""));
					}
			}

			for (StudentModel student : students) {
				String fullRoll = student.getRollNo() != null ? student.getRollNo() : "";
				String rollNo = extractShortRollNo(fullRoll, batch);

				if (rollNos.contains(rollNo)) {
					student.setOnline(true);
				}
				if (onlineEmails.contains(student.getEmail())) {
					student.setOnline(true);
				}

				markedStudents.add(student);
			}

			return markedStudents;
		}

		return null;
	}

	public AttendanceModel getStudentAllAttendanceMonthsWithStatusByBatchRollNo(String batch, String rollNo) {
		AttendanceModel attendance = new AttendanceModel();
		attendance.setMonths(new ArrayList<>());
		attendance.setTotalPresents(new ArrayList<>());
		attendance.setTotalAbsents(new ArrayList<>());

		try {

			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			String spreadsheetId = BatchsListApp.getSheetIdbyBatchNo(batch);
			String sheetName = "Attendance";

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
				return attendance;
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
				return attendance;
			}

			attendance.setRollNo(
					studentRow.get(0).toString().trim().contains("JFS") ? studentRow.get(0).toString().trim() : "");

			// ✅ FIXED: Process from column 4+ with proper absent logic
			SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
			SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.ENGLISH);

			Map<String, Integer> presentCount = new LinkedHashMap<>();
			Map<String, Integer> absentCount = new LinkedHashMap<>();
			Map<String, Integer> totalDaysCount = new LinkedHashMap<>(); // Track total working days per month

			// Process attendance columns (starting from column 4 onwards)
			for (int col = 4; col < Math.max(headerRow.size(), studentRow.size()); col++) {
				String dateHeader = (col < headerRow.size() && headerRow.get(col) != null)
						? headerRow.get(col).toString().trim()
						: "";
				String status = (col < studentRow.size() && studentRow.get(col) != null)
						? studentRow.get(col).toString().trim().toUpperCase()
						: "";

				if (dateHeader.isEmpty())
					continue;

				try {
					Date date = dateFormat.parse(dateHeader);
					if (date != null) {
						String monthName = monthFormat.format(date).toUpperCase();

						// ✅ FIXED: Count EVERY valid date as working day
						totalDaysCount.put(monthName, totalDaysCount.getOrDefault(monthName, 0) + 1);

						// ✅ FIXED: P = Present, EMPTY/Space/Other = Absent
						if ("P".equals(status)) {
							presentCount.put(monthName, presentCount.getOrDefault(monthName, 0) + 1);
//							System.out.println(monthName + " P");
						} else {
							// Empty, space, or anything else = Absent
							absentCount.put(monthName, absentCount.getOrDefault(monthName, 0) + 1);
//							System.out.println(monthName + " A");
						}
					}
				} catch (ParseException e) {
					System.out.println("Date parse failed: " + dateHeader);
					continue;
				}
			}

			// 3. Populate model with sorted months
			List<String> allMonths = new ArrayList<>(totalDaysCount.keySet());
			int grandTotalPresents = 0;
			int grandTotalDays = 0;

			for (String month : allMonths) {
				int presents = presentCount.getOrDefault(month, 0);
				int absents = absentCount.getOrDefault(month, 0);
				int totalDays = totalDaysCount.getOrDefault(month, 0);

				attendance.getMonths().add(month);
				attendance.getTotalPresents().add(presents);
				attendance.getTotalAbsents().add(absents);

				grandTotalPresents += presents;
				grandTotalDays += totalDays;

//				System.out.println(month + ": P=" + presents + ", A=" + absents + ", Total=" + totalDays);
			}

			// 4. Calculate overall totals
			attendance.setTotalAttendance(grandTotalDays);
			attendance.setStudentTotalAttendance(grandTotalPresents);

			if (grandTotalDays > 0) {
				attendance.setStudentAttendancePercentage(
						Math.round((double) grandTotalPresents / grandTotalDays * 10000.0) / 100.0);
			}

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return attendance;
	}

	public BatchAttendanceReportModel getBatchAttendanceReport(String batch, String startDateStr, String endDateStr) {
		BatchAttendanceReportModel report = new BatchAttendanceReportModel();
		report.setBatch(batch);
		report.setStudents(new ArrayList<>());

		try {
			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			String spreadsheetId = BatchsListApp.getSheetIdbyBatchNo(batch);
			if (spreadsheetId == null) {
				return report;
			}
			String sheetName = "Attendance";

			// 1) Read header row
			String headerRange = sheetName + "!A1:ZZ1";
			ValueRange headerResponse = sheetsService.spreadsheets().values().get(spreadsheetId, headerRange).execute();
			List<List<Object>> headerValues = headerResponse.getValues();
			if (headerValues == null || headerValues.isEmpty()) {
				return report;
			}
			List<Object> headerRow = headerValues.get(0);

			// 2) Read data rows
			int maxCols = Math.max(headerRow.size() + 10, 50);
			String dataRange = getSafeDataRange(sheetName, maxCols);
			ValueRange dataResponse = sheetsService.spreadsheets().values().get(spreadsheetId, dataRange).execute();
			List<List<Object>> rows = dataResponse.getValues();
			if (rows == null || rows.isEmpty()) {
				return report;
			}

			// 3) Parse start/end dates for range filtering
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
			Date startDate = null;
			Date endDate = null;
			try {
				startDate = sdf.parse(startDateStr);
				endDate = sdf.parse(endDateStr);
			} catch (ParseException e) {
				e.printStackTrace();
				return report;
			}

			// 4) Find date column indices within range (columns start at index 4)
			List<Integer> filteredDateCols = new ArrayList<>();
			for (int col = 4; col < headerRow.size(); col++) {
				String dateHeader = headerRow.get(col) != null ? headerRow.get(col).toString().trim() : "";
				if (dateHeader.isEmpty()) continue;
				try {
					Date colDate = sdf.parse(dateHeader);
					if (colDate != null && !colDate.before(startDate) && !colDate.after(endDate)) {
						filteredDateCols.add(col);
					}
				} catch (ParseException e) {
					// Skip non-date columns
				}
			}

			int totalWorkingDays = filteredDateCols.size();
			report.setTotalWorkingDays(totalWorkingDays);

			// 5) Process each student row
			int totalStudents = 0;
			int regularCount = 0;
			int irregularCount = 0;
			int grandPresent = 0;
			int grandAbsent = 0;

			for (List<Object> row : rows) {
				if (row == null || row.isEmpty()) continue;

				String rollNo = row.get(0) != null ? row.get(0).toString().trim() : "";
				String name = row.size() > 1 && row.get(1) != null ? row.get(1).toString().trim() : "";

				if (rollNo.isBlank() || name.isBlank()) continue;

				int presentDays = 0;
				int absentDays = 0;

				for (int col : filteredDateCols) {
					String status = (col < row.size() && row.get(col) != null)
							? row.get(col).toString().trim().toUpperCase()
							: "";
					if ("P".equals(status)) {
						presentDays++;
					} else {
						absentDays++;
					}
				}

				double percentage = totalWorkingDays > 0
						? Math.round((double) presentDays / totalWorkingDays * 10000.0) / 100.0
						: 0.0;
				boolean isRegular = percentage >= 75.0;

				BatchAttendanceReportModel.StudentAttendanceSummary summary = new BatchAttendanceReportModel.StudentAttendanceSummary();
				summary.setRollNo(rollNo);
				summary.setName(name);
				summary.setPresentDays(presentDays);
				summary.setAbsentDays(absentDays);
				summary.setTotalDays(totalWorkingDays);
				summary.setAttendancePercentage(percentage);
				summary.setRegular(isRegular);

				report.getStudents().add(summary);

				totalStudents++;
				grandPresent += presentDays;
				grandAbsent += absentDays;
				if (isRegular) {
					regularCount++;
				} else {
					irregularCount++;
				}
			}

			report.setTotalStudents(totalStudents);
			report.setRegularCount(regularCount);
			report.setIrregularCount(irregularCount);
			report.setTotalPresentCount(grandPresent);
			report.setTotalAbsentCount(grandAbsent);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return report;
	}

}
