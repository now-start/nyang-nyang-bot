package org.nowstart.nyangnyangbot.service;

import static org.assertj.core.api.BDDAssertions.then;

import io.socket.client.IO;
import io.socket.client.Socket;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Test;

class DefaultChzzkSocketFactoryTest {

    @Test
    void create_ShouldReturnSocket() throws URISyntaxException {
        DefaultChzzkSocketFactory factory = new DefaultChzzkSocketFactory();
        IO.Options options = new IO.Options();

        Socket socket = factory.create("http://localhost", options);

        then(socket).isNotNull();
    }
}






