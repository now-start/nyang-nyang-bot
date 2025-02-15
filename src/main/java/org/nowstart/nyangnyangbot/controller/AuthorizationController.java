package org.nowstart.nyangnyangbot.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.property.ChzzkProperty;
import org.nowstart.nyangnyangbot.service.AuthorizationService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/authorization")
public class AuthorizationController {

    private final ChzzkProperty chzzkProperty;
    private final AuthorizationService authorizationService;

    @GetMapping("/login")
    public String login() {
        log.info("[GET][/authorization/login]");
        return "redirect:https://chzzk.naver.com/account-interlock?"
            + "clientId=" + chzzkProperty.getClientId()
            + "&redirectUri=" + URLEncoder.encode(chzzkProperty.getRedirectUri() + "/authorization/token", StandardCharsets.UTF_8)
            + "&state=zxclDasdfA25";
    }

    @GetMapping("/token")
    public String token(String code, String state) {
        log.info("[GET][/authorization/token]");
        authorizationService.getAccessToken(code, state);
        return "redirect:" + chzzkProperty.getRedirectUri() + "/favorite/list";
    }
}
