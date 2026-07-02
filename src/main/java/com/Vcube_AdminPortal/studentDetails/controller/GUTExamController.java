package com.Vcube_AdminPortal.studentDetails.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.BatchsListApp;
import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.GUTExamApp;
import com.Vcube_AdminPortal.studentDetails.model.GUTExamModel;
import com.Vcube_AdminPortal.studentDetails.model.StudentModel;
import java.util.ArrayList;

@Controller
public class GUTExamController {

	@Autowired
	private GUTExamApp gutExam;
	
	private List<String> getAvailableBatches() {
		return BatchsListApp.getAllBatchesList();
	}


	@GetMapping("/gut-exam")
	public String showAttendanceForm(
			@RequestParam(name = "batch", required = false) String batch,
			@RequestParam(name = "date", required = false) String date,
			Model model) {

		List<String> batches = getAvailableBatches();
		String selectedBatch = (batch != null) ? batch.trim() : null;
		String selectedDate = (date != null && !date.isBlank()) ? date.trim() : java.time.LocalDate.now().toString();

		List<StudentModel> students = new ArrayList<>();
		if (selectedBatch != null && !selectedBatch.isBlank()) {
			students = gutExam.getStudentsGUTExamListByBatchAndDate(selectedBatch, selectedDate);
		}

		model.addAttribute("batches", batches);
		model.addAttribute("selectedBatch", selectedBatch);
		model.addAttribute("selectedDate", selectedDate);
		model.addAttribute("students", students);

		return "gut-exam-form";
	}

	@GetMapping("/students-gut-exams")
	public String showStudentsMocks(Model model) {
		model.addAttribute("mocks", new GUTExamModel());
		return "students-gut-exams";
	}

	@PostMapping("/gut-exam/submit")
	public String submitAttendance(
			@RequestParam("studentRollNos") List<String> rollNos,
			@RequestParam("gutMarks") List<String> marks,
			@RequestParam("batch") String batch,
			@RequestParam("date") String date,
			Model model) {

		try {
			String status = gutExam.saveGUTExamMarks(batch, date, rollNos, marks);

			if (status != null) {
				model.addAttribute("success", true);
				model.addAttribute("message", "GUT-Exam marks saved successfully!");
			} else {
				model.addAttribute("success", false);
				model.addAttribute("message", "Failed to save GUT-Exam marks!");
			}

		} catch (Exception e) {
			model.addAttribute("success", false);
			model.addAttribute("message", "Error: " + e.getMessage());
		}

		List<StudentModel> students = gutExam.getStudentsGUTExamListByBatchAndDate(batch, date);

		model.addAttribute("batches", getAvailableBatches());
		model.addAttribute("selectedBatch", batch);
		model.addAttribute("selectedDate", date);
		model.addAttribute("students", students);

		return "gut-exam-form";
	}

	@PostMapping("/students-gut-exams")
	public String loadStudentsMocks(@RequestParam("batchNo") String batchNo, Model model) {

		List<GUTExamModel> studentGUT = gutExam.getStudentsGUTExamDetailsByBatchNo(batchNo);

		model.addAttribute("batchNo", batchNo);
		model.addAttribute("batches", getAvailableBatches());
		model.addAttribute("studentsGUTExams", studentGUT);
		return "students-gut-exams";
	}

	
}
