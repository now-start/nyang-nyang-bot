package org.nowstart.nyangnyangbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableJpaAuditing
@EnableDiscoveryClient
@SpringBootApplication
@ConfigurationPropertiesScan
public class NyangNyangBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(NyangNyangBotApplication.class, args);
    }
}
