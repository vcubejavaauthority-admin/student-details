package com.Vcube_AdminPortal.studentDetails.excelSheetCodes;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.Vcube_AdminPortal.studentDetails.model.ProjectModel;
import com.Vcube_AdminPortal.studentDetails.utlity.GoogleSheetsUtil;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

@Service
public class StudentProjectApp {

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

	public  ProjectModel getAllProjectsDetailsWithStatusByBatchAndRolNo(String batch, String rollNo) {

		ProjectModel project = new ProjectModel();

		try {

			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			String spreadsheetId = BatchsListApp.getSheetIdbyBatchNo(batch);
			String sheetName = "Projects";

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
				return project;
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
				return project;
			}

			project.setRollNo(
					studentRow.get(0).toString().trim().contains("JFS") ? studentRow.get(0).toString().trim() : "");

			List<String> projects = new ArrayList<>();
			List<String> projectStatus = new ArrayList<>();
			int totalProjects = 0, studentTotalProjects = 0;

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
				projects.add(dateHeader);
				totalProjects++;
				if (!status.isEmpty()) {
					projectStatus.add(status.trim());
					studentTotalProjects++;

				} else {
					projectStatus.add("Not-Done");
				}

			}

			project.setProjectNames(projects);
			project.setProjectStatus(projectStatus);
			project.setTotalProjects(totalProjects);
			project.setStudentTotalProjects(studentTotalProjects);

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return project;
	}
	
	

}
