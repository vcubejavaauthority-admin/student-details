package com.Vcube_AdminPortal.studentDetails.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.SubjectSheetMapping;
import com.Vcube_AdminPortal.studentDetails.service.DailyTasksService;

@Controller
public class DailyTasksController {

    @Autowired
    private DailyTasksService dailyTasksService;

    @GetMapping("/daily-tasks")
    public String showDailyTasksPage(Model model) {
        model.addAttribute("subjects", SubjectSheetMapping.getAllMappings().keySet());
        return "daily-tasks";
    }

    @GetMapping("/api/tasks/topics")
    @ResponseBody
    public List<String> getTopics(@RequestParam String subject) {
        try {
            return dailyTasksService.getTopics(subject);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @GetMapping("/api/tasks/questions")
    @ResponseBody
    public List<String> getQuestions(
            @RequestParam String subject,
            @RequestParam String topic,
            @RequestParam String level) {
        try {
            return dailyTasksService.getQuestions(subject, topic, level);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
