/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.http.HttpHeaders
 *  org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
 *  org.springframework.stereotype.Service
 */
package com.pg.seo.login;

import com.pg.seo.login.Login;
import com.pg.seo.login.LoginRepository;
import com.pg.seo.login.LoginService;
import com.pg.seo.login.Role;
import com.pg.seo.login.RoleRepository;
import java.util.HashSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginServiceImpl
implements LoginService {
    @Autowired
    private LoginRepository loginRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    private HttpHeaders getHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", token);
        return headers;
    }

    @Override
    public void save(Login login) {
        login.setPassword(this.bCryptPasswordEncoder.encode((CharSequence)login.getPassword()));
        HashSet<Role> test = new HashSet<Role>();
        test.add((Role)this.roleRepository.findAll().get(0));
        login.setRoles(test);
        this.loginRepository.save(login);
    }

    @Override
    public Login findByUsername(String loginName) {
        Login login = this.loginRepository.findByUsername(loginName);
        return login;
    }
}

