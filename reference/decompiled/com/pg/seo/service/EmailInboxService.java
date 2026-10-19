/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.mail.BodyPart
 *  jakarta.mail.Folder
 *  jakarta.mail.Message
 *  jakarta.mail.MessagingException
 *  jakarta.mail.NoSuchProviderException
 *  jakarta.mail.Session
 *  jakarta.mail.Store
 *  jakarta.mail.internet.MimeMultipart
 *  jakarta.mail.search.AndTerm
 *  jakarta.mail.search.ReceivedDateTerm
 *  jakarta.mail.search.SearchTerm
 *  org.jsoup.Jsoup
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.scheduling.annotation.Scheduled
 *  org.springframework.stereotype.Service
 */
package com.pg.seo.service;

import com.pg.seo.entity.EmailInbox;
import com.pg.seo.entity.EmailMaster;
import com.pg.seo.model.EmailDto;
import com.pg.seo.repository.EmailInboxRepository;
import com.pg.seo.repository.EmailMasterRepository;
import jakarta.mail.BodyPart;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.NoSuchProviderException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.AndTerm;
import jakarta.mail.search.ReceivedDateTerm;
import jakarta.mail.search.SearchTerm;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class EmailInboxService {
    private SimpleDateFormat sdfDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private SimpleDateFormat sdfDate = new SimpleDateFormat("dd-MM-yyyy");
    private SimpleDateFormat sdfSqlDate = new SimpleDateFormat("yyyy-MM-dd");
    private SimpleDateFormat sdfRegularDateTime = new SimpleDateFormat("dd-MM-yyyy HH:mm");
    @Autowired
    EmailMasterRepository emailMasterRepository;
    @Autowired
    EmailInboxRepository emailInboxRepository;

    @Scheduled(cron="0 0 11 * * *")
    public void fetchEmailInbox() {
        ArrayList<EmailInbox> emailInboxList = new ArrayList<EmailInbox>();
        List emailMasterlist = this.emailMasterRepository.findAll();
        try {
            for (EmailMaster emailMaster : emailMasterlist) {
                Properties properties = new Properties();
                properties.put("mail.imap.host", "imap.gmail.com");
                properties.put("mail.imap.port", "993");
                properties.put("mail.imap.starttls.enable", "true");
                properties.put("mail.imap.ssl.trust", "imap.gmail.com");
                Session emailSession = Session.getDefaultInstance((Properties)properties);
                Store store = emailSession.getStore("imaps");
                store.connect("imap.gmail.com", emailMaster.getEmail(), emailMaster.getPassword());
                Folder inbox = store.getFolder("Inbox");
                inbox.open(2);
                ReceivedDateTerm olderThan = new ReceivedDateTerm(2, new java.util.Date());
                ReceivedDateTerm newerThan = new ReceivedDateTerm(5, new java.util.Date());
                AndTerm andTerm = new AndTerm((SearchTerm)olderThan, (SearchTerm)newerThan);
                Message[] messages = inbox.search((SearchTerm)andTerm);
                System.out.println("messages.length---" + messages.length);
                for (Message message : messages) {
                    EmailInbox emailInbox = new EmailInbox();
                    emailInbox.setFrom(message.getFrom()[0].toString());
                    emailInbox.setSubject(message.getSubject());
                    emailInbox.setEmailId(emailMaster.getEmail());
                    emailInbox.setContent(this.getTextFromMessage(message));
                    emailInbox.setDate(new Date(message.getReceivedDate().getTime()));
                    emailInboxList.add(emailInbox);
                }
                inbox.close(false);
                store.close();
                this.emailInboxRepository.deleteByEmailIdAndDate(emailMaster.getEmail(), new Date(new java.util.Date().getTime()));
                this.emailInboxRepository.saveAll(emailInboxList);
            }
        }
        catch (NoSuchProviderException e) {
            e.printStackTrace();
        }
        catch (MessagingException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<EmailDto> getEmailInbox(Long emailMasterId, String fromDate, String toDate) {
        EmailMaster emailMaster = this.emailMasterRepository.findByEmailMasterId(emailMasterId);
        ArrayList<EmailDto> emailDtoList = new ArrayList();
        List<EmailInbox> emailInboxList = this.emailInboxRepository.findByEmailId(emailMaster.getEmail());
        emailDtoList = emailInboxList.stream().map(a -> new EmailDto(a.getEmailInboxId(), a.getFrom(), a.getSubject(), a.getContent())).collect(Collectors.toList());
        return emailDtoList;
    }

    private String getTextFromMessage(Message message) throws MessagingException, IOException {
        String result = "";
        if (message.isMimeType("text/plain")) {
            result = message.getContent().toString();
        } else if (message.isMimeType("text/html")) {
            result = message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart)message.getContent();
            result = this.getTextFromMimeMultipart(mimeMultipart);
        }
        return result;
    }

    private String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws MessagingException, IOException {
        Object result = "";
        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; ++i) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result = (String)result + "\n" + bodyPart.getContent();
                break;
            }
            if (bodyPart.isMimeType("text/html")) {
                String html = (String)bodyPart.getContent();
                result = (String)result + "\n" + Jsoup.parse((String)html).text();
                continue;
            }
            if (!(bodyPart.getContent() instanceof MimeMultipart)) continue;
            result = (String)result + this.getTextFromMimeMultipart((MimeMultipart)bodyPart.getContent());
        }
        return result;
    }
}

