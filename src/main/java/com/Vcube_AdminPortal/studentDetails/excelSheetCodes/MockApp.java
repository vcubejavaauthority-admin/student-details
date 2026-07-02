package com.Vcube_AdminPortal.studentDetails.excelSheetCodes;

import java.io.IOException;
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

	private static String getSafeDataRange(String sheetName, int maxCols) {
		return sheetName + "!A2:" + indexToColumn(maxCols) + "1000";
	}

	private static String indexToColumn(int index) {
		return GoogleSheetsUtil.indexToColumn(index);
	}

	// Backwards compatibility for AssistantController
	public String setMockByRollNoSet(MockModel atd, String batch) {
		boolean mockMarked = false;
		DateTimeFormatter inFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		DateTimeFormatter outFmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		String date = LocalDate.parse(atd.getDate(), inFmt).format(outFmt);

		try {
			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			String spreadsheetId = BatchsListApp.getSheetIdbyBatchNo(batch);
			String sheetName = "Mock";

			int dateColIndex = findOrInsertMockColumns(sheetsService, spreadsheetId, sheetName, date);

			String headerRange = sheetName + "!A1:ZZ1";
			ValueRange headerResponse = sheetsService.spreadsheets().values().get(spreadsheetId, headerRange).execute();
			List<List<Object>> headerValues = headerResponse.getValues();
			if (headerValues == null || headerValues.isEmpty()) {
				System.out.println("No header row found");
				return null;
			}
			List<Object> headerRow = headerValues.get(0);

			int maxCols = Math.max(headerRow.size() + 10, 50);
			String dataRange = getSafeDataRange(sheetName, maxCols);
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
						String key = parts[0].trim().replaceFirst("^0+(?!$)", "");
						String marks = parts[1].trim();
						rollMarksMap.put(key, marks);
					}
				}
			}

			int aCount = 0;
			for (List<Object> row : rows) {
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

				rollNo = rollNo.replaceFirst("^0+(?!$)", "");

				if (rollMarksMap.containsKey(rollNo)) {
					while (row.size() <= dateColIndex + 2) {
						row.add("");
					}

					try {
						double marksValue = Double.parseDouble(rollMarksMap.get(rollNo));
						row.set(dateColIndex, marksValue); // Set as Technical mark
					} catch (NumberFormatException e) {
						row.set(dateColIndex, rollMarksMap.get(rollNo));
					}
					row.set(dateColIndex + 1, ""); // Communication mark empty
					row.set(dateColIndex + 2, "Added via assistant");

					aCount++;
					mockMarked = true;
				}
			}

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

	public List<StudentModel> getStudentsMockListByBatchAndDate(String batch, String uDate) {
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
					st.setTechnical("");
					st.setCommunication("");
					st.setRemarks("");

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

			String mockSheetName = "Mock";
			String dateStr = GoogleSheetsUtil.resolveDate(uDate);

			String headerRange = mockSheetName + "!A1:ZZ1";
			ValueRange headerResponse = sheetsService.spreadsheets().values().get(spreadsheetId, headerRange).execute();
			List<List<Object>> headerValues = headerResponse.getValues();

			int techColIndex = -1;
			if (headerValues != null && !headerValues.isEmpty()) {
				List<Object> headerRow = headerValues.get(0);
				for (int i = 2; i < headerRow.size(); i += 3) {
					Object v = headerRow.get(i);
					if (v != null && dateStr.equals(v.toString().trim())) {
						techColIndex = i;
						break;
					}
				}
			}

			if (techColIndex != -1) {
				int maxCols = techColIndex + 10;
				String mockRange = mockSheetName + "!A2:" + indexToColumn(maxCols) + "1000";
				ValueRange mockResponse = sheetsService.spreadsheets().values().get(spreadsheetId, mockRange).execute();
				List<List<Object>> mockRows = mockResponse.getValues();
				if (mockRows != null) {
					Map<String, List<Object>> mockMap = new HashMap<>();
					for (List<Object> row : mockRows) {
						if (row == null || row.isEmpty()) {
							continue;
						}
						String roll = row.get(0).toString().trim();
						if (!roll.isEmpty()) {
							mockMap.put(roll.toLowerCase(), row);
						}
					}

					for (StudentModel st : students) {
						List<Object> mRow = mockMap.get(st.getRollNo().toLowerCase());
						if (mRow != null) {
							String tech = mRow.size() > techColIndex && mRow.get(techColIndex) != null
									? mRow.get(techColIndex).toString().trim()
									: "";
							String comm = mRow.size() > (techColIndex + 1) && mRow.get(techColIndex + 1) != null
									? mRow.get(techColIndex + 1).toString().trim()
									: "";
							String rem = mRow.size() > (techColIndex + 2) && mRow.get(techColIndex + 2) != null
									? mRow.get(techColIndex + 2).toString().trim()
									: "";

							st.setTechnical(tech);
							st.setCommunication(comm);
							st.setRemarks(rem);
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return students;
	}

	public String saveMockMarks(String batch, String uDate, List<String> rollNos, List<String> techMarks,
			List<String> commMarks, List<String> remarksList) {
		String dateStr = GoogleSheetsUtil.resolveDate(uDate);

		try {
			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			String spreadsheetId = BatchsListApp.getSheetIdbyBatchNo(batch);
			String sheetName = "Mock";

			int dateColIndex = findOrInsertMockColumns(sheetsService, spreadsheetId, sheetName, dateStr);

			String headerRange = sheetName + "!A1:ZZ1";
			ValueRange headerResponse = sheetsService.spreadsheets().values().get(spreadsheetId, headerRange).execute();
			List<List<Object>> headerValues = headerResponse.getValues();
			int maxCols = 50;
			if (headerValues != null && !headerValues.isEmpty()) {
				maxCols = Math.max(headerValues.get(0).size() + 10, 50);
			}
			String dataRange = sheetName + "!A2:" + indexToColumn(maxCols) + "1000";
			ValueRange dataResponse = sheetsService.spreadsheets().values().get(spreadsheetId, dataRange).execute();
			List<List<Object>> rows = dataResponse.getValues();
			if (rows == null) {
				rows = new ArrayList<>();
			}

			Map<String, Integer> rollToIndexMap = new HashMap<>();
			for (int i = 0; i < rollNos.size(); i++) {
				rollToIndexMap.put(rollNos.get(i).trim().toLowerCase(), i);
			}

			for (List<Object> row : rows) {
				if (row == null || row.isEmpty()) {
					continue;
				}
				String roll = row.get(0).toString().trim();
				if (roll.isEmpty()) {
					continue;
				}

				Integer index = rollToIndexMap.get(roll.toLowerCase());
				if (index != null) {
					while (row.size() <= dateColIndex + 2) {
						row.add("");
					}

					String tech = techMarks.size() > index && techMarks.get(index) != null ? techMarks.get(index).trim() : "";
					String comm = commMarks.size() > index && commMarks.get(index) != null ? commMarks.get(index).trim() : "";
					String rem = remarksList.size() > index && remarksList.get(index) != null ? remarksList.get(index).trim() : "";

					if (!tech.isEmpty()) {
						try {
							row.set(dateColIndex, Double.parseDouble(tech));
						} catch (NumberFormatException e) {
							row.set(dateColIndex, tech);
						}
					} else {
						row.set(dateColIndex, "");
					}

					if (!comm.isEmpty()) {
						try {
							row.set(dateColIndex + 1, Double.parseDouble(comm));
						} catch (NumberFormatException e) {
							row.set(dateColIndex + 1, comm);
						}
					} else {
						row.set(dateColIndex + 1, "");
					}

					row.set(dateColIndex + 2, rem);
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

	public int findOrInsertMockColumns(Sheets sheetsService, String spreadsheetId, String sheetName, String dateStr)
			throws IOException {
		String headerRange = sheetName + "!A1:ZZ1";
		ValueRange headerResponse = sheetsService.spreadsheets().values().get(spreadsheetId, headerRange).execute();
		List<List<Object>> headerValues = headerResponse.getValues();

		List<Object> headerRow = new ArrayList<>();
		if (headerValues != null && !headerValues.isEmpty()) {
			headerRow.addAll(headerValues.get(0));
		}

		int dateColIndex = -1;
		for (int i = 2; i < headerRow.size(); i += 3) {
			Object v = headerRow.get(i);
			if (v != null && dateStr.equals(v.toString().trim())) {
				dateColIndex = i;
				break;
			}
		}

		if (dateColIndex != -1) {
			return dateColIndex;
		}

		java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("dd-MM-yyyy");
		java.util.Date targetDate = null;
		try {
			targetDate = fmt.parse(dateStr);
		} catch (Exception e) {
		}

		int insertIndex = headerRow.size();
		if (insertIndex < 2) {
			insertIndex = 2;
		} else {
			int remainder = (insertIndex - 2) % 3;
			if (remainder != 0) {
				insertIndex += (3 - remainder);
			}
		}

		if (targetDate != null) {
			for (int i = 2; i < headerRow.size(); i += 3) {
				Object v = headerRow.get(i);
				if (v != null && !v.toString().trim().isEmpty()) {
					try {
						java.util.Date colDate = fmt.parse(v.toString().trim());
						if (targetDate.before(colDate)) {
							insertIndex = i;
							break;
						}
					} catch (Exception e) {
					}
				}
			}
		}

		com.google.api.services.sheets.v4.model.Spreadsheet ss = sheetsService.spreadsheets().get(spreadsheetId)
				.execute();
		Integer sheetId = ss.getSheets().stream().filter(s -> sheetName.equals(s.getProperties().getTitle()))
				.findFirst().orElseThrow(() -> new IOException("Sheet not found: " + sheetName)).getProperties()
				.getSheetId();

		com.google.api.services.sheets.v4.model.InsertDimensionRequest insertCol = new com.google.api.services.sheets.v4.model.InsertDimensionRequest()
				.setRange(new com.google.api.services.sheets.v4.model.DimensionRange().setSheetId(sheetId)
						.setDimension("COLUMNS").setStartIndex(insertIndex).setEndIndex(insertIndex + 3));

		if (insertIndex > 0) {
			insertCol.setInheritFromBefore(true);
		}

		com.google.api.services.sheets.v4.model.Request req = new com.google.api.services.sheets.v4.model.Request()
				.setInsertDimension(insertCol);
		com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest batchReq = new com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest()
				.setRequests(Collections.singletonList(req));
		sheetsService.spreadsheets().batchUpdate(spreadsheetId, batchReq).execute();

		String colLetter1 = indexToColumn(insertIndex);
		String colLetter2 = indexToColumn(insertIndex + 2);
		String writeRange = sheetName + "!" + colLetter1 + "1:" + colLetter2 + "2";

		List<List<Object>> writeValues = new ArrayList<>();
		writeValues.add(List.of(dateStr, "", ""));
		writeValues.add(List.of("Technical", "Comunication", "Remarks"));

		ValueRange writeBody = new ValueRange().setValues(writeValues);
		sheetsService.spreadsheets().values().update(spreadsheetId, writeRange, writeBody).setValueInputOption("RAW")
				.execute();

		try {
			com.google.api.services.sheets.v4.model.GridRange gridRange = new com.google.api.services.sheets.v4.model.GridRange()
					.setSheetId(sheetId).setStartRowIndex(0).setEndRowIndex(1).setStartColumnIndex(insertIndex)
					.setEndColumnIndex(insertIndex + 3);

			com.google.api.services.sheets.v4.model.MergeCellsRequest mergeReq = new com.google.api.services.sheets.v4.model.MergeCellsRequest()
					.setRange(gridRange).setMergeType("MERGE_ALL");

			com.google.api.services.sheets.v4.model.Request mergeRequest = new com.google.api.services.sheets.v4.model.Request()
					.setMergeCells(mergeReq);

			com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest mergeBatch = new com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest()
					.setRequests(Collections.singletonList(mergeRequest));

			sheetsService.spreadsheets().batchUpdate(spreadsheetId, mergeBatch).execute();
		} catch (Exception e) {
			System.err.println("Warning: Could not merge cells for date: " + e.getMessage());
		}

		return insertIndex;
	}

	public List<MockModel> getStudentsMockDetailsByBatchNo(String batch) {
		List<MockModel> mocks = new ArrayList<>();

		try {
			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			String spreadsheetId = BatchsListApp.getSheetIdbyBatchNo(batch);
			String sheetName = "Mock";

			int maxCols = 50;
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

			// In new structure: Header size is total columns. Total mocks = (headerSize - 2) / 3
			int headerSize = (headerValues != null && !headerValues.isEmpty()) ? headerValues.get(0).size() : 2;
			int totalMocks = (headerSize - 2) / 3;
			if (totalMocks < 0) {
				totalMocks = 0;
			}

			for (List<Object> row : rows) {
				if (row == null || row.isEmpty() || !row.get(0).toString().trim().contains("JFS")) {
					continue;
				}

				String stRollNo = row.size() > 0 ? row.get(0).toString().trim() : "";
				String stName = row.size() > 1 ? row.get(1).toString().trim() : "";

				if (stRollNo.isBlank() || stName.isBlank()) {
					continue;
				}

				StudentModel stm = new StudentModel();
				stm.setRollNo(stRollNo);
				stm.setName(stName);
				
				int studentTotalMocks = 0;
				int studentTotalExamsMarks = 0;

				// Each mock has 3 columns starting at index 2 (Technical, Comunication, Remarks)
				for (int c = 2; c < row.size(); c += 3) {
					String techVal = row.size() > c && row.get(c) != null ? row.get(c).toString().trim() : "";
					String commVal = row.size() > (c + 1) && row.get(c + 1) != null ? row.get(c + 1).toString().trim() : "";
					
					boolean techGiven = !techVal.isEmpty();
					boolean commGiven = !commVal.isEmpty();

					if (techGiven || commGiven) {
						studentTotalMocks++;
						double mockScore = 0.0;
						if (techGiven) {
							try {
								mockScore += Double.parseDouble(techVal);
							} catch (NumberFormatException e) {}
						}
						if (commGiven) {
							try {
								mockScore += Double.parseDouble(commVal);
							} catch (NumberFormatException e) {}
						}
						studentTotalExamsMarks += mockScore;
					}
				}

				MockModel mock = new MockModel();
				mock.setBatch(batch);
				mock.setStudent(stm);
				mock.setStudentTotalMocks(studentTotalMocks);
				mock.setStudentTotalMocksMarks(studentTotalExamsMarks);
				if (studentTotalExamsMarks > 0 && totalMocks > 0) {
					double percent = (studentTotalExamsMarks * 100.0) / (totalMocks * 10.0);
					percent = Math.round(percent * 100.0) / 100.0;
					mock.setStudentPercentage(percent);
				} else {
					mock.setStudentPercentage(0.0);
				}
				mocks.add(mock);
			}

			for (MockModel m : mocks) {
				m.setTotalMocks(totalMocks);
				m.setTotalMocksMarks(totalMocks * 10);
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

			String headerRange = sheetName + "!A1:ZZ1";
			ValueRange headerResponse = sheetsService.spreadsheets().values().get(spreadsheetId, headerRange).execute();
			List<List<Object>> headerValues = headerResponse.getValues();
			if (headerValues == null || headerValues.isEmpty()) {
				System.out.println("No header row found");
				return null;
			}

			List<Object> headerRow = headerValues.get(0);
			int maxCols = Math.max(headerRow.size() + 10, 50);
			String dataRange = getSafeDataRange(sheetName, maxCols);

			ValueRange dataResponse = sheetsService.spreadsheets().values().get(spreadsheetId, dataRange).execute();
			List<List<Object>> rows = dataResponse.getValues();
			if (rows == null || rows.isEmpty()) {
				return exam;
			}

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
			List<String> techMarksList = new ArrayList<>();
			List<String> commMarksList = new ArrayList<>();
			List<String> remarksList = new ArrayList<>();

			int totalExams = 0;
			int studentTotalExams = 0;
			int studentTotalExamsMarks = 0;

			for (int col = 2; col < headerRow.size(); col += 3) {
				String dateHeader = headerRow.get(col) != null ? headerRow.get(col).toString().trim() : "";
				if (dateHeader.isEmpty()) {
					continue;
				}

				String tech = (col < studentRow.size() && studentRow.get(col) != null)
						? studentRow.get(col).toString().trim()
						: "";
				String comm = ((col + 1) < studentRow.size() && studentRow.get(col + 1) != null)
						? studentRow.get(col + 1).toString().trim()
						: "";
				String rem = ((col + 2) < studentRow.size() && studentRow.get(col + 2) != null)
						? studentRow.get(col + 2).toString().trim()
						: "";

				dates.add(dateHeader);
				totalExams++;

				boolean techGiven = !tech.isEmpty();
				boolean commGiven = !comm.isEmpty();

				if (techGiven || commGiven) {
					studentTotalExams++;
					double mockScore = 0.0;
					if (techGiven) {
						try {
							mockScore += Double.parseDouble(tech);
						} catch (NumberFormatException ex) {}
					}
					if (commGiven) {
						try {
							mockScore += Double.parseDouble(comm);
						} catch (NumberFormatException ex) {}
					}
					studentTotalExamsMarks += mockScore;
					
					techMarksList.add(tech.isEmpty() ? "0" : tech);
					commMarksList.add(comm.isEmpty() ? "0" : comm);
					remarksList.add(rem);
					marks.add(String.valueOf(mockScore)); // legacy marks field
				} else {
					techMarksList.add("Not-Given");
					commMarksList.add("Not-Given");
					remarksList.add("");
					marks.add("Not-Given");
				}
			}

			if (studentTotalExamsMarks > 0 && totalExams > 0) {
				double percent = (studentTotalExamsMarks * 100.0) / (totalExams * 10.0);
				percent = Math.round(percent * 100.0) / 100.0;
				exam.setStudentPercentage(percent);
			} else {
				exam.setStudentPercentage(0.0);
			}

			exam.setDates(dates);
			exam.setMarks(marks);
			exam.setTechnicalMarks(techMarksList);
			exam.setCommunicationMarks(commMarksList);
			exam.setRemarks(remarksList);
			exam.setStudentTotalMocks(studentTotalExams);
			exam.setStudentTotalMocksMarks(studentTotalExamsMarks);
			exam.setTotalMocks(totalExams);
			exam.setTotalMocksMarks(totalExams * 10);

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return exam;
	}

}
