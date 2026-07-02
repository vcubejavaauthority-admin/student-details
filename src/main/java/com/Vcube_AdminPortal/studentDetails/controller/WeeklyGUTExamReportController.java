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

import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.WeeklyGUTExamListApp;
import com.Vcube_AdminPortal.studentDetails.model.WeeklyGUTExamRecord;
import com.Vcube_AdminPortal.studentDetails.service.EmailService;

@Controller
public class WeeklyGUTExamReportController {

    @Autowired
    private WeeklyGUTExamListApp weeklyGUTExamListApp;

    @Autowired
    private EmailService emailService;

    @GetMapping("/weekly-gut-exam-report")
    public String showReportForm(Model model) {
        Map<String, String> yearMap = weeklyGUTExamListApp.getYearToSheetIdMap();
        model.addAttribute("years", yearMap);
        return "weekly-gut-exam-report";
    }

    @GetMapping("/api/weekly-gut-exam/sheets")
    @ResponseBody
    public List<String> getSheetsForYear(@RequestParam("spreadsheetId") String spreadsheetId) {
        return weeklyGUTExamListApp.getSheetNamesBySpreadsheetId(spreadsheetId);
    }

    @PostMapping("/weekly-gut-exam-report")
    public String fetchReportData(
            @RequestParam("year") String year,
            @RequestParam("spreadsheetId") String spreadsheetId,
            @RequestParam("sheetName") String sheetName,
            Model model) {
        
        Map<String, String> yearMap = weeklyGUTExamListApp.getYearToSheetIdMap();
        model.addAttribute("years", yearMap);
        
        model.addAttribute("selectedYear", year);
        model.addAttribute("selectedSpreadsheetId", spreadsheetId);
        model.addAttribute("selectedSheetName", sheetName);
        
        if (spreadsheetId != null && !spreadsheetId.isBlank() && sheetName != null && !sheetName.isBlank()) {
            List<WeeklyGUTExamRecord> records = weeklyGUTExamListApp.getWeeklyGUTExamListData(spreadsheetId, sheetName);
            model.addAttribute("records", records);
        }
        
        return "weekly-gut-exam-report";
    }

    @PostMapping("/api/weekly-gut-exam/send-email")
    @ResponseBody
    public Map<String, Object> sendEmail(
            @RequestParam("spreadsheetId") String spreadsheetId,
            @RequestParam("sheetName") String sheetName,
            @RequestParam("studentName") String studentName,
            @RequestParam("rollNo") String rollNo,
            @RequestParam("email") String email,
            @RequestParam("marks") String marks,
            @RequestParam("stars") String stars,
            @RequestParam("status") String status) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            double marksDouble = 0.0;
            try {
                marksDouble = Double.parseDouble(marks.trim());
            } catch (NumberFormatException e) {
                // Keep marksDouble as 0.0
            }
            
            double percentage = (marksDouble / 50.0) * 100.0;
            percentage = Math.round(percentage * 100.0) / 100.0;
            
            // Send email via service
            emailService.sendGUTExamPerformanceEmail(
                    email.trim(), 
                    studentName.trim(), 
                    rollNo.trim(), 
                    sheetName.trim(), 
                    marks.trim(), 
                    stars.trim(), 
                    percentage, 
                    status.trim()
            );
            
            // Update notify status in sheet
            boolean updated = weeklyGUTExamListApp.updateGUTExamNotifyStatus(spreadsheetId, sheetName, email.trim(), "Sent");
            if (updated) {
                response.put("success", true);
                response.put("message", "Email sent and status updated successfully!");
            } else {
                response.put("success", true);
                response.put("message", "Email sent, but sheet failed to update.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error sending email: " + e.getMessage());
        }
        return response;
    }
}
