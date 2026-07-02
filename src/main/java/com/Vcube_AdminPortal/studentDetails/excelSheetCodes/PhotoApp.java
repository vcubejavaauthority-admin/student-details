package com.Vcube_AdminPortal.studentDetails.excelSheetCodes;

import java.util.Collections;

import org.springframework.stereotype.Service;

import com.Vcube_AdminPortal.studentDetails.utlity.GoogleDriveUtil;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;

@Service
public class PhotoApp {

    // ✅ SHARED DRIVE ID HERE (URL lo id= portion copy cheyyi)
    private static final String SHARED_DRIVE_ID = "1bbqOuP8hkgVBMB0bWgGoBUM7L4EBIr9e"; 
    
    public String uploadPhotoToDrive(String rollNo, byte[] photoBytes) throws Exception {
        Drive driveService = GoogleDriveUtil.getDriveService();

        File fileMetadata = new File();
        fileMetadata.setName("student_" + rollNo + "_" + System.currentTimeMillis() + ".jpg");
        
        // ✅ SHARED DRIVE - 2 LINES ADD CHEYYI:
        fileMetadata.setParents(Collections.singletonList(SHARED_DRIVE_ID));
        
        ByteArrayContent mediaContent = new ByteArrayContent("image/jpeg", photoBytes);

        // ✅ SHARED DRIVE SUPPORT - 1 LINE ADD CHEYYI:
        File file = driveService.files().create(fileMetadata, mediaContent)
            .setFields("id, webViewLink")
            .setSupportsAllDrives(true)  // 👈 CRITICAL!
            .execute();

        Permission permission = new Permission().setType("anyone").setRole("reader");
        // ✅ SHARED DRIVE SUPPORT - Permissions kuda:
        driveService.permissions().create(file.getId(), permission)
            .setSupportsAllDrives(true)  // 👈 CRITICAL!
            .execute();

        return file.getWebViewLink();
    }

    // updateStudentPhotoInSheet method - NO CHANGE needed
    public void updateStudentPhotoInSheet(String rollNo, String photoUrl) throws Exception {
        // ... existing code same
    }

    public String uploadAndUpdatePhoto(String rollNo, byte[] photoBytes) throws Exception {
        if (photoBytes == null || photoBytes.length == 0) {
            throw new IllegalArgumentException("Photo bytes cannot be empty");
        }
        String photoUrl = uploadPhotoToDrive(rollNo, photoBytes);
        updateStudentPhotoInSheet(rollNo, photoUrl);
        return photoUrl;
    }
}

