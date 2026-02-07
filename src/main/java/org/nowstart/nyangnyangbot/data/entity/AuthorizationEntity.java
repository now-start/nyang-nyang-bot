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
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthorizationEntity extends BaseEntity {

    @Id
    @Setter
    private String channelId;
    @Setter
    private String channelName;
    @Setter
    private String accessToken;
    @Setter
    private String refreshToken;
    @Setter
    private String tokenType;
    @Setter
    private Integer expiresIn;
    @Setter
    private String scope;
    private boolean admin;

}
