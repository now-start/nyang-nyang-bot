package org.nowstart.nyangnyangbot.adapter.out.external.chzzk.client;

import org.nowstart.nyangnyangbot.adapter.out.external.chzzk.request.AuthorizationRequest;
import org.nowstart.nyangnyangbot.adapter.out.external.chzzk.request.MessageRequest;
import org.nowstart.nyangnyangbot.adapter.out.external.chzzk.response.AuthorizationResponse;
import org.nowstart.nyangnyangbot.adapter.out.external.chzzk.response.ChzzkApiResponse;
import org.nowstart.nyangnyangbot.adapter.out.external.chzzk.response.SessionResponse;
import org.nowstart.nyangnyangbot.adapter.out.external.chzzk.response.UserResponse;
import org.nowstart.nyangnyangbot.config.Authorization;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ChzzkOpenApiClient", url = "https://openapi.chzzk.naver.com")
public interface ChzzkOpenApiClient {

    @PostMapping("/auth/v1/token")
    ChzzkApiResponse<AuthorizationResponse> getAccessToken(@RequestBody AuthorizationRequest request);

    @GetMapping("/open/v1/users/me")
    ChzzkApiResponse<UserResponse> getUser(@RequestHeader("Authorization") String accessToken);

    @GetMapping("/open/v1/sessions/auth/client")
    ChzzkApiResponse<SessionResponse> getSession(
            @RequestHeader("Client-Id") String clientID,
            @RequestHeader("Client-Secret") String clientSecret
    );

    @GetMapping("/open/v1/sessions/client")
    ChzzkApiResponse<SessionResponse> getSessionList(
            @RequestHeader("Client-Id") String clientID,
            @RequestHeader("Client-Secret") String clientSecret
    );

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
    void sendMessage(@RequestBody MessageRequest message);
}
