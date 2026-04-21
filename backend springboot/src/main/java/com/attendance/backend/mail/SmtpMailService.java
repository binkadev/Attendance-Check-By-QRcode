package com.attendance.backend.mail;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
@Profile("!dev")
public class SmtpMailService implements MailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpMailService.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);

    private final JavaMailSender mailSender;
    private final AppMailProperties mailProperties;

    public SmtpMailService(JavaMailSender mailSender, AppMailProperties mailProperties) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
    }

    @Override
    public void sendPasswordResetEmail(String toEmail,
                                       String fullName,
                                       String resetUrl,
                                       Instant expiresAt) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    true, // fixed
                    StandardCharsets.UTF_8.name()
            );

            helper.setTo(toEmail);
            helper.setSubject("Reset your password");
            applyFrom(helper);

            helper.setText(
                    buildPasswordResetTextBody(fullName, resetUrl, expiresAt),
                    buildPasswordResetHtmlBody(fullName, resetUrl, expiresAt)
            );

            mailSender.send(mimeMessage);
            log.info("Password reset email sent successfully");
        } catch (Exception ex) {
            log.error("Failed to send password reset email", ex);
            throw new IllegalStateException("Failed to send password reset email", ex);
        }
    }

    @Override
    public void sendNotificationEmail(String toEmail,
                                      String subject,
                                      String title,
                                      String body) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    true, // fixed
                    StandardCharsets.UTF_8.name()
            );

            helper.setTo(toEmail);
            helper.setSubject(subject);
            applyFrom(helper);

            helper.setText(
                    buildNotificationTextBody(title, body),
                    buildNotificationHtmlBody(title, body)
            );

            mailSender.send(mimeMessage);
            log.info("Notification email sent successfully");
        } catch (Exception ex) {
            log.error("Failed to send notification email", ex);
            throw new IllegalStateException("Failed to send notification email", ex);
        }
    }

    private void applyFrom(MimeMessageHelper helper) throws Exception {
        if (StringUtils.hasText(mailProperties.getFromAddress())) {
            String fromName = StringUtils.hasText(mailProperties.getFromName())
                    ? mailProperties.getFromName()
                    : "Attendance System";
            helper.setFrom(new InternetAddress(mailProperties.getFromAddress(), fromName));
        }
    }

    private String buildPasswordResetTextBody(String fullName, String resetUrl, Instant expiresAt) {
        String displayName = StringUtils.hasText(fullName) ? fullName : "User";
        return """
                Hello %s,

                We received a request to reset your password.

                Use the link below to set a new password:
                %s

                This link expires at: %s

                If you did not request this, you can ignore this email.

                Regards,
                Attendance System
                """.formatted(displayName, resetUrl, DATE_TIME_FORMATTER.format(expiresAt));
    }

    private String buildPasswordResetHtmlBody(String fullName, String resetUrl, Instant expiresAt) {
        String displayName = HtmlUtils.htmlEscape(StringUtils.hasText(fullName) ? fullName : "User");
        String escapedUrl = HtmlUtils.htmlEscape(resetUrl);
        String expiresText = HtmlUtils.htmlEscape(DATE_TIME_FORMATTER.format(expiresAt));

        return """
                <html>
                  <body style="font-family: Arial, sans-serif; color: #1f2937;">
                    <p>Hello <strong>%s</strong>,</p>
                    <p>We received a request to reset your password.</p>
                    <p>
                      <a href="%s"
                         style="display:inline-block;padding:12px 18px;background:#2563eb;color:#ffffff;text-decoration:none;border-radius:6px;">
                        Reset password
                      </a>
                    </p>
                    <p>If the button does not work, use this link:</p>
                    <p><a href="%s">%s</a></p>
                    <p>This link expires at: <strong>%s</strong></p>
                    <p>If you did not request this, you can safely ignore this email.</p>
                    <p>Regards,<br/>Attendance System</p>
                  </body>
                </html>
                """.formatted(displayName, escapedUrl, escapedUrl, escapedUrl, expiresText);
    }

    private String buildNotificationTextBody(String title, String body) {
        String safeTitle = StringUtils.hasText(title) ? title : "Notification";
        String safeBody = StringUtils.hasText(body) ? body : "";

        return """
                %s

                %s

                Regards,
                Attendance System
                """.formatted(safeTitle, safeBody);
    }

    private String buildNotificationHtmlBody(String title, String body) {
        String safeTitle = HtmlUtils.htmlEscape(StringUtils.hasText(title) ? title : "Notification");
        String safeBody = toHtmlMultiline(body);

        return """
                <html>
                  <body style="font-family: Arial, sans-serif; color: #1f2937;">
                    <h2 style="margin-bottom: 16px;">%s</h2>
                    <div style="line-height: 1.6;">%s</div>
                    <p style="margin-top: 24px;">Regards,<br/>Attendance System</p>
                  </body>
                </html>
                """.formatted(safeTitle, safeBody);
    }

    private String toHtmlMultiline(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        return HtmlUtils.htmlEscape(raw).replace("\n", "<br/>");
    }
}