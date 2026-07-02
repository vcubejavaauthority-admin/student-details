package com.Vcube_AdminPortal.studentDetails.excelSheetCodes;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SubjectSheetMapping {

    private static final Map<String, String> SUBJECT_SHEET_MAP = new HashMap<>();

    @Value("${google.sheet.subject.core-java:}")
    public void setCoreJavaId(String id) {
        SUBJECT_SHEET_MAP.put("Core Java", id);
    }

    @Value("${google.sheet.subject.mysql:}")
    public void setMysqlId(String id) {
        SUBJECT_SHEET_MAP.put("MySQL", id);
    }

    @Value("${google.sheet.subject.html:}")
    public void setHtmlId(String id) {
        SUBJECT_SHEET_MAP.put("HTML", id);
    }

    @Value("${google.sheet.subject.css:}")
    public void setCssId(String id) {
        SUBJECT_SHEET_MAP.put("CSS", id);
    }

    @Value("${google.sheet.subject.javascript:}")
    public void setJavascriptId(String id) {
        SUBJECT_SHEET_MAP.put("JavaScript", id);
    }

    @Value("${google.sheet.subject.servlets:}")
    public void setServletsId(String id) {
        SUBJECT_SHEET_MAP.put("Servlets", id);
    }

    @Value("${google.sheet.subject.spring:}")
    public void setSpringId(String id) {
        SUBJECT_SHEET_MAP.put("Spring", id);
    }

    @Value("${google.sheet.subject.spring-boot:}")
    public void setSpringBootId(String id) {
        SUBJECT_SHEET_MAP.put("Spring Boot", id);
    }

    @Value("${google.sheet.subject.microservices:}")
    public void setMicroservicesId(String id) {
        SUBJECT_SHEET_MAP.put("Microservices", id);
    }

    @Value("${google.sheet.subject.react:}")
    public void setReactId(String id) {
        SUBJECT_SHEET_MAP.put("React", id);
    }


    public static String getSpreadsheetId(String subject) {
        return SUBJECT_SHEET_MAP.get(subject);
    }

    public static Map<String, String> getAllMappings() {
        return SUBJECT_SHEET_MAP;
    }
}
