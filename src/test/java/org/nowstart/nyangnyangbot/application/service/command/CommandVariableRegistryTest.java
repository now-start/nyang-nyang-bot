package org.nowstart.nyangnyangbot.application.service.command;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommandVariableRegistryTest {

    @Mock
    private CommandVariableContributor requestedContributor;

    @Mock
    private CommandVariableContributor unusedContributor;

    @Test
    void resolve_ShouldInvokeEachRequestedContributorOnceAndSkipUnusedContributors() {
        // 준비
        given(requestedContributor.definitions()).willReturn(List.of(
                definition("viewer.nickname"),
                definition("invocation.args")
        ));
        given(unusedContributor.definitions()).willReturn(List.of(definition("point.balance")));
        CommandVariableRegistry registry = new CommandVariableRegistry(List.of(
                requestedContributor,
                unusedContributor
        ));
        CommandVariableContext context = new CommandVariableContext(
                "user-1",
                "치즈냥",
                "!인사",
                "첫번째 두번째",
                "첫번째",
                "두번째",
                Instant.parse("2026-07-16T12:00:00Z")
        );
        Set<String> requestedKeys = Set.of("viewer.nickname", "invocation.args");
        given(requestedContributor.resolve(eq(requestedKeys), same(context)))
                .willReturn(Map.of(
                        "viewer.nickname", "치즈냥",
                        "invocation.args", "첫번째 두번째"
                ));

        // 실행
        Map<String, String> resolved = registry.resolve(requestedKeys, context);

        // 검증
        then(resolved).containsExactlyInAnyOrderEntriesOf(Map.of(
                "viewer.nickname", "치즈냥",
                "invocation.args", "첫번째 두번째"
        ));
        BDDMockito.then(requestedContributor).should(BDDMockito.times(1))
                .resolve(eq(requestedKeys), same(context));
        BDDMockito.then(unusedContributor).should(BDDMockito.never()).resolve(any(), any());
    }

    @Test
    void constructor_ShouldRejectDuplicateVariableKeys() {
        // 준비
        given(requestedContributor.definitions()).willReturn(List.of(definition("viewer.nickname")));
        given(unusedContributor.definitions()).willReturn(List.of(definition("viewer.nickname")));

        // 실행 및 검증
        thenThrownBy(() -> new CommandVariableRegistry(List.of(
                requestedContributor,
                unusedContributor
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("duplicate command variable: viewer.nickname");
    }

    @Test
    void resolve_ShouldRejectContributorThatOmitsRequestedValue() {
        // 준비
        given(requestedContributor.definitions()).willReturn(List.of(definition("viewer.nickname")));
        CommandVariableRegistry registry = new CommandVariableRegistry(List.of(requestedContributor));
        CommandVariableContext context = new CommandVariableContext(
                "user-1", "치즈냥", "!인사", "", "", "", Instant.now()
        );
        given(requestedContributor.resolve(Set.of("viewer.nickname"), context)).willReturn(Map.of());

        // 실행 및 검증
        thenThrownBy(() -> registry.resolve(Set.of("viewer.nickname"), context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("command variable was not resolved: viewer.nickname");
    }

    @Test
    void coreTimeVariables_ShouldFormatInstantInAsiaSeoul() {
        CommandVariableRegistry registry = new CommandVariableRegistry(
                List.of(new CoreCommandVariableContributor())
        );
        CommandVariableContext context = new CommandVariableContext(
                "user-1",
                "치즈냥",
                "!시간",
                "",
                "",
                "",
                Instant.parse("2026-07-16T15:00:00Z")
        );

        Map<String, String> resolved = registry.resolve(
                Set.of("time.date", "time.time", "time.datetime"),
                context
        );

        then(resolved).containsExactlyInAnyOrderEntriesOf(Map.of(
                "time.date", "2026-07-17",
                "time.time", "00:00",
                "time.datetime", "2026-07-17 00:00"
        ));
    }

    private CommandVariableDefinition definition(String key) {
        return new CommandVariableDefinition(key, key, key + " 설명", key + " 예시");
    }
}
