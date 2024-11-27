package org.nowstart.nyangnyangbot.repository;

import org.nowstart.nyangnyangbot.data.dto.ResponseChannel;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Deprecated
@FeignClient(name = "chzzk-api", url = "https://api.chzzk.naver.com")
public interface ChzzkAPI {
    @GetMapping("/service/v1/search/channels")
    ResponseChannel getChannelId(@RequestParam String keyword);
}
