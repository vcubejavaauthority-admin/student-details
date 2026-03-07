package com.Vcube_AdminPortal.studentDetails.model;

import java.util.List;

import lombok.Data;

@Data
public class SheetModel {
	
	private List<Object> headerRows;
	private List<List<Object>> bodyRows;

}
