/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.pg.seo.login;

import com.pg.seo.login.Login;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginRepository
extends JpaRepository<Login, Long> {
    public Login findByUsername(String var1);
}

