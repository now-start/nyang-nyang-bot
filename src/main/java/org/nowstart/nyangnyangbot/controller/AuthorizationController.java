package org.nowstart.nyangnyangbot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/authorization")
@Tag(name = "Authorization API", description = "Chzzk OAuth2 login entry")
public class AuthorizationController {

    @Operation(
            summary = "OAuth2 login entry",
            description = "Redirects to OAuth2 authorization endpoint."
    )
    @GetMapping("/login")
    public String login() {
        log.info("[GET][/authorization/login]");
        return "redirect:/oauth2/authorization/chzzk";
    }
}
