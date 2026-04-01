package com.example.realtime_data_firebase;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Service to send event notification emails
 */
public class EmailService {

    /**
     * Send event details via email to attendee
     * 
     * @param context          Application context
     * @param recipientEmail   Attendee's email address
     * @param eventTitle       Event title
     * @param eventDescription Event description
     * @param eventLocation    Event location
     * @param eventDate        Event date and time
     */
    public static void sendEventEmail(Context context, String recipientEmail,
            String eventTitle, String eventDescription,
            String eventLocation, String eventDate) {

        // Run email sending in background thread
        new Thread(() -> {
            try {
                // Configure SMTP properties
                Properties props = new Properties();
                props.put("mail.smtp.host", EmailConfig.SMTP_HOST);
                props.put("mail.smtp.port", EmailConfig.SMTP_PORT);
                props.put("mail.smtp.auth", String.valueOf(EmailConfig.USE_AUTH));
                props.put("mail.smtp.starttls.enable", String.valueOf(EmailConfig.USE_TLS));

                // Create session with authentication
                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                                EmailConfig.SENDER_EMAIL,
                                EmailConfig.SENDER_PASSWORD);
                    }
                });

                // Create email message
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(EmailConfig.SENDER_EMAIL));
                message.setRecipients(Message.RecipientType.TO,
                        InternetAddress.parse(recipientEmail));
                message.setSubject("Event Confirmation: " + eventTitle);

                // Format email body
                String emailBody = formatEmailBody(eventTitle, eventDescription,
                        eventLocation, eventDate);
                message.setContent(emailBody, "text/html; charset=utf-8");

                // Send email
                Transport.send(message);

                // Show success message on UI thread
                showToast(context, "Event details sent to your email!");

            } catch (MessagingException e) {
                e.printStackTrace();
                // Show error message on UI thread
                showToast(context, "Failed to send email: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Format email body with event details
     */
    private static String formatEmailBody(String title, String description,
            String location, String date) {
        return "<html><body style='font-family: Arial, sans-serif;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f5f5f5;'>" +
                "<div style='background-color: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);'>"
                +
                "<h1 style='color: #2196F3; margin-top: 0;'>🎉 Event Confirmation</h1>" +
                "<p style='font-size: 16px; color: #666;'>You've successfully registered for this event!</p>" +
                "<hr style='border: none; border-top: 2px solid #2196F3; margin: 20px 0;'>" +
                "<h2 style='color: #333;'>" + title + "</h2>" +
                "<div style='margin: 20px 0;'>" +
                "<p style='margin: 10px 0;'><strong>📅 Date & Time:</strong> " + date + "</p>" +
                "<p style='margin: 10px 0;'><strong>📍 Location:</strong> " + location + "</p>" +
                "</div>" +
                "<div style='background-color: #f9f9f9; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                "<h3 style='margin-top: 0; color: #333;'>Description:</h3>" +
                "<p style='color: #666; line-height: 1.6;'>" + description + "</p>" +
                "</div>" +
                "<p style='color: #999; font-size: 14px; margin-top: 30px;'>See you at the event!</p>" +
                "</div>" +
                "</div>" +
                "</body></html>";
    }

    /**
     * Show toast message on UI thread
     */
    private static void showToast(Context context, String message) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, message, Toast.LENGTH_LONG).show());
    }
}
