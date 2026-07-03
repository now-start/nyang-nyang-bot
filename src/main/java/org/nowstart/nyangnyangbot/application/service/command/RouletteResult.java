package org.nowstart.nyangnyangbot.application.service.command;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.RoundResult;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.ChatEventPayload;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.MessageCommand;
import org.springframework.stereotype.Service;

@Service("roulette_result")
@Transactional
@RequiredArgsConstructor
public class RouletteResult implements CommandHandler {

    private final ChzzkClientPort chzzkClientPort;
    private final RoulettePort roulettePort;

    @Override
    public void run(ChatEventPayload chat) {
        List<RoundResult> rounds = roulettePort.findTopRoundsByUserId(chat.senderChannelId(), 5);
        if (rounds.isEmpty()) {
            chzzkClientPort.sendMessage(new MessageCommand(
                    chat.profile().nickname() + "님의 최근 룰렛 결과가 없습니다."
            ));
            return;
        }
        String result = rounds.stream()
                .map(round -> round.roundNo() + "회차 " + round.itemLabel())
                .collect(Collectors.joining(", "));
        chzzkClientPort.sendMessage(new MessageCommand(
                chat.profile().nickname() + "님의 최근 룰렛 결과: " + result
        ));
    }
}
