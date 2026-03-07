package com.Vcube_AdminPortal.studentDetails.excelSheetCodes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.Vcube_AdminPortal.studentDetails.model.StudentModel;
import com.Vcube_AdminPortal.studentDetails.utlity.GoogleSheetsUtil;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

@Service
public class StudentDetails {

	public List<StudentModel> getStudentsDetailsByBatchNo(String batch) {
		List<StudentModel> students = new ArrayList<>();

		try {
			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			String spreadsheetId = BatchsListApp.getSheetIdbyBatchNo(batch);
			String sheetName = "Attendance"; // or your actual tab name

			String dataRange = sheetName + "!A2:Z1000";
			ValueRange dataResponse = sheetsService.spreadsheets().values().get(spreadsheetId, dataRange).execute();
			List<List<Object>> rows = dataResponse.getValues();
			if (rows == null) {
				rows = new ArrayList<>();
			}

			for (List<Object> row : rows) {
				if (row == null || row.isEmpty()) {
					continue;
				}

				String stRollNo = row.size() > 0 ? row.get(0).toString().trim() : "";
				String stName = row.size() > 1 ? row.get(1).toString().trim() : "";
				String stMobile = row.size() > 2 ? row.get(2).toString().trim() : "";
				String stEmail = row.size() > 3 ? row.get(3).toString().trim() : "";

				if (stRollNo.isBlank() || stName.isBlank()) {
					continue;
				}

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
				students.add(stm);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return students;
	}

	public List<StudentModel> getStudentsDetailsByBatchNoWithDate(String batch, String uDate) {

		List<StudentModel> students = new ArrayList<>();

		DateTimeFormatter inFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		DateTimeFormatter outFmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		String date = LocalDate.parse(uDate, inFmt).format(outFmt);

		try {
			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			String spreadsheetId = BatchsListApp.getSheetIdbyBatchNo(batch);
			String sheetName = "Attendance"; // or your actual tab name

			// 1) Read header row to find date column index
			String headerRange = sheetName + "!A1:ZZ1";
			ValueRange headerResponse = sheetsService.spreadsheets().values().get(spreadsheetId, headerRange).execute();
			List<List<Object>> headerValues = headerResponse.getValues();
			if (headerValues == null || headerValues.isEmpty()) {
				return students;
			}

			List<Object> headerRow = headerValues.get(0);
			int dateColIndex = -1;
			for (int i = 0; i < headerRow.size(); i++) {
				Object v = headerRow.get(i);
				if (v != null && date.equals(v.toString().trim())) {
					dateColIndex = i;
					break;
				}
			}

			if (dateColIndex == -1) {
				// date column not found, return basic details without attendance
				return students;
			}

			// 2) Read data rows
			String dataRange = sheetName + "!A2:Z1000";
			ValueRange dataResponse = sheetsService.spreadsheets().values().get(spreadsheetId, dataRange).execute();
			List<List<Object>> rows = dataResponse.getValues();
			if (rows == null) {
				rows = new ArrayList<>();
			}

			for (List<Object> row : rows) {
				if (row == null || row.isEmpty()) {
					continue;
				}

				String stRollNo = row.size() > 0 ? row.get(0).toString().trim() : "";
				String stName = row.size() > 1 ? row.get(1).toString().trim() : "";
				String stMobile = row.size() > 2 ? row.get(2).toString().trim() : "";
				String stEmail = row.size() > 3 ? row.get(3).toString().trim() : "";

				if (stRollNo.isBlank() || stName.isBlank()) {
					continue;
				}

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

				// 3) Attendance value from matched date column
				String att = "";
				if (row.size() > dateColIndex && row.get(dateColIndex) != null) {
					att = row.get(dateColIndex).toString().trim(); // usually "P" or blank
				}
				stm.setMark(att);

				students.add(stm);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return students;
	}

	public List<StudentModel> getStudentsDetailsForCheckBoxByBatchNo(String batch) {
		List<StudentModel> students = new ArrayList<>();
		List<String> onlineEmails = OnlineEmailsApp.getOnlineEmailsByBatchNoWithFilter(batch);

		try {
			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			String spreadsheetId = BatchsListApp.getSheetIdbyBatchNo(batch);
			String sheetName = "Attendance";

			String dataRange = sheetName + "!A2:Z1000";
			ValueRange dataResponse = sheetsService.spreadsheets().values().get(spreadsheetId, dataRange).execute();
			List<List<Object>> rows = dataResponse.getValues();
			if (rows == null) {
				rows = new ArrayList<>();
			}

			for (List<Object> row : rows) {
				if (row == null || row.isEmpty()) {
					continue;
				}

				String stRollNo = row.size() > 0 ? row.get(0).toString().trim() : "";
				String stName = row.size() > 1 ? row.get(1).toString().trim() : "";
				String stMobile = row.size() > 2 ? row.get(2).toString().trim() : "";
				String stEmail = row.size() > 3 ? row.get(3).toString().trim() : "";

				if (stRollNo.isBlank() || stName.isBlank()) {
					continue;
				}

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
				stm.setOnline(onlineEmails.contains(stEmail));

				students.add(stm);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return students;
	}

	public StudentModel getStudentsDetailsByBatchNoAndRollNo(String batch, String rollNo) {
		StudentModel stm = new StudentModel();
		try {
			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			String spreadsheetId = BatchsListApp.getSheetIdbyBatchNo(batch);
			String sheetName = "Student-Info";

			String dataRange = sheetName + "!A2:Z1000";
			ValueRange dataResponse = sheetsService.spreadsheets().values().get(spreadsheetId, dataRange).execute();
			List<List<Object>> rows = dataResponse.getValues();
			if (rows == null) {
				rows = new ArrayList<>();
			}

			List<Object> st = null;

			for (List<Object> row : rows) {
				if (row == null || row.isEmpty()) {
					continue;
				}
				if (row.get(0).toString().trim().equalsIgnoreCase(rollNo)) {
					st = row;
					break;
				}

			}

			String stRollNo = st.get(0).toString().trim();
			String stName = st.get(1).toString().trim();
			String stPhotoURL = st.get(2).toString().trim();
			stPhotoURL = stPhotoURL != null ? stPhotoURL : "";
			String stMobile = st.get(3).toString().trim();
			String stEmail =  st.get(4).toString().trim();

			stm.setRollNo(stRollNo);
			stm.setName(stName);
			stm.setPhotoURL("https://lh3.googleusercontent.com/d/" + stPhotoURL + "=w300?authuser=0");

			if (!stMobile.isBlank()) {
				try {
					stm.setMobile(Long.parseLong(stMobile));
				} catch (NumberFormatException ex) {
					stm.setMobile(0L);
				}
			}

			stm.setEmail(stEmail);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return stm;
	}

}
