package com.Vcube_AdminPortal.studentDetails.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.BatchsListApp;
import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.CaseStudyApp;
import com.Vcube_AdminPortal.studentDetails.model.CaseStudyModel;

@Controller
public class CaseStudyController {

	@Autowired
	private CaseStudyApp caseStudy;
	
	private List<String> getAvailableBatches() {
		return BatchsListApp.getAllBatchesList();
	}

	@GetMapping("/caseStudy")
	public String showAttendanceForm(Model model) {
		model.addAttribute("caseStudyModel", new CaseStudyModel());
		model.addAttribute("batches", getAvailableBatches());
		return "caseStudy-form";
	}

	@GetMapping("/students-caseStudies")
	public String showStudentsCaseStudies(Model model) {
		model.addAttribute("caseStudies", new CaseStudyModel());
		return "students-caseStudies";
	}

	@PostMapping("/caseStudy/submit")
	public String submitCaseStudeies(@RequestParam String rollNoSet, @RequestParam String batch,
			@RequestParam String date, Model model) {

		try {
			CaseStudyModel adm = new CaseStudyModel();
			adm.setRollNoSet(rollNoSet);
			adm.setBatch(batch); // if your model has this field

			String status = caseStudy.setCaseStudyByRollNoSet(adm,batch);

			if (status != null) {
				model.addAttribute("success", true);
				model.addAttribute("message", "CaseStudy marked successfully!");
				model.addAttribute("caseStudyeModel", new CaseStudyModel());
			} else {
				model.addAttribute("success", false);
				model.addAttribute("message", "Error: sheet not found");
			}

		} catch (Exception e) {
			model.addAttribute("success", false);
			model.addAttribute("message", "Error: " + e.getMessage());
		}

		model.addAttribute("batches", getAvailableBatches());
		return "caseStudy-form";
	}

	@PostMapping("/students-caseStudies")
	public String loadStudentsCaseStudies(@RequestParam("batchNo") String batchNo, Model model) {

		List<CaseStudyModel> caseStudies = caseStudy.getStudentsCaseStudyDetailsByBatchNo(batchNo);

		model.addAttribute("batchNo", batchNo);
		model.addAttribute("batches", getAvailableBatches());
		model.addAttribute("studentsCaseStudies", caseStudies);
		return "students-caseStudies";
	}

}
