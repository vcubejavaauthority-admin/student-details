package com.Vcube_AdminPortal.studentDetails.controller;

import java.util.List;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.BatchsListApp;
import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.ExamApp;
import com.Vcube_AdminPortal.studentDetails.model.ExamModel;
import com.Vcube_AdminPortal.studentDetails.model.StudentModel;

@Controller
public class ExamController {
	@Autowired
	private ExamApp exam;
	
	private List<String> getAvailableBatches() {
		return BatchsListApp.getAllBatchesList();
	}

	@GetMapping("/exam")
	public String showAttendanceForm(
			@RequestParam(name = "batch", required = false) String batch,
			@RequestParam(name = "date", required = false) String date,
			Model model) {

		List<String> batches = getAvailableBatches();
		String selectedBatch = (batch != null) ? batch.trim() : null;
		String selectedDate = (date != null && !date.isBlank()) ? date.trim() : java.time.LocalDate.now().toString();

		List<StudentModel> students = new ArrayList<>();
		if (selectedBatch != null && !selectedBatch.isBlank()) {
			students = exam.getStudentsExamListByBatchAndDate(selectedBatch, selectedDate);
		}

		model.addAttribute("batches", batches);
		model.addAttribute("selectedBatch", selectedBatch);
		model.addAttribute("selectedDate", selectedDate);
		model.addAttribute("students", students);

		return "exam-form";
	}

	@GetMapping("/students-exams")
	public String showStudentsExams(Model model) {
		model.addAttribute("exams", new ExamModel());
		return "students-exams";
	}

	@PostMapping("/exam/submit")
	public String submitAttendance(
			@RequestParam("studentRollNos") List<String> rollNos,
			@RequestParam("examMarks") List<String> marks,
			@RequestParam("batch") String batch,
			@RequestParam("date") String date,
			Model model) {

		try {
			String status = exam.saveExamMarks(batch, date, rollNos, marks);

			if (status != null) {
				model.addAttribute("success", true);
				model.addAttribute("message", "Exam marks saved successfully!");
			} else {
				model.addAttribute("success", false);
				model.addAttribute("message", "Failed to save Exam marks!");
			}

		} catch (Exception e) {
			model.addAttribute("success", false);
			model.addAttribute("message", "Error: " + e.getMessage());
		}

		List<StudentModel> students = exam.getStudentsExamListByBatchAndDate(batch, date);

		model.addAttribute("batches", getAvailableBatches());
		model.addAttribute("selectedBatch", batch);
		model.addAttribute("selectedDate", date);
		model.addAttribute("students", students);

		return "exam-form";
	}

	@PostMapping("/students-exams")
	public String loadStudentsExams(@RequestParam("batchNo") String batchNo, Model model) {

		List<ExamModel> studentExams = exam.getStudentsExamDetailsByBatchNo(batchNo);

		model.addAttribute("batchNo", batchNo);
		model.addAttribute("batches", getAvailableBatches());
		model.addAttribute("studentsExams", studentExams);
		return "students-exams";
	}

	
}
