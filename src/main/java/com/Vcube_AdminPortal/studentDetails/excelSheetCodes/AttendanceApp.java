package com.Vcube_AdminPortal.studentDetails.excelSheetCodes;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.Vcube_AdminPortal.studentDetails.model.AttendanceModel;
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

	public AttendanceModel setAttendanceByRollNoSet(String rollNoSet, String batch) {
		List<String> onlineEmails = OnlineEmailsApp.getOnlineEmailsByBatchNoWithFilter(batch);
		AttendanceModel attendance = new AttendanceModel();
		List<String> emails = new ArrayList<>();
//		System.out.println(batch);
//		System.out.println(rollNoSet);
		try {
			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			String spreadsheetId = BatchsListApp.getSheetIdbyBatchNo(batch);
			String sheetName = "Attendance";

			// 1) Read header row (wide range)
			String headerRange = sheetName + "!A1:ZZ1";
			ValueRange headerResponse = sheetsService.spreadsheets().values().get(spreadsheetId, headerRange).execute();
			List<List<Object>> headerValues = headerResponse.getValues();
			if (headerValues == null || headerValues.isEmpty()) {
//				System.out.println("No header row found");
				return attendance;
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
//				System.out.println("Added new date column at index " + dateColIndex);
			} else {
//				System.out.println("Found today's column at index " + dateColIndex);
			}

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
				if (fullRoll.isBlank() || !fullRoll.contains("JFS-B"))
					continue;

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

				rollNo = rollNo.replaceFirst("^0+(?!$)", "");

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

			// Header reading (same as above)
			String headerRange = sheetName + "!A1:ZZ1";
			ValueRange headerResponse = sheetsService.spreadsheets().values().get(spreadsheetId, headerRange).execute();
			List<List<Object>> headerValues = headerResponse.getValues();
			if (headerValues == null || headerValues.isEmpty()) {
				return students;
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

			if (dateColIndex == -1) {
				dateColIndex = headerRow.size();
				headerRow.add(today);
				ValueRange updateHeader = new ValueRange().setRange(headerRange)
						.setValues(Collections.singletonList(headerRow));
				sheetsService.spreadsheets().values().update(spreadsheetId, headerRange, updateHeader)
						.setValueInputOption("RAW").execute();
			}

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

			String today = new SimpleDateFormat("dd-MM-yyyy").format(new Date());

			// 1) Read first row (dates row)
			String headerRange = sheetName + "!A1:ZZ1";
			ValueRange headerResp = sheetsService.spreadsheets().values().get(spreadsheetId, headerRange).execute();
			List<List<Object>> headerValues = headerResp.getValues();
			if (headerValues == null || headerValues.isEmpty()) {
				headerValues = new ArrayList<>();
				headerValues.add(new ArrayList<>());
			}
			List<Object> headerRow = headerValues.get(0);

			int todayColIndex = -1;
			for (int c = 0; c < headerRow.size(); c++) {
				Object v = headerRow.get(c);
				if (v != null && today.equals(v.toString().trim())) {
					todayColIndex = c;
					break;
				}
			}

			// 2) If not found, insert new column at left (index 0) and set today date
			if (todayColIndex == -1) {
				Spreadsheet ss = sheetsService.spreadsheets().get(spreadsheetId).execute();
				int sheetId = ss.getSheets().stream().filter(s -> sheetName.equals(s.getProperties().getTitle()))
						.findFirst().orElseThrow(() -> new RuntimeException("Sheet not found: " + sheetName))
						.getProperties().getSheetId();

				InsertDimensionRequest insertCol = new InsertDimensionRequest().setRange(new DimensionRange()
						.setSheetId(sheetId).setDimension("COLUMNS").setStartIndex(0).setEndIndex(1))
						.setInheritFromBefore(false);

				Request req = new Request().setInsertDimension(insertCol);
				BatchUpdateSpreadsheetRequest batchReq = new BatchUpdateSpreadsheetRequest().setRequests(List.of(req));
				sheetsService.spreadsheets().batchUpdate(spreadsheetId, batchReq).execute();

				todayColIndex = 0;

				String dateCell = sheetName + "!A1";
				ValueRange dateBody = new ValueRange().setValues(List.of(List.of(today)));
				sheetsService.spreadsheets().values().update(spreadsheetId, dateCell, dateBody)
						.setValueInputOption("RAW").execute();
			}

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

				String[] rollNoset = fullRoll.split("-");
				String rollNo = "";
				if (rollNoset.length == 3) {
					if (rollNoset[1].equals(batch)) {
						rollNo = rollNoset[2].replaceFirst("^0+(?!$)", "");
					} else {
						rollNo = rollNoset[1].substring(1) + "-"
								+ rollNoset[rollNoset.length - 1].replaceFirst("^0+(?!$)", "");
					}
					rollNo = rollNo.replaceFirst("^0+(?!$)", "");
				} else if (rollNoset.length > 3 && rollNoset[1].equals(batch)) {
					rollNo = rollNoset[2].substring(1) + "-"
							+ rollNoset[rollNoset.length - 1].replaceFirst("^0+(?!$)", "");
				}

				rollNo = rollNo.replaceFirst("^0+(?!$)", ""); // remove leading zeros

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

}
