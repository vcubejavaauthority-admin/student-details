package com.Vcube_AdminPortal.studentDetails.model;

import lombok.Data;

@Data
public class StudentEntireInfoModel {
	
	private StudentModel student;
	private AttendanceModel attendanceList;
	private CaseStudyModel caseStudyList;
	private ExamModel examList;
	private GUTExamModel gutExamList;
	private MockExamModel mockExamList;
	private MockModel mockList;
	private ProjectModel projectList;
	

}
