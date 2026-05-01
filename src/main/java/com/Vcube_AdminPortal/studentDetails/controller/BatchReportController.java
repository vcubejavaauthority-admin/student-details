package com.Vcube_AdminPortal.studentDetails.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.AttendanceApp;
import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.BatchsListApp;
import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.StudentsRollNosApp;
import com.Vcube_AdminPortal.studentDetails.model.BatchAttendanceReportModel;
import com.Vcube_AdminPortal.studentDetails.utlity.GoogleSheetsUtil;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

@Controller
public class BatchReportController {

	@Autowired
	private AttendanceApp atda;

	private List<String> getAvailableBatches() {
		return BatchsListApp.getAllBatchesList();
	}

	/**
	 * Page endpoint — renders the batch report page with filters and chart containers
	 */
	@GetMapping("/batch-report")
	public String showBatchReport(Model model) {
		model.addAttribute("batches", getAvailableBatches());
		return "batch-report";
	}

	// ═══════════════════════════════════════════
	// API: Available Dates from Sheet Headers
	// ═══════════════════════════════════════════
	/**
	 * Returns available month/year combos and min/max dates from a sheet's header row.
	 * sheetType: "attendance" (col 4+), "exam" (col 2+), "mock" (col 2+)
	 */
	@GetMapping("/api/batch-available-dates")
	@ResponseBody
	public Map<String, Object> getAvailableDates(
			@RequestParam String batch,
			@RequestParam(defaultValue = "attendance") String sheetType) {

		Map<String, Object> result = new HashMap<>();
		result.put("months", new ArrayList<>());
		result.put("minDate", "");
		result.put("maxDate", "");

		try {
			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			String spreadsheetId = BatchsListApp.getSheetIdbyBatchNo(batch);
			if (spreadsheetId == null) return result;

			String sheetName;
			int dateStartCol;
			switch (sheetType.toLowerCase()) {
				case "exam":
					sheetName = "Exam";
					dateStartCol = 2;
					break;
				case "mock":
					sheetName = "Mock";
					dateStartCol = 2;
					break;
				default:
					sheetName = "Attendance";
					dateStartCol = 4;
					break;
			}

			String headerRange = sheetName + "!A1:ZZ1";
			ValueRange headerResponse = sheetsService.spreadsheets().values()
					.get(spreadsheetId, headerRange).execute();
			List<List<Object>> headerValues = headerResponse.getValues();
			if (headerValues == null || headerValues.isEmpty()) return result;

			List<Object> headerRow = headerValues.get(0);
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
			String[] monthNames = {"January", "February", "March", "April", "May", "June",
					"July", "August", "September", "October", "November", "December"};

			// Parse all date columns
			TreeSet<String> monthYearSet = new TreeSet<>(); // "yyyy-MM" for sorting
			Date minDate = null;
			Date maxDate = null;

			for (int col = dateStartCol; col < headerRow.size(); col++) {
				Object v = headerRow.get(col);
				if (v == null) continue;
				String dateStr = v.toString().trim();
				if (dateStr.isEmpty()) continue;

				try {
					Date d = sdf.parse(dateStr);
					if (d != null) {
						Calendar cal = Calendar.getInstance();
						cal.setTime(d);
						int m = cal.get(Calendar.MONTH) + 1; // 1-based
						int y = cal.get(Calendar.YEAR);
						monthYearSet.add(String.format("%04d-%02d", y, m));

						if (minDate == null || d.before(minDate)) minDate = d;
						if (maxDate == null || d.after(maxDate)) maxDate = d;
					}
				} catch (ParseException e) {
					// Skip non-date columns
				}
			}

			// Build response
			List<Map<String, Object>> monthsList = new ArrayList<>();
			for (String my : monthYearSet) {
				String[] parts = my.split("-");
				int year = Integer.parseInt(parts[0]);
				int month = Integer.parseInt(parts[1]);

				Map<String, Object> entry = new HashMap<>();
				entry.put("month", month);
				entry.put("year", year);
				entry.put("label", monthNames[month - 1] + " " + year);
				monthsList.add(entry);
			}
			result.put("months", monthsList);

			SimpleDateFormat isoFmt = new SimpleDateFormat("yyyy-MM-dd");
			if (minDate != null) result.put("minDate", isoFmt.format(minDate));
			if (maxDate != null) result.put("maxDate", isoFmt.format(maxDate));

		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	// ═══════════════════════════════════════════
	// API: Batch Exam Report (filtered by date)
	// ═══════════════════════════════════════════
	@GetMapping("/api/batch-exam-report")
	@ResponseBody
	public Object getBatchExamReport(
			@RequestParam String batch,
			@RequestParam(required = false) List<String> months,
			@RequestParam(required = false) String startDate,
			@RequestParam(required = false) String endDate,
			@RequestParam(required = false, defaultValue = "all") String studentStatus) {

		if ("ALL".equalsIgnoreCase(batch)) {
			List<String> batches = getAvailableBatches();
			List<Map<String, Object>> aggregated = new ArrayList<>();
			for (String b : batches) {
				Map<String, Object> r = getMarksReport(b, "Exam", 2, months, startDate, endDate, studentStatus);
				if (r != null && (int) r.get("totalStudents") > 0) {
					aggregated.add(r);
				}
			}
			return aggregated;
		}

		return getMarksReport(batch, "Exam", 2, months, startDate, endDate, studentStatus);
	}

	// ═══════════════════════════════════════════
	// API: Batch Mock Report (filtered by date)
	// ═══════════════════════════════════════════
	@GetMapping("/api/batch-mock-report")
	@ResponseBody
	public Object getBatchMockReport(
			@RequestParam String batch,
			@RequestParam(required = false) List<String> months,
			@RequestParam(required = false) String startDate,
			@RequestParam(required = false) String endDate,
			@RequestParam(required = false, defaultValue = "all") String studentStatus) {

		if ("ALL".equalsIgnoreCase(batch)) {
			List<String> batches = getAvailableBatches();
			List<Map<String, Object>> aggregated = new ArrayList<>();
			for (String b : batches) {
				Map<String, Object> r = getMarksReport(b, "Mock", 2, months, startDate, endDate, studentStatus);
				if (r != null && (int) r.get("totalStudents") > 0) {
					aggregated.add(r);
				}
			}
			return aggregated;
		}
		return getMarksReport(batch, "Mock", 2, months, startDate, endDate, studentStatus);
	}

	// ═══════════════════════════════════════════
	// API: Batch Project Report
	// ═══════════════════════════════════════════
	@GetMapping("/api/batch-project-report")
	@ResponseBody
	public Object getBatchProjectReport(
			@RequestParam String batch,
			@RequestParam(required = false, defaultValue = "all") String studentStatus) {

		if ("ALL".equalsIgnoreCase(batch)) {
			List<String> batches = getAvailableBatches();
			List<Map<String, Object>> aggregated = new ArrayList<>();
			for (String b : batches) {
				Map<String, Object> r = getProjectsReport(b, studentStatus);
				if (r != null && (int) r.get("totalStudents") > 0) {
					aggregated.add(r);
				}
			}
			return aggregated;
		}
		return getProjectsReport(batch, studentStatus);
	}

	private Map<String, Object> getProjectsReport(String batch, String filterStatus) {
		Map<String, Object> report = new HashMap<>();
		report.put("batch", batch);
		report.put("sheetType", "projects");
		report.put("students", new ArrayList<>());
		report.put("totalStudents", 0);
		report.put("maxProjects", 0);
		report.put("totalProjectsCount", 0);
		report.put("averageProjectsDone", 0);

		try {
			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			String spreadsheetId = BatchsListApp.getSheetIdbyBatchNo(batch);
			if (spreadsheetId == null) return report;

			String sheetName = "Projects";

			String headerRange = sheetName + "!A1:ZZ2";
			ValueRange headerResp = sheetsService.spreadsheets().values().get(spreadsheetId, headerRange).execute();
			List<List<Object>> headerValues = headerResp.getValues();
			if (headerValues == null || headerValues.size() < 2) return report;
			List<Object> row2 = headerValues.get(1);

			int maxCols = Math.max(row2.size() + 10, 50);
			String dataRange = sheetName + "!A3:" + indexToColumn(maxCols) + "1000";
			ValueRange dataResp = sheetsService.spreadsheets().values().get(spreadsheetId, dataRange).execute();
			List<List<Object>> rows = dataResp.getValues();
			if (rows == null || rows.isEmpty()) return report;

			Map<String, String> statusMap = StudentsRollNosApp.getStudentsStatusByBatchNo(batch);

			List<Map<String, Object>> studentList = new ArrayList<>();
			int totalStudents = 0;
			int totalProjectsDoneAll = 0;
			int totalMaxProjects = 0;

			for (int col = 2; col < row2.size(); col++) {
				if (row2.get(col) != null && !row2.get(col).toString().trim().isEmpty()) {
					totalMaxProjects++;
				}
			}

			for (List<Object> row : rows) {
				if (row == null || row.isEmpty()) continue;
				String rollNo = row.get(0) != null ? row.get(0).toString().trim() : "";
				String name = row.size() > 1 && row.get(1) != null ? row.get(1).toString().trim() : "";
				if (rollNo.isBlank() || name.isBlank()) continue;

				String status = statusMap.getOrDefault(rollNo, "active");
				
				if (filterStatus != null && !filterStatus.equalsIgnoreCase("all") && !status.equalsIgnoreCase(filterStatus)) {
					continue;
				}

				int projectsDone = 0;

				for (int col = 2; col < Math.max(row2.size(), row.size()); col++) {
					String projectTitle = (col < row2.size() && row2.get(col) != null) ? row2.get(col).toString().trim() : "";
					String pStatus = (col < row.size() && row.get(col) != null) ? row.get(col).toString().trim() : "";

					if (projectTitle.isEmpty()) continue;

					if (!pStatus.isEmpty() && !pStatus.equalsIgnoreCase("Not-Done") && !pStatus.equalsIgnoreCase("Not Done")) {
						projectsDone++;
					}
				}

				Map<String, Object> st = new HashMap<>();
				st.put("rollNo", rollNo);
				st.put("name", name);
				st.put("projectsDone", projectsDone);
				st.put("totalProjects", totalMaxProjects);
				st.put("status", status);
				studentList.add(st);

				totalStudents++;
				totalProjectsDoneAll += projectsDone;
			}

			report.put("students", studentList);
			report.put("totalStudents", totalStudents);
			report.put("maxProjects", totalMaxProjects);
			report.put("totalProjectsCount", totalProjectsDoneAll);
			report.put("averageProjectsDone", totalStudents > 0 ? Math.round((double) totalProjectsDoneAll / totalStudents) : 0);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return report;
	}

	/**
	 * Generic marks report builder for Exam/Mock sheets.
	 * Filters date columns within the given period and aggregates marks per student.
	 */
	private Map<String, Object> getMarksReport(String batch, String sheetName, int dateStartCol,
			List<String> selectedMonths, String startDateStr, String endDateStr, String filterStatus) {

		Map<String, Object> report = new HashMap<>();
		report.put("batch", batch);
		report.put("sheetType", sheetName.toLowerCase());
		report.put("students", new ArrayList<>());
		report.put("totalStudents", 0);
		report.put("totalExams", 0);
		report.put("averagePercentage", 0.0);

		try {
			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			String spreadsheetId = BatchsListApp.getSheetIdbyBatchNo(batch);
			if (spreadsheetId == null) return report;

			// Read header
			String headerRange = sheetName + "!A1:ZZ1";
			ValueRange headerResp = sheetsService.spreadsheets().values()
					.get(spreadsheetId, headerRange).execute();
			List<List<Object>> headerValues = headerResp.getValues();
			if (headerValues == null || headerValues.isEmpty()) return report;
			List<Object> headerRow = headerValues.get(0);

			// Read data
			int maxCols = Math.max(headerRow.size() + 10, 50);
			String colLetter = indexToColumn(maxCols);
			String dataRange = sheetName + "!A2:" + colLetter + "1000";
			ValueRange dataResp = sheetsService.spreadsheets().values()
					.get(spreadsheetId, dataRange).execute();
			List<List<Object>> rows = dataResp.getValues();
			if (rows == null || rows.isEmpty()) return report;

			// Fetch status map
			Map<String, String> statusMap = StudentsRollNosApp.getStudentsStatusByBatchNo(batch);

			// Resolve date range or specific months
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
			Date start = null, end = null;
			Set<String> selectedMonthYearSet = null;

			if (startDateStr != null && endDateStr != null && !startDateStr.isBlank() && !endDateStr.isBlank()) {
				DateTimeFormatter inFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
				DateTimeFormatter outFmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
				String s = LocalDate.parse(startDateStr, inFmt).format(outFmt);
				String e = LocalDate.parse(endDateStr, inFmt).format(outFmt);
				try { start = sdf.parse(s); end = sdf.parse(e); } catch (ParseException ex) {}
			} else if (selectedMonths != null && !selectedMonths.isEmpty()) {
				selectedMonthYearSet = new HashSet<>();
				for (String m : selectedMonths) {
					// Expecting "M-YYYY" or "MM-YYYY"
					String[] parts = m.split("-");
					if (parts.length == 2) {
						int mon = Integer.parseInt(parts[0]);
						int yr = Integer.parseInt(parts[1]);
						selectedMonthYearSet.add(String.format("%d-%d", mon, yr));
					}
				}
			}

			// Find filtered date column indices
			List<Integer> filteredCols = new ArrayList<>();
			for (int col = dateStartCol; col < headerRow.size(); col++) {
				String dateHeader = headerRow.get(col) != null ? headerRow.get(col).toString().trim() : "";
				if (dateHeader.isEmpty()) continue;

				try {
					Date colDate = sdf.parse(dateHeader);
					if (colDate != null) {
						if (start != null && end != null) {
							if (!colDate.before(start) && !colDate.after(end)) {
								filteredCols.add(col);
							}
						} else if (selectedMonthYearSet != null) {
							Calendar cal = Calendar.getInstance();
							cal.setTime(colDate);
							int m = cal.get(Calendar.MONTH) + 1;
							int y = cal.get(Calendar.YEAR);
							if (selectedMonthYearSet.contains(m + "-" + y)) {
								filteredCols.add(col);
							}
						} else {
							// No filter -> include all
							filteredCols.add(col);
						}
					}
				} catch (ParseException e) {
					// Not a date column, skip
				}
			}

			int totalExams = filteredCols.size();
			report.put("totalExams", totalExams);

			// Process each student
			List<Map<String, Object>> studentList = new ArrayList<>();
			int totalStudents = 0;
			double sumPercentage = 0;
			int passCount = 0;
			int failCount = 0;
			int maxMarksPerExam = sheetName.equalsIgnoreCase("Mock") ? 5 : 10;
			int totalMaxMarks = totalExams * maxMarksPerExam;

			for (List<Object> row : rows) {
				if (row == null || row.isEmpty()) continue;
				String rollNo = row.get(0) != null ? row.get(0).toString().trim() : "";
				String name = row.size() > 1 && row.get(1) != null ? row.get(1).toString().trim() : "";
				if (rollNo.isBlank() || name.isBlank()) continue;

				String status = statusMap.getOrDefault(rollNo, "active");
				
				// Filter by status if provided
				if (filterStatus != null && !filterStatus.equalsIgnoreCase("all") && !status.equalsIgnoreCase(filterStatus)) {
					continue;
				}

				int attended = 0;
				double totalMarks = 0;

				for (int col : filteredCols) {
					String val = (col < row.size() && row.get(col) != null)
							? row.get(col).toString().trim() : "";
					if (!val.isEmpty()) {
						try {
							totalMarks += Double.parseDouble(val);
							attended++;
						} catch (NumberFormatException e) {
							// Non-numeric, skip
						}
					}
				}

				double pct = totalMaxMarks > 0 ? Math.round(totalMarks / totalMaxMarks * 10000.0) / 100.0 : 0.0;
				boolean passed = pct >= 50.0;

				Map<String, Object> st = new HashMap<>();
				st.put("rollNo", rollNo);
				st.put("name", name);
				st.put("attended", attended);
				st.put("totalExams", totalExams);
				st.put("totalMarks", totalMarks);
				st.put("percentage", pct);
				st.put("passed", passed);
				st.put("status", status);
				studentList.add(st);

				totalStudents++;
				sumPercentage += pct;
				if (passed) passCount++;
				else failCount++;
			}

			report.put("students", studentList);
			report.put("totalStudents", totalStudents);
			report.put("passCount", passCount);
			report.put("failCount", failCount);
			report.put("averagePercentage", totalStudents > 0
					? Math.round(sumPercentage / totalStudents * 100.0) / 100.0 : 0.0);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return report;
	}

	private static String indexToColumn(int index) {
		return GoogleSheetsUtil.indexToColumn(index);
	}

	// ═══════════════════════════════════════════
	// EXISTING: Batch Attendance Report API
	// ═══════════════════════════════════════════
	/**
	 * JSON API endpoint — returns attendance report data for Chart.js rendering.
	 * Supports two modes:
	 *   1. Multiple Months mode: ?batch=B59&months=3-2026,4-2026
	 *   2. Custom Range mode: ?batch=B59&startDate=2026-03-01&endDate=2026-03-31
	 * If batch is "ALL", aggregates across all batches.
	 */
	@GetMapping("/api/batch-attendance-report")
	@ResponseBody
	public Object getBatchAttendanceReport(
			@RequestParam String batch,
			@RequestParam(required = false) List<String> months,
			@RequestParam(required = false) String startDate,
			@RequestParam(required = false) String endDate,
			@RequestParam(required = false, defaultValue = "all") String studentStatus) {

		// Resolve date range
		String start = null;
		String end = null;

		if (startDate != null && endDate != null && !startDate.isBlank() && !endDate.isBlank()) {
			// Custom range mode (input: yyyy-MM-dd → convert to dd-MM-yyyy)
			DateTimeFormatter inFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			DateTimeFormatter outFmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
			start = LocalDate.parse(startDate, inFmt).format(outFmt);
			end = LocalDate.parse(endDate, inFmt).format(outFmt);
		} else if (months == null || months.isEmpty()) {
			// Default: current month (only if not custom range and no specific months)
			if (!"ALL".equalsIgnoreCase(batch)) {
				YearMonth ym = YearMonth.now();
				DateTimeFormatter outFmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
				start = ym.atDay(1).format(outFmt);
				end = ym.atEndOfMonth().format(outFmt);
			}
		}

		if ("ALL".equalsIgnoreCase(batch)) {
			// Fetch reports for all batches in parallel
			List<String> batches = getAvailableBatches();
			if (batches == null || batches.isEmpty()) {
				return new ArrayList<>();
			}

			List<CompletableFuture<BatchAttendanceReportModel>> futures = new ArrayList<>();
			final String fStart = start;
			final String fEnd = end;
			final List<String> fMonths = months;

			for (String b : batches) {
				futures.add(CompletableFuture.supplyAsync(() -> atda.getBatchAttendanceReport(b, fStart, fEnd, fMonths, studentStatus)));
			}

			CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

			List<BatchAttendanceReportModel> reports = new ArrayList<>();
			for (CompletableFuture<BatchAttendanceReportModel> f : futures) {
				BatchAttendanceReportModel r = f.join();
				if (r != null && r.getTotalStudents() > 0) {
					reports.add(r);
				}
			}
			return reports;
		} else {
			// Single batch
			return atda.getBatchAttendanceReport(batch, start, end, months, studentStatus);
		}
	}
}
