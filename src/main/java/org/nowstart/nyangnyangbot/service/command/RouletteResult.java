package org.nowstart.nyangnyangbot.service.command;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.data.dto.chzzk.ChatDto;
import org.nowstart.nyangnyangbot.data.dto.chzzk.MessageRequestDto;
import org.nowstart.nyangnyangbot.data.entity.RouletteRoundResultEntity;
import org.nowstart.nyangnyangbot.repository.ChzzkOpenApi;
import org.nowstart.nyangnyangbot.repository.RouletteRoundResultRepository;
import org.springframework.stereotype.Service;

@Service("roulette_result")
@Transactional
@RequiredArgsConstructor
public class RouletteResult implements Command {

    private final ChzzkOpenApi chzzkOpenApi;
    private final RouletteRoundResultRepository rouletteRoundResultRepository;

    @Override
    public void run(ChatDto chatDto) {
        List<RouletteRoundResultEntity> rounds =
                rouletteRoundResultRepository.findTop5ByRouletteEventUserIdOrderByCreateDateDesc(
                        chatDto.senderChannelId()
                );
        if (rounds.isEmpty()) {
            chzzkOpenApi.sendMessage(new MessageRequestDto(
                    chatDto.profile().nickname() + "님의 최근 룰렛 결과가 없습니다."
            ));
            return;
        }
        String result = rounds.stream()
                .map(round -> round.getRoundNo() + "회차 " + round.getItemLabel())
                .collect(Collectors.joining(", "));
        chzzkOpenApi.sendMessage(new MessageRequestDto(
                chatDto.profile().nickname() + "님의 최근 룰렛 결과: " + result
        ));
    }
}
