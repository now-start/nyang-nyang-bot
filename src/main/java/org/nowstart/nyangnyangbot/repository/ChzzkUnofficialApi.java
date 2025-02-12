package org.nowstart.nyangnyangbot.repository;

import org.springframework.cloud.openfeign.FeignClient;

@Deprecated
@FeignClient(name = "ChzzkUnofficialApi", url = "https://api.chzzk.naver.com")
public interface ChzzkUnofficialApi {
}
