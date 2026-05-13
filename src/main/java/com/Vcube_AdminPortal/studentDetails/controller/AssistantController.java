package com.Vcube_AdminPortal.studentDetails.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.AttendanceApp;
import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.BatchsListApp;
import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.CaseStudyApp;
import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.ExamApp;
import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.GUTExamApp;
import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.MockApp;
import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.MockExamApp;
import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.StudentDetails;
import com.Vcube_AdminPortal.studentDetails.model.AttendanceModel;
import com.Vcube_AdminPortal.studentDetails.model.CaseStudyModel;
import com.Vcube_AdminPortal.studentDetails.model.ExamModel;
import com.Vcube_AdminPortal.studentDetails.model.MockModel;
import com.Vcube_AdminPortal.studentDetails.model.StudentModel;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    @Autowired
    private AttendanceApp attendanceApp;
    @Autowired
    private ExamApp examApp;
    @Autowired
    private MockApp mockApp;
    @Autowired
    private CaseStudyApp caseStudyApp;
    @Autowired
    private GUTExamApp gutExamApp;
    @Autowired
    private MockExamApp mockExamApp;
    @Autowired
    private StudentDetails studentDetails;

    @GetMapping("/batches")
    public List<String> getAllBatches() {
        List<String> batches = BatchsListApp.getAllBatchesList();
        return batches != null ? batches : new ArrayList<>();
    }

    // ========== READ ENDPOINTS ==========

    @GetMapping("/attendance-report")
    public Map<String, Object> getAttendanceReport(@RequestParam String batch) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<String> batches = "ALL".equalsIgnoreCase(batch) ? BatchsListApp.getAllBatchesList() : List.of(batch);
            List<Map<String, Object>> studentList = new ArrayList<>();
            int totalStudents = 0;

            for (String b : batches) {
                List<StudentModel> students = studentDetails.getStudentsDetailsByBatchNo(b);
                if (students != null) {
                    totalStudents += students.size();
                    for (StudentModel s : students) {
                        Map<String, Object> st = new HashMap<>();
                        st.put("rollNo", s.getRollNo());
                        st.put("name", s.getName());
                        st.put("batch", b);
                        st.put("email", s.getEmail() != null ? s.getEmail() : "");
                        studentList.add(st);
                    }
                }
            }
            result.put("success", true);
            result.put("type", "attendance");
            result.put("batch", batch);
            result.put("totalStudents", totalStudents);
            result.put("students", studentList);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Error fetching attendance: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/exam-report")
    public Map<String, Object> getExamReport(@RequestParam String batch) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<String> batches = "ALL".equalsIgnoreCase(batch) ? BatchsListApp.getAllBatchesList() : List.of(batch);
            List<Map<String, Object>> list = new ArrayList<>();
            int totalStudents = 0;

            for (String b : batches) {
                List<ExamModel> exams = examApp.getStudentsExamDetailsByBatchNo(b);
                if (exams != null) {
                    totalStudents += exams.size();
                    for (ExamModel e : exams) {
                        Map<String, Object> m = new HashMap<>();
                        m.put("rollNo", e.getStudent().getRollNo());
                        m.put("name", e.getStudent().getName());
                        m.put("batch", b);
                        m.put("totalExams", e.getTotalExams());
                        m.put("attended", e.getStudentTotalExams());
                        m.put("totalMarks", e.getStudentTotalExamsMarks());
                        m.put("percentage", e.getStudentPercentage());
                        list.add(m);
                    }
                }
            }
            result.put("success", true);
            result.put("type", "exam");
            result.put("batch", batch);
            result.put("totalStudents", totalStudents);
            result.put("students", list);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Error fetching exam data: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/mock-report")
    public Map<String, Object> getMockReport(@RequestParam String batch) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<String> batches = "ALL".equalsIgnoreCase(batch) ? BatchsListApp.getAllBatchesList() : List.of(batch);
            List<Map<String, Object>> list = new ArrayList<>();
            int totalStudents = 0;

            for (String b : batches) {
                List<MockModel> mocks = mockApp.getStudentsMockDetailsByBatchNo(b);
                if (mocks != null) {
                    totalStudents += mocks.size();
                    for (MockModel m : mocks) {
                        Map<String, Object> mp = new HashMap<>();
                        mp.put("rollNo", m.getStudent().getRollNo());
                        mp.put("name", m.getStudent().getName());
                        mp.put("batch", b);
                        mp.put("totalMocks", m.getTotalMocks());
                        mp.put("attended", m.getStudentTotalMocks());
                        mp.put("totalMarks", m.getStudentTotalMocksMarks());
                        mp.put("percentage", m.getStudentPercentage());
                        list.add(mp);
                    }
                }
            }
            result.put("success", true);
            result.put("type", "mock");
            result.put("batch", batch);
            result.put("totalStudents", totalStudents);
            result.put("students", list);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Error fetching mock data: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/casestudy-report")
    public Map<String, Object> getCaseStudyReport(@RequestParam String batch) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<CaseStudyModel> studies = caseStudyApp.getStudentsCaseStudyDetailsByBatchNo(batch);
            result.put("success", true);
            result.put("type", "casestudy");
            result.put("batch", batch);
            result.put("totalStudents", studies.size());

            List<Map<String, Object>> list = new ArrayList<>();
            for (CaseStudyModel c : studies) {
                Map<String, Object> mp = new HashMap<>();
                mp.put("rollNo", c.getStudent().getRollNo());
                mp.put("name", c.getStudent().getName());
                mp.put("totalCaseStudies", c.getTotalCaseStudies());
                mp.put("attended", c.getStudentTotalCaseStudies());
                list.add(mp);
            }
            result.put("students", list);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Error fetching case study data: " + e.getMessage());
        }
        return result;
    }

    // ========== WRITE ENDPOINTS ==========

    @PostMapping("/mark-attendance")
    public Map<String, Object> markAttendance(@RequestBody Map<String, String> payload) {
        Map<String, Object> result = new HashMap<>();
        try {
            String batch = payload.get("batch");
            String rollNos = payload.get("rollNos");
            String date = payload.get("date");

            if (batch == null || rollNos == null) {
                result.put("success", false);
                result.put("message", "Batch and roll numbers are required");
                return result;
            }

            AttendanceModel attendance = attendanceApp.setAttendanceByRollNoSet(rollNos, batch, date);
            result.put("success", true);
            result.put("message", "Attendance marked for " + attendance.getAttendanceCount() + " students");
            if (attendance.getInvalidRollSet() != null && !attendance.getInvalidRollSet().isEmpty()) {
                result.put("invalidRolls", attendance.getInvalidRollSet());
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
        }
        return result;
    }

    @PostMapping("/add-exam-marks")
    public Map<String, Object> addExamMarks(@RequestBody Map<String, String> payload) {
        Map<String, Object> result = new HashMap<>();
        try {
            String batch = payload.get("batch");
            String rollMarks = payload.get("rollMarks");
            String date = payload.get("date");

            if (batch == null || rollMarks == null || date == null) {
                result.put("success", false);
                result.put("message", "Batch, date, and roll marks (format: roll=marks) are required");
                return result;
            }

            ExamModel model = new ExamModel();
            model.setRollNoSetWithMarks(rollMarks);
            model.setDate(date);
            model.setBatch(batch);

            String status = examApp.setExamMarksByRollNoSet(model, batch);
            result.put("success", status != null);
            result.put("message", status != null ? "Exam marks added successfully" : "Failed to add exam marks");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
        }
        return result;
    }

    @PostMapping("/add-mock-marks")
    public Map<String, Object> addMockMarks(@RequestBody Map<String, String> payload) {
        Map<String, Object> result = new HashMap<>();
        try {
            String batch = payload.get("batch");
            String rollMarks = payload.get("rollMarks");
            String date = payload.get("date");

            if (batch == null || rollMarks == null || date == null) {
                result.put("success", false);
                result.put("message", "Batch, date, and roll marks are required");
                return result;
            }

            MockModel model = new MockModel();
            model.setRollNoSetWithMarks(rollMarks);
            model.setDate(date);
            model.setBatch(batch);

            String status = mockApp.setMockByRollNoSet(model, batch);
            result.put("success", status != null);
            result.put("message", status != null ? "Mock marks added successfully" : "Failed to add mock marks");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
        }
        return result;
    }

    @PostMapping("/add-casestudy-marks")
    public Map<String, Object> addCaseStudyMarks(@RequestBody Map<String, String> payload) {
        Map<String, Object> result = new HashMap<>();
        try {
            String batch = payload.get("batch");
            String rollMarks = payload.get("rollMarks");

            if (batch == null || rollMarks == null) {
                result.put("success", false);
                result.put("message", "Batch and roll numbers are required");
                return result;
            }

            CaseStudyModel model = new CaseStudyModel();
            model.setRollNoSet(rollMarks.replaceAll("=\\d+", "")); // strip marks, keep roll numbers
            model.setBatch(batch);

            String status = caseStudyApp.setCaseStudyByRollNoSet(model, batch);
            result.put("success", status != null);
            result.put("message",
                    status != null ? "Case-study marks added successfully" : "Failed to add case-study marks");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
        }
        return result;
    }

    // ========== CHAT PROCESSOR ==========

    @PostMapping("/chat")
    public Map<String, Object> processChat(@RequestBody Map<String, String> payload) {
        String message = payload.get("message");
        if (message == null || message.isBlank()) {
            Map<String, Object> r = new HashMap<>();
            r.put("reply", "I didn't catch that. Could you say it again?");
            r.put("type", "error");
            return r;
        }

        String msg = message.toLowerCase().trim();

        // Parse the intent
        if (containsAny(msg, "attendance", "present", "absent")) {
            return handleAttendanceIntent(msg, message);
        } else if (containsAny(msg, "exam", "exam marks", "exam report")) {
            return handleExamIntent(msg, message);
        } else if (containsAny(msg, "mock mark", "mock report", "mock data")) {
            return handleMockIntent(msg, message);
        } else if (containsAny(msg, "case study", "casestudy", "case-study")) {
            return handleCaseStudyIntent(msg, message);
        } else if (containsAny(msg, "batch", "batches", "all batch")) {
            return handleBatchIntent();
        } else if (containsAny(msg, "hello", "hi", "hey", "help", "what can you do")) {
            return helpResponse();
        } else if (containsAny(msg, "student", "roll")) {
            return handleStudentIntent(msg);
        } else {
            return helpResponse();
        }
    }

    private Map<String, Object> handleAttendanceIntent(String msg, String original) {
        Map<String, Object> result = new HashMap<>();
        String batch = extractBatch(msg);

        // Check if it's a write operation
        if (containsAny(msg, "mark", "add", "submit", "update", "modify")) {
            if (batch == null) {
                result.put("reply",
                        "Which batch should I mark attendance for? Say something like 'mark attendance for batch B59'");
                result.put("type", "ask");
                result.put("action", "mark-attendance");
                return result;
            }

            String rollNos = extractRollNumbers(msg);
            if (rollNos == null || rollNos.isBlank()) {
                result.put("reply",
                        "I found batch " + batch + ". Now tell me the roll numbers, like: 'roll numbers 1,2,3'");
                result.put("type", "ask");
                result.put("action", "mark-attendance");
                result.put("batch", batch);
                return result;
            }

            try {
                AttendanceModel attendance = attendanceApp.setAttendanceByRollNoSet(rollNos, batch, null);
                result.put("reply",
                        "✅ Attendance marked for " + attendance.getAttendanceCount() + " students in batch " + batch);
                result.put("type", "success");
            } catch (Exception e) {
                result.put("reply", "❌ Failed to mark attendance: " + e.getMessage());
                result.put("type", "error");
            }
            return result;
        }

        // Read operation
        if (batch == null) {
            result.put("reply",
                    "Which batch do you want the attendance report for? Say something like 'attendance report for batch B59'");
            result.put("type", "ask");
            return result;
        }

        return getAttendanceReport(batch);
    }

    private Map<String, Object> handleExamIntent(String msg, String original) {
        Map<String, Object> result = new HashMap<>();
        String batch = extractBatch(msg);

        if (containsAny(msg, "add", "submit", "update", "modify", "mark")) {
            if (batch == null) {
                result.put("reply", "Which batch should I add exam marks for?");
                result.put("type", "ask");
                result.put("action", "add-exam-marks");
                return result;
            }
            result.put("reply", "To add exam marks for batch " + batch
                    + ", please use the Exam Marks form. I'll redirect you there.");
            result.put("type", "redirect");
            result.put("url", "/exam");
            return result;
        }

        if (batch == null) {
            result.put("reply", "Which batch do you want the exam report for?");
            result.put("type", "ask");
            return result;
        }

        return getExamReport(batch);
    }

    private Map<String, Object> handleMockIntent(String msg, String original) {
        Map<String, Object> result = new HashMap<>();
        String batch = extractBatch(msg);

        if (containsAny(msg, "add", "submit", "update", "modify", "mark")) {
            if (batch == null) {
                result.put("reply", "Which batch should I add mock marks for?");
                result.put("type", "ask");
                result.put("action", "add-mock-marks");
                return result;
            }
            result.put("reply", "To add mock marks for batch " + batch + ", use the Mock Marks form.");
            result.put("type", "redirect");
            result.put("url", "/mock");
            return result;
        }

        if (batch == null) {
            result.put("reply", "Which batch do you want the mock report for?");
            result.put("type", "ask");
            return result;
        }

        return getMockReport(batch);
    }

    private Map<String, Object> handleCaseStudyIntent(String msg, String original) {
        Map<String, Object> result = new HashMap<>();
        String batch = extractBatch(msg);

        if (batch == null) {
            result.put("reply", "Which batch do you want the case study report for?");
            result.put("type", "ask");
            return result;
        }

        return getCaseStudyReport(batch);
    }

    private Map<String, Object> handleBatchIntent() {
        Map<String, Object> result = new HashMap<>();
        List<String> batches = BatchsListApp.getAllBatchesList();
        if (batches != null && !batches.isEmpty()) {
            result.put("reply", "📋 Available batches: " + String.join(", ", batches));
            result.put("type", "info");
            result.put("batches", batches);
        } else {
            result.put("reply", "No batches found.");
            result.put("type", "error");
        }
        return result;
    }

    private Map<String, Object> handleStudentIntent(String msg) {
        Map<String, Object> result = new HashMap<>();
        String batch = extractBatch(msg);
        if (batch == null) {
            result.put("reply", "Which batch do you want student details from?");
            result.put("type", "ask");
            return result;
        }

        try {
            List<StudentModel> students = studentDetails.getStudentsDetailsByBatchNo(batch);
            result.put("success", true);
            result.put("reply",
                    "📋 Batch " + batch + " has " + (students != null ? students.size() : 0) + " students.");
            result.put("type", "info");
            result.put("totalStudents", students != null ? students.size() : 0);
        } catch (Exception e) {
            result.put("reply", "Error loading students: " + e.getMessage());
            result.put("type", "error");
        }
        return result;
    }

    private Map<String, Object> helpResponse() {
        Map<String, Object> result = new HashMap<>();
        result.put("reply", "👋 Hi! I'm Vcube Assistant. Here's what I can do:\n\n" +
                "📊 **Reports:** \"Show exam report for B59\", \"Attendance report for B60\"\n" +
                "✅ **Mark Attendance:** \"Mark attendance for batch B59, roll numbers 1,2,3\"\n" +
                "📋 **Batches:** \"Show all batches\"\n" +
                "👤 **Students:** \"How many students in B59?\"\n\n" +
                "Try asking me anything!");
        result.put("type", "help");
        return result;
    }

    // ========== UTILITY ==========

    private boolean containsAny(String msg, String... words) {
        for (String w : words) {
            if (msg.contains(w))
                return true;
        }
        return false;
    }

    private String extractBatch(String msg) {
        // Match patterns like B59, B60, batch 59, batch B59
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\b[Bb](\\d{1,3})\\b");
        java.util.regex.Matcher matcher = pattern.matcher(msg);
        if (matcher.find()) {
            return "B" + matcher.group(1);
        }

        // Try "batch 59" pattern
        pattern = java.util.regex.Pattern.compile("batch\\s+(\\d{1,3})");
        matcher = pattern.matcher(msg);
        if (matcher.find()) {
            return "B" + matcher.group(1);
        }

        return null;
    }

    private String extractRollNumbers(String msg) {
        // Look for patterns like "roll numbers 1,2,3" or "1,2,3,4"
        java.util.regex.Pattern pattern = java.util.regex.Pattern
                .compile("(?:roll\\s*(?:numbers?)?\\s*)(\\d[\\d,\\s-]+)");
        java.util.regex.Matcher matcher = pattern.matcher(msg);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // Try matching just comma-separated numbers
        pattern = java.util.regex.Pattern.compile("(\\d+(?:\\s*,\\s*\\d+)+)");
        matcher = pattern.matcher(msg);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }
}
