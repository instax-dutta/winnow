/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.persistence.Entity
 *  jakarta.persistence.GeneratedValue
 *  jakarta.persistence.GenerationType
 *  jakarta.persistence.Id
 *  jakarta.persistence.Table
 *  org.hibernate.annotations.Where
 */
package com.pg.seo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Where;

@Entity
@Table(name="email_master")
@Where(clause="isActive =1")
public class EmailMaster {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long emailMasterId;
    private String email;
    private String protocol;
    private String host;
    private String username;
    private Integer port;
    private String auth;
    private String enable;
    private String password;
    private Long loginId;
    private int isActive;
    private String createdBy;
    private String createdDate;
    private String modifiedBy;
    private String modifiedDate;

    public Long getEmailMasterId() {
        return this.emailMasterId;
    }

    public String getEmail() {
        return this.email;
    }

    public String getProtocol() {
        return this.protocol;
    }

    public String getHost() {
        return this.host;
    }

    public String getUsername() {
        return this.username;
    }

    public Integer getPort() {
        return this.port;
    }

    public String getAuth() {
        return this.auth;
    }

    public String getEnable() {
        return this.enable;
    }

    public String getPassword() {
        return this.password;
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

    public void setEmailMasterId(Long emailMasterId) {
        this.emailMasterId = emailMasterId;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public void setAuth(String auth) {
        this.auth = auth;
    }

    public void setEnable(String enable) {
        this.enable = enable;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof EmailMaster)) {
            return false;
        }
        EmailMaster other = (EmailMaster)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.getIsActive() != other.getIsActive()) {
            return false;
        }
        Long this$emailMasterId = this.getEmailMasterId();
        Long other$emailMasterId = other.getEmailMasterId();
        if (this$emailMasterId == null ? other$emailMasterId != null : !((Object)this$emailMasterId).equals(other$emailMasterId)) {
            return false;
        }
        Integer this$port = this.getPort();
        Integer other$port = other.getPort();
        if (this$port == null ? other$port != null : !((Object)this$port).equals(other$port)) {
            return false;
        }
        Long this$loginId = this.getLoginId();
        Long other$loginId = other.getLoginId();
        if (this$loginId == null ? other$loginId != null : !((Object)this$loginId).equals(other$loginId)) {
            return false;
        }
        String this$email = this.getEmail();
        String other$email = other.getEmail();
        if (this$email == null ? other$email != null : !this$email.equals(other$email)) {
            return false;
        }
        String this$protocol = this.getProtocol();
        String other$protocol = other.getProtocol();
        if (this$protocol == null ? other$protocol != null : !this$protocol.equals(other$protocol)) {
            return false;
        }
        String this$host = this.getHost();
        String other$host = other.getHost();
        if (this$host == null ? other$host != null : !this$host.equals(other$host)) {
            return false;
        }
        String this$username = this.getUsername();
        String other$username = other.getUsername();
        if (this$username == null ? other$username != null : !this$username.equals(other$username)) {
            return false;
        }
        String this$auth = this.getAuth();
        String other$auth = other.getAuth();
        if (this$auth == null ? other$auth != null : !this$auth.equals(other$auth)) {
            return false;
        }
        String this$enable = this.getEnable();
        String other$enable = other.getEnable();
        if (this$enable == null ? other$enable != null : !this$enable.equals(other$enable)) {
            return false;
        }
        String this$password = this.getPassword();
        String other$password = other.getPassword();
        if (this$password == null ? other$password != null : !this$password.equals(other$password)) {
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
        return !(this$modifiedDate == null ? other$modifiedDate != null : !this$modifiedDate.equals(other$modifiedDate));
    }

    protected boolean canEqual(Object other) {
        return other instanceof EmailMaster;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + this.getIsActive();
        Long $emailMasterId = this.getEmailMasterId();
        result = result * 59 + ($emailMasterId == null ? 43 : ((Object)$emailMasterId).hashCode());
        Integer $port = this.getPort();
        result = result * 59 + ($port == null ? 43 : ((Object)$port).hashCode());
        Long $loginId = this.getLoginId();
        result = result * 59 + ($loginId == null ? 43 : ((Object)$loginId).hashCode());
        String $email = this.getEmail();
        result = result * 59 + ($email == null ? 43 : $email.hashCode());
        String $protocol = this.getProtocol();
        result = result * 59 + ($protocol == null ? 43 : $protocol.hashCode());
        String $host = this.getHost();
        result = result * 59 + ($host == null ? 43 : $host.hashCode());
        String $username = this.getUsername();
        result = result * 59 + ($username == null ? 43 : $username.hashCode());
        String $auth = this.getAuth();
        result = result * 59 + ($auth == null ? 43 : $auth.hashCode());
        String $enable = this.getEnable();
        result = result * 59 + ($enable == null ? 43 : $enable.hashCode());
        String $password = this.getPassword();
        result = result * 59 + ($password == null ? 43 : $password.hashCode());
        String $createdBy = this.getCreatedBy();
        result = result * 59 + ($createdBy == null ? 43 : $createdBy.hashCode());
        String $createdDate = this.getCreatedDate();
        result = result * 59 + ($createdDate == null ? 43 : $createdDate.hashCode());
        String $modifiedBy = this.getModifiedBy();
        result = result * 59 + ($modifiedBy == null ? 43 : $modifiedBy.hashCode());
        String $modifiedDate = this.getModifiedDate();
        result = result * 59 + ($modifiedDate == null ? 43 : $modifiedDate.hashCode());
        return result;
    }

    public String toString() {
        return "EmailMaster(emailMasterId=" + this.getEmailMasterId() + ", email=" + this.getEmail() + ", protocol=" + this.getProtocol() + ", host=" + this.getHost() + ", username=" + this.getUsername() + ", port=" + this.getPort() + ", auth=" + this.getAuth() + ", enable=" + this.getEnable() + ", password=" + this.getPassword() + ", loginId=" + this.getLoginId() + ", isActive=" + this.getIsActive() + ", createdBy=" + this.getCreatedBy() + ", createdDate=" + this.getCreatedDate() + ", modifiedBy=" + this.getModifiedBy() + ", modifiedDate=" + this.getModifiedDate() + ")";
    }
}

