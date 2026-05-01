package com.Vcube_AdminPortal.studentDetails.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.BatchsListApp;
import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.MockDriveApp;
import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.StudentDetails;
import com.Vcube_AdminPortal.studentDetails.model.MockDriveModel;
import com.Vcube_AdminPortal.studentDetails.model.StudentModel;
import com.Vcube_AdminPortal.studentDetails.service.EmailService;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.Map;
import java.util.HashMap;

@Controller
public class MockDriveController {

    @Autowired
    private MockDriveApp mockDriveApp;

    @Autowired
    private EmailService emailService;

    @Autowired
    private StudentDetails studentDetails;

    @GetMapping("/mock-drive")
    public String showMockDriveForm(Model model) {
        model.addAttribute("mockDrive", new MockDriveModel());
        model.addAttribute("batches", BatchsListApp.getAllBatchesList());
        return "mock-drive-form";
    }

    @PostMapping("/mock-drive/submit")
    public String submitMockDriveFeedback(@ModelAttribute MockDriveModel mockDrive, RedirectAttributes redirectAttributes) {
        try {
            // 1) Save to Google Sheet
            boolean saved = mockDriveApp.saveFeedback(mockDrive);
            
            if (saved) {
                // 2) Send Email
                try {
                    emailService.sendFeedbackEmail(mockDrive);
                    redirectAttributes.addFlashAttribute("success", "Feedback saved and email sent successfully!");
                } catch (Exception e) {
                    e.printStackTrace();
                    redirectAttributes.addFlashAttribute("warning", "Feedback saved, but failed to send email: " + e.getMessage());
                }
            } else {
                redirectAttributes.addFlashAttribute("error", "Failed to save feedback to Google Sheet.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "An error occurred: " + e.getMessage());
        }
        return "redirect:/mock-drive";
    }

    @GetMapping("/mock-drive-report")
    public String showMockDriveReport(Model model) {
        model.addAttribute("sheetNames", mockDriveApp.getSheetNames());
        return "mock-drive-report";
    }

    @GetMapping("/api/mock-drive/sheet-names")
    @ResponseBody
    public List<String> getSheetNames() {
        return mockDriveApp.getSheetNames();
    }

    @GetMapping("/api/mock-drive/data")
    @ResponseBody
    public List<MockDriveModel> getSheetData(@RequestParam String sheetName) {
        return mockDriveApp.getSheetData(sheetName);
    }

    @PostMapping("/api/mock-drive/send-email")
    @ResponseBody
    public Map<String, Object> sendEmail(@RequestParam String sheetName, @ModelAttribute MockDriveModel mockDrive) {
        Map<String, Object> response = new HashMap<>();
        try {
            emailService.sendFeedbackEmail(mockDrive);
            boolean updated = mockDriveApp.updateNotifyStatus(sheetName, mockDrive.getEmailId(), "sent");
            if (updated) {
                response.put("success", true);
                response.put("message", "Email sent and sheet updated!");
            } else {
                response.put("success", false);
                response.put("message", "Email sent, but failed to update sheet.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error sending email: " + e.getMessage());
        }
        return response;
    }

    @GetMapping("/api/mock-drive/students")
    @ResponseBody
    public List<StudentModel> getStudentsByBatch(@RequestParam String batch) {
        return studentDetails.getStudentsDetailsByBatchNo(batch);
    }
}
