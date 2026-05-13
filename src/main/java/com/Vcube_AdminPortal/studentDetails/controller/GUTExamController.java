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

@Controller
public class GUTExamController {

	@Autowired
	private GUTExamApp gutExam;
	
	private List<String> getAvailableBatches() {
		return BatchsListApp.getAllBatchesList();
	}


	@GetMapping("/gut-exam")
	public String showAttendanceForm(Model model) {
		model.addAttribute("attendanceModel", new GUTExamModel());
		model.addAttribute("batches", getAvailableBatches());
		return "gut-exam-form";
	}

	@GetMapping("/students-gut-exams")
	public String showStudentsMocks(Model model) {
		model.addAttribute("mocks", new GUTExamModel());
		return "students-gut-exams";
	}

	@PostMapping("/gut-exam/submit")
	public String submitAttendance(@RequestParam String rollNoSet, @RequestParam String batch,
			@RequestParam String date, Model model) {

		try {
			GUTExamModel adm = new GUTExamModel();
			adm.setRollNoSetWithMarks(rollNoSet);
			adm.setBatch(batch); // if your model has this field
			adm.setDate(date);

			String status = gutExam.setGUTExamByRollNoSet(adm,batch);

			if (status != null) {
				model.addAttribute("success", true);
				model.addAttribute("message", "Mock marked successfully..!");
				model.addAttribute("attendanceModel", new GUTExamModel());
			} else {
				model.addAttribute("success", false);
				model.addAttribute("message", "Mock marked failed..!");
				model.addAttribute("attendanceModel", new GUTExamModel());
			}

		} catch (Exception e) {
			model.addAttribute("success", false);
			model.addAttribute("message", "Error: " + e.getMessage());
		}

		model.addAttribute("batches", getAvailableBatches());
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
