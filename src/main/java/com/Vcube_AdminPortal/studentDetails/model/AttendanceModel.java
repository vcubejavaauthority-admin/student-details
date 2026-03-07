package com.Vcube_AdminPortal.studentDetails.model;

import java.util.List;

import lombok.Data;

@Data
public class AttendanceModel {

	private String batch;
	private String rollNoSet;
	private String rollNo;
	private String date;
	
	private List<String> emailsList;
	private List<String> onlineEmailsList;
	private List<String> invalidRollSet;
	private Integer attendanceCount;
	
	private List<String> dates;
	private List<String> attendanceStatus;
	private List<String> months;
	private List<Integer> totalPresents;
	private List<Integer> totalAbsents;
	private Integer totalAttendance;
	private Integer StudentTotalAttendance;
	private Double StudentAttendancePercentage;
	private StudentModel student;
}
