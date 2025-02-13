package org.nowstart.nyangnyangbot.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.property.ChzzkProperty;
import org.nowstart.nyangnyangbot.service.AuthorizationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/authorization")
public class AuthorizationController {

    private final ChzzkProperty chzzkProperty;
    private final AuthorizationService authorizationService;

    @GetMapping("/code")
    public RedirectView code() {
        log.info("[GET][/code]");
        return new RedirectView("redirect:https://chzzk.naver.com/account-interlock?"
            + "clientId=" + chzzkProperty.getClientId()
            + "&redirectUri=" + URLEncoder.encode(chzzkProperty.getRedirectUri(), StandardCharsets.UTF_8)
            + "&state=zxclDasdfA25");
    }

    @GetMapping("/token")
    public String token(String code, String state) {
        log.info("[GET][/token]");
        authorizationService.getAccessToken(code, state);
        return "SUCCESS";
    }
}
