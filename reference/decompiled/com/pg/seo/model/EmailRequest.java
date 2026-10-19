/*
 * Decompiled with CFR 0.152.
 */
package com.pg.seo.model;

import com.pg.seo.model.EmailData;
import java.time.LocalDateTime;
import java.util.List;

public class EmailRequest {
    private String fromEmail;
    private String draftSubject;
    private List<EmailData> emails;
    private LocalDateTime scheduleTime;

    public String getFromEmail() {
        return this.fromEmail;
    }

    public String getDraftSubject() {
        return this.draftSubject;
    }

    public List<EmailData> getEmails() {
        return this.emails;
    }

    public LocalDateTime getScheduleTime() {
        return this.scheduleTime;
    }

    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }

    public void setDraftSubject(String draftSubject) {
        this.draftSubject = draftSubject;
    }

    public void setEmails(List<EmailData> emails) {
        this.emails = emails;
    }

    public void setScheduleTime(LocalDateTime scheduleTime) {
        this.scheduleTime = scheduleTime;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof EmailRequest)) {
            return false;
        }
        EmailRequest other = (EmailRequest)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$fromEmail = this.getFromEmail();
        String other$fromEmail = other.getFromEmail();
        if (this$fromEmail == null ? other$fromEmail != null : !this$fromEmail.equals(other$fromEmail)) {
            return false;
        }
        String this$draftSubject = this.getDraftSubject();
        String other$draftSubject = other.getDraftSubject();
        if (this$draftSubject == null ? other$draftSubject != null : !this$draftSubject.equals(other$draftSubject)) {
            return false;
        }
        List<EmailData> this$emails = this.getEmails();
        List<EmailData> other$emails = other.getEmails();
        if (this$emails == null ? other$emails != null : !((Object)this$emails).equals(other$emails)) {
            return false;
        }
        LocalDateTime this$scheduleTime = this.getScheduleTime();
        LocalDateTime other$scheduleTime = other.getScheduleTime();
        return !(this$scheduleTime == null ? other$scheduleTime != null : !((Object)this$scheduleTime).equals(other$scheduleTime));
    }

    protected boolean canEqual(Object other) {
        return other instanceof EmailRequest;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $fromEmail = this.getFromEmail();
        result = result * 59 + ($fromEmail == null ? 43 : $fromEmail.hashCode());
        String $draftSubject = this.getDraftSubject();
        result = result * 59 + ($draftSubject == null ? 43 : $draftSubject.hashCode());
        List<EmailData> $emails = this.getEmails();
        result = result * 59 + ($emails == null ? 43 : ((Object)$emails).hashCode());
        LocalDateTime $scheduleTime = this.getScheduleTime();
        result = result * 59 + ($scheduleTime == null ? 43 : ((Object)$scheduleTime).hashCode());
        return result;
    }

    public String toString() {
        return "EmailRequest(fromEmail=" + this.getFromEmail() + ", draftSubject=" + this.getDraftSubject() + ", emails=" + this.getEmails() + ", scheduleTime=" + this.getScheduleTime() + ")";
    }
}

