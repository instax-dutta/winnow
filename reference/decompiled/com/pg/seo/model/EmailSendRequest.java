/*
 * Decompiled with CFR 0.152.
 */
package com.pg.seo.model;

import java.util.Arrays;

public class EmailSendRequest {
    private String fromEmail;
    private String emailType;
    private String group;
    private String[] client;
    private String template;

    public EmailSendRequest(String fromEmail, String emailType, String group, String[] client, String template) {
        this.fromEmail = fromEmail;
        this.emailType = emailType;
        this.group = group;
        this.client = client;
        this.template = template;
    }

    public EmailSendRequest() {
    }

    public String getFromEmail() {
        return this.fromEmail;
    }

    public String getEmailType() {
        return this.emailType;
    }

    public String getGroup() {
        return this.group;
    }

    public String[] getClient() {
        return this.client;
    }

    public String getTemplate() {
        return this.template;
    }

    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }

    public void setEmailType(String emailType) {
        this.emailType = emailType;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setClient(String[] client) {
        this.client = client;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof EmailSendRequest)) {
            return false;
        }
        EmailSendRequest other = (EmailSendRequest)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$fromEmail = this.getFromEmail();
        String other$fromEmail = other.getFromEmail();
        if (this$fromEmail == null ? other$fromEmail != null : !this$fromEmail.equals(other$fromEmail)) {
            return false;
        }
        String this$emailType = this.getEmailType();
        String other$emailType = other.getEmailType();
        if (this$emailType == null ? other$emailType != null : !this$emailType.equals(other$emailType)) {
            return false;
        }
        String this$group = this.getGroup();
        String other$group = other.getGroup();
        if (this$group == null ? other$group != null : !this$group.equals(other$group)) {
            return false;
        }
        if (!Arrays.deepEquals(this.getClient(), other.getClient())) {
            return false;
        }
        String this$template = this.getTemplate();
        String other$template = other.getTemplate();
        return !(this$template == null ? other$template != null : !this$template.equals(other$template));
    }

    protected boolean canEqual(Object other) {
        return other instanceof EmailSendRequest;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $fromEmail = this.getFromEmail();
        result = result * 59 + ($fromEmail == null ? 43 : $fromEmail.hashCode());
        String $emailType = this.getEmailType();
        result = result * 59 + ($emailType == null ? 43 : $emailType.hashCode());
        String $group = this.getGroup();
        result = result * 59 + ($group == null ? 43 : $group.hashCode());
        result = result * 59 + Arrays.deepHashCode(this.getClient());
        String $template = this.getTemplate();
        result = result * 59 + ($template == null ? 43 : $template.hashCode());
        return result;
    }

    public String toString() {
        return "EmailSendRequest(fromEmail=" + this.getFromEmail() + ", emailType=" + this.getEmailType() + ", group=" + this.getGroup() + ", client=" + Arrays.deepToString(this.getClient()) + ", template=" + this.getTemplate() + ")";
    }
}

