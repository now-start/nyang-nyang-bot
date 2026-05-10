package org.nowstart.nyangnyangbot.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.auth.OAuthStateService;
import org.nowstart.nyangnyangbot.application.model.AuthorizationAccount;
import org.nowstart.nyangnyangbot.data.property.ChzzkProperty;
import org.nowstart.nyangnyangbot.service.AuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Controller
@RequiredArgsConstructor
@Tag(name = "Authorization API", description = "Chzzk 연동 로그인 및 토큰 관련 API")
public class AuthorizationController {

    public static final String OAUTH_STATE_SESSION_ATTRIBUTE = "CHZZK_OAUTH_STATE";

    private final ChzzkProperty chzzkProperty;
    private final AuthorizationService authorizationService;
    private final OAuthStateService oAuthStateService;

    @Operation(
            summary = "로그인 페이지 리다이렉트",
            description = "사용자를 Chzzk 로그인 페이지로 리다이렉트합니다."
    )
    @GetMapping("/login")
    public String login(HttpSession session) {
        log.info("[GET][/login]");
        String state = oAuthStateService.generateState();
        session.setAttribute(OAUTH_STATE_SESSION_ATTRIBUTE, state);
        return "redirect:https://chzzk.naver.com/account-interlock?"
                + "clientId=" + chzzkProperty.clientId()
                + "&redirectUri=" + URLEncoder.encode(chzzkProperty.redirectUri() + "/token", StandardCharsets.UTF_8)
                + "&state=" + state;
    }

    @Operation(
            summary = "토큰 발급 처리",
            description = "Chzzk에서 전달된 code와 state를 이용해 AccessToken을 발급받고, 즐겨찾기 리스트 페이지로 리다이렉트합니다."
    )
    @GetMapping("/token")
    public String token(String code, String state, HttpSession session) {
        log.info("[GET][/token]");
        String expectedState = (String) session.getAttribute(OAUTH_STATE_SESSION_ATTRIBUTE);
        session.removeAttribute(OAUTH_STATE_SESSION_ATTRIBUTE);
        if (!oAuthStateService.matches(expectedState, state)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid OAuth state");
        }
        AuthorizationAccount authorization = authorizationService.getAccessToken(code, state);
        List<GrantedAuthority> authorities = authorization.admin()
                ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                : Collections.emptyList();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(authorization.channelId(), "N/A", authorities);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return "redirect:" + chzzkProperty.redirectUri() + "/favorite/list";
    }
}
