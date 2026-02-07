package org.nowstart.nyangnyangbot.repository;

import org.nowstart.nyangnyangbot.config.Authorization;
import org.nowstart.nyangnyangbot.data.dto.chzzk.ApiResponseDto;
import org.nowstart.nyangnyangbot.data.dto.chzzk.AuthorizationDto;
import org.nowstart.nyangnyangbot.data.dto.chzzk.AuthorizationRequestDto;
import org.nowstart.nyangnyangbot.data.dto.chzzk.MessageRequestDto;
import org.nowstart.nyangnyangbot.data.dto.chzzk.SessionDto;
import org.nowstart.nyangnyangbot.data.dto.chzzk.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ChzzkOpenApi", url = "https://openapi.chzzk.naver.com")
public interface ChzzkOpenApi {

    @PostMapping("/auth/v1/token")
    ApiResponseDto<AuthorizationDto> getAccessToken(@RequestBody AuthorizationRequestDto authorizationRequestDto);

    @GetMapping("/open/v1/users/me")
    ApiResponseDto<UserDto> getUser(@RequestHeader("Authorization") String accessToken);

    @GetMapping("/open/v1/sessions/auth/client")
    ApiResponseDto<SessionDto> getSession(@RequestHeader("Client-Id") String clientID, @RequestHeader("Client-Secret") String clientSecret);

    @GetMapping("/open/v1/sessions/client")
    ApiResponseDto<SessionDto> getSessionList(@RequestHeader("Client-Id") String clientID, @RequestHeader("Client-Secret") String clientSecret);

    @Authorization
    @PostMapping("/open/v1/sessions/events/subscribe/chat")
    void subscribeChatEvent(@RequestParam("sessionKey") String sessionKey);

    @Authorization
    @PostMapping("/open/v1/sessions/events/subscribe/donation")
    void subscribeDonationEvent(@RequestParam("sessionKey") String sessionKey);

    @Authorization
    @PostMapping("/open/v1/sessions/events/subscribe/subscription")
    void subscribeSubscriptionEvent(@RequestParam("sessionKey") String sessionKey);

    @Authorization
    @PostMapping("/open/v1/chats/send")
    void sendMessage(@RequestBody MessageRequestDto message);
}
