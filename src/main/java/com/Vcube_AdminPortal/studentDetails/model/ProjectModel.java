package com.Vcube_AdminPortal.studentDetails.model;

import java.util.List;

import lombok.Data;

@Data
public class ProjectModel {
	
	private String projectName;
	private String status;
	private String rollNo;
	private List<String> projectNames;
	private List<String> projectStatus;
	private Integer totalProjects;
	private Integer studentTotalProjects;
	private StudentModel student;

}
