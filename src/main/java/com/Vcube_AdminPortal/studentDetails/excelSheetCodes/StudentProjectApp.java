package com.Vcube_AdminPortal.studentDetails.excelSheetCodes;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.Vcube_AdminPortal.studentDetails.model.ProjectModel;
import com.Vcube_AdminPortal.studentDetails.model.ProjectSubmissionModel;
import com.Vcube_AdminPortal.studentDetails.utlity.GoogleSheetsUtil;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;

@Service
public class StudentProjectApp {

	// ✅ FIXED: Dynamic column range calculation
	private static String getSafeDataRange(String sheetName, int maxCols) {
		return sheetName + "!A2:" + indexToColumn(maxCols) + "1000";
	}

	// ✅ Helper method for safe column conversion
	private static String indexToColumn(int index) {
		return GoogleSheetsUtil.indexToColumn(index);
	}

	public  ProjectModel getAllProjectsDetailsWithStatusByBatchAndRolNo(String batch, String rollNo) {

		ProjectModel project = new ProjectModel();

		try {

			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			String spreadsheetId = BatchsListApp.getSheetIdbyBatchNo(batch);
			String sheetName = findSheetName(sheetsService, spreadsheetId, "Projects");
			if (sheetName == null) {
				System.err.println("Sheet 'Projects' not found in spreadsheet: " + spreadsheetId);
				return project;
			}

			// 1) Read header rows (Row 1 and Row 2)
			String headerRange = sheetName + "!A1:ZZ2";
			ValueRange headerResponse = sheetsService.spreadsheets().values().get(spreadsheetId, headerRange).execute();
			List<List<Object>> headerValues = headerResponse.getValues();
			if (headerValues == null || headerValues.isEmpty()) {
				System.out.println("No header row found");
				return null;
			}

			List<Object> row1 = headerValues.get(0);
			List<Object> row2 = headerValues.size() > 1 ? headerValues.get(1) : new ArrayList<>();

			// ✅ FIXED: Dynamic range based on header size + buffer
			int maxCols = Math.max(row2.size() + 10, 50); // 50 columns minimum
			String dataRange = sheetName + "!A3:" + indexToColumn(maxCols) + "1000"; // Students start at Row 3

			// 2) Read all data rows
			ValueRange dataResponse = sheetsService.spreadsheets().values().get(spreadsheetId, dataRange).execute();
			List<List<Object>> rows = dataResponse.getValues();
			if (rows == null || rows.isEmpty()) {
				return project;
			}

			// 2. Find student row by rollNo (column 0)
			List<Object> studentRow = null;
			for (int i = 0; i < rows.size(); i++) {
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

			// Process columns starting from column 2 (index 2)
			for (int col = 2; col < Math.max(row2.size(), studentRow.size()); col++) {
				String projectTitle = (col < row2.size() && row2.get(col) != null)
						? row2.get(col).toString().trim()
						: "";
				String status = (col < studentRow.size() && studentRow.get(col) != null)
						? studentRow.get(col).toString().trim()
						: "";

				if (projectTitle.isEmpty())
					continue;

				projects.add(projectTitle);
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

	public boolean saveProjectSubmission(com.Vcube_AdminPortal.studentDetails.model.ProjectSubmissionModel submission) {
		try {
			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			String spreadsheetId = BatchsListApp.getSheetIdbyBatchNo(submission.getBatch());
			String sheetName = findSheetName(sheetsService, spreadsheetId, "Projects");
			if (sheetName == null) return false;

			// 1) Read headers to find column mappings
			String headerRange = sheetName + "!A1:ZZ2";
			ValueRange headerResponse = sheetsService.spreadsheets().values().get(spreadsheetId, headerRange).execute();
			List<List<Object>> headerValues = headerResponse.getValues();
			if (headerValues == null || headerValues.size() < 2) return false;

			List<Object> row1 = headerValues.get(0);
			List<Object> row2 = headerValues.get(1);

			// 2) Find student row
			String dataRange = sheetName + "!A3:A1000";
			ValueRange dataResponse = sheetsService.spreadsheets().values().get(spreadsheetId, dataRange).execute();
			List<List<Object>> rows = dataResponse.getValues();
			int studentRowIndex = -1;
			if (rows != null) {
				for (int i = 0; i < rows.size(); i++) {
					if (!rows.get(i).isEmpty() && submission.getRollNo().equalsIgnoreCase(rows.get(i).get(0).toString().trim())) {
						studentRowIndex = i + 3; // +3 because students start at row 3
						break;
					}
				}
			}

			if (studentRowIndex == -1) return false;

			// 3) Prepare updates
			List<ValueRange> data = new ArrayList<>();

			// Map for Core Java (Checkboxes)
			for (int col = 0; col < row2.size(); col++) {
				String title = row2.get(col).toString().trim();
				if (title.isEmpty()) continue;

				// Check if this column is under Core Java or Web Apps
				String category = getCategoryForColumn(col, row1);
				
				if ("Core-Java-Console-Projects".equalsIgnoreCase(category)) {
					String value = submission.getCoreJavaProjects() != null && submission.getCoreJavaProjects().contains(title) ? "Done" : "";
					String range = sheetName + "!" + indexToColumn(col) + studentRowIndex;
					data.add(new ValueRange().setRange(range).setValues(List.of(List.of(value))));
				} else if ("Web-Applications".equalsIgnoreCase(category)) {
					String value = submission.getWebApplications() != null ? submission.getWebApplications().get(title) : null;
					if (value != null) {
						String range = sheetName + "!" + indexToColumn(col) + studentRowIndex;
						data.add(new ValueRange().setRange(range).setValues(List.of(List.of(value))));
					}
				}
			}

			if (!data.isEmpty()) {
				com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest body = new com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest()
						.setValueInputOption("RAW")
						.setData(data);
				sheetsService.spreadsheets().values().batchUpdate(spreadsheetId, body).execute();
			}

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private String findSheetName(Sheets service, String spreadsheetId, String target) throws java.io.IOException {
		com.google.api.services.sheets.v4.model.Spreadsheet ss = service.spreadsheets().get(spreadsheetId).execute();
		for (com.google.api.services.sheets.v4.model.Sheet sheet : ss.getSheets()) {
			String name = sheet.getProperties().getTitle();
			if (name.equalsIgnoreCase(target)) return name;
		}
		return null;
	}

	private String getCategoryForColumn(int col, List<Object> row1) {
		// Iterate backwards from the column to find the merged header in row 1
		for (int i = col; i >= 0; i--) {
			if (i < row1.size() && row1.get(i) != null && !row1.get(i).toString().trim().isEmpty()) {
				return row1.get(i).toString().trim();
			}
		}
		return "";
	}

	public List<String> getProjectHeaders(String batch, String category) {
		List<String> headers = new ArrayList<>();
		try {
			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			String spreadsheetId = BatchsListApp.getSheetIdbyBatchNo(batch);
			String sheetName = findSheetName(sheetsService, spreadsheetId, "Projects");
			if (sheetName == null) return headers;

			String headerRange = sheetName + "!A1:ZZ2";
			ValueRange headerResponse = sheetsService.spreadsheets().values().get(spreadsheetId, headerRange).execute();
			List<List<Object>> headerValues = headerResponse.getValues();
			if (headerValues == null || headerValues.size() < 2) return headers;

			List<Object> row1 = headerValues.get(0);
			List<Object> row2 = headerValues.get(1);

			for (int col = 0; col < row2.size(); col++) {
				if (category.equalsIgnoreCase(getCategoryForColumn(col, row1))) {
					headers.add(row2.get(col).toString().trim());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return headers;
	}

	public List<ProjectModel> getStudentsProjectDetailsByBatchNo(String batch) {
		List<ProjectModel> projectModels = new ArrayList<>();
		try {
			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			String spreadsheetId = BatchsListApp.getSheetIdbyBatchNo(batch);
			if (spreadsheetId == null) return projectModels;
			String sheetName = findSheetName(sheetsService, spreadsheetId, "Projects");
			if (sheetName == null) return projectModels;

			String headerRange = sheetName + "!A1:ZZ2";
			ValueRange headerResp = sheetsService.spreadsheets().values().get(spreadsheetId, headerRange).execute();
			List<List<Object>> headerValues = headerResp.getValues();
			if (headerValues == null || headerValues.size() < 2) return projectModels;
			List<Object> row2 = headerValues.get(1);

			int maxCols = Math.max(row2.size() + 10, 50);
			String dataRange = sheetName + "!A3:" + indexToColumn(maxCols) + "1000";
			ValueRange dataResp = sheetsService.spreadsheets().values().get(spreadsheetId, dataRange).execute();
			List<List<Object>> rows = dataResp.getValues();
			if (rows == null || rows.isEmpty()) return projectModels;

			int totalMaxProjects = 0;
			for (int col = 2; col < row2.size(); col++) {
				if (row2.get(col) != null && !row2.get(col).toString().trim().isEmpty()) {
					totalMaxProjects++;
				}
			}

			// Map to fetch student names
			List<com.Vcube_AdminPortal.studentDetails.model.StudentModel> students = new StudentDetails().getStudentsDetailsByBatchNo(batch);
			java.util.Map<String, String> nameMap = new java.util.HashMap<>();
			if (students != null) {
				for (com.Vcube_AdminPortal.studentDetails.model.StudentModel s : students) {
					nameMap.put(s.getRollNo(), s.getName());
				}
			}

			for (List<Object> row : rows) {
				if (row == null || row.isEmpty()) continue;
				String rollNo = row.get(0) != null ? row.get(0).toString().trim() : "";
				if (rollNo.isBlank() || !rollNo.contains("JFS")) continue;

				String name = (row.size() > 1 && row.get(1) != null) ? row.get(1).toString().trim() : nameMap.getOrDefault(rollNo, "");

				ProjectModel pm = new ProjectModel();
				pm.setRollNo(rollNo);

				com.Vcube_AdminPortal.studentDetails.model.StudentModel sm = new com.Vcube_AdminPortal.studentDetails.model.StudentModel();
				sm.setRollNo(rollNo);
				sm.setName(name);
				pm.setStudent(sm);

				List<String> projectNames = new ArrayList<>();
				List<String> projectStatus = new ArrayList<>();
				int projectsDone = 0;

				for (int col = 2; col < Math.max(row2.size(), row.size()); col++) {
					String projectTitle = (col < row2.size() && row2.get(col) != null) ? row2.get(col).toString().trim() : "";
					String pStatus = (col < row.size() && row.get(col) != null) ? row.get(col).toString().trim() : "";

					if (projectTitle.isEmpty()) continue;
					
					projectNames.add(projectTitle);
					if (!pStatus.isEmpty() && !pStatus.equalsIgnoreCase("Not-Done") && !pStatus.equalsIgnoreCase("Not Done")) {
						projectStatus.add(pStatus);
						projectsDone++;
					} else {
						projectStatus.add("Not-Done");
					}
				}

				pm.setProjectNames(projectNames);
				pm.setProjectStatus(projectStatus);
				pm.setTotalProjects(totalMaxProjects);
				pm.setStudentTotalProjects(projectsDone);
				projectModels.add(pm);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return projectModels;
	}
	
	

}
