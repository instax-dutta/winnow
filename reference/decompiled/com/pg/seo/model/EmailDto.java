/*
 * Decompiled with CFR 0.152.
 */
package com.pg.seo.model;

public class EmailDto {
    private Long emailInboxId;
    private String from;
    private String subject;
    private String content;

    public EmailDto(Long emailInboxId, String from, String subject, String content) {
        this.emailInboxId = emailInboxId;
        this.from = from;
        this.subject = subject;
        this.content = content;
    }

    public EmailDto() {
    }

    public Long getEmailInboxId() {
        return this.emailInboxId;
    }

    public String getFrom() {
        return this.from;
    }

    public String getSubject() {
        return this.subject;
    }

    public String getContent() {
        return this.content;
    }

    public void setEmailInboxId(Long emailInboxId) {
        this.emailInboxId = emailInboxId;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof EmailDto)) {
            return false;
        }
        EmailDto other = (EmailDto)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$emailInboxId = this.getEmailInboxId();
        Long other$emailInboxId = other.getEmailInboxId();
        if (this$emailInboxId == null ? other$emailInboxId != null : !((Object)this$emailInboxId).equals(other$emailInboxId)) {
            return false;
        }
        String this$from = this.getFrom();
        String other$from = other.getFrom();
        if (this$from == null ? other$from != null : !this$from.equals(other$from)) {
            return false;
        }
        String this$subject = this.getSubject();
        String other$subject = other.getSubject();
        if (this$subject == null ? other$subject != null : !this$subject.equals(other$subject)) {
            return false;
        }
        String this$content = this.getContent();
        String other$content = other.getContent();
        return !(this$content == null ? other$content != null : !this$content.equals(other$content));
    }

    protected boolean canEqual(Object other) {
        return other instanceof EmailDto;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $emailInboxId = this.getEmailInboxId();
        result = result * 59 + ($emailInboxId == null ? 43 : ((Object)$emailInboxId).hashCode());
        String $from = this.getFrom();
        result = result * 59 + ($from == null ? 43 : $from.hashCode());
        String $subject = this.getSubject();
        result = result * 59 + ($subject == null ? 43 : $subject.hashCode());
        String $content = this.getContent();
        result = result * 59 + ($content == null ? 43 : $content.hashCode());
        return result;
    }

    public String toString() {
        return "EmailDto(emailInboxId=" + this.getEmailInboxId() + ", from=" + this.getFrom() + ", subject=" + this.getSubject() + ", content=" + this.getContent() + ")";
    }
}

