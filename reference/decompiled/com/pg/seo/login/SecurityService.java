/*
 * Decompiled with CFR 0.152.
 */
package com.pg.seo.login;

public interface SecurityService {
    public String findLoggedInUsername();

    public void autologin(String var1, String var2);
}

