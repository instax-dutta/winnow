/*
 * Decompiled with CFR 0.152.
 */
package com.pg.seo.model;

public class BaseResponse {
    private String statusCode;
    private String statusMessage;
    private String statusDesc;

    public String getStatusCode() {
        return this.statusCode;
    }

    public String getStatusMessage() {
        return this.statusMessage;
    }

    public String getStatusDesc() {
        return this.statusDesc;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public void setStatusDesc(String statusDesc) {
        this.statusDesc = statusDesc;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof BaseResponse)) {
            return false;
        }
        BaseResponse other = (BaseResponse)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$statusCode = this.getStatusCode();
        String other$statusCode = other.getStatusCode();
        if (this$statusCode == null ? other$statusCode != null : !this$statusCode.equals(other$statusCode)) {
            return false;
        }
        String this$statusMessage = this.getStatusMessage();
        String other$statusMessage = other.getStatusMessage();
        if (this$statusMessage == null ? other$statusMessage != null : !this$statusMessage.equals(other$statusMessage)) {
            return false;
        }
        String this$statusDesc = this.getStatusDesc();
        String other$statusDesc = other.getStatusDesc();
        return !(this$statusDesc == null ? other$statusDesc != null : !this$statusDesc.equals(other$statusDesc));
    }

    protected boolean canEqual(Object other) {
        return other instanceof BaseResponse;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $statusCode = this.getStatusCode();
        result = result * 59 + ($statusCode == null ? 43 : $statusCode.hashCode());
        String $statusMessage = this.getStatusMessage();
        result = result * 59 + ($statusMessage == null ? 43 : $statusMessage.hashCode());
        String $statusDesc = this.getStatusDesc();
        result = result * 59 + ($statusDesc == null ? 43 : $statusDesc.hashCode());
        return result;
    }

    public String toString() {
        return "BaseResponse(statusCode=" + this.getStatusCode() + ", statusMessage=" + this.getStatusMessage() + ", statusDesc=" + this.getStatusDesc() + ")";
    }
}

