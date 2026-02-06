package org.nowstart.nyangnyangbot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ConnectController {

    @GetMapping("/connect")
    public String connectLogin() {
        log.info("[GET][/connect]");
        return "redirect:/oauth2/authorization/chzzk";
    }
}
