package org.nowstart.nyangnyangbot.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.data.dto.CollectorStatusDto;
import org.nowstart.nyangnyangbot.service.ChatCollectionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/collector")
@RequiredArgsConstructor
public class CollectorController {

    private static final String ALLOWED_CHANNEL_ID = "78f1fccfc5bcf0c27b525f57675ea5d2";

    private final ChatCollectionService chatCollectionService;

    @GetMapping
    public String page(@AuthenticationPrincipal OAuth2User principal) {
        assertAuthorized(principal);
        return "collector";
    }

    @GetMapping("/status")
    @ResponseBody
    public CollectorStatusDto status(@AuthenticationPrincipal OAuth2User principal) {
        assertAuthorized(principal);
        return chatCollectionService.status();
    }

    @GetMapping("/nicknames")
    @ResponseBody
    public List<String> nicknames(@AuthenticationPrincipal OAuth2User principal) {
        assertAuthorized(principal);
        return chatCollectionService.getCollectedNicknames();
    }

    @PostMapping("/toggle")
    @ResponseBody
    public ResponseEntity<CollectorStatusDto> toggle(@AuthenticationPrincipal OAuth2User principal) {
        assertAuthorized(principal);
        return ResponseEntity.ok(chatCollectionService.toggle());
    }

    private void assertAuthorized(OAuth2User principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        String channelId = principal.getAttribute("channelId");
        if (!ALLOWED_CHANNEL_ID.equals(channelId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
}
