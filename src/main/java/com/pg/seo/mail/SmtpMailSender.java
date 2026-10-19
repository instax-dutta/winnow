package com.pg.seo.mail;

import com.pg.seo.config.MailProperties;
import com.pg.seo.config.SecretStore;
import com.pg.seo.domain.AccountProtocol;
import com.pg.seo.domain.AuthMethod;
import com.pg.seo.domain.SmtpAccount;
import com.pg.seo.repository.SmtpAccountRepository;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

/**
 * Sends through an SMTP account.
 *
 * <p>The password is resolved from the secret store at call time and is never cached on
 * this object, never written to a log and never returned. One sender is built per send,
 * because the account carries the host, the port and the credential.
 */
@Service
public class SmtpMailSender implements MailSender {

    private static final Logger LOG = LoggerFactory.getLogger(SmtpMailSender.class);

    private final SmtpAccountRepository accounts;
    private final SecretStore secrets;
    private final MailProperties mail;

    public SmtpMailSender(SmtpAccountRepository accounts, SecretStore secrets, MailProperties mail) {
        this.accounts = accounts;
        this.secrets = secrets;
        this.mail = mail;
    }

    @Override
    public String providerName() {
        return "smtp";
    }

    @Override
    public SendResult send(SendRequest request) {
        SmtpAccount account = resolveAccount(request);
        String password = resolveSecret(account.getSecretRef());
        try {
            JavaMailSenderImpl sender = buildSender(account, password);
            MimeMessage message = MimeMessageBuilder.build(sender.createMimeMessage(), request);
            sender.send(message);
            String messageId = message.getMessageID();
            LOG.info("SMTP message sent for account {} (message id {})", account.getId(), messageId);
            return SendResult.success(messageId);
        } catch (Exception e) {
            // The message of a mail exception can carry the response of the server, which can
            // carry the account name. It is never copied into the exception message.
            LOG.warn("SMTP send failed for account {} ({})", account.getId(), e.getClass().getSimpleName());
            throw new MailException("SMTP send failed (" + e.getClass().getSimpleName() + ")", e);
        }
    }

    private SmtpAccount resolveAccount(SendRequest request) {
        if (request.ownerUserId() == null) {
            throw new MailException("The send request does not identify the tenant that owns the sending account");
        }
        if (request.accountId() != null) {
            return accounts
                    .findByIdAndOwnerUserIdAndDeletedAtIsNull(request.accountId(), request.ownerUserId())
                    .orElseThrow(() -> new MailException("SMTP account " + request.accountId() + " was not found"));
        }
        if (request.fromEmail() == null || request.fromEmail().isBlank()) {
            throw new MailException("The send request does not identify the sending account");
        }
        return accounts
                .findByOwnerUserIdAndEmailAndDeletedAtIsNull(request.ownerUserId(), request.fromEmail())
                .orElseThrow(() -> new MailException("SMTP account for " + request.fromEmail() + " was not found"));
    }

    private String resolveSecret(String secretRef) {
        if (secretRef == null || secretRef.isBlank()) {
            return null;
        }
        String secret = secrets.resolve(secretRef);
        if (secret == null) {
            throw new MailException("The credential of the sending account is not configured");
        }
        return secret;
    }

    private JavaMailSenderImpl buildSender(SmtpAccount account, String password) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(account.getHost());
        sender.setPort(account.getPort());
        sender.setUsername(account.getUsername());
        if (password != null) {
            sender.setPassword(password);
        }
        sender.setProtocol(account.getProtocol().name());
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());

        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.connectiontimeout", Long.toString(mail.connectTimeout().toMillis()));
        props.put("mail.smtp.timeout", Long.toString(mail.readTimeout().toMillis()));
        props.put("mail.smtp.writetimeout", Long.toString(mail.writeTimeout().toMillis()));
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.auth", Boolean.toString(account.getAuthMethod() != AuthMethod.NONE));
        if (account.getAuthMethod() != AuthMethod.NONE) {
            props.put("mail.smtp.auth.mechanisms", account.getAuthMethod().name());
        }
        if (account.getProtocol() == AccountProtocol.SMTPS) {
            props.put("mail.smtps.ssl.enable", "true");
            props.put("mail.smtps.connectiontimeout", Long.toString(mail.connectTimeout().toMillis()));
            props.put("mail.smtps.timeout", Long.toString(mail.readTimeout().toMillis()));
            props.put("mail.smtps.writetimeout", Long.toString(mail.writeTimeout().toMillis()));
        }
        return sender;
    }
}
