package org.nowstart.nyangnyangbot.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

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
                    csrf.ignoringRequestMatchers(PathPatternRequestMatcher.pathPattern(
                            HttpMethod.POST,
                            "/overlay/roulette/events/{displayEventId}/displayed"
                    ));
                    if (h2ConsoleEnabled) {
                        csrf.ignoringRequestMatchers("/h2-console/**");
                    }
                })
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/assets/**", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                            .requestMatchers("/actuator/**", "/v3/api-docs").permitAll()
                            .requestMatchers("/", "/login", "/token").permitAll()
                            .requestMatchers("/overlay/roulette", "/overlay/roulette/events/**").permitAll();
                    if (h2ConsoleEnabled) {
                        auth.requestMatchers("/h2-console/**").permitAll();
                    }
                    auth.anyRequest().authenticated();
                }).exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
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
