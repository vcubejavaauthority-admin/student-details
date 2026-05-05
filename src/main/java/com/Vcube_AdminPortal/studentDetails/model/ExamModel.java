package com.Vcube_AdminPortal.studentDetails.model;

import java.util.List;

import lombok.Data;

@Data
public class ExamModel {

	private String batch;
	private String rollNoSetWithMarks;
	private String rollNo;
	private String date;
	private List<String> dates;
	private List<String> marks;
	private Integer totalExams;
	private Integer StudentTotalExams;
	private Integer totalExamsMarks;
	private Integer StudentTotalExamsMarks;
	private Double StudentPercentage;
	private StudentModel student;

}
