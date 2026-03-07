package com.Vcube_AdminPortal.studentDetails.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.AttendanceApp;
import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.BatchsListApp;
import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.CaseStudyApp;
import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.ExamApp;
import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.GUTExamApp;
import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.MockApp;
import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.MockExamApp;
import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.PhotoApp;
import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.StudentDetails;
import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.StudentProjectApp;
import com.Vcube_AdminPortal.studentDetails.model.AttendanceModel;
import com.Vcube_AdminPortal.studentDetails.model.CaseStudyModel;
import com.Vcube_AdminPortal.studentDetails.model.ExamModel;
import com.Vcube_AdminPortal.studentDetails.model.GUTExamModel;
import com.Vcube_AdminPortal.studentDetails.model.MockExamModel;
import com.Vcube_AdminPortal.studentDetails.model.MockModel;
import com.Vcube_AdminPortal.studentDetails.model.ProjectModel;
import com.Vcube_AdminPortal.studentDetails.model.StudentModel;

@Controller
public class StudentUiController {

	@Autowired
	private StudentDetails studentDetails;
	@Autowired
	private PhotoApp photoApp;

	@Autowired
	private AttendanceApp atda;

	@Autowired
	private CaseStudyApp caseStudy;

	@Autowired
	private ExamApp exam;

	@Autowired
	private GUTExamApp gutExam;

	@Autowired
	private MockApp mock;

	@Autowired
	private MockExamApp mockExam;

	@Autowired
	private StudentProjectApp project;

	private List<String> getAvailableBatches() {
		return BatchsListApp.getAllBatchesList();
	}

//	@GetMapping("/students")
//	public String showForm(Model model) {
//		model.addAttribute("students", new StudentModel());
//		return "students-view-details";
//	}

	@GetMapping("/students")
	public String showStudentDashboard(Model model) {
		model.addAttribute("students", new StudentModel());
		return "student-details";
	}

	// Initial load and filter (batch + date + keyword), same as form action
	@GetMapping("/filter-students")
	public String listStudents(@RequestParam(name = "batch", required = false) String batch,
			@RequestParam(name = "date", required = false) String date,
			@RequestParam(name = "keyword", required = false) String keyword, Model model) {

		List<String> batches = getAvailableBatches();
		String selectedBatch = (batch == null || batch.isBlank()) ? "B59" : batch;

		// if date is null/blank you can call old method without attendance
		List<StudentModel> all;
		if (date == null || date.isBlank()) {
			all = studentDetails.getStudentsDetailsByBatchNo(selectedBatch);
		} else {
			all = studentDetails.getStudentsDetailsByBatchNoWithDate(selectedBatch, date);
		}

		if (keyword != null && !keyword.isBlank()) {
			String kw = keyword.toLowerCase();
			all = all.stream()
					.filter(s -> (s.getRollNo() != null && s.getRollNo().toLowerCase().contains(kw))
							|| (s.getName() != null && s.getName().toLowerCase().contains(kw))
							|| (s.getEmail() != null && s.getEmail().toLowerCase().contains(kw))
							|| (s.getMobile() != null && String.valueOf(s.getMobile()).contains(kw))
							|| (s.getMark() != null && s.getMark().toLowerCase().contains(kw)))
					.toList();
		}

		model.addAttribute("batches", batches);
		model.addAttribute("selectedBatch", selectedBatch);
		model.addAttribute("selectedDate", date == null ? "" : date);
		model.addAttribute("keyword", keyword == null ? "" : keyword);
		model.addAttribute("students", all);

		return "students-view-details";
	}

	@GetMapping("/students/photos")
	public String photoUploadPage(Model model) {
		model.addAttribute("batches", getAvailableBatches());
		return "student-photos";
	}

	@PostMapping("/students")
	public String loadStudents(@RequestParam("batchNo") String batchNo, Model model) {

		List<StudentModel> students = studentDetails.getStudentsDetailsByBatchNo(batchNo);

		model.addAttribute("batchNo", batchNo);
		model.addAttribute("students", students);
		return "students-view-details";
	}

	@PostMapping("/student-details")
	public String getStudentDetails(@RequestParam String batchNo, @RequestParam String rollNo, Model model) {

		try {

			model.addAttribute("batchNo", batchNo);

			StudentModel studentInfo = studentDetails.getStudentsDetailsByBatchNoAndRollNo(batchNo, rollNo);
			AttendanceModel attendanceInfo = atda.getStudentAllAttendanceMonthsWithStatusByBatchRollNo(batchNo, rollNo);
			CaseStudyModel caseStudiesInfo = caseStudy.getStudentAllCaseStudiessWithStatusByBatchRollNo(batchNo,
					rollNo);
			ExamModel examsInfo = exam.getStudentAllExamsWithStatusByBatchRollNo(batchNo, rollNo);
			GUTExamModel gutExamsInfo = gutExam.getStudentAllGUTExamsWithStatusByBatchRollNo(batchNo, rollNo);
			MockModel mocksInfo = mock.getStudentAllMocksWithStatusByBatchRollNo(batchNo, rollNo);
			MockExamModel mockExamsInfo = mockExam.getStudentAllMockExamsWithStatusByBatchRollNo(batchNo, rollNo);
			ProjectModel projectsInfo = project.getAllProjectsDetailsWithStatusByBatchAndRolNo(batchNo, rollNo);

//			System.out.println(studentInfo);
//			System.out.println(attendanceInfo);
//			System.out.println(mocksInf0);
//			System.out.println(examsInfo);
//			System.out.println(caseStudiesInfo);
//			System.out.println(mockExamsInfo);
//			System.out.println(gutExamsInfo);
//			System.out.println(projectsInfo);

//			String stRollNo = studentInfo.getRollNo();
//
//			if (studentInfo.getRollNo().equalsIgnoreCase(stRollNo)
//					&& attendanceInfo.getRollNo().equalsIgnoreCase(stRollNo)
//					&& mocksInfo.getRollNo().equalsIgnoreCase(stRollNo)
//					&& examsInfo.getRollNo().equalsIgnoreCase(stRollNo)
//					&& caseStudiesInfo.getRollNo().equalsIgnoreCase(stRollNo)
//					&& mockExamsInfo.getRollNo().equalsIgnoreCase(stRollNo)
//					&& gutExamsInfo.getRollNo().equalsIgnoreCase(stRollNo)
//					&& projectsInfo.getRollNo().equalsIgnoreCase(stRollNo)) {

			// Load ALL models
			model.addAttribute("student", studentInfo);
			model.addAttribute("rollNo", studentInfo.getRollNo());
			model.addAttribute("attendance", attendanceInfo);
			model.addAttribute("mock", mocksInfo);
			model.addAttribute("exam", examsInfo);
			model.addAttribute("caseStudy", caseStudiesInfo);
			model.addAttribute("mockExam", mockExamsInfo);
			model.addAttribute("gutExam", gutExamsInfo);
			model.addAttribute("project", projectsInfo);

			return "student-details";

//			} else {
//				model.addAttribute("error", "Student not found: " + rollNo);
//				return "student-details";
//			}

		} catch (Exception e) {
			model.addAttribute("error", "Student not found: " + rollNo);
			return "student-details";
		}
	}

	@PostMapping("/students/upload-photo")
	public String uploadPhoto(@RequestParam String rollNo, @RequestParam("photo") MultipartFile photo,
			RedirectAttributes ra) {

		// ✅ Validation
		if (photo.isEmpty()) {
			ra.addFlashAttribute("error", "❌ Please select a photo file");
			return "redirect:/students/photos";
		}

		if (rollNo.trim().isEmpty()) {
			ra.addFlashAttribute("error", "❌ Roll number required");
			return "redirect:/students/photos";
		}

		try {
			byte[] photoBytes = photo.getBytes();
			String photoUrl = photoApp.uploadAndUpdatePhoto(rollNo.trim(), photoBytes);

			ra.addFlashAttribute("success", "✅ Photo uploaded for " + rollNo + "! URL: " + photoUrl);

		} catch (Exception e) {
			e.printStackTrace(); // 👈 Debug log
			ra.addFlashAttribute("error", "❌ Upload failed: " + e.getMessage());
		}
		return "redirect:/students/photos";
	}

}
