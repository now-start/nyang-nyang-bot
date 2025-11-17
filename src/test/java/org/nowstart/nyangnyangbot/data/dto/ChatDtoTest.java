package org.nowstart.nyangnyangbot.data.dto;

import static org.assertj.core.api.BDDAssertions.then;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ChatDtoTest {

    @Test
    void chatDto_ShouldBuildCorrectly() {
        // given
        Map<String, String> emojis = new HashMap<>();
        emojis.put("smile", ":)");

        ChatDto.Profile profile = ChatDto.Profile.builder()
                .nickname("testUser")
                .verifiedMark(true)
                .build();

        // when
        ChatDto chatDto = ChatDto.builder()
                .channelId("channel123")
                .senderChannelId("sender123")
                .profile(profile)
                .content("Hello")
                .emojis(emojis)
                .messageTime(12345678L)
                .build();

        // then
        then(chatDto.getChannelId()).isEqualTo("channel123");
        then(chatDto.getSenderChannelId()).isEqualTo("sender123");
        then(chatDto.getContent()).isEqualTo("Hello");
        then(chatDto.getEmojis()).containsEntry("smile", ":)");
        then(chatDto.getMessageTime()).isEqualTo(12345678L);
    }

    @Test
    void profile_ShouldBuildCorrectly() {
        // when
        ChatDto.Profile profile = ChatDto.Profile.builder()
                .nickname("유저123")
                .verifiedMark(false)
                .badges(List.of(
                        Map.of("name", "badge1"),
                        Map.of("name", "badge2")
                ))
                .build();

        // then
        then(profile.getNickname()).isEqualTo("유저123");
        then(profile.getVerifiedMark()).isFalse();
        then(profile.getBadges()).hasSize(2);
    }

    @Test
    void chatDto_ShouldHandleNullValues() {
        // when
        ChatDto chatDto = ChatDto.builder()
                .channelId(null)
                .content(null)
                .build();

        // then
        then(chatDto.getChannelId()).isNull();
        then(chatDto.getContent()).isNull();
    }

    @Test
    void chatDto_ShouldSupportSetters() {
        // given
        ChatDto chatDto = new ChatDto();

        // when
        chatDto.setChannelId("newChannel");
        chatDto.setContent("newContent");

        // then
        then(chatDto.getChannelId()).isEqualTo("newChannel");
        then(chatDto.getContent()).isEqualTo("newContent");
    }

    @Test
    void profile_ShouldHandleEmptyBadges() {
        // when
        ChatDto.Profile profile = ChatDto.Profile.builder()
                .nickname("user")
                .badges(List.of())
                .build();

        // then
        then(profile.getBadges()).isEmpty();
    }
}
