package com.Vcube_AdminPortal.studentDetails.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.BatchsListApp;
import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.StudentDetails;
import com.Vcube_AdminPortal.studentDetails.model.StudentModel;
import com.Vcube_AdminPortal.studentDetails.service.BatchShiftingService;

@Controller
public class BatchShiftingController {

	@Autowired
	private BatchShiftingService batchShiftingService;

	@Autowired
	private StudentDetails studentDetails;

	@GetMapping("/batch-shifting")
	public String showBatchShiftingPage(Model model) {
		List<String> allBatchesList = BatchsListApp.getAllBatchesList();
		model.addAttribute("batches", allBatchesList);
		return "batch-shifting";
	}

	@GetMapping("/api/batch-shifting/students")
	@ResponseBody
	public List<StudentModel> getStudentsByBatch(@RequestParam String batchNo) {
		return studentDetails.getStudentsDetailsByBatchNo(batchNo);
	}

	@PostMapping("/api/batch-shifting/shift")
	@ResponseBody
	public Map<String, Object> shiftStudent(
			@RequestParam String presentBatch, 
			@RequestParam String rollNo,
			@RequestParam String targetBatch) {
		
		Map<String, Object> response = new HashMap<>();
		
		if (presentBatch == null || presentBatch.isBlank() || 
			rollNo == null || rollNo.isBlank() || 
			targetBatch == null || targetBatch.isBlank()) {
			response.put("success", false);
			response.put("message", "All fields are required.");
			return response;
		}

		if (presentBatch.equals(targetBatch)) {
			response.put("success", false);
			response.put("message", "Present batch and Shift to batch cannot be the same.");
			return response;
		}

		boolean success = batchShiftingService.shiftStudent(presentBatch, rollNo, targetBatch);
		
		if (success) {
			response.put("success", true);
			response.put("message", "Student shifted successfully.");
		} else {
			response.put("success", false);
			response.put("message", "Failed to shift student. Check server logs.");
		}
		
		return response;
	}
}
