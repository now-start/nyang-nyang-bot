package org.nowstart.nyangnyangbot.adapter.in.web.root;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Tag(name = "Root", description = "기본 진입 페이지")
public class RootController {

    @Operation(summary = "기본 페이지")
    @GetMapping({"", "/"})
    public String index() {
        return "redirect:/favorite/list";
    }
}
