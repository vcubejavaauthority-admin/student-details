package com.Vcube_AdminPortal.studentDetails.controller;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.BatchsListApp;
import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.StudentProjectApp;
import com.Vcube_AdminPortal.studentDetails.model.ProjectSubmissionModel;
import com.Vcube_AdminPortal.studentDetails.model.ProjectModel;

@Controller
public class ProjectController {

    @Autowired
    private StudentProjectApp studentProjectApp;

    @GetMapping("/projects/form")
    public String showProjectForm(Model model) {
        model.addAttribute("submission", new ProjectSubmissionModel());
        model.addAttribute("batches", BatchsListApp.getAllBatchesList());
        return "project-form";
    }

    @GetMapping("/api/projects/headers")
    @ResponseBody
    public Map<String, List<String>> getProjectHeaders(@RequestParam String batch) {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("coreJava", studentProjectApp.getProjectHeaders(batch, "Core-Java-Console-Projects"));
        headers.put("webApps", studentProjectApp.getProjectHeaders(batch, "Web-Applications"));
        return headers;
    }

    @GetMapping("/api/projects/data")
    @ResponseBody
    public ProjectModel getProjectData(@RequestParam String batch, @RequestParam String rollNo) {
        return studentProjectApp.getAllProjectsDetailsWithStatusByBatchAndRolNo(batch, rollNo);
    }

    @PostMapping("/projects/submit")
    public String submitProjects(@ModelAttribute ProjectSubmissionModel submission, RedirectAttributes ra) {
        boolean saved = studentProjectApp.saveProjectSubmission(submission);
        if (saved) {
            ra.addFlashAttribute("success", "Project details updated successfully!");
        } else {
            ra.addFlashAttribute("error", "Failed to update project details.");
        }
        return "redirect:/projects/form";
    }
}
