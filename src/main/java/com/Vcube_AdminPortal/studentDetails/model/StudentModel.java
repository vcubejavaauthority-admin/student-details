package com.Vcube_AdminPortal.studentDetails.model;

import lombok.Data;

@Data
public class StudentModel {

	private String rollNo;
	private String Name;
	private Long mobile;
	private String email;
	private String photoURL;
	private String mark;
	private boolean online;

}
