package com.attendance.backend.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class EmailOutboxService {

    private final EmailOutboxRepository repository;
    private final EmailOutboxCryptoService cryptoService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public EmailOutboxService(EmailOutboxRepository repository,
                              EmailOutboxCryptoService cryptoService,
                              ObjectMapper objectMapper,
                              Clock clock) {
        this.repository = repository;
        this.cryptoService = cryptoService;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public void enqueuePasswordResetEmail(UUID passwordResetTokenId,
                                          String toEmail,
                                          String fullName,
                                          String resetUrl,
                                          Instant expiresAt) {
        try {
            PasswordResetMailPayload payload = new PasswordResetMailPayload(fullName, resetUrl, expiresAt);
            enqueue(
                    EmailOutbox.AGGREGATE_TYPE_PASSWORD_RESET,
                    passwordResetTokenId,
                    toEmail,
                    "Reset your password",
                    payload
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to enqueue password reset email", ex);
        }
    }

    @Transactional
    public UUID enqueueNotificationEmail(UUID notificationDeliveryId,
                                         String toEmail,
                                         String subject,
                                         String title,
                                         String body) {
        try {
            NotificationMailPayload payload = new NotificationMailPayload(title, body);
            return enqueue(
                    EmailOutbox.AGGREGATE_TYPE_NOTIFICATION_DELIVERY,
                    notificationDeliveryId,
                    toEmail,
                    subject,
                    payload
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to enqueue notification email", ex);
        }
    }

    public PasswordResetMailPayload readPasswordResetPayload(EmailOutbox row) {
        try {
            byte[] plain = cryptoService.decrypt(row.payloadNonce, row.payloadCiphertext);
            return objectMapper.readValue(plain, PasswordResetMailPayload.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to read password reset payload", ex);
        }
    }

    public NotificationMailPayload readNotificationPayload(EmailOutbox row) {
        try {
            byte[] plain = cryptoService.decrypt(row.payloadNonce, row.payloadCiphertext);
            return objectMapper.readValue(plain, NotificationMailPayload.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to read notification payload", ex);
        }
    }

    private UUID enqueue(String aggregateType,
                         UUID aggregateId,
                         String toEmail,
                         String subject,
                         Object payload) throws Exception {
        byte[] plain = objectMapper.writeValueAsBytes(payload);
        EmailOutboxCryptoService.EncryptedPayload encrypted = cryptoService.encrypt(plain);

        EmailOutbox row = new EmailOutbox();
        row.id = UUID.randomUUID();
        row.aggregateType = aggregateType;
        row.aggregateId = aggregateId;
        row.toEmail = toEmail;
        row.subject = subject;
        row.payloadNonce = encrypted.nonce();
        row.payloadCiphertext = encrypted.ciphertext();
        row.status = EmailOutbox.STATUS_PENDING;
        row.retryCount = 0;
        row.nextAttemptAt = Instant.now(clock);

        repository.save(row);
        return row.id;
    }

    public record PasswordResetMailPayload(
            String fullName,
            String resetUrl,
            Instant expiresAt
    ) {
    }

    public record NotificationMailPayload(
            String title,
            String body
    ) {
    }
}