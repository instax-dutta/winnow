/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.persistence.CascadeType
 *  jakarta.persistence.Entity
 *  jakarta.persistence.FetchType
 *  jakarta.persistence.GeneratedValue
 *  jakarta.persistence.GenerationType
 *  jakarta.persistence.Id
 *  jakarta.persistence.JoinColumn
 *  jakarta.persistence.ManyToOne
 *  jakarta.persistence.Table
 *  org.hibernate.annotations.Where
 */
package com.pg.seo.entity;

import com.pg.seo.entity.ClientGroup;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Where;

@Entity
@Table(name="client_master")
@Where(clause="isActive =1")
public class ClientMaster {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long clientMasterId;
    private String name;
    private String email;
    private String webLink;
    private Long loginId;
    @ManyToOne(cascade={CascadeType.ALL}, fetch=FetchType.LAZY)
    @JoinColumn(name="groupCode", referencedColumnName="groupCode")
    private ClientGroup clientGroup;
    private int isActive;
    private String createdBy;
    private String createdDate;
    private String modifiedBy;
    private String modifiedDate;

    public Long getClientMasterId() {
        return this.clientMasterId;
    }

    public String getName() {
        return this.name;
    }

    public String getEmail() {
        return this.email;
    }

    public String getWebLink() {
        return this.webLink;
    }

    public Long getLoginId() {
        return this.loginId;
    }

    public ClientGroup getClientGroup() {
        return this.clientGroup;
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

    public void setClientMasterId(Long clientMasterId) {
        this.clientMasterId = clientMasterId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setWebLink(String webLink) {
        this.webLink = webLink;
    }

    public void setLoginId(Long loginId) {
        this.loginId = loginId;
    }

    public void setClientGroup(ClientGroup clientGroup) {
        this.clientGroup = clientGroup;
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
        if (!(o instanceof ClientMaster)) {
            return false;
        }
        ClientMaster other = (ClientMaster)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.getIsActive() != other.getIsActive()) {
            return false;
        }
        Long this$clientMasterId = this.getClientMasterId();
        Long other$clientMasterId = other.getClientMasterId();
        if (this$clientMasterId == null ? other$clientMasterId != null : !((Object)this$clientMasterId).equals(other$clientMasterId)) {
            return false;
        }
        Long this$loginId = this.getLoginId();
        Long other$loginId = other.getLoginId();
        if (this$loginId == null ? other$loginId != null : !((Object)this$loginId).equals(other$loginId)) {
            return false;
        }
        String this$name = this.getName();
        String other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
            return false;
        }
        String this$email = this.getEmail();
        String other$email = other.getEmail();
        if (this$email == null ? other$email != null : !this$email.equals(other$email)) {
            return false;
        }
        String this$webLink = this.getWebLink();
        String other$webLink = other.getWebLink();
        if (this$webLink == null ? other$webLink != null : !this$webLink.equals(other$webLink)) {
            return false;
        }
        ClientGroup this$clientGroup = this.getClientGroup();
        ClientGroup other$clientGroup = other.getClientGroup();
        if (this$clientGroup == null ? other$clientGroup != null : !((Object)this$clientGroup).equals(other$clientGroup)) {
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
        return other instanceof ClientMaster;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + this.getIsActive();
        Long $clientMasterId = this.getClientMasterId();
        result = result * 59 + ($clientMasterId == null ? 43 : ((Object)$clientMasterId).hashCode());
        Long $loginId = this.getLoginId();
        result = result * 59 + ($loginId == null ? 43 : ((Object)$loginId).hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $email = this.getEmail();
        result = result * 59 + ($email == null ? 43 : $email.hashCode());
        String $webLink = this.getWebLink();
        result = result * 59 + ($webLink == null ? 43 : $webLink.hashCode());
        ClientGroup $clientGroup = this.getClientGroup();
        result = result * 59 + ($clientGroup == null ? 43 : ((Object)$clientGroup).hashCode());
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
        return "ClientMaster(clientMasterId=" + this.getClientMasterId() + ", name=" + this.getName() + ", email=" + this.getEmail() + ", webLink=" + this.getWebLink() + ", loginId=" + this.getLoginId() + ", clientGroup=" + this.getClientGroup() + ", isActive=" + this.getIsActive() + ", createdBy=" + this.getCreatedBy() + ", createdDate=" + this.getCreatedDate() + ", modifiedBy=" + this.getModifiedBy() + ", modifiedDate=" + this.getModifiedDate() + ")";
    }
}

