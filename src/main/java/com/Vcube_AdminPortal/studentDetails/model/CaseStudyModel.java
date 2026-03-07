package com.Vcube_AdminPortal.studentDetails.model;

import java.util.List;

import lombok.Data;

@Data
public class CaseStudyModel {
	
	private String batch;
	private String rollNoSet;
	private String rollNo;
	private String date;
	private Integer totalCaseStudies;
	private Integer studentTotalCaseStudies;
	private List<String> dates;
	private List<String> caseStudyStatus;
	private StudentModel student;

}
