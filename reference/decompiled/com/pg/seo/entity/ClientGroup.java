/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonIgnoreProperties
 *  jakarta.persistence.CascadeType
 *  jakarta.persistence.Entity
 *  jakarta.persistence.FetchType
 *  jakarta.persistence.GeneratedValue
 *  jakarta.persistence.GenerationType
 *  jakarta.persistence.Id
 *  jakarta.persistence.OneToMany
 *  jakarta.persistence.Table
 *  org.hibernate.annotations.Where
 */
package com.pg.seo.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.pg.seo.entity.ClientMaster;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.Where;

@Entity
@Table(name="client_group")
@Where(clause="isActive =1")
public class ClientGroup {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long groupId;
    private String groupCode;
    private String groupDesc;
    private Long loginId;
    private int isActive;
    private String createdBy;
    private String createdDate;
    private String modifiedBy;
    private String modifiedDate;
    @JsonIgnoreProperties
    @OneToMany(cascade={CascadeType.ALL}, fetch=FetchType.LAZY, mappedBy="clientGroup")
    private List<ClientMaster> clientMaster = new ArrayList<ClientMaster>();

    public Long getGroupId() {
        return this.groupId;
    }

    public String getGroupCode() {
        return this.groupCode;
    }

    public String getGroupDesc() {
        return this.groupDesc;
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

    public List<ClientMaster> getClientMaster() {
        return this.clientMaster;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public void setGroupCode(String groupCode) {
        this.groupCode = groupCode;
    }

    public void setGroupDesc(String groupDesc) {
        this.groupDesc = groupDesc;
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

    @JsonIgnoreProperties
    public void setClientMaster(List<ClientMaster> clientMaster) {
        this.clientMaster = clientMaster;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ClientGroup)) {
            return false;
        }
        ClientGroup other = (ClientGroup)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.getIsActive() != other.getIsActive()) {
            return false;
        }
        Long this$groupId = this.getGroupId();
        Long other$groupId = other.getGroupId();
        if (this$groupId == null ? other$groupId != null : !((Object)this$groupId).equals(other$groupId)) {
            return false;
        }
        Long this$loginId = this.getLoginId();
        Long other$loginId = other.getLoginId();
        if (this$loginId == null ? other$loginId != null : !((Object)this$loginId).equals(other$loginId)) {
            return false;
        }
        String this$groupCode = this.getGroupCode();
        String other$groupCode = other.getGroupCode();
        if (this$groupCode == null ? other$groupCode != null : !this$groupCode.equals(other$groupCode)) {
            return false;
        }
        String this$groupDesc = this.getGroupDesc();
        String other$groupDesc = other.getGroupDesc();
        if (this$groupDesc == null ? other$groupDesc != null : !this$groupDesc.equals(other$groupDesc)) {
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
        if (this$modifiedDate == null ? other$modifiedDate != null : !this$modifiedDate.equals(other$modifiedDate)) {
            return false;
        }
        List<ClientMaster> this$clientMaster = this.getClientMaster();
        List<ClientMaster> other$clientMaster = other.getClientMaster();
        return !(this$clientMaster == null ? other$clientMaster != null : !((Object)this$clientMaster).equals(other$clientMaster));
    }

    protected boolean canEqual(Object other) {
        return other instanceof ClientGroup;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + this.getIsActive();
        Long $groupId = this.getGroupId();
        result = result * 59 + ($groupId == null ? 43 : ((Object)$groupId).hashCode());
        Long $loginId = this.getLoginId();
        result = result * 59 + ($loginId == null ? 43 : ((Object)$loginId).hashCode());
        String $groupCode = this.getGroupCode();
        result = result * 59 + ($groupCode == null ? 43 : $groupCode.hashCode());
        String $groupDesc = this.getGroupDesc();
        result = result * 59 + ($groupDesc == null ? 43 : $groupDesc.hashCode());
        String $createdBy = this.getCreatedBy();
        result = result * 59 + ($createdBy == null ? 43 : $createdBy.hashCode());
        String $createdDate = this.getCreatedDate();
        result = result * 59 + ($createdDate == null ? 43 : $createdDate.hashCode());
        String $modifiedBy = this.getModifiedBy();
        result = result * 59 + ($modifiedBy == null ? 43 : $modifiedBy.hashCode());
        String $modifiedDate = this.getModifiedDate();
        result = result * 59 + ($modifiedDate == null ? 43 : $modifiedDate.hashCode());
        List<ClientMaster> $clientMaster = this.getClientMaster();
        result = result * 59 + ($clientMaster == null ? 43 : ((Object)$clientMaster).hashCode());
        return result;
    }

    public String toString() {
        return "ClientGroup(groupId=" + this.getGroupId() + ", groupCode=" + this.getGroupCode() + ", groupDesc=" + this.getGroupDesc() + ", loginId=" + this.getLoginId() + ", isActive=" + this.getIsActive() + ", createdBy=" + this.getCreatedBy() + ", createdDate=" + this.getCreatedDate() + ", modifiedBy=" + this.getModifiedBy() + ", modifiedDate=" + this.getModifiedDate() + ", clientMaster=" + this.getClientMaster() + ")";
    }
}

