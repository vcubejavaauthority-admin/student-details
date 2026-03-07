package com.Vcube_AdminPortal.studentDetails.model;


import java.util.List;

import lombok.Data;

@Data
public class MockModel {
	
	private String batch;
	private String rollNoSetWithMarks;
	private String rollNo;
	private String date;
	private List<String> dates;
	private List<String> marks;
	private Integer totalMocks;
	private Integer StudentTotalMocks;
	private Integer totalMocksMarks;
	private Integer StudentTotalMocksMarks;
	private Double StudentPercentage;
	private StudentModel student;

}
