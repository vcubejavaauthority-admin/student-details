package com.Vcube_AdminPortal.studentDetails.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.WeeklyMockListApp;
import com.Vcube_AdminPortal.studentDetails.model.WeeklyMockRecord;

@Controller
public class WeeklyMockReportController {

	@Autowired
	private WeeklyMockListApp weeklyMockListApp;

	@GetMapping("/weekly-mock-report")
	public String showReportForm(Model model) {
		Map<String, String> yearMap = weeklyMockListApp.getYearToSheetIdMap();
		model.addAttribute("years", yearMap);
		return "weekly-mock-report";
	}

	@GetMapping("/api/weekly-mock/sheets")
	@ResponseBody
	public List<String> getSheetsForYear(@RequestParam("spreadsheetId") String spreadsheetId) {
		return weeklyMockListApp.getSheetNamesBySpreadsheetId(spreadsheetId);
	}

	@PostMapping("/weekly-mock-report")
	public String fetchReportData(
			@RequestParam("year") String year,
			@RequestParam("spreadsheetId") String spreadsheetId,
			@RequestParam("sheetName") String sheetName,
			Model model) {
		
		Map<String, String> yearMap = weeklyMockListApp.getYearToSheetIdMap();
		model.addAttribute("years", yearMap);
		
		model.addAttribute("selectedYear", year);
		model.addAttribute("selectedSpreadsheetId", spreadsheetId);
		model.addAttribute("selectedSheetName", sheetName);
		
		if (spreadsheetId != null && !spreadsheetId.isBlank() && sheetName != null && !sheetName.isBlank()) {
			List<WeeklyMockRecord> records = weeklyMockListApp.getWeeklyMockListData(spreadsheetId, sheetName);
			model.addAttribute("records", records);
		}
		
		return "weekly-mock-report";
	}
}
