package com.attendance.backend.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Profile("dev")
public class LoggingMailService implements MailService {

    private static final Logger log = LoggerFactory.getLogger(LoggingMailService.class);

    @Override
    public void sendPasswordResetEmail(String toEmail,
                                       String fullName,
                                       String resetUrl,
                                       Instant expiresAt) {
        log.info("""
                
                ==================== DEV PASSWORD RESET MAIL ====================
                toEmail   : {}
                fullName  : {}
                expiresAt : {}
                resetUrl  : {}
                =================================================================
                """,
                toEmail,
                fullName,
                expiresAt,
                resetUrl
        );
    }

    @Override
    public void sendNotificationEmail(String toEmail,
                                      String subject,
                                      String title,
                                      String body) {
        log.info("""
                
                ==================== DEV NOTIFICATION MAIL ======================
                toEmail   : {}
                subject   : {}
                title     : {}
                body      : {}
                =================================================================
                """,
                toEmail,
                subject,
                title,
                body
        );
    }
}