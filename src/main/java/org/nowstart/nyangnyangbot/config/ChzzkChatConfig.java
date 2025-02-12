package org.nowstart.nyangnyangbot.config;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.Cookie;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.data.dto.ChzzkDto;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import xyz.r2turntrue.chzzk4j.Chzzk;
import xyz.r2turntrue.chzzk4j.ChzzkBuilder;
import xyz.r2turntrue.chzzk4j.chat.ChzzkChat;
import xyz.r2turntrue.chzzk4j.naver.Naver;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ChzzkChatConfig {

    private final ChzzkDto chzzkDto;
    private final ChzzkChatListenerConfig chzzkChatListenerConfig;
    private Chzzk chzzk;
    private ChzzkChat chzzkChat;

    @PostConstruct
    public void init() {
        try (Playwright playwright = Playwright.create();
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage()
        ) {
            page.navigate("https://nid.naver.com/nidlogin.login");
            page.getByLabel("아이디 또는 전화번호").fill(chzzkDto.getId());
            page.getByLabel("비밀번호").fill(chzzkDto.getPassword());
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("로그인")).nth(0).click();
            page.waitForSelector("#account > div.MyView-module__my_info___GNmHz > div > button");
            Map<String, String> cookiesMap = new HashMap<>();
            for (Cookie cookie : page.context().cookies()) {
                cookiesMap.put(cookie.name, cookie.value);
            }

            log.info("[Chzzk][INIT] : {}\n {}", cookiesMap.get(Naver.Cookie.NID_AUT.toString()), cookiesMap.get(Naver.Cookie.NID_SES.toString()));
            chzzk = new ChzzkBuilder()
                .withAuthorization(cookiesMap.get(Naver.Cookie.NID_AUT.toString()), cookiesMap.get(Naver.Cookie.NID_SES.toString()))
                .build();
        }
    }

    @Scheduled(fixedDelay = 1000 * 60)
    public void startChat() {
        try {
            if (chzzkChat == null && chzzk.getLiveDetail(chzzkDto.getChannelId()).isOnline()) {
                log.info("[ChzzkChat][START]");
                chzzkChat = chzzk.chat(chzzkDto.getChannelId())
                    .withChatListener(chzzkChatListenerConfig)
                    .build();
                chzzkChat.connectAsync();
            } else if (chzzkChat != null && !chzzk.getLiveDetail(chzzkDto.getChannelId()).isOnline()) {
                log.info("[ChzzkChat][END]");
                chzzkChat.closeAsync();
                chzzkChat = null;
            }
        } catch (IOException e) {
            // ignore
        }
    }
}