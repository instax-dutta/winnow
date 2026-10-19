/*
 * Decompiled with CFR 0.152.
 */
package com.pg.seo.model;

import com.pg.seo.model.BaseResponse;

public class DashboardDbReport
extends BaseResponse {
    private Integer emailCount;
    private Integer groupCount;
    private Integer clientCount;
    private Integer templateCount;

    public Integer getEmailCount() {
        return this.emailCount;
    }

    public Integer getGroupCount() {
        return this.groupCount;
    }

    public Integer getClientCount() {
        return this.clientCount;
    }

    public Integer getTemplateCount() {
        return this.templateCount;
    }

    public void setEmailCount(Integer emailCount) {
        this.emailCount = emailCount;
    }

    public void setGroupCount(Integer groupCount) {
        this.groupCount = groupCount;
    }

    public void setClientCount(Integer clientCount) {
        this.clientCount = clientCount;
    }

    public void setTemplateCount(Integer templateCount) {
        this.templateCount = templateCount;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof DashboardDbReport)) {
            return false;
        }
        DashboardDbReport other = (DashboardDbReport)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Integer this$emailCount = this.getEmailCount();
        Integer other$emailCount = other.getEmailCount();
        if (this$emailCount == null ? other$emailCount != null : !((Object)this$emailCount).equals(other$emailCount)) {
            return false;
        }
        Integer this$groupCount = this.getGroupCount();
        Integer other$groupCount = other.getGroupCount();
        if (this$groupCount == null ? other$groupCount != null : !((Object)this$groupCount).equals(other$groupCount)) {
            return false;
        }
        Integer this$clientCount = this.getClientCount();
        Integer other$clientCount = other.getClientCount();
        if (this$clientCount == null ? other$clientCount != null : !((Object)this$clientCount).equals(other$clientCount)) {
            return false;
        }
        Integer this$templateCount = this.getTemplateCount();
        Integer other$templateCount = other.getTemplateCount();
        return !(this$templateCount == null ? other$templateCount != null : !((Object)this$templateCount).equals(other$templateCount));
    }

    @Override
    protected boolean canEqual(Object other) {
        return other instanceof DashboardDbReport;
    }

    @Override
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $emailCount = this.getEmailCount();
        result = result * 59 + ($emailCount == null ? 43 : ((Object)$emailCount).hashCode());
        Integer $groupCount = this.getGroupCount();
        result = result * 59 + ($groupCount == null ? 43 : ((Object)$groupCount).hashCode());
        Integer $clientCount = this.getClientCount();
        result = result * 59 + ($clientCount == null ? 43 : ((Object)$clientCount).hashCode());
        Integer $templateCount = this.getTemplateCount();
        result = result * 59 + ($templateCount == null ? 43 : ((Object)$templateCount).hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "DashboardDbReport(emailCount=" + this.getEmailCount() + ", groupCount=" + this.getGroupCount() + ", clientCount=" + this.getClientCount() + ", templateCount=" + this.getTemplateCount() + ")";
    }

    public DashboardDbReport(Integer emailCount, Integer groupCount, Integer clientCount, Integer templateCount) {
        this.emailCount = emailCount;
        this.groupCount = groupCount;
        this.clientCount = clientCount;
        this.templateCount = templateCount;
    }

    public DashboardDbReport() {
    }
}

