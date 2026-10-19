/*
 * Decompiled with CFR 0.152.
 */
package com.pg.seo.model;

public class EmailTemplateRequest {
    private String templateName;
    private String templateContent;

    public EmailTemplateRequest(String templateName, String templateContent) {
        this.templateName = templateName;
        this.templateContent = templateContent;
    }

    public EmailTemplateRequest() {
    }

    public String getTemplateName() {
        return this.templateName;
    }

    public String getTemplateContent() {
        return this.templateContent;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public void setTemplateContent(String templateContent) {
        this.templateContent = templateContent;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof EmailTemplateRequest)) {
            return false;
        }
        EmailTemplateRequest other = (EmailTemplateRequest)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$templateName = this.getTemplateName();
        String other$templateName = other.getTemplateName();
        if (this$templateName == null ? other$templateName != null : !this$templateName.equals(other$templateName)) {
            return false;
        }
        String this$templateContent = this.getTemplateContent();
        String other$templateContent = other.getTemplateContent();
        return !(this$templateContent == null ? other$templateContent != null : !this$templateContent.equals(other$templateContent));
    }

    protected boolean canEqual(Object other) {
        return other instanceof EmailTemplateRequest;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $templateName = this.getTemplateName();
        result = result * 59 + ($templateName == null ? 43 : $templateName.hashCode());
        String $templateContent = this.getTemplateContent();
        result = result * 59 + ($templateContent == null ? 43 : $templateContent.hashCode());
        return result;
    }

    public String toString() {
        return "EmailTemplateRequest(templateName=" + this.getTemplateName() + ", templateContent=" + this.getTemplateContent() + ")";
    }
}

