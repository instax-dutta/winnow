package com.pg.seo.scheduler;

import com.pg.seo.config.MailProperties;
import com.pg.seo.config.SecretStore;
import com.pg.seo.domain.InboxMessage;
import com.pg.seo.domain.SmtpAccount;
import com.pg.seo.service.InboxService;
import com.pg.seo.repository.SmtpAccountRepository;
import jakarta.mail.BodyPart;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.search.FlagTerm;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls the mailboxes of the configured sending accounts and captures the new messages.
 *
 * <p>The IMAP connection has an explicit connect, read and write timeout. The original left
 * the socket timeout at the infinite default, so one stalled server blocked a thread for
 * ever and every other job behind it waited.
 *
 * <p>The poll is idempotent. A message is identified by the id the provider gave it, and
 * the table has a unique key on the account and that id, so a repeated poll stores it once.
 *
 * <p>Note for the service layer: this job writes the captured message itself, because the
 * service has no store method yet. When {@code InboxService.store} exists this class should
 * call it instead of the repository.
 */
@Component
public class InboxPollJob {

    private static final Logger LOG = LoggerFactory.getLogger(InboxPollJob.class);

    /** How many accounts are read in one query. */
    private static final int ACCOUNT_PAGE_SIZE = 100;

    /** The most pages of accounts one poll reads. */
    private static final int MAX_ACCOUNT_PAGES = 50;

    /** How many messages are written in one batch. */
    private static final int WRITE_BATCH_SIZE = 200;

    private final SmtpAccountRepository smtpAccounts;
    private final InboxService inboxes;
    private final SecretStore secrets;
    private final MailProperties mail;

    public InboxPollJob(
            SmtpAccountRepository smtpAccounts,
            InboxService inboxes,
            SecretStore secrets,
            MailProperties mail) {
        this.smtpAccounts = smtpAccounts;
        this.inboxes = inboxes;
        this.secrets = secrets;
        this.mail = mail;
    }

    /**
     * Polls every enabled mailbox once.
     */
    @Scheduled(fixedDelayString = "${seo.scheduler.inbox-delay-ms:300000}")
    public void poll() {
        for (int page = 0; page < MAX_ACCOUNT_PAGES; page++) {
            List<SmtpAccount> accounts = smtpAccounts.findAll(PageRequest.of(page, ACCOUNT_PAGE_SIZE)).getContent();
            if (accounts.isEmpty()) {
                return;
            }
            for (SmtpAccount account : accounts) {
                if (account.getDeletedAt() != null || !account.isEnabled()) {
                    continue;
                }
                try {
                    pollAccount(account);
                } catch (RuntimeException e) {
                    LOG.warn("The mailbox of account {} could not be polled ({})",
                            account.getId(), e.getClass().getSimpleName());
                }
            }
            if (accounts.size() < ACCOUNT_PAGE_SIZE) {
                return;
            }
        }
        LOG.warn("The inbox poll stopped after {} pages of accounts", MAX_ACCOUNT_PAGES);
    }

    private void pollAccount(SmtpAccount account) {
        String credential = account.getSecretRef() == null ? null : secrets.resolve(account.getSecretRef());
        if (credential == null) {
            LOG.warn("The account {} has no credential, its mailbox is not polled", account.getId());
            return;
        }
        Properties props = new Properties();
        props.put("mail.store.protocol", "imap");
        props.put("mail.imap.host", account.getHost());
        props.put("mail.imap.port", Integer.toString(account.getPort()));
        props.put("mail.imap.connectiontimeout", Long.toString(mail.connectTimeout().toMillis()));
        props.put("mail.imap.timeout", Long.toString(mail.readTimeout().toMillis()));
        props.put("mail.imap.writetimeout", Long.toString(mail.writeTimeout().toMillis()));

        Session session = Session.getInstance(props);
        Store store;
        try {
            store = session.getStore("imap");
            store.connect(account.getHost(), account.getPort(), account.getUsername(), credential);
        } catch (jakarta.mail.MessagingException e) {
            LOG.warn("The mailbox of account {} could not be opened ({})",
                    account.getId(), e.getClass().getSimpleName());
            return;
        }
        try {
            Folder folder = store.getFolder("INBOX");
            folder.open(Folder.READ_WRITE);
            try {
                Message[] messages = folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
                capture(account, messages);
            } finally {
                folder.close(false);
            }
        } catch (jakarta.mail.MessagingException e) {
            LOG.warn("The mailbox of account {} could not be read ({})",
                    account.getId(), e.getClass().getSimpleName());
        } finally {
            try {
                store.close();
            } catch (jakarta.mail.MessagingException e) {
                LOG.debug("The store of account {} did not close cleanly", account.getId());
            }
        }
    }

    /**
     * Stores the messages that are not stored yet and marks them as seen, so the next poll
     * does not read them again.
     */
    private void capture(SmtpAccount account, Message[] messages) throws jakarta.mail.MessagingException {
        List<InboxMessage> batch = new ArrayList<>(WRITE_BATCH_SIZE);
        for (Message message : messages) {
            String providerMessageId = providerMessageId(message);
            if (providerMessageId == null) {
                continue;
            }
            batch.add(toRow(account, message, providerMessageId));
            if (batch.size() >= WRITE_BATCH_SIZE) {
                save(account, batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            save(account, batch);
        }
    }

    /**
     * Writes the batch through the service layer, so the scheduler never touches the
     * repository directly and the tenant is checked on the way in.
     */
    private void save(SmtpAccount account, List<InboxMessage> batch) {
        try {
            inboxes.store(account.getOwnerUserId(), account.getId(), batch);
        } catch (RuntimeException e) {
            LOG.warn("The messages of account {} could not be stored ({})",
                    account.getId(), e.getClass().getSimpleName());
        }
    }

    private static InboxMessage toRow(SmtpAccount account, Message message, String providerMessageId)
            throws jakarta.mail.MessagingException {
        InboxMessage row = new InboxMessage();
        row.setAccountId(account.getId());
        row.setProviderMessageId(providerMessageId);
        row.setFromAddress(fromAddress(message));
        row.setSubject(clean(message.getSubject()));
        row.setBodyHtml(body(message));
        row.setReceivedAt(receivedAt(message));
        row.setRead(false);
        row.setCreatedAt(Instant.now());
        return row;
    }

    private static void markSeen(Message message) {
        try {
            message.setFlag(Flags.Flag.SEEN, true);
        } catch (jakarta.mail.MessagingException e) {
            LOG.debug("A message could not be marked as seen");
        }
    }

    /**
     * The id of the message at the provider. When the provider gave none, a stable id is
     * derived from the content, so the same message is still stored once.
     */
    private static String providerMessageId(Message message) throws jakarta.mail.MessagingException {
        String[] headers = message.getHeader("Message-ID");
        String id = headers != null && headers.length > 0 ? headers[0] : null;
        if (id != null && !id.isBlank()) {
            id = id.trim();
            return id.length() > 255 ? id.substring(0, 255) : id;
        }
        StringBuilder seed = new StringBuilder();
        seed.append(message.getSubject()).append('|').append(fromAddress(message)).append('|').append(receivedAt(message));
        return sha256(seed.toString());
    }

    private static String fromAddress(Message message) {
        try {
            jakarta.mail.Address[] from = message.getFrom();
            if (from == null || from.length == 0) {
                return "";
            }
            if (from[0] instanceof InternetAddress internet) {
                return internet.getAddress() == null ? "" : internet.getAddress();
            }
            return from[0].toString();
        } catch (jakarta.mail.MessagingException e) {
            return "";
        }
    }

    private static Instant receivedAt(Message message) throws jakarta.mail.MessagingException {
        Date received = message.getReceivedDate();
        if (received == null) {
            received = message.getSentDate();
        }
        return received == null ? Instant.now() : received.toInstant();
    }

    /**
     * The body of the message, without any markup that could run in the browser of the
     * operator. The original stored the raw HTML of every inbound message and rendered it
     * unescaped, which let any sender run script in the console of the operator.
     */
    private static String body(Message message) {
        try {
            return bodyOf(message, 0);
        } catch (Exception e) {
            return null;
        }
    }

    private static String bodyOf(Message message, int depth) throws IOException, jakarta.mail.MessagingException {
        Object content = message.getContent();
        if (content instanceof String text) {
            return sanitise(text, isHtml(message.getContentType()));
        }
        if (content instanceof Multipart multipart) {
            return bodyOf(multipart, depth + 1);
        }
        return null;
    }

    private static String bodyOf(Multipart multipart, int depth) throws IOException, jakarta.mail.MessagingException {
        if (depth > 3) {
            return null;
        }
        String plain = null;
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            Object partContent = part.getContent();
            if (partContent instanceof String text) {
                if (isHtml(part.getContentType())) {
                    return sanitise(text, true);
                }
                if (plain == null) {
                    plain = text;
                }
            } else if (partContent instanceof Multipart nested) {
                String found = bodyOf(nested, depth + 1);
                if (found != null) {
                    return found;
                }
            }
        }
        return sanitise(plain, false);
    }

    private static boolean isHtml(String contentType) {
        return contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("text/html");
    }

    private static String sanitise(String text, boolean html) {
        if (text == null) {
            return null;
        }
        if (html) {
            return Jsoup.clean(text, Safelist.basic());
        }
        return Jsoup.parse(text).text();
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll("[\\p{Cntrl}]", "").trim();
        return cleaned.length() > 998 ? cleaned.substring(0, 998) : cleaned;
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xf, 16)).append(Character.forDigit(b & 0xf, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

}
