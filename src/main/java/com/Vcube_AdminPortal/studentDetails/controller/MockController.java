package com.Vcube_AdminPortal.studentDetails.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.BatchsListApp;
import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.MockApp;
import com.Vcube_AdminPortal.studentDetails.model.MockModel;

@Controller
public class MockController {

	@Autowired
	private MockApp mock;
	
	private List<String> getAvailableBatches() {
		return BatchsListApp.getAllBatchesList();
	}

	@GetMapping("/mock")
	public String showAttendanceForm(Model model) {
		model.addAttribute("attendanceModel", new MockModel());
		model.addAttribute("batches", getAvailableBatches());
		return "mock-form";
	}

	@GetMapping("/students-mocks")
	public String showStudentsMocks(Model model) {
		model.addAttribute("mocks", new MockModel());
		return "students-mocks";
	}

	@PostMapping("/mock/submit")
	public String submitAttendance(@RequestParam String rollNoSet, @RequestParam String batch,
			@RequestParam String date, Model model) {

		try {
			MockModel adm = new MockModel();
			adm.setRollNoSetWithMarks(rollNoSet);
			adm.setBatch(batch); // if your model has this field
			adm.setDate(date);

			String status = mock.setMockByRollNoSet(adm,batch);

			if (status != null) {
				model.addAttribute("success", true);
				model.addAttribute("message", "Mock marked successfully..!");
				model.addAttribute("attendanceModel", new MockModel());
			} else {
				model.addAttribute("success", false);
				model.addAttribute("message", "Mock marked failed..!");
				model.addAttribute("attendanceModel", new MockModel());
			}

		} catch (Exception e) {
			model.addAttribute("success", false);
			model.addAttribute("message", "Error: " + e.getMessage());
		}

		model.addAttribute("batches", getAvailableBatches());
		return "mock-form";
	}

	@PostMapping("/students-mocks")
	public String loadStudentsMocks(@RequestParam("batchNo") String batchNo, Model model) {

		List<MockModel> studentMocks = mock.getStudentsMockDetailsByBatchNo(batchNo);

		model.addAttribute("batchNo", batchNo);
		model.addAttribute("batches", getAvailableBatches());
		model.addAttribute("studentsMocks", studentMocks);
		return "students-mocks";
	}

	

}
