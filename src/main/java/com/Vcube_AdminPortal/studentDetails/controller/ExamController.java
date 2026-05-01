package com.Vcube_AdminPortal.studentDetails.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.BatchsListApp;
import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.ExamApp;
import com.Vcube_AdminPortal.studentDetails.model.ExamModel;

@Controller
public class ExamController {
	@Autowired
	private ExamApp exam;
	
	private List<String> getAvailableBatches() {
		return BatchsListApp.getAllBatchesList();
	}

	@GetMapping("/exam")
	public String showAttendanceForm(Model model) {
		model.addAttribute("attendanceModel", new ExamModel());
		model.addAttribute("batches", getAvailableBatches());
		return "exam-form";
	}

	@GetMapping("/students-exams")
	public String showStudentsExams(Model model) {
		model.addAttribute("exams", new ExamModel());
		return "students-exams";
	}

	@PostMapping("/exam/submit")
	public String submitAttendance(@RequestParam String rollNoSet, @RequestParam String batch,
			@RequestParam String date, Model model) {

		try {
			ExamModel adm = new ExamModel();
			adm.setRollNoSetWithMarks(rollNoSet);
			adm.setBatch(batch); // if your model has this field
			adm.setDate(date);

			String status = exam.setExamMarksByRollNoSet(adm,batch);

			if (status != null) {
				model.addAttribute("success", true);
				model.addAttribute("message", "Exam marks added successfully..!");
				model.addAttribute("attendanceModel", new ExamModel());
			} else {
				model.addAttribute("success", false);
				model.addAttribute("message", "Exam marks added failed..!");
				model.addAttribute("attendanceModel", new ExamModel());
			}

		} catch (Exception e) {
			model.addAttribute("success", false);
			model.addAttribute("message", "Error: " + e.getMessage());
		}

		model.addAttribute("batches", getAvailableBatches());
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
