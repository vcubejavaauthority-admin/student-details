package com.Vcube_AdminPortal.studentDetails.model;

public class WeeklyGUTExamRecord {
    private String serialNo;
    private String batchAndRollNo;
    private String name;
    private String email;
    private String marks;
    private String starRating;
    private String notificationStatus;
    private String status;

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public String getSerialNo() {
        return serialNo;
    }
    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;
    }
    public String getBatchAndRollNo() {
        return batchAndRollNo;
    }
    public void setBatchAndRollNo(String batchAndRollNo) {
        this.batchAndRollNo = batchAndRollNo;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getMarks() {
        return marks;
    }
    public void setMarks(String marks) {
        this.marks = marks;
    }
    public String getStarRating() {
        return starRating;
    }
    public void setStarRating(String starRating) {
        this.starRating = starRating;
    }
    public String getNotificationStatus() {
        return notificationStatus;
    }
    public void setNotificationStatus(String notificationStatus) {
        this.notificationStatus = notificationStatus;
    }
}
