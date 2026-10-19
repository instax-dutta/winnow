package com.pg.seo.security;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the login form.
 *
 * <p>The form is a plain Spring Security form login. The processing URL is {@code /login}
 * and the filter chain handles the POST, so this controller only renders the page and the
 * two outcome messages.
 *
 * <p>Both messages are deliberately generic. The failure message never says whether the
 * username exists, whether the account is disabled and whether the account is locked -
 * distinguishing them would let an unauthenticated caller enumerate valid accounts.
 */
@Controller
public class LoginController {

    /** The query parameter the failure handler appends. */
    static final String ERROR_PARAMETER = "error";

    /** The query parameter the logout handler appends. */
    static final String LOGOUT_PARAMETER = "logout";

    @GetMapping("/login")
    public String login(Model model,
                        @org.springframework.web.bind.annotation.RequestParam(
                                name = ERROR_PARAMETER, required = false) String error,
                        @org.springframework.web.bind.annotation.RequestParam(
                                name = LOGOUT_PARAMETER, required = false) String logout) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password.");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been signed out.");
        }
        return "login";
    }
}
