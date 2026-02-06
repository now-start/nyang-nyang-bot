package org.nowstart.nyangnyangbot.service;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.data.dto.ApiResponseDto;
import org.nowstart.nyangnyangbot.data.dto.SessionDto;
import org.nowstart.nyangnyangbot.data.dto.SystemDto;
import org.nowstart.nyangnyangbot.data.property.ChzzkProperty;
import org.nowstart.nyangnyangbot.repository.ChzzkOpenApi;

@ExtendWith(MockitoExtension.class)
class SystemServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ChzzkProperty chzzkProperty;

    @Mock
    private ChzzkOpenApi chzzkOpenApi;

    @InjectMocks
    private SystemService systemService;

    private SystemDto connectedSystemDto;
    private String jsonString;

    @BeforeEach
    void setUp() {
        SystemDto.SystemData systemData = SystemDto.SystemData.builder()
                .sessionKey("session123")
                .eventType("chat")
                .channelId("channel123")
                .build();

        connectedSystemDto = SystemDto.builder()
                .type("connected")
                .data(systemData)
                .build();

        jsonString = "{\"type\":\"connected\",\"data\":{\"sessionKey\":\"session123\"}}";
    }

    @Test
    void call_ShouldSetSessionKeyAndSubscribe_WhenConnected() throws Exception {
        // given
        given(objectMapper.readValue(jsonString, SystemDto.class)).willReturn(connectedSystemDto);

        // when
        systemService.call(jsonString);

        // then
        BDDMockito.then(objectMapper).should().readValue(jsonString, SystemDto.class);
        BDDMockito.then(chzzkOpenApi).should().subscribeChatEvent("session123");
    }

    @Test
    void call_ShouldNotSubscribe_WhenNotConnected() throws Exception {
        // given
        SystemDto disconnectedDto = SystemDto.builder()
                .type("disconnected")
                .data(SystemDto.SystemData.builder().sessionKey("session456").build())
                .build();

        String disconnectedJson = "{\"type\":\"disconnected\"}";
        given(objectMapper.readValue(disconnectedJson, SystemDto.class)).willReturn(disconnectedDto);

        // when
        systemService.call(disconnectedJson);

        // then
        BDDMockito.then(objectMapper).should().readValue(disconnectedJson, SystemDto.class);
        BDDMockito.then(chzzkOpenApi).should(never()).subscribeChatEvent(anyString());
    }

    @Test
    void isConnected_ShouldReturnFalse_WhenSessionKeyIsNull() {
        // when
        boolean result = systemService.isConnected();

        // then
        then(result).isFalse();
        BDDMockito.then(chzzkOpenApi).should(never()).getSessionList(anyString(), anyString());
    }

    @Test
    void isConnected_ShouldReturnTrue_WhenSessionExistsAndNotDisconnected() throws Exception {
        // given
        given(chzzkProperty.getClientId()).willReturn("testClientId");
        given(chzzkProperty.getClientSecret()).willReturn("testClientSecret");
        given(objectMapper.readValue(jsonString, SystemDto.class)).willReturn(connectedSystemDto);
        systemService.call(jsonString);

        SessionDto.SessionData.SubscribedEvents subscribedEvent = SessionDto.SessionData.SubscribedEvents.builder()
                .eventType("chat")
                .channelId("channel123")
                .build();

        SessionDto.SessionData activeSession = SessionDto.SessionData.builder()
                .sessionKey("session123")
                .connectedDate("2025-01-01T00:00:00")
                .disconnectedDate(null)
                .subscribedEvents(Collections.singletonList(subscribedEvent))
                .build();

        SessionDto sessionDto = SessionDto.builder()
                .data(Collections.singletonList(activeSession))
                .build();

        ApiResponseDto<SessionDto> response = ApiResponseDto.<SessionDto>builder().build();
        response.setContent(sessionDto);

        given(chzzkOpenApi.getSessionList("testClientId", "testClientSecret")).willReturn(response);

        // when
        boolean result = systemService.isConnected();

        // then
        then(result).isTrue();
        BDDMockito.then(chzzkOpenApi).should().getSessionList("testClientId", "testClientSecret");
    }

    @Test
    void isConnected_ShouldReturnFalse_WhenSessionIsDisconnected() throws Exception {
        // given
        given(chzzkProperty.getClientId()).willReturn("testClientId");
        given(chzzkProperty.getClientSecret()).willReturn("testClientSecret");
        given(objectMapper.readValue(jsonString, SystemDto.class)).willReturn(connectedSystemDto);
        systemService.call(jsonString);

        SessionDto.SessionData disconnectedSession = SessionDto.SessionData.builder()
                .sessionKey("session123")
                .connectedDate("2025-01-01T00:00:00")
                .disconnectedDate("2025-01-01T01:00:00")
                .build();

        SessionDto sessionDto = SessionDto.builder()
                .data(Collections.singletonList(disconnectedSession))
                .build();

        ApiResponseDto<SessionDto> response = ApiResponseDto.<SessionDto>builder().build();
        response.setContent(sessionDto);

        given(chzzkOpenApi.getSessionList("testClientId", "testClientSecret")).willReturn(response);

        // when
        boolean result = systemService.isConnected();

        // then
        then(result).isFalse();
    }

    @Test
    void isConnected_ShouldReturnFalse_WhenSessionKeyNotFound() throws Exception {
        // given
        given(chzzkProperty.getClientId()).willReturn("testClientId");
        given(chzzkProperty.getClientSecret()).willReturn("testClientSecret");
        given(objectMapper.readValue(jsonString, SystemDto.class)).willReturn(connectedSystemDto);
        systemService.call(jsonString);

        SessionDto.SessionData otherSession = SessionDto.SessionData.builder()
                .sessionKey("differentSession")
                .disconnectedDate(null)
                .build();

        SessionDto sessionDto = SessionDto.builder()
                .data(Collections.singletonList(otherSession))
                .build();

        ApiResponseDto<SessionDto> response = ApiResponseDto.<SessionDto>builder().build();
        response.setContent(sessionDto);

        given(chzzkOpenApi.getSessionList("testClientId", "testClientSecret")).willReturn(response);

        // when
        boolean result = systemService.isConnected();

        // then
        then(result).isFalse();
    }

    @Test
    void getSession_ShouldReturnSessionUrl() {
        // given
        given(chzzkProperty.getClientId()).willReturn("testClientId");
        given(chzzkProperty.getClientSecret()).willReturn("testClientSecret");
        SessionDto sessionDto = SessionDto.builder()
                .url("wss://chat.chzzk.naver.com/socket")
                .build();

        ApiResponseDto<SessionDto> response = ApiResponseDto.<SessionDto>builder().build();
        response.setContent(sessionDto);

        given(chzzkOpenApi.getSession("testClientId", "testClientSecret")).willReturn(response);

        // when
        String result = systemService.getSession();

        // then
        then(result).isEqualTo("wss://chat.chzzk.naver.com/socket");
        BDDMockito.then(chzzkOpenApi).should().getSession("testClientId", "testClientSecret");
    }

    @Test
    void call_ShouldHandleDifferentEventTypes() throws Exception {
        // given
        SystemDto errorDto = SystemDto.builder()
                .type("error")
                .data(SystemDto.SystemData.builder().sessionKey("session789").build())
                .build();

        String errorJson = "{\"type\":\"error\"}";
        given(objectMapper.readValue(errorJson, SystemDto.class)).willReturn(errorDto);

        // when
        systemService.call(errorJson);

        // then
        BDDMockito.then(objectMapper).should().readValue(errorJson, SystemDto.class);
        BDDMockito.then(chzzkOpenApi).should(never()).subscribeChatEvent(anyString());
    }

    @Test
    void isConnected_ShouldHandleMultipleSessions() throws Exception {
        // given
        given(chzzkProperty.getClientId()).willReturn("testClientId");
        given(chzzkProperty.getClientSecret()).willReturn("testClientSecret");
        given(objectMapper.readValue(jsonString, SystemDto.class)).willReturn(connectedSystemDto);
        systemService.call(jsonString);

        List<SessionDto.SessionData> sessions = Arrays.asList(
                SessionDto.SessionData.builder()
                        .sessionKey("other1")
                        .disconnectedDate(null)
                        .build(),
                SessionDto.SessionData.builder()
                        .sessionKey("session123")
                        .disconnectedDate(null)
                        .build(),
                SessionDto.SessionData.builder()
                        .sessionKey("other2")
                        .disconnectedDate("2025-01-01T00:00:00")
                        .build()
        );

        SessionDto sessionDto = SessionDto.builder()
                .data(sessions)
                .build();

        ApiResponseDto<SessionDto> response = ApiResponseDto.<SessionDto>builder().build();
        response.setContent(sessionDto);

        given(chzzkOpenApi.getSessionList("testClientId", "testClientSecret")).willReturn(response);

        // when
        boolean result = systemService.isConnected();

        // then
        then(result).isTrue();
    }
}






