package tn.esprit.service;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class EmailService {

    private final Properties emailProperties;
    private final String username;
    private final String password;

    public EmailService() {
        emailProperties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("config.properties not found in resources");
            }
            emailProperties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Error loading config.properties", e);
        }

        username = emailProperties.getProperty("email.username");
        password = emailProperties.getProperty("email.password");

        // Set up mail server properties
        emailProperties.put("mail.smtp.host", emailProperties.getProperty("email.smtp.host"));
        emailProperties.put("mail.smtp.port", emailProperties.getProperty("email.smtp.port"));
        emailProperties.put("mail.smtp.auth", emailProperties.getProperty("email.smtp.auth"));
        emailProperties.put("mail.smtp.starttls.enable", emailProperties.getProperty("email.smtp.starttls.enable"));
    }

    public void sendStatusUpdateEmail(int entrepreneurId, int applicationId, String status, float amount, String aiResult) {
        String subject = "Funding Application Status Update";
        String body = buildEmailBody(entrepreneurId, applicationId, status, amount, aiResult);

        // For demo purposes, we'll send to a placeholder email
        // In real implementation, you'd need entrepreneur email from database
        String toEmail = "entrepreneur" + entrepreneurId + "@example.com";

        sendEmail(toEmail, subject, body);
    }

    private String buildEmailBody(int entrepreneurId, int applicationId, String status, float amount, String aiResult) {
        StringBuilder body = new StringBuilder();
        body.append("Dear Entrepreneur,\n\n");
        body.append("Your funding application has been ").append(status.toLowerCase()).append(".\n\n");
        body.append("Application Details:\n");
        body.append("- Application ID: ").append(applicationId).append("\n");
        body.append("- Entrepreneur ID: ").append(entrepreneurId).append("\n");
        body.append("- Amount Requested: $").append(amount).append("\n");
        body.append("- Status: ").append(status).append("\n");

        if (aiResult != null && !aiResult.isEmpty()) {
            body.append("- AI Evaluation: ").append(aiResult).append("\n");
        }

        body.append("\nThank you for using our platform.\n");
        body.append("Best regards,\n");
        body.append("StartupFlow Team");

        return body.toString();
    }

    private void sendEmail(String to, String subject, String body) {
        Session session = Session.getInstance(emailProperties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);
            System.out.println("Email sent successfully to: " + to);

        } catch (MessagingException e) {
            System.err.println("Failed to send email: " + e.getMessage());
            // In production, you might want to log this or handle it differently
        }
    }
}