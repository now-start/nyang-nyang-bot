package org.nowstart.nyangnyangbot.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.nowstart.nyangnyangbot.data.entity.AuthorizationEntity;
import org.nowstart.nyangnyangbot.repository.AuthorizationRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class ChannelIdAuthenticationFilter extends OncePerRequestFilter {

    public static final String CHANNEL_ID_HEADER = "X-Channel-Id";

    private final AuthorizationRepository authorizationRepository;

    public ChannelIdAuthenticationFilter(AuthorizationRepository authorizationRepository) {
        this.authorizationRepository = authorizationRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String channelId = request.getHeader(CHANNEL_ID_HEADER);
        if (StringUtils.hasText(channelId) && SecurityContextHolder.getContext().getAuthentication() == null) {
            AuthorizationEntity authorizationEntity = authorizationRepository.findById(channelId).orElse(null);
            if (authorizationEntity != null) {
                List<GrantedAuthority> authorities = authorizationEntity.isAdmin()
                        ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        : Collections.emptyList();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(channelId, "N/A", authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
