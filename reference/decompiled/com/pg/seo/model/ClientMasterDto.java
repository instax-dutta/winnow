/*
 * Decompiled with CFR 0.152.
 */
package com.pg.seo.model;

public class ClientMasterDto {
    private Long clientMasterId;
    private String name;
    private String email;
    private String webLink;
    private String groupCode;

    public ClientMasterDto(Long clientMasterId, String name, String email, String webLink, String groupCode) {
        this.clientMasterId = clientMasterId;
        this.name = name;
        this.email = email;
        this.webLink = webLink;
        this.groupCode = groupCode;
    }

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

    public String getGroupCode() {
        return this.groupCode;
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

    public void setGroupCode(String groupCode) {
        this.groupCode = groupCode;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ClientMasterDto)) {
            return false;
        }
        ClientMasterDto other = (ClientMasterDto)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$clientMasterId = this.getClientMasterId();
        Long other$clientMasterId = other.getClientMasterId();
        if (this$clientMasterId == null ? other$clientMasterId != null : !((Object)this$clientMasterId).equals(other$clientMasterId)) {
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
        String this$groupCode = this.getGroupCode();
        String other$groupCode = other.getGroupCode();
        return !(this$groupCode == null ? other$groupCode != null : !this$groupCode.equals(other$groupCode));
    }

    protected boolean canEqual(Object other) {
        return other instanceof ClientMasterDto;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $clientMasterId = this.getClientMasterId();
        result = result * 59 + ($clientMasterId == null ? 43 : ((Object)$clientMasterId).hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $email = this.getEmail();
        result = result * 59 + ($email == null ? 43 : $email.hashCode());
        String $webLink = this.getWebLink();
        result = result * 59 + ($webLink == null ? 43 : $webLink.hashCode());
        String $groupCode = this.getGroupCode();
        result = result * 59 + ($groupCode == null ? 43 : $groupCode.hashCode());
        return result;
    }

    public String toString() {
        return "ClientMasterDto(clientMasterId=" + this.getClientMasterId() + ", name=" + this.getName() + ", email=" + this.getEmail() + ", webLink=" + this.getWebLink() + ", groupCode=" + this.getGroupCode() + ")";
    }
}

