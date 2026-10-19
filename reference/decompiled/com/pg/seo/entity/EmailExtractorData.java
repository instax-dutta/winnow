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
@Table(name="email_extractor_data")
public class EmailExtractorData {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long emailExtractorDataId;
    private Long emailExtractorId;
    private Date startDate;
    private String domain;
    private String email;
    private String createdBy;
    private String createdDate;

    public Long getEmailExtractorDataId() {
        return this.emailExtractorDataId;
    }

    public Long getEmailExtractorId() {
        return this.emailExtractorId;
    }

    public Date getStartDate() {
        return this.startDate;
    }

    public String getDomain() {
        return this.domain;
    }

    public String getEmail() {
        return this.email;
    }

    public String getCreatedBy() {
        return this.createdBy;
    }

    public String getCreatedDate() {
        return this.createdDate;
    }

    public void setEmailExtractorDataId(Long emailExtractorDataId) {
        this.emailExtractorDataId = emailExtractorDataId;
    }

    public void setEmailExtractorId(Long emailExtractorId) {
        this.emailExtractorId = emailExtractorId;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setEmail(String email) {
        this.email = email;
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
        if (!(o instanceof EmailExtractorData)) {
            return false;
        }
        EmailExtractorData other = (EmailExtractorData)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$emailExtractorDataId = this.getEmailExtractorDataId();
        Long other$emailExtractorDataId = other.getEmailExtractorDataId();
        if (this$emailExtractorDataId == null ? other$emailExtractorDataId != null : !((Object)this$emailExtractorDataId).equals(other$emailExtractorDataId)) {
            return false;
        }
        Long this$emailExtractorId = this.getEmailExtractorId();
        Long other$emailExtractorId = other.getEmailExtractorId();
        if (this$emailExtractorId == null ? other$emailExtractorId != null : !((Object)this$emailExtractorId).equals(other$emailExtractorId)) {
            return false;
        }
        Date this$startDate = this.getStartDate();
        Date other$startDate = other.getStartDate();
        if (this$startDate == null ? other$startDate != null : !((Object)this$startDate).equals(other$startDate)) {
            return false;
        }
        String this$domain = this.getDomain();
        String other$domain = other.getDomain();
        if (this$domain == null ? other$domain != null : !this$domain.equals(other$domain)) {
            return false;
        }
        String this$email = this.getEmail();
        String other$email = other.getEmail();
        if (this$email == null ? other$email != null : !this$email.equals(other$email)) {
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
        return other instanceof EmailExtractorData;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $emailExtractorDataId = this.getEmailExtractorDataId();
        result = result * 59 + ($emailExtractorDataId == null ? 43 : ((Object)$emailExtractorDataId).hashCode());
        Long $emailExtractorId = this.getEmailExtractorId();
        result = result * 59 + ($emailExtractorId == null ? 43 : ((Object)$emailExtractorId).hashCode());
        Date $startDate = this.getStartDate();
        result = result * 59 + ($startDate == null ? 43 : ((Object)$startDate).hashCode());
        String $domain = this.getDomain();
        result = result * 59 + ($domain == null ? 43 : $domain.hashCode());
        String $email = this.getEmail();
        result = result * 59 + ($email == null ? 43 : $email.hashCode());
        String $createdBy = this.getCreatedBy();
        result = result * 59 + ($createdBy == null ? 43 : $createdBy.hashCode());
        String $createdDate = this.getCreatedDate();
        result = result * 59 + ($createdDate == null ? 43 : $createdDate.hashCode());
        return result;
    }

    public String toString() {
        return "EmailExtractorData(emailExtractorDataId=" + this.getEmailExtractorDataId() + ", emailExtractorId=" + this.getEmailExtractorId() + ", startDate=" + this.getStartDate() + ", domain=" + this.getDomain() + ", email=" + this.getEmail() + ", createdBy=" + this.getCreatedBy() + ", createdDate=" + this.getCreatedDate() + ")";
    }
}

