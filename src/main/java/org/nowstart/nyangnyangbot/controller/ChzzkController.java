package org.nowstart.nyangnyangbot.controller;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import io.socket.client.IO;
import io.socket.client.Socket;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.property.ChzzkProperty;
import org.nowstart.nyangnyangbot.data.type.EventType;
import org.nowstart.nyangnyangbot.service.ChatService;
import org.nowstart.nyangnyangbot.service.SystemService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.r2turntrue.chzzk4j.Chzzk;
import xyz.r2turntrue.chzzk4j.ChzzkBuilder;
import xyz.r2turntrue.chzzk4j.chat.ChzzkChat;
import xyz.r2turntrue.chzzk4j.naver.Naver;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/chzzk")
public class ChzzkController {

    private final ChzzkProperty chzzkProperty;
    private final ChatService chatService;
    private final SystemService systemService;
    private Socket socket;
    private Chzzk chzzk;
    private ChzzkChat chzzkChat;

    // @PostConstruct
    public void init() {
        @Cleanup Playwright playwright = Playwright.create();
        @Cleanup Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
        @Cleanup Page page = browser.newPage();

        page.navigate("https://nid.naver.com/nidlogin.login");
        page.getByLabel("아이디 또는 전화번호").fill(chzzkProperty.getId());
        page.getByLabel("비밀번호").fill(chzzkProperty.getPassword());
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("로그인")).nth(0).click();
        page.waitForSelector("#account > div.MyView-module__my_info___GNmHz > div > button");

        Map<String, String> cookiesMap = page.context().cookies().stream()
            .collect(Collectors.toMap(cookie -> cookie.name, cookie -> cookie.value, (a, b) -> b));

        chzzk = new ChzzkBuilder()
            .withAuthorization(cookiesMap.get(Naver.Cookie.NID_AUT.toString()), cookiesMap.get(Naver.Cookie.NID_SES.toString()))
            .build();
    }

    @SneakyThrows
    @GetMapping("/connect")
    @Scheduled(fixedDelay = 1000 * 60)
    public String connect() {
        try {
            if (chzzkChat == null && socket == null && systemService.isOnline(chzzkProperty.getChannelId())) {
                log.info("[ChzzkChat][START]");
                IO.Options option = new IO.Options();
                option.reconnection = false;

                socket = IO.socket(systemService.getSession(), option);

                socket.on(EventType.SYSTEM.name(), systemService);
                socket.on(EventType.CHAT.name(), chatService);
                socket.connect();
            } else if (socket != null && !systemService.isOnline(chzzkProperty.getChannelId())) {
                log.info("[ChzzkChat][END]");
                socket.disconnect();
                socket = null;
            } else if (chzzkChat != null && !systemService.isOnline(chzzkProperty.getChannelId())) {
                log.info("[ChzzkChat][END][LEGACY]");
                chzzkChat.closeAsync();
                chzzkChat = null;
            }
        } catch (Exception e) {
            log.error("[ChzzkChat][ERROR] : ", e);
            socket = null;
            // legacy();
        }

        return "SUCCESS";
    }

    @Deprecated
    private void legacy() throws IOException {
        if (chzzkChat == null && systemService.isOnline(chzzkProperty.getChannelId())) {
            log.info("[ChzzkChat][START][LEGACY]");
            chzzkChat = chzzk.chat(chzzkProperty.getChannelId())
                .withChatListener(chatService)
                .build();
            chzzkChat.connectAsync();
        }
    }
}
