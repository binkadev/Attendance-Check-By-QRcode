package com.attendance.backend.mail;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class EmailOutboxProcessorService {

    private final EmailOutboxRepository repository;
    private final EmailOutboxService emailOutboxService;
    private final MailService mailService;
    private final EmailOutboxProperties properties;
    private final Clock clock;

    public EmailOutboxProcessorService(EmailOutboxRepository repository,
                                       EmailOutboxService emailOutboxService,
                                       MailService mailService,
                                       EmailOutboxProperties properties,
                                       Clock clock) {
        this.repository = repository;
        this.emailOutboxService = emailOutboxService;
        this.mailService = mailService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public List<UUID> claimDueIds() {
        Instant now = Instant.now(clock);
        List<EmailOutbox> rows = repository.findDueForClaim(now, properties.getBatchSize());

        for (EmailOutbox row : rows) {
            row.markProcessing(now);
        }

        repository.saveAll(rows);
        return rows.stream().map(r -> r.id).toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processOne(UUID id) {
        Instant now = Instant.now(clock);

        EmailOutbox row = repository.findByIdForUpdate(id).orElse(null);
        if (row == null || !EmailOutbox.STATUS_PROCESSING.equals(row.status)) {
            return;
        }

        try {
            switch (row.aggregateType) {
                case EmailOutbox.AGGREGATE_TYPE_PASSWORD_RESET -> processPasswordReset(row, now);
                case EmailOutbox.AGGREGATE_TYPE_NOTIFICATION_DELIVERY -> processNotification(row, now);
                default -> row.markDead(
                        now,
                        "UNSUPPORTED_AGGREGATE_TYPE",
                        trimError("Unsupported aggregate type: " + row.aggregateType)
                );
            }

            repository.save(row);
        } catch (Exception ex) {
            String errorMessage = trimError(ex.getMessage());

            if (row.retryCount + 1 >= properties.getMaxAttempts()) {
                row.markDead(now, "SEND_FAILED", errorMessage);
            } else {
                long delay = computeBackoffSeconds(row.retryCount);
                row.markRetry(now, delay, "SEND_FAILED", errorMessage);
            }

            repository.save(row);
        }
    }

    private void processPasswordReset(EmailOutbox row, Instant now) {
        EmailOutboxService.PasswordResetMailPayload payload = emailOutboxService.readPasswordResetPayload(row);

        mailService.sendPasswordResetEmail(
                row.toEmail,
                payload.fullName(),
                payload.resetUrl(),
                payload.expiresAt()
        );

        row.markSent(now);
    }

    private void processNotification(EmailOutbox row, Instant now) {
        EmailOutboxService.NotificationMailPayload payload = emailOutboxService.readNotificationPayload(row);

        mailService.sendNotificationEmail(
                row.toEmail,
                row.subject,
                payload.title(),
                payload.body()
        );

        row.markSent(now);
    }

    private long computeBackoffSeconds(int currentRetryCount) {
        long multiplier = 1L << Math.min(currentRetryCount, 5);
        return properties.getBaseBackoffSeconds() * multiplier;
    }

    private String trimError(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Unknown mail delivery error";
        }
        return raw.length() <= 500 ? raw : raw.substring(0, 500);
    }
}