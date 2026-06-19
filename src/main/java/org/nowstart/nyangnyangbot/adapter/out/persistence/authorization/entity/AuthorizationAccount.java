package org.nowstart.nyangnyangbot.adapter.out.persistence.authorization.entity;

import org.nowstart.nyangnyangbot.adapter.out.persistence.common.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
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
public class AuthorizationAccount extends BaseEntity {

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
    @Setter
    private LocalDateTime favoriteHistoryLastSeenAt;
    private boolean admin;

}
