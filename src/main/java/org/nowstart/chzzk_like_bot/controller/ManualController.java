package org.nowstart.chzzk_like_bot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.chzzk_like_bot.scheduler.DbSync;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ManualController {

    private final DbSync dbSync;

    @GetMapping("/manual/sync")
    public void sync() {
        log.info("====================[START][DBSync]====================");
        dbSync.syncDatabase();
    }

}
