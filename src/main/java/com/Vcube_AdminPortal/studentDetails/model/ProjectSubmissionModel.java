package com.Vcube_AdminPortal.studentDetails.model;

import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class ProjectSubmissionModel {
    private String batch;
    private String rollNo;
    private String studentName;
    private List<String> coreJavaProjects; // List of selected checkbox values
    private Map<String, String> webApplications; // Map of category -> project name
}
