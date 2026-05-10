package org.nowstart.nyangnyangbot.application.command;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.domain.model.RouletteRound;
import org.nowstart.nyangnyangbot.application.gateway.out.chzzk.ChzzkClientPort;
import org.nowstart.nyangnyangbot.application.gateway.out.roulette.RoulettePort;
import org.nowstart.nyangnyangbot.application.chzzk.dto.ChatDto;
import org.nowstart.nyangnyangbot.application.chzzk.dto.MessageRequestDto;
import org.springframework.stereotype.Service;

@Service("roulette_result")
@Transactional
@RequiredArgsConstructor
public class RouletteResult implements Command {

    private final ChzzkClientPort chzzkClientPort;
    private final RoulettePort roulettePort;

    @Override
    public void run(ChatDto chatDto) {
        List<RouletteRound> rounds = roulettePort.findTopRoundsByUserId(chatDto.senderChannelId(), 5);
        if (rounds.isEmpty()) {
            chzzkClientPort.sendMessage(new MessageRequestDto(
                    chatDto.profile().nickname() + "님의 최근 룰렛 결과가 없습니다."
            ));
            return;
        }
        String result = rounds.stream()
                .map(round -> round.roundNo() + "회차 " + round.itemLabel())
                .collect(Collectors.joining(", "));
        chzzkClientPort.sendMessage(new MessageRequestDto(
                chatDto.profile().nickname() + "님의 최근 룰렛 결과: " + result
        ));
    }
}
