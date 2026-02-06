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
        run(args, appArgs -> SpringApplication.run(NyangNyangBotApplication.class, appArgs));
    }

    static void run(String[] args, ApplicationRunner runner) {
        if (Boolean.getBoolean("app.skipRun")) {
            return;
        }

        runner.run(args);
    }

    @FunctionalInterface
    interface ApplicationRunner {
        void run(String... args);
    }
}
