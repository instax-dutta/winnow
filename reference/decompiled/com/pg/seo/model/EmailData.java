/*
 * Decompiled with CFR 0.152.
 */
package com.pg.seo.model;

public class EmailData {
    private String toEmail;
    private String domain;
    private String uniqueId;

    public String getToEmail() {
        return this.toEmail;
    }

    public String getDomain() {
        return this.domain;
    }

    public String getUniqueId() {
        return this.uniqueId;
    }

    public void setToEmail(String toEmail) {
        this.toEmail = toEmail;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof EmailData)) {
            return false;
        }
        EmailData other = (EmailData)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$toEmail = this.getToEmail();
        String other$toEmail = other.getToEmail();
        if (this$toEmail == null ? other$toEmail != null : !this$toEmail.equals(other$toEmail)) {
            return false;
        }
        String this$domain = this.getDomain();
        String other$domain = other.getDomain();
        if (this$domain == null ? other$domain != null : !this$domain.equals(other$domain)) {
            return false;
        }
        String this$uniqueId = this.getUniqueId();
        String other$uniqueId = other.getUniqueId();
        return !(this$uniqueId == null ? other$uniqueId != null : !this$uniqueId.equals(other$uniqueId));
    }

    protected boolean canEqual(Object other) {
        return other instanceof EmailData;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $toEmail = this.getToEmail();
        result = result * 59 + ($toEmail == null ? 43 : $toEmail.hashCode());
        String $domain = this.getDomain();
        result = result * 59 + ($domain == null ? 43 : $domain.hashCode());
        String $uniqueId = this.getUniqueId();
        result = result * 59 + ($uniqueId == null ? 43 : $uniqueId.hashCode());
        return result;
    }

    public String toString() {
        return "EmailData(toEmail=" + this.getToEmail() + ", domain=" + this.getDomain() + ", uniqueId=" + this.getUniqueId() + ")";
    }
}

