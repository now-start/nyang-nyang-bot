package org.nowstart.nyangnyangbot.config;

import static org.assertj.core.api.BDDAssertions.then;

import io.swagger.v3.oas.models.OpenAPI;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;

class SwaggerConfigTest {

    @Test
    void customOpenAPI_ShouldUseBuildVersion() {
        Properties props = new Properties();
        props.setProperty("version", "1.2.3");
        BuildProperties buildProperties = new BuildProperties(props);
        SwaggerConfig config = new SwaggerConfig(buildProperties);

        OpenAPI api = config.customOpenAPI();

        then(api.getInfo().getVersion()).isEqualTo("1.2.3");
        then(api.getInfo().getTitle()).isNotBlank();
        then(api.getInfo().getDescription()).isNotBlank();
    }
}






