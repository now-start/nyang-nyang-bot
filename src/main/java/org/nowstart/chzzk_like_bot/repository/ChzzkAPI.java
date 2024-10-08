package org.nowstart.chzzk_like_bot.repository;

import org.nowstart.chzzk_like_bot.dto.ResponseChannel;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "chzzk-api", url = "https://api.chzzk.naver.com")
public interface ChzzkAPI {
    @GetMapping("/service/v1/search/channels")
    ResponseChannel getChannelId(@RequestParam String keyword);
}
