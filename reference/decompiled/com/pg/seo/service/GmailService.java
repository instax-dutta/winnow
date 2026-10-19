/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.api.client.googleapis.auth.oauth2.GoogleCredential
 *  com.google.api.client.googleapis.auth.oauth2.GoogleCredential$Builder
 *  com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
 *  com.google.api.client.http.HttpRequestInitializer
 *  com.google.api.client.http.HttpTransport
 *  com.google.api.client.json.JsonFactory
 *  com.google.api.client.json.gson.GsonFactory
 *  com.google.api.services.gmail.Gmail
 *  com.google.api.services.gmail.Gmail$Builder
 *  com.google.api.services.gmail.model.Draft
 *  com.google.api.services.gmail.model.ListDraftsResponse
 *  com.google.api.services.gmail.model.ListMessagesResponse
 *  com.google.api.services.gmail.model.Message
 *  com.google.api.services.gmail.model.MessagePartHeader
 *  jakarta.mail.Address
 *  jakarta.mail.BodyPart
 *  jakarta.mail.Message$RecipientType
 *  jakarta.mail.Multipart
 *  jakarta.mail.Session
 *  jakarta.mail.internet.InternetAddress
 *  jakarta.mail.internet.MimeMessage
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.stereotype.Service
 */
package com.pg.seo.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Draft;
import com.google.api.services.gmail.model.ListDraftsResponse;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.pg.seo.entity.GmailMaster;
import com.pg.seo.repository.GmailMasterRepository;
import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GmailService {
    @Autowired
    GmailMasterRepository gmailMasterRepository;
    private static final int PERSONAL_GMAIL_LIMIT = 500;
    private static final int WORKSPACE_GMAIL_LIMIT = 1500;
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    public Gmail createGmailService(String email) throws Exception {
        GmailMaster gmailMaster = this.gmailMasterRepository.findByEmail(email);
        GoogleCredential credential = new GoogleCredential.Builder().setClientSecrets(gmailMaster.getClientId(), gmailMaster.getSecret()).setTransport((HttpTransport)GoogleNetHttpTransport.newTrustedTransport()).setJsonFactory(JSON_FACTORY).build().setRefreshToken(gmailMaster.getRefreshToken());
        credential.refreshToken();
        return new Gmail.Builder((HttpTransport)GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, (HttpRequestInitializer)credential).setApplicationName("MAIL ELEXYT").build();
    }

    public Map<String, String> getAllDraftSubjectsWithIds(String email) throws Exception {
        Gmail gmail = this.createGmailService(email);
        HashMap<String, String> draftMap = new HashMap<String, String>();
        List drafts = ((ListDraftsResponse)gmail.users().drafts().list("me").execute()).getDrafts();
        if (drafts != null) {
            block0: for (Draft draft : drafts) {
                Message message = ((Draft)gmail.users().drafts().get("me", draft.getId()).execute()).getMessage();
                for (MessagePartHeader header : message.getPayload().getHeaders()) {
                    if (!"Subject".equalsIgnoreCase(header.getName())) continue;
                    draftMap.put(draft.getId(), header.getValue());
                    continue block0;
                }
            }
        }
        return draftMap;
    }

    public void sendDraftEmailById(String fromEmail, String toEmail, String draftId, String domainValue) throws Exception {
        Gmail gmail = this.createGmailService(fromEmail);
        Draft draft = (Draft)gmail.users().drafts().get("me", draftId).setFormat("raw").execute();
        String raw = draft.getMessage().getRaw();
        if (raw == null) {
            throw new Exception("Draft raw content is missing.");
        }
        byte[] decodedBytes = Base64.getUrlDecoder().decode(raw);
        Session session = Session.getDefaultInstance((Properties)new Properties(), null);
        MimeMessage mimeMessage = new MimeMessage(session, (InputStream)new ByteArrayInputStream(decodedBytes));
        String originalSubject = mimeMessage.getSubject();
        if (originalSubject != null) {
            String newSubject = originalSubject.replace("{{Domain}}", domainValue);
            mimeMessage.setSubject(newSubject, "UTF-8");
        }
        mimeMessage.setRecipients(Message.RecipientType.TO, (Address[])InternetAddress.parse((String)toEmail, (boolean)false));
        Object content = mimeMessage.getContent();
        if (content instanceof Multipart) {
            Multipart multipart = (Multipart)content;
            for (int i = 0; i < multipart.getCount(); ++i) {
                BodyPart part = multipart.getBodyPart(i);
                if (!part.getContentType().toLowerCase().contains("text/html")) continue;
                String html = (String)part.getContent();
                html = html.replace("{{Domain}}", domainValue);
                part.setContent((Object)html, "text/html; charset=UTF-8");
            }
            mimeMessage.setContent(multipart);
            mimeMessage.saveChanges();
        } else if (content instanceof String) {
            String body = (String)content;
            body = body.replace("{{Domain}}", domainValue);
            mimeMessage.setContent((Object)body, mimeMessage.getContentType());
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        mimeMessage.writeTo((OutputStream)buffer);
        String encodedEmail = Base64.getUrlEncoder().encodeToString(buffer.toByteArray());
        Message newMsg = new Message().setRaw(encodedEmail);
        gmail.users().messages().send("me", newMsg).execute();
        System.out.println("Email sent to " + toEmail);
    }

    public Map<String, Object> getEmailSendStats(String fromEmail) throws Exception {
        ListMessagesResponse response;
        Gmail gmail = this.createGmailService(fromEmail);
        long epoch24HoursAgo = Instant.now().minus(24L, ChronoUnit.HOURS).getEpochSecond();
        String query = "in:sent after:" + epoch24HoursAgo;
        int totalMessages = 0;
        String pageToken = null;
        do {
            List messages;
            if ((messages = (response = (ListMessagesResponse)gmail.users().messages().list("me").setQ(query).setMaxResults(Long.valueOf(500L)).setPageToken(pageToken).execute()).getMessages()) == null) continue;
            totalMessages += messages.size();
        } while ((pageToken = response.getNextPageToken()) != null);
        int dailyLimit = this.determineQuotaLimit(fromEmail);
        int remainingQuota = Math.max(0, dailyLimit - totalMessages);
        HashMap<String, Object> stats = new HashMap<String, Object>();
        stats.put("fromEmail", fromEmail);
        stats.put("emailsSentInLast24Hours", totalMessages);
        stats.put("limitEstimate", dailyLimit);
        stats.put("remainingQuotaEstimate", remainingQuota);
        return stats;
    }

    private int determineQuotaLimit(String email) {
        if (email.toLowerCase().endsWith("@gmail.com")) {
            return 500;
        }
        return 1500;
    }

    public List<GmailMaster> getAllGmail() {
        return this.gmailMasterRepository.findAll();
    }
}

