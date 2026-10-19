/*
 * Decompiled with CFR 0.152.
 */
package com.pg.seo.model;

public class EmailExtractDto {
    private String domain;
    private String email;

    public String getDomain() {
        return this.domain;
    }

    public String getEmail() {
        return this.email;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof EmailExtractDto)) {
            return false;
        }
        EmailExtractDto other = (EmailExtractDto)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$domain = this.getDomain();
        String other$domain = other.getDomain();
        if (this$domain == null ? other$domain != null : !this$domain.equals(other$domain)) {
            return false;
        }
        String this$email = this.getEmail();
        String other$email = other.getEmail();
        return !(this$email == null ? other$email != null : !this$email.equals(other$email));
    }

    protected boolean canEqual(Object other) {
        return other instanceof EmailExtractDto;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $domain = this.getDomain();
        result = result * 59 + ($domain == null ? 43 : $domain.hashCode());
        String $email = this.getEmail();
        result = result * 59 + ($email == null ? 43 : $email.hashCode());
        return result;
    }

    public String toString() {
        return "EmailExtractDto(domain=" + this.getDomain() + ", email=" + this.getEmail() + ")";
    }

    public EmailExtractDto(String domain, String email) {
        this.domain = domain;
        this.email = email;
    }

    public EmailExtractDto() {
    }
}

