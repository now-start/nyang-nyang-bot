package org.nowstart.nyangnyangbot.adapter.in.web.local;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.config.LocalTestAccounts;
import org.nowstart.nyangnyangbot.config.LocalTestAccounts.Account;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
@Profile("local")
@RequiredArgsConstructor
public class LocalTestLoginController {

    private static final String DEFAULT_REDIRECT = "/points/list";
    private static final String LOGIN_VIEW = "local/test-login";

    @GetMapping("/local/test-login")
    public String login(
            @RequestParam(defaultValue = DEFAULT_REDIRECT) String redirect,
            HttpSession session,
            Model model
    ) {
        model.addAttribute("accounts", LocalTestAccounts.accounts());
        model.addAttribute("currentAccount", currentAccount(session));
        model.addAttribute("redirect", safeRedirect(redirect));
        return LOGIN_VIEW;
    }

    @PostMapping("/local/test-login")
    public String switchAccount(
            @RequestParam String userId,
            @RequestParam(defaultValue = DEFAULT_REDIRECT) String redirect,
            HttpSession session
    ) {
        Account account = LocalTestAccounts.find(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "unknown local test account"));
        session.setAttribute(LocalTestAccounts.SESSION_USER_ID, account.userId());
        session.setAttribute(LocalTestAccounts.SESSION_NICK_NAME, account.nickName());
        session.setAttribute(LocalTestAccounts.SESSION_ADMIN, account.admin());
        SecurityContextHolder.clearContext();
        return "redirect:" + safeRedirect(redirect);
    }

    @PostMapping("/local/test-logout")
    public String clearAccount(HttpSession session) {
        session.removeAttribute(LocalTestAccounts.SESSION_USER_ID);
        session.removeAttribute(LocalTestAccounts.SESSION_NICK_NAME);
        session.removeAttribute(LocalTestAccounts.SESSION_ADMIN);
        SecurityContextHolder.clearContext();
        return "redirect:/local/test-login";
    }

    private Account currentAccount(HttpSession session) {
        Object userId = session.getAttribute(LocalTestAccounts.SESSION_USER_ID);
        if (!(userId instanceof String selectedUserId)) {
            return null;
        }
        return LocalTestAccounts.find(selectedUserId).orElse(null);
    }

    private String safeRedirect(String redirect) {
        if (redirect == null
                || !redirect.startsWith("/")
                || redirect.startsWith("//")
                || redirect.startsWith("/local/test-login")) {
            return DEFAULT_REDIRECT;
        }
        return redirect;
    }
}
