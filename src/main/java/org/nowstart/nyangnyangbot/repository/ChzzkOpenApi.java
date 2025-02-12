package org.nowstart.nyangnyangbot.repository;

import org.nowstart.nyangnyangbot.data.dto.ResponseChannel;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ChzzkOpenApi", url = "https://openapi.chzzk.naver.com")
public interface ChzzkOpenApi {

    @PostMapping("/auth/v1/token")
    void getAccessToken(String authorizationCode, String clientId, String clientSecret, String code, String state);

    @GetMapping("/service/v1/search/channels")
    ResponseChannel getChannelId(@RequestParam String keyword);

    void getUser(String code);

}
