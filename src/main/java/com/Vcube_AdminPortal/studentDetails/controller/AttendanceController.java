package com.Vcube_AdminPortal.studentDetails.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.AttendanceApp;
import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.BatchsListApp;
import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.StudentDetails;
import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.StudentsRollNosApp;
import com.Vcube_AdminPortal.studentDetails.model.AttendanceModel;
import com.Vcube_AdminPortal.studentDetails.model.StudentModel;

@Controller
public class AttendanceController {

	@Autowired
	private StudentDetails studentDetails;

	@Autowired
	private AttendanceApp atda;

	private List<String> getAvailableBatches() {
		return BatchsListApp.getAllBatchesList();
	}

	private List<String> getStudentsRollNos(String batch) {
		return StudentsRollNosApp.getStudentsRollNosByBatchNo(batch);
	}

	@GetMapping("/")
	public String index(Model model) {
		model.addAttribute("batches", getAvailableBatches());
		return "index"; // index.html
	}

	@GetMapping("/get-rollnos")
	@ResponseBody
	public List<String> getRollNosByBatch(@RequestParam String batchNo) {
		return getStudentsRollNos(batchNo);
	}

	@GetMapping("/attendance")
	public String showAttendanceForm(Model model) {
		model.addAttribute("attendanceModel", new AttendanceModel());
		model.addAttribute("batches", getAvailableBatches());
		return "attendance-form";
	}

	@GetMapping("/attendance/form")
	public String showAttendanceFormWithDetails(Model model) {
		model.addAttribute("attendanceModel", new AttendanceModel());
		model.addAttribute("batches", getAvailableBatches());
		return "attendance-sheet";
	}

	// Initial load and filter (batch + date + keyword), same as form action
	@GetMapping("/attendance/filter-students")
	public String listStudents(@RequestParam(name = "batch", required = false) String batch,
			@RequestParam(name = "date", required = false) String date,
			@RequestParam(name = "keyword", required = false) String keyword,
			@RequestParam(name = "rollInput", required = false) String rollInput,
			@RequestParam(name = "action", required = false) String action, Model model) {

		List<String> batches = getAvailableBatches();
		String selectedBatch = batch;

		List<StudentModel> all;
		if (date == null || date.isBlank()) {
			all = studentDetails.getStudentsDetailsForCheckBoxByBatchNo(selectedBatch);
		} else {
			all = studentDetails.getStudentsDetailsByBatchNoWithDate(selectedBatch, date);
		}

		if (rollInput != null && !rollInput.isEmpty() && !rollInput.isBlank()) {
			List<StudentModel> markedAll = atda.setTickMarksForCheckboxesByRollNos(all, selectedBatch, rollInput);
			all = markedAll != null ? markedAll : all;
		}

		// 1) keyword filter (optional)
		if (keyword != null && !keyword.isBlank()) {
			String kw = keyword.toLowerCase();
			all = all.stream()
					.filter(s -> (s.getRollNo() != null && s.getRollNo().toLowerCase().contains(kw))
							|| (s.getName() != null && s.getName().toLowerCase().contains(kw))
							|| (s.getEmail() != null && s.getEmail().toLowerCase().contains(kw))
							|| (s.getMobile() != null && String.valueOf(s.getMobile()).contains(kw))
							|| (s.getMark() != null && s.getMark().toLowerCase().contains(kw)))
					.toList();
		}

		model.addAttribute("batches", batches);
		model.addAttribute("selectedBatch", selectedBatch);
		model.addAttribute("selectedDate", date == null ? "" : date);
		model.addAttribute("keyword", keyword == null ? "" : keyword);
		model.addAttribute("rollInput", rollInput == null ? "" : rollInput);
		model.addAttribute("students", all);

		return "attendance-sheet";
	}

	@PostMapping("/attendance/submit")
	public String submitAttendance(@RequestParam String rollNoSet, @RequestParam String batch,
			@RequestParam String date, Model model) {

		try {
			AttendanceModel updateAttendance = atda.setAttendanceByRollNoSet(rollNoSet, batch);

//			atda.setEmailsForDailyAttendance(updateAttendance.getEmailsList(), batch);

			model.addAttribute("success", true);
			model.addAttribute("message",
					"Attendance marked successfully! Emails: " + updateAttendance.getEmailsList().size());
			model.addAttribute("onlineEmails", updateAttendance.getOnlineEmailsList());
			model.addAttribute("emails", updateAttendance.getEmailsList());
			model.addAttribute("invalidRollNos", updateAttendance.getInvalidRollSet());
			model.addAttribute("attendanceCount", updateAttendance.getAttendanceCount());
			model.addAttribute("attendanceModel", new AttendanceModel());

		} catch (Exception e) {
			model.addAttribute("success", false);
			model.addAttribute("message", "Error: " + e.getMessage());
		}

		model.addAttribute("batches", getAvailableBatches());
		return "attendance-form";
	}

	@PostMapping("/attendance/submit-attendance")
	public String submitAttendanceFromList(
			@RequestParam(name = "presentRolls", required = false) List<String> presentRolls,
			@RequestParam String batch, @RequestParam String date, Model model) {

		// presentRolls can be null if nothing checked
		if (presentRolls == null) {
			presentRolls = List.of();
		}

		List<StudentModel> students = atda.setAttendanceByRollNoSetWithCheckBox(batch, date, presentRolls);

		if (students != null) {
			List<String> emails = new ArrayList<>();
			for (StudentModel student : students) {
				emails.add(student.getEmail());
			}

			atda.setEmailsForDailyAttendance(emails, "B64");
		}

		List<String> batches = getAvailableBatches();
		model.addAttribute("batches", batches);
		model.addAttribute("selectedBatch", batch);
		model.addAttribute("selectedDate", date);
		model.addAttribute("keyword", "");
		model.addAttribute("students", students);

		model.addAttribute("message", "Saved attendance for " + presentRolls.size() + " students.");
		model.addAttribute("success", true);

		return "attendance-sheet";
	}

}
