/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.persistence.Entity
 *  jakarta.persistence.GeneratedValue
 *  jakarta.persistence.GenerationType
 *  jakarta.persistence.Id
 *  jakarta.persistence.Table
 */
package com.pg.seo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name="email_queue")
public class EmailQueue {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long emailQueueId;
    private String fromEmail;
    private String toEmail;
    private String draftId;
    private String domain;
    private LocalDateTime scheduledTime;
    private String status;
    private String errorMessage;

    public Long getEmailQueueId() {
        return this.emailQueueId;
    }

    public String getFromEmail() {
        return this.fromEmail;
    }

    public String getToEmail() {
        return this.toEmail;
    }

    public String getDraftId() {
        return this.draftId;
    }

    public String getDomain() {
        return this.domain;
    }

    public LocalDateTime getScheduledTime() {
        return this.scheduledTime;
    }

    public String getStatus() {
        return this.status;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public void setEmailQueueId(Long emailQueueId) {
        this.emailQueueId = emailQueueId;
    }

    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }

    public void setToEmail(String toEmail) {
        this.toEmail = toEmail;
    }

    public void setDraftId(String draftId) {
        this.draftId = draftId;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setScheduledTime(LocalDateTime scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof EmailQueue)) {
            return false;
        }
        EmailQueue other = (EmailQueue)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$emailQueueId = this.getEmailQueueId();
        Long other$emailQueueId = other.getEmailQueueId();
        if (this$emailQueueId == null ? other$emailQueueId != null : !((Object)this$emailQueueId).equals(other$emailQueueId)) {
            return false;
        }
        String this$fromEmail = this.getFromEmail();
        String other$fromEmail = other.getFromEmail();
        if (this$fromEmail == null ? other$fromEmail != null : !this$fromEmail.equals(other$fromEmail)) {
            return false;
        }
        String this$toEmail = this.getToEmail();
        String other$toEmail = other.getToEmail();
        if (this$toEmail == null ? other$toEmail != null : !this$toEmail.equals(other$toEmail)) {
            return false;
        }
        String this$draftId = this.getDraftId();
        String other$draftId = other.getDraftId();
        if (this$draftId == null ? other$draftId != null : !this$draftId.equals(other$draftId)) {
            return false;
        }
        String this$domain = this.getDomain();
        String other$domain = other.getDomain();
        if (this$domain == null ? other$domain != null : !this$domain.equals(other$domain)) {
            return false;
        }
        LocalDateTime this$scheduledTime = this.getScheduledTime();
        LocalDateTime other$scheduledTime = other.getScheduledTime();
        if (this$scheduledTime == null ? other$scheduledTime != null : !((Object)this$scheduledTime).equals(other$scheduledTime)) {
            return false;
        }
        String this$status = this.getStatus();
        String other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) {
            return false;
        }
        String this$errorMessage = this.getErrorMessage();
        String other$errorMessage = other.getErrorMessage();
        return !(this$errorMessage == null ? other$errorMessage != null : !this$errorMessage.equals(other$errorMessage));
    }

    protected boolean canEqual(Object other) {
        return other instanceof EmailQueue;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $emailQueueId = this.getEmailQueueId();
        result = result * 59 + ($emailQueueId == null ? 43 : ((Object)$emailQueueId).hashCode());
        String $fromEmail = this.getFromEmail();
        result = result * 59 + ($fromEmail == null ? 43 : $fromEmail.hashCode());
        String $toEmail = this.getToEmail();
        result = result * 59 + ($toEmail == null ? 43 : $toEmail.hashCode());
        String $draftId = this.getDraftId();
        result = result * 59 + ($draftId == null ? 43 : $draftId.hashCode());
        String $domain = this.getDomain();
        result = result * 59 + ($domain == null ? 43 : $domain.hashCode());
        LocalDateTime $scheduledTime = this.getScheduledTime();
        result = result * 59 + ($scheduledTime == null ? 43 : ((Object)$scheduledTime).hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        String $errorMessage = this.getErrorMessage();
        result = result * 59 + ($errorMessage == null ? 43 : $errorMessage.hashCode());
        return result;
    }

    public String toString() {
        return "EmailQueue(emailQueueId=" + this.getEmailQueueId() + ", fromEmail=" + this.getFromEmail() + ", toEmail=" + this.getToEmail() + ", draftId=" + this.getDraftId() + ", domain=" + this.getDomain() + ", scheduledTime=" + this.getScheduledTime() + ", status=" + this.getStatus() + ", errorMessage=" + this.getErrorMessage() + ")";
    }
}

