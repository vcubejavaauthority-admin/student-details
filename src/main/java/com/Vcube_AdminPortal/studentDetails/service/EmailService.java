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
}
