/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.servlet.http.HttpServletRequest
 *  jakarta.servlet.http.HttpServletResponse
 *  jakarta.servlet.http.HttpSession
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.stereotype.Controller
 *  org.springframework.ui.Model
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestMethod
 */
package com.pg.seo.login;

import com.pg.seo.login.LoginServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class LoginController {
    @Autowired
    LoginServiceImpl loginService;

    @RequestMapping(value={"/login"}, method={RequestMethod.GET})
    public String login(Model model, String error, String logout, HttpServletRequest req, HttpSession ses, HttpServletResponse response) {
        System.out.println("Inside Login");
        if (error != null) {
            model.addAttribute("error", (Object)"Your username and password is invalid.");
        }
        if (logout != null) {
            model.addAttribute("message", (Object)"You have been logged out successfully.");
        }
        return "login";
    }

    @RequestMapping(value={"/", "/welcome"}, method={RequestMethod.GET})
    public String welcome(Model model, HttpServletRequest req, HttpSession ses) throws Exception {
        return "NewDashBoard";
    }

    @RequestMapping(value={"MainDashBoard.htm"}, method={RequestMethod.GET})
    public String MainDashBoard(HttpServletRequest req, HttpSession ses) throws Exception {
        return "MainDashBoard";
    }

    @RequestMapping(value={"/sessionExpired", "/invalidSession"}, method={RequestMethod.GET})
    public String sessionExpired(Model model, HttpServletRequest req, HttpSession ses) {
        return "SessionExp";
    }

    @RequestMapping(value={"/accessdenied"}, method={RequestMethod.GET})
    public String accessdenied(Model model, HttpServletRequest req, HttpSession ses) {
        System.out.println("accessdenied");
        return "accessdenied";
    }
}

