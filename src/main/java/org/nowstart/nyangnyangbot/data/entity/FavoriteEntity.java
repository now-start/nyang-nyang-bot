package org.nowstart.nyangnyangbot.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
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
public class FavoriteEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private ChannelEntity ownerChannel;
    @ManyToOne
    private ChannelEntity targetChannel;
    @Setter
    private Integer favorite;

    public String getNickName() {
        return targetChannel == null ? null : targetChannel.getName();
    }

    public void setNickName(String nickName) {
        if (targetChannel == null) {
            return;
        }
        if (nickName != null && !nickName.isBlank()) {
            targetChannel.setName(nickName);
        }
    }
}
