package org.nowstart.chzzk_like_bot;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableAdminServer
@SpringBootApplication
public class ChzzkLikeBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChzzkLikeBotApplication.class, args);
    }
}
