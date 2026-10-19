/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.auth0.jwt.JWT
 *  com.auth0.jwt.interfaces.DecodedJWT
 *  io.jsonwebtoken.ExpiredJwtException
 *  io.jsonwebtoken.MalformedJwtException
 *  io.jsonwebtoken.UnsupportedJwtException
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.core.env.Environment
 *  org.springframework.security.core.authority.SimpleGrantedAuthority
 *  org.springframework.security.core.userdetails.User
 *  org.springframework.security.core.userdetails.UserDetails
 *  org.springframework.security.core.userdetails.UserDetailsService
 *  org.springframework.security.core.userdetails.UsernameNotFoundException
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.pg.seo.login;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.pg.seo.login.Login;
import com.pg.seo.login.LoginRepository;
import com.pg.seo.login.Role;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginDetailsServiceImpl
implements UserDetailsService {
    @Autowired
    private Environment env;
    @Autowired
    private LoginRepository loginRepository;

    @Transactional(readOnly=false)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            InetAddress address = InetAddress.getLocalHost();
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(address);
            byte[] mac = networkInterface.getHardwareAddress();
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < mac.length; ++i) {
                stringBuilder.append(String.format("%02X%s", mac[i], i < mac.length - 1 ? "-" : ""));
            }
            System.out.print("MAC address  : " + stringBuilder.toString());
            if (!Objects.requireNonNull(this.env.getProperty("mac")).equalsIgnoreCase(stringBuilder.toString())) {
                throw new UsernameNotFoundException("License expired license renewal contact administrator");
            }
            DecodedJWT jwt = JWT.decode((String)Objects.requireNonNull(this.env.getProperty("license")));
            if (jwt.getExpiresAt().before(new Date())) {
                throw new UsernameNotFoundException("License expired license renewal contact administrator");
            }
        }
        catch (MalformedJwtException | UnsupportedJwtException | IllegalArgumentException ex) {
            ex.printStackTrace();
            throw new UsernameNotFoundException("License expired license renewal contact administrator");
        }
        catch (ExpiredJwtException | UnknownHostException ex) {
            throw new UsernameNotFoundException("License expired license renewal contact administrator");
        }
        catch (SocketException e) {
            throw new RuntimeException(e);
        }
        Login login = this.loginRepository.findByUsername(username);
        if (login != null && login.getIsActive() == 1) {
            HashSet<SimpleGrantedAuthority> grantedAuthorities = new HashSet<SimpleGrantedAuthority>();
            for (Role role : login.getRoles()) {
                grantedAuthorities.add(new SimpleGrantedAuthority(role.getRoleName()));
            }
            return new User(login.getUsername(), login.getPassword(), grantedAuthorities);
        }
        throw new UsernameNotFoundException("username not found");
    }
}

