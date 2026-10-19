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
import java.sql.Date;

@Entity
@Table(name="email_send_status")
public class EmailSendStatus {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long emailSendStatusId;
    private String email;
    private Date date;
    private Long emailTemplateId;
    private String status;
    private Long loginId;
    private String createdBy;
    private String createdDate;

    public Long getEmailSendStatusId() {
        return this.emailSendStatusId;
    }

    public String getEmail() {
        return this.email;
    }

    public Date getDate() {
        return this.date;
    }

    public Long getEmailTemplateId() {
        return this.emailTemplateId;
    }

    public String getStatus() {
        return this.status;
    }

    public Long getLoginId() {
        return this.loginId;
    }

    public String getCreatedBy() {
        return this.createdBy;
    }

    public String getCreatedDate() {
        return this.createdDate;
    }

    public void setEmailSendStatusId(Long emailSendStatusId) {
        this.emailSendStatusId = emailSendStatusId;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setEmailTemplateId(Long emailTemplateId) {
        this.emailTemplateId = emailTemplateId;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setLoginId(Long loginId) {
        this.loginId = loginId;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof EmailSendStatus)) {
            return false;
        }
        EmailSendStatus other = (EmailSendStatus)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$emailSendStatusId = this.getEmailSendStatusId();
        Long other$emailSendStatusId = other.getEmailSendStatusId();
        if (this$emailSendStatusId == null ? other$emailSendStatusId != null : !((Object)this$emailSendStatusId).equals(other$emailSendStatusId)) {
            return false;
        }
        Long this$emailTemplateId = this.getEmailTemplateId();
        Long other$emailTemplateId = other.getEmailTemplateId();
        if (this$emailTemplateId == null ? other$emailTemplateId != null : !((Object)this$emailTemplateId).equals(other$emailTemplateId)) {
            return false;
        }
        Long this$loginId = this.getLoginId();
        Long other$loginId = other.getLoginId();
        if (this$loginId == null ? other$loginId != null : !((Object)this$loginId).equals(other$loginId)) {
            return false;
        }
        String this$email = this.getEmail();
        String other$email = other.getEmail();
        if (this$email == null ? other$email != null : !this$email.equals(other$email)) {
            return false;
        }
        Date this$date = this.getDate();
        Date other$date = other.getDate();
        if (this$date == null ? other$date != null : !((Object)this$date).equals(other$date)) {
            return false;
        }
        String this$status = this.getStatus();
        String other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) {
            return false;
        }
        String this$createdBy = this.getCreatedBy();
        String other$createdBy = other.getCreatedBy();
        if (this$createdBy == null ? other$createdBy != null : !this$createdBy.equals(other$createdBy)) {
            return false;
        }
        String this$createdDate = this.getCreatedDate();
        String other$createdDate = other.getCreatedDate();
        return !(this$createdDate == null ? other$createdDate != null : !this$createdDate.equals(other$createdDate));
    }

    protected boolean canEqual(Object other) {
        return other instanceof EmailSendStatus;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $emailSendStatusId = this.getEmailSendStatusId();
        result = result * 59 + ($emailSendStatusId == null ? 43 : ((Object)$emailSendStatusId).hashCode());
        Long $emailTemplateId = this.getEmailTemplateId();
        result = result * 59 + ($emailTemplateId == null ? 43 : ((Object)$emailTemplateId).hashCode());
        Long $loginId = this.getLoginId();
        result = result * 59 + ($loginId == null ? 43 : ((Object)$loginId).hashCode());
        String $email = this.getEmail();
        result = result * 59 + ($email == null ? 43 : $email.hashCode());
        Date $date = this.getDate();
        result = result * 59 + ($date == null ? 43 : ((Object)$date).hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        String $createdBy = this.getCreatedBy();
        result = result * 59 + ($createdBy == null ? 43 : $createdBy.hashCode());
        String $createdDate = this.getCreatedDate();
        result = result * 59 + ($createdDate == null ? 43 : $createdDate.hashCode());
        return result;
    }

    public String toString() {
        return "EmailSendStatus(emailSendStatusId=" + this.getEmailSendStatusId() + ", email=" + this.getEmail() + ", date=" + this.getDate() + ", emailTemplateId=" + this.getEmailTemplateId() + ", status=" + this.getStatus() + ", loginId=" + this.getLoginId() + ", createdBy=" + this.getCreatedBy() + ", createdDate=" + this.getCreatedDate() + ")";
    }
}

