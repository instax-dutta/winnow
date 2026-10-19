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
@Table(name="email_send_job")
public class EmailSendJob {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long emailSendJobId;
    private String fromEmail;
    private String type;
    private Date date;
    private String status;
    private Integer noOfEmail;
    private Integer noOfSuccess;
    private Long loginId;
    private String createdBy;
    private String createdDate;

    public Long getEmailSendJobId() {
        return this.emailSendJobId;
    }

    public String getFromEmail() {
        return this.fromEmail;
    }

    public String getType() {
        return this.type;
    }

    public Date getDate() {
        return this.date;
    }

    public String getStatus() {
        return this.status;
    }

    public Integer getNoOfEmail() {
        return this.noOfEmail;
    }

    public Integer getNoOfSuccess() {
        return this.noOfSuccess;
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

    public void setEmailSendJobId(Long emailSendJobId) {
        this.emailSendJobId = emailSendJobId;
    }

    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setNoOfEmail(Integer noOfEmail) {
        this.noOfEmail = noOfEmail;
    }

    public void setNoOfSuccess(Integer noOfSuccess) {
        this.noOfSuccess = noOfSuccess;
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
        if (!(o instanceof EmailSendJob)) {
            return false;
        }
        EmailSendJob other = (EmailSendJob)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$emailSendJobId = this.getEmailSendJobId();
        Long other$emailSendJobId = other.getEmailSendJobId();
        if (this$emailSendJobId == null ? other$emailSendJobId != null : !((Object)this$emailSendJobId).equals(other$emailSendJobId)) {
            return false;
        }
        Integer this$noOfEmail = this.getNoOfEmail();
        Integer other$noOfEmail = other.getNoOfEmail();
        if (this$noOfEmail == null ? other$noOfEmail != null : !((Object)this$noOfEmail).equals(other$noOfEmail)) {
            return false;
        }
        Integer this$noOfSuccess = this.getNoOfSuccess();
        Integer other$noOfSuccess = other.getNoOfSuccess();
        if (this$noOfSuccess == null ? other$noOfSuccess != null : !((Object)this$noOfSuccess).equals(other$noOfSuccess)) {
            return false;
        }
        Long this$loginId = this.getLoginId();
        Long other$loginId = other.getLoginId();
        if (this$loginId == null ? other$loginId != null : !((Object)this$loginId).equals(other$loginId)) {
            return false;
        }
        String this$fromEmail = this.getFromEmail();
        String other$fromEmail = other.getFromEmail();
        if (this$fromEmail == null ? other$fromEmail != null : !this$fromEmail.equals(other$fromEmail)) {
            return false;
        }
        String this$type = this.getType();
        String other$type = other.getType();
        if (this$type == null ? other$type != null : !this$type.equals(other$type)) {
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
        return other instanceof EmailSendJob;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $emailSendJobId = this.getEmailSendJobId();
        result = result * 59 + ($emailSendJobId == null ? 43 : ((Object)$emailSendJobId).hashCode());
        Integer $noOfEmail = this.getNoOfEmail();
        result = result * 59 + ($noOfEmail == null ? 43 : ((Object)$noOfEmail).hashCode());
        Integer $noOfSuccess = this.getNoOfSuccess();
        result = result * 59 + ($noOfSuccess == null ? 43 : ((Object)$noOfSuccess).hashCode());
        Long $loginId = this.getLoginId();
        result = result * 59 + ($loginId == null ? 43 : ((Object)$loginId).hashCode());
        String $fromEmail = this.getFromEmail();
        result = result * 59 + ($fromEmail == null ? 43 : $fromEmail.hashCode());
        String $type = this.getType();
        result = result * 59 + ($type == null ? 43 : $type.hashCode());
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
        return "EmailSendJob(emailSendJobId=" + this.getEmailSendJobId() + ", fromEmail=" + this.getFromEmail() + ", type=" + this.getType() + ", date=" + this.getDate() + ", status=" + this.getStatus() + ", noOfEmail=" + this.getNoOfEmail() + ", noOfSuccess=" + this.getNoOfSuccess() + ", loginId=" + this.getLoginId() + ", createdBy=" + this.getCreatedBy() + ", createdDate=" + this.getCreatedDate() + ")";
    }
}

