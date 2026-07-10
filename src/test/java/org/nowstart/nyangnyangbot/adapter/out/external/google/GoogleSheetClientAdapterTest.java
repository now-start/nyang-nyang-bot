package org.nowstart.nyangnyangbot.adapter.out.external.google;

import static org.assertj.core.api.BDDAssertions.then;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.application.port.out.google.GoogleSheetPort.GoogleSheetRow;
import org.nowstart.nyangnyangbot.config.property.GoogleProperty;

class GoogleSheetClientAdapterTest {

    private final GoogleSheetClientAdapter adapter = new GoogleSheetClientAdapter(new GoogleProperty(null, null));

    @Test
    void toRows_ShouldReturnOnlyValidRows() {
        var rows = adapter.toRows(List.of(
                List.<Object>of("정상", "user-1", 10),
                List.<Object>of("공백", "user-2", " "),
                List.<Object>of("부족")
        ));

        then(rows).containsExactly(
                new GoogleSheetRow("정상", "user-1", 10)
        );
    }

    @Test
    void toRows_ShouldTreatMissingValuesAsEmptySheet() {
        then(adapter.toRows(null)).isEmpty();
    }
}
