package com.Vcube_AdminPortal.studentDetails.excelSheetCodes;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import com.Vcube_AdminPortal.studentDetails.utlity.GoogleSheetsUtil;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

@Service
public class OnlineEmailsApp {
	
	public boolean upsertOnlineEmail(String batch,String email, String newDate) {
		DateTimeFormatter inFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		DateTimeFormatter outFmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		String date = LocalDate.parse(newDate, inFmt).format(outFmt);
	    try {
	        Sheets sheets = GoogleSheetsUtil.getSheetsService();
	        String spreadsheetId =  BatchsListApp.getSheetIdbyBatchNo(batch);
	        String sheetName = "Online";

	        // 1) Read existing A,B columns from row 2 downwards
	        String range = sheetName + "!A2:C";
	        ValueRange resp = sheets.spreadsheets().values()
	                .get(spreadsheetId, range)
	                .execute();

	        List<List<Object>> rows = resp.getValues();
	        int foundRowIndex = -1;
	        String existingDate = null;

	        if (rows != null) {
	            for (int i = 0; i < rows.size(); i++) {
	                List<Object> row = rows.get(i);
	                if (row == null || row.isEmpty()) continue;

	                String existingMail = row.get(0).toString().trim();
	                if (existingMail.equalsIgnoreCase(email.trim())) {
	                    foundRowIndex = i;
	                    if (row.size() > 1 && row.get(1) != null) {
	                        existingDate = row.get(1).toString().trim();
	                    }
	                    break;
	                }
	            }
	        }

	        String cleanedMail = email.trim();
	        String cleanedDate = date.trim();

	        // 2) Email found
	        if (foundRowIndex >= 0) {
	            // Same date already? -> do nothing
	            if (existingDate != null && existingDate.equals(cleanedDate)) {
	                return true; // no change
	            }

	            // Different date -> update B column only
	            int targetRow = foundRowIndex + 2; // because A2 = index 0
	            String dateCell = sheetName + "!B" + targetRow;
	            ValueRange body = new ValueRange()
	                    .setValues(List.of(List.of(cleanedDate)));

	            sheets.spreadsheets().values()
	                    .update(spreadsheetId, dateCell, body)
	                    .setValueInputOption("RAW")
	                    .execute();
	        } else {
	            // 3) Email not found -> append new row A=email, B=date
	            List<List<Object>> newRows = new ArrayList<>();
	            newRows.add(List.of(cleanedMail, cleanedDate));

	            ValueRange body = new ValueRange().setValues(newRows);
	            sheets.spreadsheets().values()
	                    .append(spreadsheetId, sheetName + "!A:B", body)
	                    .setValueInputOption("RAW")
	                    .setInsertDataOption("INSERT_ROWS")
	                    .execute();
	        }

	        return true;
	    } catch (Exception e) {
	        e.printStackTrace();
	        return false;
	    }
	}
	
	public static List<String> getOnlineEmailsByBatchNoWithFilter(String batch) {
		List<String> emails = new ArrayList<>();
		try {
			Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
			String spreadsheetId =BatchsListApp.getSheetIdbyBatchNo(batch);
			String sheetName = "Online"; // or your actual tab name

			String today = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
			String []todayDate =today.split("-");
			int todayDateNumber = Integer.parseInt(todayDate[0]);
			int todayMonthNumber = Integer.parseInt(todayDate[1]);
			int todayYearNumber = Integer.parseInt(todayDate[2]);
	
			String dataRange = sheetName + "!A2:Z1000";
			ValueRange dataResponse = sheetsService.spreadsheets().values().get(spreadsheetId, dataRange).execute();
			List<List<Object>> rows = dataResponse.getValues();
			if (rows == null) {
				rows = new ArrayList<>();
			}

			for (List<Object> row : rows) {
				if (row == null || row.isEmpty()) {
					continue;
				}

				String onlineEmail = row.size() > 0 ? row.get(0).toString().trim() : "";
				String validity = row.size() > 1 ? row.get(1).toString().trim() : "";

				if (onlineEmail.isBlank() || validity.isEmpty()) {
					continue;
				}

				if (validity.equalsIgnoreCase("online")) {
					if(!emails.contains(onlineEmail)) {
						emails.add(onlineEmail);
					}
				} else {
					String []date =validity.split("-");
					if (todayDateNumber <= Integer.parseInt(date[0]) 
							&& todayMonthNumber<= Integer.parseInt(date[1])
							&& todayYearNumber<= Integer.parseInt(date[2])) {
						if(!emails.contains(onlineEmail)) {
							emails.add(onlineEmail);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return emails;
	}
}
