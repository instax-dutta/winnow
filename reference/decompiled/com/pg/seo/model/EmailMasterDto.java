/*
 * Decompiled with CFR 0.152.
 */
package com.pg.seo.model;

public class EmailMasterDto {
    private Long emailMasterId;
    private String email;
    private String protocol;
    private String host;
    private String username;
    private Integer port;
    private String auth;
    private String enable;
    private String password;

    public EmailMasterDto(Long emailMasterId, String email, String protocol, String host, String username, Integer port, String auth, String enable, String password) {
        this.emailMasterId = emailMasterId;
        this.email = email;
        this.protocol = protocol;
        this.host = host;
        this.username = username;
        this.port = port;
        this.auth = auth;
        this.enable = enable;
        this.password = password;
    }

    public EmailMasterDto() {
    }

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

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof EmailMasterDto)) {
            return false;
        }
        EmailMasterDto other = (EmailMasterDto)o;
        if (!other.canEqual(this)) {
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
        return !(this$password == null ? other$password != null : !this$password.equals(other$password));
    }

    protected boolean canEqual(Object other) {
        return other instanceof EmailMasterDto;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $emailMasterId = this.getEmailMasterId();
        result = result * 59 + ($emailMasterId == null ? 43 : ((Object)$emailMasterId).hashCode());
        Integer $port = this.getPort();
        result = result * 59 + ($port == null ? 43 : ((Object)$port).hashCode());
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
        return result;
    }

    public String toString() {
        return "EmailMasterDto(emailMasterId=" + this.getEmailMasterId() + ", email=" + this.getEmail() + ", protocol=" + this.getProtocol() + ", host=" + this.getHost() + ", username=" + this.getUsername() + ", port=" + this.getPort() + ", auth=" + this.getAuth() + ", enable=" + this.getEnable() + ", password=" + this.getPassword() + ")";
    }
}

