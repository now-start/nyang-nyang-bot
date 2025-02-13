package org.nowstart.nyangnyangbot.repository;

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
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ChzzkOpenApi", url = "https://openapi.chzzk.naver.com")
public interface ChzzkOpenApi {

    @PostMapping("/auth/v1/token")
    ApiResponseDto<AuthorizationDto> getAccessToken(@RequestBody AuthorizationRequestDto authorizationRequestDto);

    @GetMapping("/open/v1/users/me")
    ApiResponseDto<UserDto> getUser();

    @GetMapping("/open/v1/sessions/auth")
    ApiResponseDto<SessionDto> getSession();

    @PostMapping("/open/v1/sessions/events/subscribe/chat")
    void subscribeChatEvent(@RequestParam("sessionKey") String sessionKey);

    @PostMapping("/open/v1/chats/send")
    void sendMessage(@RequestBody MessageRequestDto message);

}
