package io.shinhanlife.dat.lib.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.shinhanlife.dat.lib.validation.ToolArgumentSchemaValidator;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class ToolSchemaConfigurationTest {

    @Test
    void providesToolArgumentSchemaValidatorWithoutComponentScanning() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(ObjectMapper.class);
            context.register(ToolSchemaConfiguration.class);
            context.refresh();

            assertNotNull(context.getBean(ToolArgumentSchemaValidator.class));
        }
    }
}
