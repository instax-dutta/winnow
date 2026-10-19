/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.persistence.Entity
 *  jakarta.persistence.GeneratedValue
 *  jakarta.persistence.GenerationType
 *  jakarta.persistence.Id
 *  jakarta.persistence.JoinColumn
 *  jakarta.persistence.JoinTable
 *  jakarta.persistence.ManyToMany
 *  jakarta.persistence.Table
 */
package com.pg.seo.login;

import com.pg.seo.login.Role;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.Set;

@Entity
@Table(name="login")
public class Login {
    private Long LoginId;
    private String Username;
    private String Password;
    private String EmpId;
    private int IsActive;
    private String CreatedBy;
    private String CreatedDate;
    private String ModifiedBy;
    private String ModifiedDate;
    private Set<Role> roles;

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    public Long getLoginId() {
        return this.LoginId;
    }

    public void setLoginId(Long loginId) {
        this.LoginId = loginId;
    }

    public String getUsername() {
        return this.Username;
    }

    public void setUsername(String username) {
        this.Username = username;
    }

    public String getPassword() {
        return this.Password;
    }

    public void setPassword(String password) {
        this.Password = password;
    }

    public String getEmpId() {
        return this.EmpId;
    }

    public void setEmpId(String empId) {
        this.EmpId = empId;
    }

    @ManyToMany
    @JoinTable(name="login_role_security", joinColumns={@JoinColumn(name="LoginId")}, inverseJoinColumns={@JoinColumn(name="RoleId")})
    public Set<Role> getRoles() {
        return this.roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public int getIsActive() {
        return this.IsActive;
    }

    public void setIsActive(int isActive) {
        this.IsActive = isActive;
    }

    public String getCreatedDate() {
        return this.CreatedDate;
    }

    public void setCreatedDate(String createdDate) {
        this.CreatedDate = createdDate;
    }

    public String getCreatedBy() {
        return this.CreatedBy;
    }

    public void setCreatedBy(String createdBy) {
        this.CreatedBy = createdBy;
    }

    public String getModifiedBy() {
        return this.ModifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.ModifiedBy = modifiedBy;
    }

    public String getModifiedDate() {
        return this.ModifiedDate;
    }

    public void setModifiedDate(String modifiedDate) {
        this.ModifiedDate = modifiedDate;
    }
}

