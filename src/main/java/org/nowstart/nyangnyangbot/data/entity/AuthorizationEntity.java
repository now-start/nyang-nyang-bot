package org.nowstart.nyangnyangbot.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.nowstart.nyangnyangbot.data.dto.AuthorizationDto;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationEntity extends BaseEntity {

    @Id
    private String channelId;
    private String channelName;
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private int expiresIn;
    private String scope;

    public void refreshToken(AuthorizationDto authorizationDto) {
        this.accessToken = authorizationDto.getAccessToken();
        this.refreshToken = authorizationDto.getRefreshToken();
        this.tokenType = authorizationDto.getTokenType();
        this.expiresIn = authorizationDto.getExpiresIn();
        this.scope = authorizationDto.getScope();
    }
}