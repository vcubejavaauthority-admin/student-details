package com.Vcube_AdminPortal.studentDetails.model;

import java.util.List;

import lombok.Data;

@Data
public class MockExamModel {

	private String batch;
	private String rollNoSetWithMarks;
	private String rollNo;
	private String date;
	private List<String> dates;
	private List<String> marks;
	private Integer totalMockExams;
	private Integer StudentTotalMockExams;
	private Integer totalMockExamsMarks;
	private Integer StudentTotalMockExamsMarks;
	private Double StudentPercentage;
	private StudentModel student;

}
