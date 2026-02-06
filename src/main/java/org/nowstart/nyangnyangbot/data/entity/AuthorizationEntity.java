package org.nowstart.nyangnyangbot.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
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
    private Integer expiresIn;
    private String scope;
    private boolean admin;
}