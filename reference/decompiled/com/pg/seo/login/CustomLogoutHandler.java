/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.servlet.http.HttpServletRequest
 *  jakarta.servlet.http.HttpServletResponse
 *  jakarta.servlet.http.HttpSession
 *  org.springframework.security.core.Authentication
 *  org.springframework.security.web.authentication.logout.LogoutHandler
 */
package com.pg.seo.login;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;

public class CustomLogoutHandler
implements LogoutHandler {
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        HttpSession ses = request.getSession();
    }
}

