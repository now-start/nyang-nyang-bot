package org.nowstart.nyangnyangbot.adapter.out.persistence.timer.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;
import org.nowstart.nyangnyangbot.adapter.out.persistence.common.BaseEntity;

@Entity
@DynamicUpdate
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TimerMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Lob
    private String messageTemplate;
    private Integer intervalMinutes;
    private Integer minChatCount;
    private boolean active;
    private LocalDateTime nextRunAt;
    private long chatCountSinceLastSend;
    private long claimedChatCount;
    private String claimToken;
    private LocalDateTime claimExpiresAt;
    private LocalDateTime lastSentAt;
    private String createdBy;
    private String updatedBy;

    public void update(
            String messageTemplate,
            Integer intervalMinutes,
            Integer minChatCount,
            boolean active,
            LocalDateTime nextRunAt,
            boolean resetSchedule,
            String updatedBy
    ) {
        this.messageTemplate = messageTemplate;
        this.intervalMinutes = intervalMinutes;
        this.minChatCount = minChatCount;
        this.active = active;
        this.nextRunAt = nextRunAt;
        this.updatedBy = updatedBy;
        if (resetSchedule) {
            this.chatCountSinceLastSend = 0;
        }
    }
}
