package org.nowstart.nyangnyangbot.config.oauth;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class ChzzkOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    public static final String CHANNEL_ID_PARAMETER = "channel_id";
    public static final String CHANNEL_NAME_PARAMETER = "channel_name";
    public static final String ADMIN_PARAMETER = "admin";
    public static final String CHANNEL_ID_ATTRIBUTE = "channelId";
    private static final String CHANNEL_NAME_ATTRIBUTE = "channelName";
    private static final String ADMIN_ATTRIBUTE = "admin";

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        Map<String, Object> additionalParameters = userRequest.getAdditionalParameters();
        boolean admin = Boolean.TRUE.equals(additionalParameters.get(ADMIN_PARAMETER));

        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put(CHANNEL_ID_ATTRIBUTE, additionalParameters.get(CHANNEL_ID_PARAMETER));
        attributes.put(CHANNEL_NAME_ATTRIBUTE, additionalParameters.get(CHANNEL_NAME_PARAMETER));
        attributes.put(ADMIN_ATTRIBUTE, admin);

        List<GrantedAuthority> authorities = admin
                ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                : List.of();
        return new DefaultOAuth2User(authorities, attributes, CHANNEL_ID_ATTRIBUTE);
    }
}
