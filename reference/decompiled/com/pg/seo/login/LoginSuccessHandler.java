/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.servlet.http.HttpServletRequest
 *  jakarta.servlet.http.HttpServletResponse
 *  jakarta.servlet.http.HttpSession
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.security.core.Authentication
 *  org.springframework.security.web.authentication.AuthenticationSuccessHandler
 *  org.springframework.stereotype.Component
 */
package com.pg.seo.login;

import com.pg.seo.login.Login;
import com.pg.seo.login.LoginRepository;
import com.pg.seo.login.LoginServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class LoginSuccessHandler
implements AuthenticationSuccessHandler {
    @Autowired
    LoginServiceImpl loginService;
    @Autowired
    LoginRepository loginRepository;

    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        HttpSession ses = request.getSession();
        Login login = new Login();
        login.setUsername(request.getParameter("username"));
        login.setPassword(request.getParameter("password"));
        Object token = null;
        try {
            login = this.loginRepository.findByUsername(request.getParameter("username"));
            ses.setAttribute("loginId", (Object)login.getLoginId());
            ses.setAttribute("Username", (Object)request.getParameter("username"));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        response.sendRedirect("/welcome");
    }
}

