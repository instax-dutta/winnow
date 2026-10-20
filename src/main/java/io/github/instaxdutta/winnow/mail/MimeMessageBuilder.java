package io.github.instaxdutta.winnow.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.UUID;
import org.jsoup.Jsoup;
import org.springframework.mail.javamail.MimeMessageHelper;

/**
 * Builds one MIME message for one recipient.
 *
 * <p>Every message gets a plain-text alternative next to the HTML body, a subject of its
 * own and a Message-ID. Reusing a single mutable message across recipients leaks
 * partially-applied state on failure, and using the template name as the subject loses the
 * subject the operator wrote.
 */
final class MimeMessageBuilder {

    private MimeMessageBuilder() {}

    /**
     * Fills {@code message} from {@code request}. One message per recipient: the caller
     * creates a fresh instance for every send.
     */
    static MimeMessage build(MimeMessage message, SendRequest request) throws MessagingException {
        MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
        helper.setFrom(request.fromEmail());
        helper.setTo(request.toEmail());
        helper.setSubject(request.subject() == null ? "" : request.subject());
        if (request.replyTo() != null && !request.replyTo().isBlank()) {
            helper.setReplyTo(request.replyTo());
        }

        String html = request.htmlBody() == null ? "" : request.htmlBody();
        String text = request.textBody();
        if (text == null || text.isBlank()) {
            text = Jsoup.parse(html).text();
        }

        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(text, StandardCharsets.UTF_8.name());
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(html, "text/html; charset=" + StandardCharsets.UTF_8.name());
        message.setContent(new MimeMultipart("alternative", textPart, htmlPart));

        // jakarta.mail regenerates the Message-ID on every saveChanges, so the effective
        // identifier is read back from the message after the send.
        message.setHeader("Message-ID", messageId(request.fromEmail()));
        return message;
    }

    /**
     * A message id of the form {@code <uuid@domain>}, using the domain of the sending
     * mailbox.
     */
    static String messageId(String fromEmail) {
        String domain = "localhost";
        if (fromEmail != null) {
            int at = fromEmail.indexOf('@');
            if (at > 0 && at < fromEmail.length() - 1) {
                domain = fromEmail.substring(at + 1);
            }
        }
        return "<" + UUID.randomUUID() + "@" + domain + ">";
    }

    /**
     * The session properties for a message that is written to bytes rather than handed to
     * a transport.
     */
    static Properties sessionProperties() {
        Properties props = new Properties();
        props.put("mail.mime.charset", StandardCharsets.UTF_8.name());
        return props;
    }
}
