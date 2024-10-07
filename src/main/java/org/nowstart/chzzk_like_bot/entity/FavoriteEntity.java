package org.nowstart.chzzk_like_bot.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.Getter;

@Entity
@Getter
public class LikeEntity {

    @Id
    private String userId;
    private String nickName;
    private int favorite;
    private LocalDateTime create;
    private LocalDateTime modify;
}
