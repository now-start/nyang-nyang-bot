package org.nowstart.nyangnyangbot.application.service.command;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class CoreCommandVariableContributor implements CommandVariableContributor {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(SEOUL);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm").withZone(SEOUL);
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(SEOUL);
    private static final List<CommandVariableDefinition> DEFINITIONS = List.of(
            definition("viewer.nickname", "시청자 닉네임", "명령어를 호출한 시청자의 표시 이름", "치즈냥"),
            definition("invocation.command", "호출 명령어", "채팅에서 입력된 명령어", "치하"),
            definition("invocation.args", "전체 인자", "명령어 뒤에 입력된 전체 내용", "첫번째 두번째"),
            definition("invocation.arg1", "첫 번째 인자", "명령어의 첫 번째 인자", "첫번째"),
            definition("invocation.arg2", "두 번째 인자", "명령어의 두 번째 인자", "두번째"),
            definition("count.total", "전체 실행 횟수", "이 명령어의 승인된 전체 실행 순번", "42"),
            definition("count.user", "사용자 실행 횟수", "이 사용자의 승인된 명령 실행 순번", "7"),
            definition("streak.current", "현재 연속 실행일", "서울 날짜 기준 현재 연속 실행일", "3"),
            definition("streak.longest", "최장 연속 실행일", "서울 날짜 기준 최장 연속 실행일", "12"),
            definition("time.date", "오늘 날짜", "실행 시점의 날짜", "2026-07-16"),
            definition("time.time", "현재 시각", "실행 시점의 시각", "21:00"),
            definition("time.datetime", "현재 일시", "실행 시점의 날짜와 시각", "2026-07-16 21:00")
    );

    @Override
    public List<CommandVariableDefinition> definitions() {
        return DEFINITIONS;
    }

    @Override
    public Map<String, String> resolve(Set<String> requestedKeys, CommandVariableContext context) {
        Instant now = context.now() == null ? Instant.now() : context.now();
        Map<String, String> values = new LinkedHashMap<>();
        for (String key : requestedKeys) {
            values.put(key, switch (key) {
                case "viewer.nickname" -> value(context.nickname());
                case "invocation.command" -> value(context.command());
                case "invocation.args" -> value(context.args());
                case "invocation.arg1" -> value(context.arg1());
                case "invocation.arg2" -> value(context.arg2());
                case "count.total" -> String.valueOf(context.totalCount());
                case "count.user" -> String.valueOf(context.userCount());
                case "streak.current" -> String.valueOf(context.currentStreak());
                case "streak.longest" -> String.valueOf(context.longestStreak());
                case "time.date" -> DATE_FORMAT.format(now);
                case "time.time" -> TIME_FORMAT.format(now);
                case "time.datetime" -> DATE_TIME_FORMAT.format(now);
                default -> throw new IllegalArgumentException("unsupported core command variable: " + key);
            });
        }
        return Map.copyOf(values);
    }

    private static CommandVariableDefinition definition(
            String key,
            String label,
            String description,
            String example
    ) {
        return new CommandVariableDefinition(key, label, description, example);
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
