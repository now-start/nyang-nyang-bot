package org.nowstart.nyangnyangbot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.entity.AuthorizationEntity;
import org.nowstart.nyangnyangbot.data.property.ChzzkProperty;
import org.nowstart.nyangnyangbot.service.AuthorizationService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
@RequiredArgsConstructor
@Tag(name = "Authorization API", description = "Chzzk 연동 로그인 및 토큰 관련 API")
public class AuthorizationController {

    private final ChzzkProperty chzzkProperty;
    private final AuthorizationService authorizationService;

    @Operation(
            summary = "로그인 페이지 리다이렉트",
            description = "사용자를 Chzzk 로그인 페이지로 리다이렉트합니다."
    )
    @GetMapping("/login")
    public String login() {
        log.info("[GET][/authorization/login]");
        return "redirect:https://chzzk.naver.com/account-interlock?"
                + "clientId=" + chzzkProperty.getClientId()
                + "&redirectUri=" + URLEncoder.encode(chzzkProperty.getRedirectUri() + "/token", StandardCharsets.UTF_8)
                + "&state=zxclDasdfA25";
    }

    @Operation(
            summary = "토큰 발급 처리",
            description = "Chzzk에서 전달된 code와 state를 이용해 AccessToken을 발급받고, 즐겨찾기 리스트 페이지로 리다이렉트합니다."
    )
    @GetMapping("/token")
    public String token(String code, String state, HttpSession session) {
        log.info("[GET][/authorization/token]");
        AuthorizationEntity authorizationEntity = authorizationService.getAccessToken(code, state);
        List<GrantedAuthority> authorities = authorizationEntity.isAdmin()
                ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                : Collections.emptyList();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(authorizationEntity.getChannelId(), "N/A", authorities);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return "redirect:" + chzzkProperty.getRedirectUri() + "/favorite/list";
    }
}
