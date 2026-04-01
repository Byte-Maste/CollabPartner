package com.example.realtime_data_firebase;

/**
 * Email configuration for sending event notifications
 * 
 * MANUAL SETUP REQUIRED:
 * 1. Replace YOUR_GMAIL_HERE with your Gmail address
 * 2. Replace YOUR_APP_PASSWORD_HERE with your Gmail app password (16
 * characters, no spaces)
 * 
 * See gmail_setup_guide.md for detailed instructions on getting an app password
 */
public class EmailConfig {

    // ⚠️ CRITICAL: Use your "App Password", NOT your regular Gmail password!
    // Your regular password will NOT work. Follow gmail_setup_guide.md to get one.
    public static final String SENDER_EMAIL = "krishnachoudhary4616@gmail.com";
    public static final String SENDER_PASSWORD = "Kkrishna@2005"; // ⚠️ REPLACE WITH APP PASSWORD!

    // SMTP Configuration (Don't change these)
    public static final String SMTP_HOST = "smtp.gmail.com";
    public static final String SMTP_PORT = "587";
    public static final boolean USE_TLS = true;
    public static final boolean USE_AUTH = true;
}
