package com.Vcube_AdminPortal.studentDetails.model;

import java.util.List;

import lombok.Data;

@Data
public class GUTExamModel {
	
	private String batch;
	private String rollNoSetWithMarks;
	private String rollNo;
	private String date;
	private List<String> dates;
	private List<String> marks;
	private Integer totalGUTExams;
	private Integer StudentTotalGUTExams;
	private Integer totalGUTExamsMarks;
	private Integer StudentTotalGUTExamsMarks;
	private Double StudentPercentage;
	private StudentModel student;

}
