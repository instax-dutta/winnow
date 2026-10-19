package com.pg.seo.mail;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.pg.seo.config.MailProperties;
import com.pg.seo.config.SecretStore;
import com.pg.seo.domain.GmailAccount;
import com.pg.seo.repository.GmailAccountRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Sends through a Google OAuth account.
 *
 * <p>The client secret and the refresh token are resolved from the secret store at call
 * time. The access token that the refresh produces lives only in this call: it is never
 * written to the database, never logged and never returned.
 */
@Service
public class GmailMailSender implements MailSender {

    private static final Logger LOG = LoggerFactory.getLogger(GmailMailSender.class);
    private static final String APPLICATION_NAME = "seo";

    private final GmailAccountRepository accounts;
    private final SecretStore secrets;
    private final MailProperties mail;

    public GmailMailSender(GmailAccountRepository accounts, SecretStore secrets, MailProperties mail) {
        this.accounts = accounts;
        this.secrets = secrets;
        this.mail = mail;
    }

    @Override
    public String providerName() {
        return "gmail";
    }

    @Override
    public SendResult send(SendRequest request) {
        GmailAccount account = resolveAccount(request);
        String clientSecret = resolveSecret(account.getSecretRef());
        String refreshToken = resolveSecret(account.getRefreshTokenRef());
        try {
            HttpTransport transport = new NetHttpTransport();
            JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
            GoogleCredential credential = new GoogleCredential.Builder()
                    .setTransport(transport)
                    .setJsonFactory(jsonFactory)
                    .setClientSecrets(account.getOauthClientId(), clientSecret)
                    .build()
                    .setRefreshToken(refreshToken);
            credential.refreshToken();
            if (credential.getAccessToken() == null) {
                throw new MailException("The Gmail account did not return an access token");
            }

            int connectTimeout = (int) mail.connectTimeout().toMillis();
            int readTimeout = (int) mail.readTimeout().toMillis();
            HttpRequestInitializer initializer = httpRequest -> {
                httpRequest.setConnectTimeout(connectTimeout);
                httpRequest.setReadTimeout(readTimeout);
                credential.initialize(httpRequest);
            };
            Gmail gmail = new Gmail.Builder(transport, jsonFactory, initializer)
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            MimeMessage mime = MimeMessageBuilder.build(
                    new MimeMessage(Session.getInstance(MimeMessageBuilder.sessionProperties())), request);
            mime.saveChanges();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            mime.writeTo(buffer);

            Message outgoing = new Message()
                    .setRaw(Base64.getUrlEncoder().encodeToString(buffer.toByteArray()));
            Message sent = gmail.users().messages().send("me", outgoing).execute();
            String messageId = sent == null ? null : sent.getId();
            LOG.info("Gmail message sent for account {} (message id {})", account.getId(), messageId);
            return SendResult.success(messageId);
        } catch (MailException e) {
            throw e;
        } catch (Exception e) {
            LOG.warn("Gmail send failed for account {} ({})", account.getId(), e.getClass().getSimpleName());
            throw new MailException("Gmail send failed (" + e.getClass().getSimpleName() + ")", e);
        }
    }

    private GmailAccount resolveAccount(SendRequest request) {
        if (request.ownerUserId() == null) {
            throw new MailException("The send request does not identify the tenant that owns the sending account");
        }
        if (request.accountId() != null) {
            return accounts
                    .findByIdAndOwnerUserIdAndDeletedAtIsNull(request.accountId(), request.ownerUserId())
                    .orElseThrow(() -> new MailException("Gmail account " + request.accountId() + " was not found"));
        }
        if (request.fromEmail() == null || request.fromEmail().isBlank()) {
            throw new MailException("The send request does not identify the sending account");
        }
        return accounts
                .findByOwnerUserIdAndEmailAndDeletedAtIsNull(request.ownerUserId(), request.fromEmail())
                .orElseThrow(() -> new MailException("Gmail account for " + request.fromEmail() + " was not found"));
    }

    private String resolveSecret(String secretRef) {
        if (secretRef == null || secretRef.isBlank()) {
            throw new MailException("The credential of the sending account is not configured");
        }
        String secret = secrets.resolve(secretRef);
        if (secret == null) {
            throw new MailException("The credential of the sending account is not configured");
        }
        return secret;
    }
}
