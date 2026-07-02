package com.Vcube_AdminPortal.studentDetails.model;

import lombok.Data;

@Data
public class StudentModel {

	private String batchNo;
	private String rollNo;
	private String Name;
	private Long mobile;
	private String email;
	private String photoURL;
	private String mark;
	private boolean online;
	private String status;
	private String technical;
	private String communication;
	private String remarks;

}

