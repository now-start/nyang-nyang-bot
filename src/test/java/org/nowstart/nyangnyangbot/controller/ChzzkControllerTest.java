package org.nowstart.nyangnyangbot.controller;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.times;

import java.net.URISyntaxException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.service.ChatService;
import org.nowstart.nyangnyangbot.service.SystemService;

@ExtendWith(MockitoExtension.class)
class ChzzkControllerTest {

    @Mock
    private SystemService systemService;

    @Mock
    private ChatService chatService;

    @InjectMocks
    private ChzzkController chzzkController;

    @BeforeEach
    void setUp() {
        given(systemService.getSession()).willReturn("wss://chat.chzzk.naver.com/socket");
    }

    @Test
    void connect_ShouldReturnSuccess_WhenNotConnected() throws URISyntaxException {
        // given
        given(systemService.isConnected()).willReturn(false);

        // when
        String result = chzzkController.connect();

        // then
        then(result).isEqualTo("SUCCESS");
        BDDMockito.then(systemService).should().isConnected();
        BDDMockito.then(systemService).should().getSession();
    }

    @Test
    void connect_ShouldReturnSuccess_WhenAlreadyConnected() throws URISyntaxException {
        // given
        given(systemService.isConnected()).willReturn(true);

        // when
        String result = chzzkController.connect();

        // then
        then(result).isEqualTo("SUCCESS");
        BDDMockito.then(systemService).should().isConnected();
        BDDMockito.then(systemService).should(never()).getSession();
    }

    @Test
    void connect_ShouldCallSystemService_WhenNotConnected() throws URISyntaxException {
        // given
        given(systemService.isConnected()).willReturn(false);

        // when
        chzzkController.connect();

        // then
        BDDMockito.then(systemService).should().isConnected();
        BDDMockito.then(systemService).should().getSession();
    }

    @Test
    void connect_ShouldNotReconnect_WhenAlreadyConnected() throws URISyntaxException {
        // given
        given(systemService.isConnected()).willReturn(true);

        // when
        String result = chzzkController.connect();

        // then
        then(result).isEqualTo("SUCCESS");
        BDDMockito.then(systemService).should(times(1)).isConnected();
        BDDMockito.then(systemService).shouldHaveNoMoreInteractions();
    }

    @Test
    void connect_ShouldHandleMultipleCalls() throws URISyntaxException {
        // given
        given(systemService.isConnected())
                .willReturn(false)
                .willReturn(true);

        // when
        String result1 = chzzkController.connect();
        String result2 = chzzkController.connect();

        // then
        then(result1).isEqualTo("SUCCESS");
        then(result2).isEqualTo("SUCCESS");
        BDDMockito.then(systemService).should(times(2)).isConnected();
        BDDMockito.then(systemService).should(times(1)).getSession();
    }

    @Test
    void connect_ShouldGetNewSession_EachTimeWhenDisconnected() throws URISyntaxException {
        // given
        given(systemService.isConnected()).willReturn(false);

        // when
        chzzkController.connect();
        chzzkController.connect();

        // then
        BDDMockito.then(systemService).should(times(2)).isConnected();
        BDDMockito.then(systemService).should(times(2)).getSession();
    }
}
