package org.nowstart.nyangnyangbot.service;

import io.socket.client.IO;
import io.socket.client.Socket;
import java.net.URISyntaxException;
import org.springframework.stereotype.Component;

@Component
public class DefaultChzzkSocketFactory {

    public Socket create(String url, IO.Options options) throws URISyntaxException {
        return IO.socket(url, options);
    }
}






