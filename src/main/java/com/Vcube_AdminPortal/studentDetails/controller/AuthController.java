package com.Vcube_AdminPortal.studentDetails.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.Vcube_AdminPortal.studentDetails.utlity.GoogleSheetsUtil;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {

    // Master spreadsheet that holds the Batchs-List tab (same one used by BatchsListApp)
    private static final String MASTER_SPREADSHEET_ID = "1oOQCVdAD7d0l0VbWk1AIHT0hMsNMCBRI4GP30rkayFA";
    private static final String ADMINS_SHEET = "Admins";

    @GetMapping("/")
    public String landingPage(HttpSession session) {
        if (session.getAttribute("loggedInUser") != null) {
            return "redirect:/dashboard";
        }
        return "landing";
    }

    @GetMapping("/login")
    public String showLoginPage(HttpSession session) {
        if (session.getAttribute("loggedInUser") != null) {
            return "redirect:/dashboard";
        }
        return "login";
    }

    @PostMapping("/auth/login")
    public String handleLogin(@RequestParam String username, @RequestParam String password,
                              HttpSession session, Model model) {
        try {
            Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
            String range = ADMINS_SHEET + "!A2:D";
            ValueRange response = sheetsService.spreadsheets().values()
                    .get(MASTER_SPREADSHEET_ID, range).execute();

            List<List<Object>> rows = response.getValues();
            if (rows != null) {
                for (List<Object> row : rows) {
                    if (row.size() >= 3) {
                        String dbUser = row.get(0).toString().trim();
                        String dbPass = row.get(1).toString().trim();
                        String dbRole = row.get(2).toString().trim();

                        if (username.equals(dbUser) && password.equals(dbPass)) {
                            session.setAttribute("loggedInUser", dbUser);
                            session.setAttribute("userRole", dbRole);
                            String displayName = row.size() > 3 ? row.get(3).toString().trim() : dbUser;
                            session.setAttribute("displayName", displayName);
                            // Store first name for non-admin display
                            String firstName = displayName.contains(" ") ? displayName.split(" ")[0] : displayName;
                            session.setAttribute("firstName", firstName);
                            return "redirect:/dashboard";
                        }
                    }
                }
            }

            model.addAttribute("error", "Credentials wrong please contact Administrator");
            return "login";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Login service unavailable. Try again later.");
            return "login";
        }
    }

    @GetMapping("/auth/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    @GetMapping("/admin/users")
    public String showAdminUsers(HttpSession session, Model model) {
        String role = (String) session.getAttribute("userRole");
        if (!"admin".equalsIgnoreCase(role)) {
            return "redirect:/dashboard";
        }

        try {
            Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
            String range = ADMINS_SHEET + "!A2:D";
            ValueRange response = sheetsService.spreadsheets().values()
                    .get(MASTER_SPREADSHEET_ID, range).execute();

            List<Map<String, String>> users = new ArrayList<>();
            List<List<Object>> rows = response.getValues();
            if (rows != null) {
                for (List<Object> row : rows) {
                    Map<String, String> user = new HashMap<>();
                    user.put("username", row.size() > 0 ? row.get(0).toString() : "");
                    user.put("role", row.size() > 2 ? row.get(2).toString() : "user");
                    user.put("displayName", row.size() > 3 ? row.get(3).toString() : "");
                    users.add(user);
                }
            }
            model.addAttribute("users", users);
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Failed to load users");
        }
        return "admin-users";
    }

    @PostMapping("/admin/users/create")
    @ResponseBody
    public Map<String, Object> createUser(@RequestParam String username, @RequestParam String password,
                                           @RequestParam String role, @RequestParam String displayName,
                                           HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        String currentRole = (String) session.getAttribute("userRole");

        if (!"admin".equalsIgnoreCase(currentRole)) {
            result.put("success", false);
            result.put("message", "Only admins can create users");
            return result;
        }

        try {
            Sheets sheetsService = GoogleSheetsUtil.getSheetsService();

            // Check if username already exists
            String readRange = ADMINS_SHEET + "!A2:A";
            ValueRange existing = sheetsService.spreadsheets().values()
                    .get(MASTER_SPREADSHEET_ID, readRange).execute();
            if (existing.getValues() != null) {
                for (List<Object> row : existing.getValues()) {
                    if (!row.isEmpty() && username.equals(row.get(0).toString().trim())) {
                        result.put("success", false);
                        result.put("message", "Username already exists");
                        return result;
                    }
                }
            }

            // Append new user
            List<List<Object>> newRow = Collections.singletonList(
                    List.of(username, password, role, displayName));
            ValueRange body = new ValueRange().setValues(newRow);
            sheetsService.spreadsheets().values()
                    .append(MASTER_SPREADSHEET_ID, ADMINS_SHEET + "!A:D", body)
                    .setValueInputOption("RAW")
                    .execute();

            result.put("success", true);
            result.put("message", "User created successfully");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
        }
        return result;
    }

    @PostMapping("/admin/users/delete")
    @ResponseBody
    public Map<String, Object> deleteUser(@RequestParam String username, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        String currentRole = (String) session.getAttribute("userRole");
        String currentUser = (String) session.getAttribute("loggedInUser");

        if (!"admin".equalsIgnoreCase(currentRole)) {
            result.put("success", false);
            result.put("message", "Only admins can delete users");
            return result;
        }

        if (username.equals(currentUser)) {
            result.put("success", false);
            result.put("message", "You cannot delete your own account");
            return result;
        }

        try {
            Sheets sheetsService = GoogleSheetsUtil.getSheetsService();
            String range = ADMINS_SHEET + "!A2:D";
            ValueRange response = sheetsService.spreadsheets().values()
                    .get(MASTER_SPREADSHEET_ID, range).execute();

            List<List<Object>> rows = response.getValues();
            if (rows != null) {
                List<List<Object>> updated = new ArrayList<>();
                boolean found = false;
                for (List<Object> row : rows) {
                    if (!row.isEmpty() && username.equals(row.get(0).toString().trim())) {
                        found = true;
                        continue; // skip this row
                    }
                    updated.add(row);
                }

                if (found) {
                    // Clear old data and write back
                    sheetsService.spreadsheets().values()
                            .clear(MASTER_SPREADSHEET_ID, range,
                                    new com.google.api.services.sheets.v4.model.ClearValuesRequest())
                            .execute();

                    if (!updated.isEmpty()) {
                        ValueRange body = new ValueRange().setValues(updated);
                        sheetsService.spreadsheets().values()
                                .update(MASTER_SPREADSHEET_ID, ADMINS_SHEET + "!A2", body)
                                .setValueInputOption("RAW")
                                .execute();
                    }
                    result.put("success", true);
                    result.put("message", "User deleted");
                } else {
                    result.put("success", false);
                    result.put("message", "User not found");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
        }
        return result;
    }
}
