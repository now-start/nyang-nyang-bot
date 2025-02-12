package org.nowstart.nyangnyangbot.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.dto.ChzzkDto;
import org.nowstart.nyangnyangbot.service.AuthorizationService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/authorization")
public class AuthorizationController {

    private final ChzzkDto chzzkDto;
    private final AuthorizationService authorizationService;

    @GetMapping("/code")
    public String code(HttpServletRequest request) {
        log.info("[GET][/code]");
        return "redirect:https://chzzk.naver.com/account-interlock?"
            + "clientId=" + chzzkDto.getClientId()
            + "&redirectUri=" + URLEncoder.encode("http://" + request.getHeader("Host") + "/authorization/token", StandardCharsets.UTF_8)
            + "&state=zxclDasdfA25";
    }

    @GetMapping("/token")
    public void token(String code, String state) {
        log.info("[GET][/token]");
        authorizationService.getAccessToken(chzzkDto, code, state);
    }
}
