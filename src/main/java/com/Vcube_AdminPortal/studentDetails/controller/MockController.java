package com.Vcube_AdminPortal.studentDetails.controller;

import java.time.LocalDate;
import java.util.ArrayList;
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
import com.Vcube_AdminPortal.studentDetails.model.StudentModel;

@Controller
public class MockController {

	@Autowired
	private MockApp mock;

	private List<String> getAvailableBatches() {
		return BatchsListApp.getAllBatchesList();
	}

	@GetMapping("/mock")
	public String showMockMarksForm(
			@RequestParam(name = "batch", required = false) String batch,
			@RequestParam(name = "date", required = false) String date,
			Model model) {

		List<String> batches = getAvailableBatches();
		String selectedBatch = (batch != null) ? batch.trim() : null;
		
		// If date is null or empty, default to today's date in yyyy-MM-dd
		String selectedDate = (date != null && !date.isBlank()) ? date.trim() : LocalDate.now().toString();

		List<StudentModel> students = new ArrayList<>();
		if (selectedBatch != null && !selectedBatch.isBlank()) {
			students = mock.getStudentsMockListByBatchAndDate(selectedBatch, selectedDate);
		}

		model.addAttribute("batches", batches);
		model.addAttribute("selectedBatch", selectedBatch);
		model.addAttribute("selectedDate", selectedDate);
		model.addAttribute("students", students);

		return "mock-form";
	}

	@PostMapping("/mock/submit")
	public String submitMockMarks(
			@RequestParam("studentRollNos") List<String> rollNos,
			@RequestParam("technicalMarks") List<String> technical,
			@RequestParam("communicationMarks") List<String> communication,
			@RequestParam("remarks") List<String> remarksList,
			@RequestParam("batch") String batch,
			@RequestParam("date") String date,
			Model model) {

		try {
			String status = mock.saveMockMarks(batch, date, rollNos, technical, communication, remarksList);

			if (status != null) {
				model.addAttribute("success", true);
				model.addAttribute("message", "Mock marks saved successfully!");
			} else {
				model.addAttribute("success", false);
				model.addAttribute("message", "Failed to save mock marks!");
			}

		} catch (Exception e) {
			model.addAttribute("success", false);
			model.addAttribute("message", "Error: " + e.getMessage());
		}

		// Reload students list
		List<StudentModel> students = mock.getStudentsMockListByBatchAndDate(batch, date);

		model.addAttribute("batches", getAvailableBatches());
		model.addAttribute("selectedBatch", batch);
		model.addAttribute("selectedDate", date);
		model.addAttribute("students", students);

		return "mock-form";
	}

	@GetMapping("/students-mocks")
	public String showStudentsMocks(Model model) {
		model.addAttribute("mocks", new MockModel());
		model.addAttribute("batches", getAvailableBatches());
		return "students-mocks";
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
