package org.nowstart.nyangnyangbot.data.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthorizationRequestDto {

    private String grantType;
    private String clientId;
    private String clientSecret;
    private String code;
    private String state;
    private String refreshToken;
}
