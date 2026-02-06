package org.nowstart.nyangnyangbot.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.data.dto.ApiResponseDto;
import org.nowstart.nyangnyangbot.data.dto.UserDto;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestOperations;

@Service
@RequiredArgsConstructor
public class ChzzkOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final ObjectMapper objectMapper;
    private final RestOperations restOperations;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        String userInfoUri = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUri();
        if (!StringUtils.hasText(userInfoUri)) {
            throw new IllegalStateException("UserInfo URI missing");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(userRequest.getAccessToken().getTokenValue());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restOperations.exchange(userInfoUri, HttpMethod.GET, entity, String.class);
        ApiResponseDto<UserDto> apiResponse = readApiResponse(response.getBody());
        if (apiResponse == null || apiResponse.getContent() == null) {
            throw new IllegalStateException("UserInfo response missing");
        }

        Map<String, Object> attributes = objectMapper.convertValue(
                apiResponse.getContent(),
                new TypeReference<Map<String, Object>>() {
                }
        );
        String userNameAttribute = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        if (!StringUtils.hasText(userNameAttribute)) {
            userNameAttribute = "channelId";
        }

        return new DefaultOAuth2User(AuthorityUtils.createAuthorityList("ROLE_USER"), attributes, userNameAttribute);
    }

    private ApiResponseDto<UserDto> readApiResponse(String body) {
        try {
            return objectMapper.readValue(body, new TypeReference<ApiResponseDto<UserDto>>() {
            });
        } catch (Exception ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }
}






