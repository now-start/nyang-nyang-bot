package org.nowstart.nyangnyangbot.adapter.out.persistence.command.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.out.persistence.common.BaseEntity;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Command extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String triggerToken;
    @Lob
    private String messageTemplate;
    private boolean active;
    private Integer userCooldownSeconds;
    private String createdBy;
    private String updatedBy;

    public void update(
            String triggerToken,
            String messageTemplate,
            boolean active,
            Integer userCooldownSeconds,
            String updatedBy
    ) {
        this.triggerToken = triggerToken;
        this.messageTemplate = messageTemplate;
        this.active = active;
        this.userCooldownSeconds = userCooldownSeconds;
        this.updatedBy = updatedBy;
    }
}
