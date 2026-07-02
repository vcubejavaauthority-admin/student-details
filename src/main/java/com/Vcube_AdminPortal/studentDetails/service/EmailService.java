package com.Vcube_AdminPortal.studentDetails.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.Vcube_AdminPortal.studentDetails.model.MockDriveModel;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendFeedbackEmail(MockDriveModel model) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(model.getEmailId());
        helper.setSubject("Mock Drive Feedback - Vcube Java");

        String jamStars = toStars(model.getJamRatingRound2(), 10);
        String overallStars = toStars(model.getOverall(), 5);

        String content = "<html><body style='font-family: Arial, sans-serif; color: #333;'>"
                + "<div style='max-width: 600px; margin: auto; border: 1px solid #ddd; padding: 20px; border-radius: 10px;'>"
                + "<h2 style='color: #ffc107; text-align: center;'>Mock Drive Feedback Report</h2>"
                + "<p>Dear <b>" + model.getStudentName() + "</b>,</p>"
                + "<p>Well done on completing your mock drive! Here is your detailed performance feedback:</p>"
                + "<table style='width: 100%; border-collapse: collapse; margin-top: 20px;'>"
                + "  <tr style='background-color: #f8f9fa;'>"
                + "    <th style='border: 1px solid #ddd; padding: 12px; text-align: left;'>Rounds</th>"
                + "    <th style='border: 1px solid #ddd; padding: 12px; text-align: left;'>Assessment / Score</th>"
                + "  </tr>"
                + "  <tr>"
                + "    <td style='border: 1px solid #ddd; padding: 10px;'><b>Round-1: Aptitude</b></td>"
                + "    <td style='border: 1px solid #ddd; padding: 10px;'>" + model.getAptitudeRound1() + "</td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td style='border: 1px solid #ddd; padding: 10px;'><b>Round-1: Technical</b></td>"
                + "    <td style='border: 1px solid #ddd; padding: 10px;'>" + model.getTechnicalRound1() + "</td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td style='border: 1px solid #ddd; padding: 10px;'><b>Round-2: JAM Rating</b></td>"
                + "    <td style='border: 1px solid #ddd; padding: 10px; color: #ffc107; font-size: 1.2rem;'>" + jamStars + "</td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td style='border: 1px solid #ddd; padding: 10px;'><b>Round-3: Technical Interview Score</b></td>"
                + "    <td style='border: 1px solid #ddd; padding: 10px;'>" + model.getTechnicalInterviewScoreRound3() + " / 10</td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td style='border: 1px solid #ddd; padding: 10px;'><b>Round-3: Technical Feedback</b></td>"
                + "    <td style='border: 1px solid #ddd; padding: 10px;'>" + model.getTechnicalInterviewFeedbackRound3() + "</td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td style='border: 1px solid #ddd; padding: 10px;'><b>Round-4: HR Interview Score</b></td>"
                + "    <td style='border: 1px solid #ddd; padding: 10px;'>" + model.getHrInterviewScoreRound4() + " / 10</td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td style='border: 1px solid #ddd; padding: 10px;'><b>Round-4: HR Feedback</b></td>"
                + "    <td style='border: 1px solid #ddd; padding: 10px;'>" + model.getHrInterviewFeedbackRound4() + "</td>"
                + "  </tr>"
                + "  <tr style='background-color: #fff3cd;'>"
                + "    <td style='border: 1px solid #ddd; padding: 10px;'><b>Overall Rating</b></td>"
                + "    <td style='border: 1px solid #ddd; padding: 10px; color: #ffc107; font-size: 1.2rem;'><b>" + overallStars + "</b></td>"
                + "  </tr>"
                + "</table>"
                + "<p style='margin-top: 25px;'>Keep practicing and improving! Best of luck for your future interviews.</p>"
                + "<p>Best Regards,<br><b>Vcube Training Institute</b></p>"
                + "</div>"
                + "</body></html>";

        helper.setText(content, true);
        mailSender.send(message);
    }

    private String toStars(String rating, int max) {
        if (rating == null || rating.isEmpty()) return "N/A";
        try {
            double r = Double.parseDouble(rating);
            StringBuilder stars = new StringBuilder();
            for (int i = 0; i < max; i++) {
                if (i < r) stars.append("⭐");
                else stars.append("☆");
            }
            return stars.toString();
        } catch (Exception e) {
            return rating;
        }
    }

    public void sendMonthlyPerformanceEmail(
            String toEmail, String studentName, String rollNo, String batch, String monthLabel,
            int attPresent, int attTotal, double attPercentage,
            int examAttended, int examTotal, double examMarks, double examPercentage,
            int mockAttended, int mockTotal, double mockMarks, double mockPercentage) throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(toEmail);
        helper.setSubject("Monthly Student Performance Report - " + monthLabel);

        // Formatting attendance status
        String attStatusBadge = attPercentage >= 75.0 
                ? "<span style='background-color: #d1e7dd; color: #0f5132; padding: 4px 8px; border-radius: 4px; font-size: 0.85rem; font-weight: bold;'>Regular</span>"
                : "<span style='background-color: #f8d7da; color: #842029; padding: 4px 8px; border-radius: 4px; font-size: 0.85rem; font-weight: bold;'>Irregular</span>";

        // Formatting exam status
        String examStatusBadge = examPercentage >= 50.0 
                ? "<span style='background-color: #d1e7dd; color: #0f5132; padding: 4px 8px; border-radius: 4px; font-size: 0.85rem; font-weight: bold;'>Pass</span>"
                : "<span style='background-color: #f8d7da; color: #842029; padding: 4px 8px; border-radius: 4px; font-size: 0.85rem; font-weight: bold;'>Fail</span>";

        // Formatting mock status
        String mockStatusBadge = mockPercentage >= 50.0 
                ? "<span style='background-color: #d1e7dd; color: #0f5132; padding: 4px 8px; border-radius: 4px; font-size: 0.85rem; font-weight: bold;'>Pass</span>"
                : "<span style='background-color: #f8d7da; color: #842029; padding: 4px 8px; border-radius: 4px; font-size: 0.85rem; font-weight: bold;'>Fail</span>";

        String content = "<html><body style='font-family: Arial, sans-serif; color: #333; line-height: 1.5; background-color: #f4f6f9; padding: 20px;'>"
                + "<div style='max-width: 600px; margin: auto; background-color: #ffffff; border: 1px solid #e3e6f0; padding: 30px; border-radius: 12px; box-shadow: 0 4px 8px rgba(0,0,0,0.05);'>"
                + "  <div style='text-align: center; margin-bottom: 25px; border-bottom: 2px solid #f1f3f9; padding-bottom: 20px;'>"
                + "    <h2 style='color: #0d6efd; margin: 0 0 5px 0;'>Vcube Training Institute</h2>"
                + "    <h3 style='color: #495057; margin: 0; font-weight: normal;'>Monthly Performance Report - " + monthLabel + "</h3>"
                + "  </div>"
                + "  <p>Dear <b>" + studentName + "</b>,</p>"
                + "  <p>Here is your academic and attendance performance report for the month of <b>" + monthLabel + "</b>:</p>"
                + "  "
                + "  <div style='background-color: #f8f9fa; border-radius: 8px; padding: 15px; margin-bottom: 25px; border-left: 4px solid #0d6efd;'>"
                + "    <table style='width: 100%; border-collapse: collapse;'>"
                + "      <tr><td style='padding: 4px 0; color: #6c757d; width: 30%;'>Roll Number:</td><td style='padding: 4px 0; font-weight: bold;'>" + rollNo + "</td></tr>"
                + "      <tr><td style='padding: 4px 0; color: #6c757d;'>Batch:</td><td style='padding: 4px 0; font-weight: bold;'>" + batch + "</td></tr>"
                + "    </table>"
                + "  </div>"
                + "  "
                + "  <table style='width: 100%; border-collapse: collapse; margin-bottom: 25px;'>"
                + "    <thead>"
                + "      <tr style='background-color: #0d6efd; color: #ffffff;'>"
                + "        <th style='padding: 12px 10px; border: 1px solid #dee2e6; text-align: left;'>Metric</th>"
                + "        <th style='padding: 12px 10px; border: 1px solid #dee2e6; text-align: center;'>Summary / Score</th>"
                + "        <th style='padding: 12px 10px; border: 1px solid #dee2e6; text-align: center;'>Percentage</th>"
                + "        <th style='padding: 12px 10px; border: 1px solid #dee2e6; text-align: center;'>Status</th>"
                + "      </tr>"
                + "    </thead>"
                + "    <tbody>"
                + "      <tr>"
                + "        <td style='padding: 12px 10px; border: 1px solid #dee2e6;'><b>Attendance</b></td>"
                + "        <td style='padding: 12px 10px; border: 1px solid #dee2e6; text-align: center;'>" + attPresent + " / " + attTotal + " days</td>"
                + "        <td style='padding: 12px 10px; border: 1px solid #dee2e6; text-align: center; font-weight: bold;'>" + attPercentage + "%</td>"
                + "        <td style='padding: 12px 10px; border: 1px solid #dee2e6; text-align: center;'>" + attStatusBadge + "</td>"
                + "      </tr>"
                + "      <tr>"
                + "        <td style='padding: 12px 10px; border: 1px solid #dee2e6;'><b>Exams</b></td>"
                + "        <td style='padding: 12px 10px; border: 1px solid #dee2e6; text-align: center;'>" + examMarks + " marks (" + examAttended + "/" + examTotal + " exams)</td>"
                + "        <td style='padding: 12px 10px; border: 1px solid #dee2e6; text-align: center; font-weight: bold;'>" + examPercentage + "%</td>"
                + "        <td style='padding: 12px 10px; border: 1px solid #dee2e6; text-align: center;'>" + examStatusBadge + "</td>"
                + "      </tr>"
                + "      <tr>"
                + "        <td style='padding: 12px 10px; border: 1px solid #dee2e6;'><b>Mocks</b></td>"
                + "        <td style='padding: 12px 10px; border: 1px solid #dee2e6; text-align: center;'>" + mockMarks + " marks (" + mockAttended + "/" + mockTotal + " mocks)</td>"
                + "        <td style='padding: 12px 10px; border: 1px solid #dee2e6; text-align: center; font-weight: bold;'>" + mockPercentage + "%</td>"
                + "        <td style='padding: 12px 10px; border: 1px solid #dee2e6; text-align: center;'>" + mockStatusBadge + "</td>"
                + "      </tr>"
                + "    </tbody>"
                + "  </table>"
                + "  "
                + "  <div style='margin-top: 30px; border-top: 1px solid #f1f3f9; padding-top: 20px; font-size: 0.95rem; color: #555;'>"
                + "    <p>Please review these results. Continuous attendance and passing marks are essential for final placement eligibility.</p>"
                + "    <p>Keep up the hard work! If you have any queries regarding your marks, please reach out to your coordinator.</p>"
                + "    <p style='margin-bottom: 0;'>Best Regards,<br><b>Vcube Team</b></p>"
                + "  </div>"
                + "</div>"
                + "</body></html>";

        helper.setText(content, true);
        mailSender.send(message);
    }

    public void sendGUTExamPerformanceEmail(
            String toEmail, String studentName, String rollNo, String dateLabel,
            String marks, String stars, double percentage, String status) throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(toEmail);

        boolean isShortlisted = status.equalsIgnoreCase("shortlist");
        String subject = isShortlisted 
                ? "Congratulations! You are Shortlisted - GUT Exam (" + dateLabel + ")"
                : "GUT Exam Performance Feedback - " + dateLabel;
        helper.setSubject(subject);

        // Styling based on shortlist status
        String themeColor = isShortlisted ? "#28a745" : "#5a6268";
        String statusLabel = isShortlisted ? "Shortlisted" : "Not Shortlisted";

        String statusCell = isShortlisted
                ? "<td style='padding: 12px 10px; border: 1px solid #dee2e6; text-align: center; color: #155724; font-weight: bold; background-color: #d4edda;'>" + statusLabel + "</td>"
                : "<td style='padding: 12px 10px; border: 1px solid #dee2e6; text-align: center; color: #721c24; font-weight: bold; background-color: #f8d7da;'>" + statusLabel + "</td>";

        String welcomeText = isShortlisted
                ? "<p>Dear <b>" + studentName + "</b>,</p>"
                  + "<p style='font-size: 1.1rem; color: #155724;'><b>Congratulations!</b> We are thrilled to inform you that you have been <b>shortlisted</b> based on your performance in the GUT Exam conducted on <b>" + dateLabel + "</b>.</p>"
                : "<p>Dear <b>" + studentName + "</b>,</p>"
                  + "<p>Thank you for participating in the GUT Exam conducted on <b>" + dateLabel + "</b>. We have processed the results and prepared your performance scorecard below.</p>";

        String closingText = isShortlisted
                ? "<p style='margin-top: 25px; font-weight: bold;'>Keep up the outstanding work! Our team will contact you soon regarding the next steps in the process.</p>"
                : "<p style='margin-top: 25px;'>While you were not shortlisted this time, we encourage you to use this feedback constructively to review your performance and prepare thoroughly for future opportunities. Continuous practice is key to success!</p>";

        String content = "<html><body style='font-family: Arial, sans-serif; color: #333; line-height: 1.5; background-color: #f4f6f9; padding: 20px;'>"
                + "<div style='max-width: 600px; margin: auto; background-color: #ffffff; border: 1px solid #e3e6f0; padding: 30px; border-radius: 12px; box-shadow: 0 4px 8px rgba(0,0,0,0.05);'>"
                + "  <div style='text-align: center; margin-bottom: 25px; border-bottom: 2px solid #f1f3f9; padding-bottom: 20px;'>"
                + "    <h2 style='color: " + themeColor + "; margin: 0 0 5px 0;'>Vcube Training Institute</h2>"
                + "    <h3 style='color: #495057; margin: 0; font-weight: normal;'>GUT Exam Performance Report</h3>"
                + "  </div>"
                + "  " + welcomeText
                + "  "
                + "  <div style='background-color: #f8f9fa; border-radius: 8px; padding: 15px; margin-bottom: 25px; border-left: 4px solid " + themeColor + ";'>"
                + "    <table style='width: 100%; border-collapse: collapse;'>"
                + "      <tr><td style='padding: 4px 0; color: #6c757d; width: 30%;'>Roll Number:</td><td style='padding: 4px 0; font-weight: bold;'>" + rollNo + "</td></tr>"
                + "      <tr><td style='padding: 4px 0; color: #6c757d;'>Exam Date:</td><td style='padding: 4px 0; font-weight: bold;'>" + dateLabel + "</td></tr>"
                + "    </table>"
                + "  </div>"
                + "  "
                + "  <table style='width: 100%; border-collapse: collapse; margin-bottom: 25px;'>"
                + "    <thead>"
                + "      <tr style='background-color: " + themeColor + "; color: #ffffff;'>"
                + "        <th style='padding: 12px 10px; border: 1px solid #dee2e6; text-align: left;'>Metric</th>"
                + "        <th style='padding: 12px 10px; border: 1px solid #dee2e6; text-align: center;'>Summary / Score</th>"
                + "        <th style='padding: 12px 10px; border: 1px solid #dee2e6; text-align: center;'>Percentage</th>"
                + "        <th style='padding: 12px 10px; border: 1px solid #dee2e6; text-align: center;'>Star Rating</th>"
                + "        <th style='padding: 12px 10px; border: 1px solid #dee2e6; text-align: center;'>Status</th>"
                + "      </tr>"
                + "    </thead>"
                + "    <tbody>"
                + "      <tr>"
                + "        <td style='padding: 12px 10px; border: 1px solid #dee2e6;'><b>GUT Exam</b></td>"
                + "        <td style='padding: 12px 10px; border: 1px solid #dee2e6; text-align: center; font-weight: bold;'>" + marks + " / 50</td>"
                + "        <td style='padding: 12px 10px; border: 1px solid #dee2e6; text-align: center; font-weight: bold;'>" + percentage + "%</td>"
                + "        <td style='padding: 12px 10px; border: 1px solid #dee2e6; text-align: center; color: #ffc107; font-size: 1.1rem;'>" + stars + "</td>"
                + "        " + statusCell
                + "      </tr>"
                + "    </tbody>"
                + "  </table>"
                + "  " + closingText
                + "  <div style='margin-top: 30px; border-top: 1px solid #f1f3f9; padding-top: 20px; font-size: 0.95rem; color: #555;'>"
                + "    <p style='margin-bottom: 0;'>Best Regards,<br><b>Vcube Team</b></p>"
                + "  </div>"
                + "</div>"
                + "</body></html>";

        helper.setText(content, true);
        mailSender.send(message);
    }
}
