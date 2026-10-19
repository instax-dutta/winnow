/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.persistence.Entity
 *  jakarta.persistence.GeneratedValue
 *  jakarta.persistence.GenerationType
 *  jakarta.persistence.Id
 *  jakarta.persistence.ManyToMany
 *  jakarta.persistence.Table
 */
package com.pg.seo.login;

import com.pg.seo.login.Login;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.Set;

@Entity
@Table(name="role_security")
public class Role {
    private Long RoleId;
    private String RoleName;
    private Set<Login> Login;

    public String getRoleName() {
        return this.RoleName;
    }

    public void setRoleName(String roleName) {
        this.RoleName = roleName;
    }

    @ManyToMany(mappedBy="roles")
    public Set<Login> getLogin() {
        return this.Login;
    }

    public void setLogin(Set<Login> login) {
        this.Login = login;
    }

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    public Long getRoleId() {
        return this.RoleId;
    }

    public void setRoleId(Long roleId) {
        this.RoleId = roleId;
    }
}

