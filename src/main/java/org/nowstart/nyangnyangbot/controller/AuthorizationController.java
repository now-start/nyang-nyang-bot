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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

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

    @ResponseBody
    @GetMapping("/token")
    public String token(String code, String state) {
        log.info("[GET][/token]");
        authorizationService.getAccessToken(code, state);
        return "성공";
    }
}
