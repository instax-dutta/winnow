/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.persistence.Entity
 *  jakarta.persistence.GeneratedValue
 *  jakarta.persistence.GenerationType
 *  jakarta.persistence.Id
 *  jakarta.persistence.Table
 *  org.hibernate.annotations.Where
 */
package com.pg.seo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Date;
import org.hibernate.annotations.Where;

@Entity
@Table(name="email_inbox")
@Where(clause="isActive =1")
public class EmailInbox {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long emailInboxId;
    private String emailId;
    private String from;
    private String subject;
    private String content;
    private Date date;
    private int isActive;

    public Long getEmailInboxId() {
        return this.emailInboxId;
    }

    public String getEmailId() {
        return this.emailId;
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

    public Date getDate() {
        return this.date;
    }

    public int getIsActive() {
        return this.isActive;
    }

    public void setEmailInboxId(Long emailInboxId) {
        this.emailInboxId = emailInboxId;
    }

    public void setEmailId(String emailId) {
        this.emailId = emailId;
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

    public void setDate(Date date) {
        this.date = date;
    }

    public void setIsActive(int isActive) {
        this.isActive = isActive;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof EmailInbox)) {
            return false;
        }
        EmailInbox other = (EmailInbox)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.getIsActive() != other.getIsActive()) {
            return false;
        }
        Long this$emailInboxId = this.getEmailInboxId();
        Long other$emailInboxId = other.getEmailInboxId();
        if (this$emailInboxId == null ? other$emailInboxId != null : !((Object)this$emailInboxId).equals(other$emailInboxId)) {
            return false;
        }
        String this$emailId = this.getEmailId();
        String other$emailId = other.getEmailId();
        if (this$emailId == null ? other$emailId != null : !this$emailId.equals(other$emailId)) {
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
        if (this$content == null ? other$content != null : !this$content.equals(other$content)) {
            return false;
        }
        Date this$date = this.getDate();
        Date other$date = other.getDate();
        return !(this$date == null ? other$date != null : !((Object)this$date).equals(other$date));
    }

    protected boolean canEqual(Object other) {
        return other instanceof EmailInbox;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + this.getIsActive();
        Long $emailInboxId = this.getEmailInboxId();
        result = result * 59 + ($emailInboxId == null ? 43 : ((Object)$emailInboxId).hashCode());
        String $emailId = this.getEmailId();
        result = result * 59 + ($emailId == null ? 43 : $emailId.hashCode());
        String $from = this.getFrom();
        result = result * 59 + ($from == null ? 43 : $from.hashCode());
        String $subject = this.getSubject();
        result = result * 59 + ($subject == null ? 43 : $subject.hashCode());
        String $content = this.getContent();
        result = result * 59 + ($content == null ? 43 : $content.hashCode());
        Date $date = this.getDate();
        result = result * 59 + ($date == null ? 43 : ((Object)$date).hashCode());
        return result;
    }

    public String toString() {
        return "EmailInbox(emailInboxId=" + this.getEmailInboxId() + ", emailId=" + this.getEmailId() + ", from=" + this.getFrom() + ", subject=" + this.getSubject() + ", content=" + this.getContent() + ", date=" + this.getDate() + ", isActive=" + this.getIsActive() + ")";
    }
}

