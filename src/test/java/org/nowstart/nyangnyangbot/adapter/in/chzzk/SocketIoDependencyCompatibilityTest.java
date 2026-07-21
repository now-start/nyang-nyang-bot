package org.nowstart.nyangnyangbot.adapter.in.chzzk;

import static org.assertj.core.api.Assertions.assertThat;

import io.socket.parser.IOParser;
import io.socket.parser.Packet;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

class SocketIoDependencyCompatibilityTest {

    @Test
    void decoder_ShouldParseEventWithSelectedJsonVersion() {
        IOParser.Decoder decoder = new IOParser.Decoder();
        AtomicReference<Packet<?>> decoded = new AtomicReference<>();
        decoder.onDecoded(decoded::set);

        decoder.add("2[\"chat\",{\"content\":\"치하\"}]");

        Packet<?> packet = decoded.get();
        assertThat(packet).isNotNull();
        assertThat(packet.type).isEqualTo(2);
        JSONArray payload = (JSONArray) packet.data;
        assertThat(payload.getString(0)).isEqualTo("chat");
        assertThat(((JSONObject) payload.get(1)).getString("content")).isEqualTo("치하");
    }
}
