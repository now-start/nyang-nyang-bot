package org.nowstart.nyangnyangbot.data.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthorizationDto {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private int expiresIn;
    private String scope;
}
