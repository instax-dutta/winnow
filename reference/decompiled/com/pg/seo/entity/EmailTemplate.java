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
import org.hibernate.annotations.Where;

@Entity
@Table(name="email_template")
@Where(clause="isActive =1")
public class EmailTemplate {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long emailTemplateId;
    private String templateName;
    private String templateContent;
    private String status;
    private Long loginId;
    private int isActive;
    private String createdBy;
    private String createdDate;
    private String modifiedBy;
    private String modifiedDate;

    public Long getEmailTemplateId() {
        return this.emailTemplateId;
    }

    public String getTemplateName() {
        return this.templateName;
    }

    public String getTemplateContent() {
        return this.templateContent;
    }

    public String getStatus() {
        return this.status;
    }

    public Long getLoginId() {
        return this.loginId;
    }

    public int getIsActive() {
        return this.isActive;
    }

    public String getCreatedBy() {
        return this.createdBy;
    }

    public String getCreatedDate() {
        return this.createdDate;
    }

    public String getModifiedBy() {
        return this.modifiedBy;
    }

    public String getModifiedDate() {
        return this.modifiedDate;
    }

    public void setEmailTemplateId(Long emailTemplateId) {
        this.emailTemplateId = emailTemplateId;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public void setTemplateContent(String templateContent) {
        this.templateContent = templateContent;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setLoginId(Long loginId) {
        this.loginId = loginId;
    }

    public void setIsActive(int isActive) {
        this.isActive = isActive;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public void setModifiedDate(String modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof EmailTemplate)) {
            return false;
        }
        EmailTemplate other = (EmailTemplate)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.getIsActive() != other.getIsActive()) {
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
        String this$templateName = this.getTemplateName();
        String other$templateName = other.getTemplateName();
        if (this$templateName == null ? other$templateName != null : !this$templateName.equals(other$templateName)) {
            return false;
        }
        String this$templateContent = this.getTemplateContent();
        String other$templateContent = other.getTemplateContent();
        if (this$templateContent == null ? other$templateContent != null : !this$templateContent.equals(other$templateContent)) {
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
        if (this$createdDate == null ? other$createdDate != null : !this$createdDate.equals(other$createdDate)) {
            return false;
        }
        String this$modifiedBy = this.getModifiedBy();
        String other$modifiedBy = other.getModifiedBy();
        if (this$modifiedBy == null ? other$modifiedBy != null : !this$modifiedBy.equals(other$modifiedBy)) {
            return false;
        }
        String this$modifiedDate = this.getModifiedDate();
        String other$modifiedDate = other.getModifiedDate();
        return !(this$modifiedDate == null ? other$modifiedDate != null : !this$modifiedDate.equals(other$modifiedDate));
    }

    protected boolean canEqual(Object other) {
        return other instanceof EmailTemplate;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + this.getIsActive();
        Long $emailTemplateId = this.getEmailTemplateId();
        result = result * 59 + ($emailTemplateId == null ? 43 : ((Object)$emailTemplateId).hashCode());
        Long $loginId = this.getLoginId();
        result = result * 59 + ($loginId == null ? 43 : ((Object)$loginId).hashCode());
        String $templateName = this.getTemplateName();
        result = result * 59 + ($templateName == null ? 43 : $templateName.hashCode());
        String $templateContent = this.getTemplateContent();
        result = result * 59 + ($templateContent == null ? 43 : $templateContent.hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        String $createdBy = this.getCreatedBy();
        result = result * 59 + ($createdBy == null ? 43 : $createdBy.hashCode());
        String $createdDate = this.getCreatedDate();
        result = result * 59 + ($createdDate == null ? 43 : $createdDate.hashCode());
        String $modifiedBy = this.getModifiedBy();
        result = result * 59 + ($modifiedBy == null ? 43 : $modifiedBy.hashCode());
        String $modifiedDate = this.getModifiedDate();
        result = result * 59 + ($modifiedDate == null ? 43 : $modifiedDate.hashCode());
        return result;
    }

    public String toString() {
        return "EmailTemplate(emailTemplateId=" + this.getEmailTemplateId() + ", templateName=" + this.getTemplateName() + ", templateContent=" + this.getTemplateContent() + ", status=" + this.getStatus() + ", loginId=" + this.getLoginId() + ", isActive=" + this.getIsActive() + ", createdBy=" + this.getCreatedBy() + ", createdDate=" + this.getCreatedDate() + ", modifiedBy=" + this.getModifiedBy() + ", modifiedDate=" + this.getModifiedDate() + ")";
    }
}

