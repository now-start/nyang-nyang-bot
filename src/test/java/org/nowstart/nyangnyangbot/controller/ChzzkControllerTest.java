package org.nowstart.nyangnyangbot.controller;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.never;

import java.net.URISyntaxException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.service.ChzzkConnectionService;
import org.nowstart.nyangnyangbot.service.LeaderElectionService;

@ExtendWith(MockitoExtension.class)
class ChzzkControllerTest {

    @Mock
    private ChzzkConnectionService connectionService;

    @Mock
    private LeaderElectionService leaderElectionService;

    @InjectMocks
    private ChzzkController chzzkController;

    @Test
    void connect_ShouldReturnSuccess_WhenLeader() throws URISyntaxException {
        // given
        BDDMockito.given(leaderElectionService.isLeader()).willReturn(true);

        // when
        String result = chzzkController.connect();

        // then
        then(result).isEqualTo("SUCCESS");
        BDDMockito.then(connectionService).should().connectIfNeeded();
    }

    @Test
    void connect_ShouldReturnSuccess_WhenNotLeader() throws URISyntaxException {
        // given
        BDDMockito.given(leaderElectionService.isLeader()).willReturn(false);

        // when
        String result = chzzkController.connect();

        // then
        then(result).isEqualTo("SUCCESS");
        BDDMockito.then(connectionService).should(never()).connectIfNeeded();
    }
}






