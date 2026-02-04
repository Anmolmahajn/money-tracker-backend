package com.moneytracker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;
import com.moneytracker.model.User;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendNotificationEmail(User user, String subject, String message) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(buildEmailTemplate(user.getFullName(), message), true);

            mailSender.send(mimeMessage);
            log.info("Email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    public void sendBudgetAlert(User user, String budgetName, double percentageUsed,
                                double amountSpent, double budgetLimit) {
        String subject = "Budget Alert: " + budgetName;
        String message = String.format(
                "You've used %.1f%% of your %s budget.\\n" +
                        "Spent: ₹%.2f / ₹%.2f",
                percentageUsed, budgetName, amountSpent, budgetLimit
        );
        sendNotificationEmail(user, subject, message);
    }

    public void sendMonthlySummary(User user, String summaryContent) {
        sendNotificationEmail(user, "Your Monthly Expense Summary", summaryContent);
    }

    private String buildEmailTemplate(String userName, String content) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); 
                             color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background: #f9f9f9; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>Money Tracker Notification</h2>
                    </div>
                    <div class="content">
                        <p>Hi %s,</p>
                        <p>%s</p>
                    </div>
                    <div class="footer">
                        <p>This is an automated message from Money Tracker</p>
                    </div>
                </div>
            </body>
            </html>
            """, userName != null ? userName : "User", content);
    }
}
