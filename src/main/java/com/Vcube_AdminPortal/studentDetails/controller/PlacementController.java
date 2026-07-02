package com.Vcube_AdminPortal.studentDetails.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.Vcube_AdminPortal.studentDetails.model.StudentPlacementModel;
import com.Vcube_AdminPortal.studentDetails.service.StudentPlacementService;

@RestController
public class PlacementController {

	@Autowired
	private StudentPlacementService placementService;

	@GetMapping("/api/placements/filter")
	public Map<String, Object> filterPlacements(
			@RequestParam(name = "status", defaultValue = "active") String status,
			@RequestParam(name = "batch", required = false) String batch) {
		
		List<StudentPlacementModel> students = placementService.getPlacementDetails(status, batch);

		TreeSet<String> qualifications = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		TreeSet<String> streams = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		TreeSet<String> years = new TreeSet<>();

		for (StudentPlacementModel s : students) {
			if (s.getUgQualification() != null && !s.getUgQualification().isBlank()) {
				qualifications.add(s.getUgQualification().trim());
			}
			if (s.getPgQualification() != null && !s.getPgQualification().isBlank()) {
				qualifications.add(s.getPgQualification().trim());
			}
			if (s.getUgStream() != null && !s.getUgStream().isBlank()) {
				streams.add(s.getUgStream().trim());
			}
			if (s.getUgPassedOutYear() != null && !s.getUgPassedOutYear().isBlank()) {
				years.add(s.getUgPassedOutYear().trim());
			}
			if (s.getPgPassedOutYear() != null && !s.getPgPassedOutYear().isBlank()) {
				years.add(s.getPgPassedOutYear().trim());
			}
		}

		Map<String, Object> response = new HashMap<>();
		response.put("students", students);
		response.put("qualifications", qualifications);
		response.put("streams", streams);
		response.put("years", years);

		return response;
	}
}
