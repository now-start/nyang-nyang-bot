package org.nowstart.nyangnyangbot.config;

import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.service.ChzzkOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ChzzkOAuth2UserService chzzkOAuth2UserService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/authorization/login", "/connect", "/oauth2/**").permitAll()
                        .requestMatchers("/collector/**").authenticated()
                        .anyRequest().permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(chzzkOAuth2UserService))
                        .defaultSuccessUrl("/collector", true)
                );

        return http.build();
    }
}






