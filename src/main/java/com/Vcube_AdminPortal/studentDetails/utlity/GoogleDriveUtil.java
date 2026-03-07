package com.Vcube_AdminPortal.studentDetails.utlity;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

public class GoogleDriveUtil {

    private static final String APPLICATION_NAME = "Student Photos App";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES =
            Collections.singletonList(DriveScopes.DRIVE);

    public static Drive getDriveService() throws IOException, GeneralSecurityException {
        InputStream in = GoogleSheetsUtil.class
                .getResourceAsStream("/credentialKeys/vcube-admin-portal-credential.json");

        if (in == null) {
            throw new IOException("Credentials file not found: /credentialKeys/vcube-admin-portal-credential.json");
        }

        GoogleCredentials credentials = GoogleCredentials.fromStream(in)
                .createScoped(SCOPES);

        HttpTransport httpTransport = new NetHttpTransport();

        return new Drive.Builder(httpTransport, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
}
