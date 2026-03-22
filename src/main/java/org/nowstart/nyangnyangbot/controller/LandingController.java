package org.nowstart.nyangnyangbot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@Tag(name = "Landing", description = "공개 랜딩 페이지")
public class LandingController {

    @Operation(summary = "랜딩 페이지")
    @GetMapping({"", "/"})
    public ModelAndView landing() {
        ModelAndView modelAndView = new ModelAndView("index");
        modelAndView.addObject("landingMode", true);
        return modelAndView;
    }
}
