package com.Vcube_AdminPortal.studentDetails.utlity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
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
    private static final List<String> SCOPES =
            Collections.singletonList(SheetsScopes.SPREADSHEETS);

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

        return new Sheets.Builder(
                httpTransport,
                JSON_FACTORY,
                new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
}