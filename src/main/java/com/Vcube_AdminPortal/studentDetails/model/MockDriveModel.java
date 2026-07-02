package com.Vcube_AdminPortal.studentDetails.model;

import lombok.Data;

@Data
public class MockDriveModel {
    private String sNo;
    private String studentName;
    private String batchNoRollNo;
    private String emailId;
    
    // Round 1
    private String aptitudeRound1;
    private String technicalRound1;
    
    // Round 2
    private String jamRatingRound2;
    
    // Round 3
    private String technicalInterviewScoreRound3;
    private String technicalInterviewFeedbackRound3;
    
    // Round 4
    private String hrInterviewScoreRound4;
    private String hrInterviewFeedbackRound4;
    
    private String overall;
    private String notifyEmail;
}
