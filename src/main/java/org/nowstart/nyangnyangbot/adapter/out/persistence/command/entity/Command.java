package org.nowstart.nyangnyangbot.adapter.out.persistence.command.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.out.persistence.common.BaseEntity;
import org.nowstart.nyangnyangbot.domain.type.CommandActionKey;
import org.nowstart.nyangnyangbot.domain.type.CommandType;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Command extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    private CommandType type;
    private String triggerToken;
    @Enumerated(EnumType.STRING)
    private CommandActionKey actionKey;
    @Lob
    private String messageTemplate;
    private Integer timerIntervalMinutes;
    private Integer timerMinChatCount;
    private boolean active;
    private String requiredRole;
    private Integer userCooldownSeconds;
    private String createdBy;
    private String updatedBy;

    public void update(
            String triggerToken,
            String messageTemplate,
            Integer timerIntervalMinutes,
            Integer timerMinChatCount,
            boolean active,
            String requiredRole,
            Integer userCooldownSeconds,
            String updatedBy
    ) {
        this.triggerToken = triggerToken;
        this.messageTemplate = messageTemplate;
        this.timerIntervalMinutes = timerIntervalMinutes;
        this.timerMinChatCount = timerMinChatCount;
        this.active = active;
        this.requiredRole = requiredRole;
        this.userCooldownSeconds = userCooldownSeconds;
        this.updatedBy = updatedBy;
    }
}
