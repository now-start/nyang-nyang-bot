package org.nowstart.nyangnyangbot.repository;

import org.nowstart.nyangnyangbot.data.dto.ApiResponseDto;
import org.nowstart.nyangnyangbot.data.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Deprecated
@FeignClient(name = "ChzzkUnofficialApi", url = "https://api.chzzk.naver.com")
public interface ChzzkUnofficialApi {

    @GetMapping("/service/v2/channels/{channelId}/live-detail")
    ApiResponseDto<UserDto> isOnline(@PathVariable String channelId);
}
