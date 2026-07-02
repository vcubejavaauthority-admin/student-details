package com.Vcube_AdminPortal.studentDetails.model;

import java.util.List;

import lombok.Data;

@Data
public class BatchAttendanceReportModel {

	private String batch;
	private int totalStudents;
	private int regularCount;       // ≥75% attendance
	private int irregularCount;     // <75% attendance
	private int totalWorkingDays;
	private int totalPresentCount;
	private int totalAbsentCount;

	// Per-student breakdown
	private List<StudentAttendanceSummary> students;

	@Data
	public static class StudentAttendanceSummary {
		private String rollNo;
		private String name;
		private int presentDays;
		private int absentDays;
		private int totalDays;
		private double attendancePercentage;
		private boolean regular; // ≥75%
		private String status;
	}
}
