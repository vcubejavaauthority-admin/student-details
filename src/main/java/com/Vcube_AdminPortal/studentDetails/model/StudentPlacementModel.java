package com.Vcube_AdminPortal.studentDetails.model;

import lombok.Data;

@Data
public class StudentPlacementModel {
	
	private String batchNo;
	private String rollNo;
	private String name;
	private String photoURL;
	private String mobile;
	private String alternateMobile;
	private String email;
	private String modeOfTraining;
	private String pgQualification; // Post Graduation (PG)
	private String pgPassedOutYear;
	private String pgPercentage;
	private String ugQualification; // Under Graduation (UG)
	private String ugStream; // UG Stream (Branch)
	private String ugPassedOutYear;
	private String ugPercentage;
	private String universityName;
	private String collegeName;
	private String workExperience;
	private String previousJob;
	private String previousJobRole;
	private String previousCompanyName;

}
