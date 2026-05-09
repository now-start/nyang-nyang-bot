package org.nowstart.nyangnyangbot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@Tag(name = "Overlay Page", description = "OBS 오버레이 페이지")
public class OverlayController {

    @Operation(summary = "룰렛 OBS 오버레이")
    @GetMapping("/overlay/roulette")
    public ModelAndView rouletteOverlay() {
        return new ModelAndView("overlay-roulette");
    }
}
