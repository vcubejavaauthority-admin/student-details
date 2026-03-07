package com.Vcube_AdminPortal.studentDetails.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.BatchsListApp;
import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.MockExamApp;
import com.Vcube_AdminPortal.studentDetails.model.MockExamModel;

@Controller
public class MockExamController {
	@Autowired
	private MockExamApp mock;
	
	private List<String> getAvailableBatches() {
		return BatchsListApp.getAllBatchesList();
	}

	@GetMapping("/mock-exam")
	public String showAttendanceForm(Model model) {
		model.addAttribute("attendanceModel", new MockExamModel());
		model.addAttribute("batches", getAvailableBatches());
		return "mock-exam-form";
	}

	@GetMapping("/students-mock-exams")
	public String showStudentsMocks(Model model) {
		model.addAttribute("mocks", new MockExamModel());
		return "students-mock-exams";
	}

	@PostMapping("/mock-exam/submit")
	public String submitAttendance(@RequestParam String rollNoSet, @RequestParam String batch,
			@RequestParam String date, Model model) {

		try {
			MockExamModel adm = new MockExamModel();
			adm.setRollNoSetWithMarks(rollNoSet);
			adm.setBatch(batch); // if your model has this field
			adm.setDate(date);

			String status = mock.setMockExamByRollNoSet(adm,batch);

			if (status != null) {
				model.addAttribute("success", true);
				model.addAttribute("message", "Mock marked successfully..!");
				model.addAttribute("attendanceModel", new MockExamModel());
			} else {
				model.addAttribute("success", false);
				model.addAttribute("message", "Mock marked failed..!");
				model.addAttribute("attendanceModel", new MockExamModel());
			}

		} catch (Exception e) {
			model.addAttribute("success", false);
			model.addAttribute("message", "Error: " + e.getMessage());
		}

		model.addAttribute("batches", getAvailableBatches());
		return "mock-exam-form";
	}

	@PostMapping("/students-mock-exams")
	public String loadStudentsMocks(@RequestParam("batchNo") String batchNo, Model model) {

		List<MockExamModel> studentMocks = mock.getStudentsMockExamDetailsByBatchNo(batchNo);

		model.addAttribute("batchNo", batchNo);
		model.addAttribute("batches", getAvailableBatches());
		model.addAttribute("studentsMocks", studentMocks);
		return "students-mock-exams";
	}


}

