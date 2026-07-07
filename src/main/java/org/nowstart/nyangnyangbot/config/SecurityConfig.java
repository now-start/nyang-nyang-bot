package org.nowstart.nyangnyangbot.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.config.oauth.ChzzkOAuth2AccessTokenResponseClient;
import org.nowstart.nyangnyangbot.config.oauth.ChzzkOAuth2AuthorizationRequestResolver;
import org.nowstart.nyangnyangbot.config.oauth.ChzzkOAuth2UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String OAUTH2_AUTHORIZATION_ENDPOINT = "/oauth2/authorization/chzzk";
    private static final String OAUTH2_SUCCESS_LOCATION = "/favorite/list";
    private final ChzzkOAuth2AuthorizationRequestResolver chzzkOAuth2AuthorizationRequestResolver;
    private final ChzzkOAuth2AccessTokenResponseClient chzzkOAuth2AccessTokenResponseClient;
    private final ChzzkOAuth2UserService chzzkOAuth2UserService;
    @Value("${nyang.local-auth.enabled:false}")
    private boolean localAuthEnabled;
    @Value("${nyang.local-auth.user-id:local-channel}")
    private String localAuthUserId;
    @Value("${nyang.local-auth.admin:true}")
    private boolean localAuthAdmin;
    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                .csrf(csrf -> {
                    if (h2ConsoleEnabled) {
                        csrf.ignoringRequestMatchers("/h2-console/**");
                    }
                })
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/assets/**", "/images/**", "/favicon.ico").permitAll()
                            .requestMatchers("/actuator/**", "/v3/api-docs").permitAll()
                            .requestMatchers("/oauth2/authorization/**", "/login/oauth2/code/**").permitAll()
                            .requestMatchers("/overlay/roulette", "/overlay/roulette/events/**").permitAll();
                    if (h2ConsoleEnabled) {
                        auth.requestMatchers("/h2-console/**").permitAll();
                    }
                    auth.anyRequest().authenticated();
                }).exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint(OAUTH2_AUTHORIZATION_ENDPOINT))
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage(OAUTH2_AUTHORIZATION_ENDPOINT)
                        .authorizationEndpoint(authorization -> authorization
                                .authorizationRequestResolver(chzzkOAuth2AuthorizationRequestResolver)
                        )
                        .tokenEndpoint(token -> token
                                .accessTokenResponseClient(chzzkOAuth2AccessTokenResponseClient)
                        )
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(chzzkOAuth2UserService)
                        )
                        .defaultSuccessUrl(OAUTH2_SUCCESS_LOCATION, true)
                );

        if (h2ConsoleEnabled) {
            http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));
        }

        if (localAuthEnabled) {
            http.addFilterBefore(localAuthenticationFilter(), AnonymousAuthenticationFilter.class);
        }

        return http.build();
    }

    private OncePerRequestFilter localAuthenticationFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    FilterChain filterChain
            ) throws ServletException, IOException {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null || !authentication.isAuthenticated()) {
                    List<GrantedAuthority> authorities = localAuthAdmin
                            ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                            : Collections.emptyList();
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(localAuthUserId, "N/A", authorities)
                    );
                }
                filterChain.doFilter(request, response);
            }
        };
    }
}
