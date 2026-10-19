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
@Table(name="gmail_master")
@Where(clause="isActive =1")
public class GmailMaster {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long gmailMasterId;
    private String email;
    private String clientId;
    private String secret;
    private String refreshToken;
    private String accessToken;
    private int isActive;
    private String createdBy;
    private String createdDate;
    private String modifiedBy;
    private String modifiedDate;

    public Long getGmailMasterId() {
        return this.gmailMasterId;
    }

    public String getEmail() {
        return this.email;
    }

    public String getClientId() {
        return this.clientId;
    }

    public String getSecret() {
        return this.secret;
    }

    public String getRefreshToken() {
        return this.refreshToken;
    }

    public String getAccessToken() {
        return this.accessToken;
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

    public void setGmailMasterId(Long gmailMasterId) {
        this.gmailMasterId = gmailMasterId;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
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
        if (!(o instanceof GmailMaster)) {
            return false;
        }
        GmailMaster other = (GmailMaster)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.getIsActive() != other.getIsActive()) {
            return false;
        }
        Long this$gmailMasterId = this.getGmailMasterId();
        Long other$gmailMasterId = other.getGmailMasterId();
        if (this$gmailMasterId == null ? other$gmailMasterId != null : !((Object)this$gmailMasterId).equals(other$gmailMasterId)) {
            return false;
        }
        String this$email = this.getEmail();
        String other$email = other.getEmail();
        if (this$email == null ? other$email != null : !this$email.equals(other$email)) {
            return false;
        }
        String this$clientId = this.getClientId();
        String other$clientId = other.getClientId();
        if (this$clientId == null ? other$clientId != null : !this$clientId.equals(other$clientId)) {
            return false;
        }
        String this$secret = this.getSecret();
        String other$secret = other.getSecret();
        if (this$secret == null ? other$secret != null : !this$secret.equals(other$secret)) {
            return false;
        }
        String this$refreshToken = this.getRefreshToken();
        String other$refreshToken = other.getRefreshToken();
        if (this$refreshToken == null ? other$refreshToken != null : !this$refreshToken.equals(other$refreshToken)) {
            return false;
        }
        String this$accessToken = this.getAccessToken();
        String other$accessToken = other.getAccessToken();
        if (this$accessToken == null ? other$accessToken != null : !this$accessToken.equals(other$accessToken)) {
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
        return other instanceof GmailMaster;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + this.getIsActive();
        Long $gmailMasterId = this.getGmailMasterId();
        result = result * 59 + ($gmailMasterId == null ? 43 : ((Object)$gmailMasterId).hashCode());
        String $email = this.getEmail();
        result = result * 59 + ($email == null ? 43 : $email.hashCode());
        String $clientId = this.getClientId();
        result = result * 59 + ($clientId == null ? 43 : $clientId.hashCode());
        String $secret = this.getSecret();
        result = result * 59 + ($secret == null ? 43 : $secret.hashCode());
        String $refreshToken = this.getRefreshToken();
        result = result * 59 + ($refreshToken == null ? 43 : $refreshToken.hashCode());
        String $accessToken = this.getAccessToken();
        result = result * 59 + ($accessToken == null ? 43 : $accessToken.hashCode());
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
        return "GmailMaster(gmailMasterId=" + this.getGmailMasterId() + ", email=" + this.getEmail() + ", clientId=" + this.getClientId() + ", secret=" + this.getSecret() + ", refreshToken=" + this.getRefreshToken() + ", accessToken=" + this.getAccessToken() + ", isActive=" + this.getIsActive() + ", createdBy=" + this.getCreatedBy() + ", createdDate=" + this.getCreatedDate() + ", modifiedBy=" + this.getModifiedBy() + ", modifiedDate=" + this.getModifiedDate() + ")";
    }
}

