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
@Table(name="email_extractor")
public class EmailExtractor {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long emailExtractorId;
    private Long noOfDomain;
    private Date startDate;
    private String status;
    private Long loginId;
    private String fileName;
    private String createdBy;
    private String createdDate;

    public Long getEmailExtractorId() {
        return this.emailExtractorId;
    }

    public Long getNoOfDomain() {
        return this.noOfDomain;
    }

    public Date getStartDate() {
        return this.startDate;
    }

    public String getStatus() {
        return this.status;
    }

    public Long getLoginId() {
        return this.loginId;
    }

    public String getFileName() {
        return this.fileName;
    }

    public String getCreatedBy() {
        return this.createdBy;
    }

    public String getCreatedDate() {
        return this.createdDate;
    }

    public void setEmailExtractorId(Long emailExtractorId) {
        this.emailExtractorId = emailExtractorId;
    }

    public void setNoOfDomain(Long noOfDomain) {
        this.noOfDomain = noOfDomain;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setLoginId(Long loginId) {
        this.loginId = loginId;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
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
        if (!(o instanceof EmailExtractor)) {
            return false;
        }
        EmailExtractor other = (EmailExtractor)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$emailExtractorId = this.getEmailExtractorId();
        Long other$emailExtractorId = other.getEmailExtractorId();
        if (this$emailExtractorId == null ? other$emailExtractorId != null : !((Object)this$emailExtractorId).equals(other$emailExtractorId)) {
            return false;
        }
        Long this$noOfDomain = this.getNoOfDomain();
        Long other$noOfDomain = other.getNoOfDomain();
        if (this$noOfDomain == null ? other$noOfDomain != null : !((Object)this$noOfDomain).equals(other$noOfDomain)) {
            return false;
        }
        Long this$loginId = this.getLoginId();
        Long other$loginId = other.getLoginId();
        if (this$loginId == null ? other$loginId != null : !((Object)this$loginId).equals(other$loginId)) {
            return false;
        }
        Date this$startDate = this.getStartDate();
        Date other$startDate = other.getStartDate();
        if (this$startDate == null ? other$startDate != null : !((Object)this$startDate).equals(other$startDate)) {
            return false;
        }
        String this$status = this.getStatus();
        String other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) {
            return false;
        }
        String this$fileName = this.getFileName();
        String other$fileName = other.getFileName();
        if (this$fileName == null ? other$fileName != null : !this$fileName.equals(other$fileName)) {
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
        return other instanceof EmailExtractor;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $emailExtractorId = this.getEmailExtractorId();
        result = result * 59 + ($emailExtractorId == null ? 43 : ((Object)$emailExtractorId).hashCode());
        Long $noOfDomain = this.getNoOfDomain();
        result = result * 59 + ($noOfDomain == null ? 43 : ((Object)$noOfDomain).hashCode());
        Long $loginId = this.getLoginId();
        result = result * 59 + ($loginId == null ? 43 : ((Object)$loginId).hashCode());
        Date $startDate = this.getStartDate();
        result = result * 59 + ($startDate == null ? 43 : ((Object)$startDate).hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        String $fileName = this.getFileName();
        result = result * 59 + ($fileName == null ? 43 : $fileName.hashCode());
        String $createdBy = this.getCreatedBy();
        result = result * 59 + ($createdBy == null ? 43 : $createdBy.hashCode());
        String $createdDate = this.getCreatedDate();
        result = result * 59 + ($createdDate == null ? 43 : $createdDate.hashCode());
        return result;
    }

    public String toString() {
        return "EmailExtractor(emailExtractorId=" + this.getEmailExtractorId() + ", noOfDomain=" + this.getNoOfDomain() + ", startDate=" + this.getStartDate() + ", status=" + this.getStatus() + ", loginId=" + this.getLoginId() + ", fileName=" + this.getFileName() + ", createdBy=" + this.getCreatedBy() + ", createdDate=" + this.getCreatedDate() + ")";
    }
}

