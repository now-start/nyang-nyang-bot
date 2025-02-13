package org.nowstart.nyangnyangbot.repository;

import feign.Headers;
import org.nowstart.nyangnyangbot.data.dto.ApiResponseDto;
import org.nowstart.nyangnyangbot.data.dto.AuthorizationDto;
import org.nowstart.nyangnyangbot.data.dto.AuthorizationRequestDto;
import org.nowstart.nyangnyangbot.data.dto.MessageRequestDto;
import org.nowstart.nyangnyangbot.data.dto.SessionDto;
import org.nowstart.nyangnyangbot.data.dto.UserDto;
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
    @Headers("Content-Type: application/json")
    ApiResponseDto<UserDto> getUser(@RequestHeader("Authorization") String authorization);

    @GetMapping("/open/v1/sessions/auth")
    @Headers("Content-Type: application/json")
    ApiResponseDto<SessionDto> getSession(@RequestHeader("Authorization") String authorization);

    @PostMapping("/open/v1/sessions/events/subscribe/chat")
    @Headers("Content-Type: application/json")
    void subscribeChatEvent(@RequestHeader("Authorization") String authorization,
        @RequestParam("sessionKey") String sessionKey);

    @PostMapping("/open/v1/chats/send")
    @Headers("Content-Type: application/json")
    void sendMessage(@RequestHeader("Authorization") String authorization,
        @RequestBody MessageRequestDto message);

}
