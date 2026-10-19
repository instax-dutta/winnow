/*
 * Decompiled with CFR 0.152.
 */
package com.pg.seo.model;

public class ClientGroupDto {
    private Long groupId;
    private String groupCode;
    private String groupDesc;

    public ClientGroupDto(Long groupId, String groupCode, String groupDesc) {
        this.groupId = groupId;
        this.groupCode = groupCode;
        this.groupDesc = groupDesc;
    }

    public Long getGroupId() {
        return this.groupId;
    }

    public String getGroupCode() {
        return this.groupCode;
    }

    public String getGroupDesc() {
        return this.groupDesc;
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

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ClientGroupDto)) {
            return false;
        }
        ClientGroupDto other = (ClientGroupDto)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$groupId = this.getGroupId();
        Long other$groupId = other.getGroupId();
        if (this$groupId == null ? other$groupId != null : !((Object)this$groupId).equals(other$groupId)) {
            return false;
        }
        String this$groupCode = this.getGroupCode();
        String other$groupCode = other.getGroupCode();
        if (this$groupCode == null ? other$groupCode != null : !this$groupCode.equals(other$groupCode)) {
            return false;
        }
        String this$groupDesc = this.getGroupDesc();
        String other$groupDesc = other.getGroupDesc();
        return !(this$groupDesc == null ? other$groupDesc != null : !this$groupDesc.equals(other$groupDesc));
    }

    protected boolean canEqual(Object other) {
        return other instanceof ClientGroupDto;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $groupId = this.getGroupId();
        result = result * 59 + ($groupId == null ? 43 : ((Object)$groupId).hashCode());
        String $groupCode = this.getGroupCode();
        result = result * 59 + ($groupCode == null ? 43 : $groupCode.hashCode());
        String $groupDesc = this.getGroupDesc();
        result = result * 59 + ($groupDesc == null ? 43 : $groupDesc.hashCode());
        return result;
    }

    public String toString() {
        return "ClientGroupDto(groupId=" + this.getGroupId() + ", groupCode=" + this.getGroupCode() + ", groupDesc=" + this.getGroupDesc() + ")";
    }
}

