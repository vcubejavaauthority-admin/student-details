package com.Vcube_AdminPortal.studentDetails.utlity;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

public class GoogleSheetsUtil {

	private static final String APPLICATION_NAME = "Attendance App";
	private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
	private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);

	/**
	 * Builds and returns an authorized Sheets client service using a service
	 * account. Make sure the credentials file exists at:
	 * src/main/resources/credentialKeys/vcube-admin-portal-credential.json and that
	 * the sheet is shared with the service-account email.
	 */
	public static Sheets getSheetsService() throws IOException, GeneralSecurityException {

     // Read credentials from environment variable
        String credentialsJson = System.getenv("GOOGLE_CREDENTIALS");

        if (credentialsJson == null) {
            throw new IOException("GOOGLE_CREDENTIALS environment variable not set.");
        }

        ByteArrayInputStream credentialsStream =
                new ByteArrayInputStream(credentialsJson.getBytes());

        GoogleCredentials credentials = GoogleCredentials
                .fromStream(credentialsStream)
                .createScoped(SCOPES);


        HttpTransport httpTransport = new NetHttpTransport();

        return new Sheets.Builder(httpTransport, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

	/**
	 * Resolves the HTML5 date input (yyyy-MM-dd) to Google Sheets expected format
	 * (dd-MM-yyyy). If date is empty or invalid, defaults to today's date.
	 */
	public static String resolveDate(String inputDate) {
		if (inputDate == null || inputDate.isBlank()) {
			return new SimpleDateFormat("dd-MM-yyyy").format(new Date());
		}
		try {
			if (inputDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
				Date parsed = new SimpleDateFormat("yyyy-MM-dd").parse(inputDate);
				return new SimpleDateFormat("dd-MM-yyyy").format(parsed);
			}
			return inputDate;
		} catch (Exception e) {
			return new SimpleDateFormat("dd-MM-yyyy").format(new Date());
		}
	}

	public static int findOrInsertDateColumn(Sheets sheetsService, String spreadsheetId, String sheetName,
			String todayStr, int dateStartIndex) throws IOException {
		String headerRange = sheetName + "!A1:ZZ1";
		com.google.api.services.sheets.v4.model.ValueRange headerResponse = sheetsService.spreadsheets().values()
				.get(spreadsheetId, headerRange).execute();
		List<List<Object>> headerValues = headerResponse.getValues();

		List<Object> headerRow = new java.util.ArrayList<>();
		if (headerValues != null && !headerValues.isEmpty()) {
			headerRow.addAll(headerValues.get(0));
		}

		int dateColIndex = -1;
		for (int i = 0; i < headerRow.size(); i++) {
			Object v = headerRow.get(i);
			if (v != null && todayStr.equals(v.toString().trim())) {
				dateColIndex = i;
				break;
			}
		}

		if (dateColIndex != -1) {
			return dateColIndex;
		}

		// Column not found, find chronological insertion point
		java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("dd-MM-yyyy");
		java.util.Date todayDate = null;
		try {
			todayDate = fmt.parse(todayStr);
		} catch (Exception e) {
		}

		int insertIndex = headerRow.size();
		if (todayDate != null) {
			for (int i = dateStartIndex; i < headerRow.size(); i++) {
				Object v = headerRow.get(i);
				if (v != null) {
					try {
						java.util.Date colDate = fmt.parse(v.toString().trim());
						if (todayDate.before(colDate)) {
							insertIndex = i;
							break;
						}
					} catch (Exception e) {
					}
				}
			}
		}

		// Shift columns using Google Sheets API (always insert if not appending at the
		// very wide end)
		com.google.api.services.sheets.v4.model.Spreadsheet ss = sheetsService.spreadsheets().get(spreadsheetId)
				.execute();
		Integer sheetId = ss.getSheets().stream().filter(s -> sheetName.equals(s.getProperties().getTitle()))
				.findFirst().orElseThrow(() -> new IOException("Sheet not found: " + sheetName)).getProperties()
				.getSheetId();

		com.google.api.services.sheets.v4.model.InsertDimensionRequest insertCol = new com.google.api.services.sheets.v4.model.InsertDimensionRequest()
				.setRange(new com.google.api.services.sheets.v4.model.DimensionRange().setSheetId(sheetId)
						.setDimension("COLUMNS").setStartIndex(insertIndex).setEndIndex(insertIndex + 1));

		if (insertIndex > 0) {
			insertCol.setInheritFromBefore(true);
		}

		com.google.api.services.sheets.v4.model.Request req = new com.google.api.services.sheets.v4.model.Request()
				.setInsertDimension(insertCol);
		com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest batchReq = new com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest()
				.setRequests(Collections.singletonList(req));
		sheetsService.spreadsheets().batchUpdate(spreadsheetId, batchReq).execute();

		// Write the date header value at exactly the right cell
		String colLetter = indexToColumn(insertIndex);
		String dateCellRange = sheetName + "!" + colLetter + "1";
		com.google.api.services.sheets.v4.model.ValueRange dateBody = new com.google.api.services.sheets.v4.model.ValueRange()
				.setValues(Collections.singletonList(Collections.singletonList(todayStr)));

		sheetsService.spreadsheets().values().update(spreadsheetId, dateCellRange, dateBody).setValueInputOption("RAW")
				.execute();

		return insertIndex;
	}

	/** 0 -> A, 1 -> B, 2 -> C ... */
	public static String indexToColumn(int index) {
		StringBuilder sb = new StringBuilder();
		int n = index;
		do {
			int rem = n % 26;
			sb.insert(0, (char) ('A' + rem));
			n = n / 26 - 1;
		} while (n >= 0);
		return sb.toString();
	}
}
