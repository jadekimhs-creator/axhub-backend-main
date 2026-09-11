package io.shinhanlife.dat.lib.metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

/** classpath의 Tool 정의를 기동 시 한 번 읽어 name 기준으로 캐시합니다. */
@Component
public class ToolDefinitionRepository {
    public static final String DEFAULT_LOCATION = "classpath*:tool-definitions/**/*.yml";

    private final Map<String, ToolDefinition> definitions;

    @Autowired
    public ToolDefinitionRepository(ResourceLoader resourceLoader) {
        this(new ObjectMapper(new YAMLFactory()), resourceLoader, DEFAULT_LOCATION);
    }

    public ToolDefinitionRepository(ObjectMapper yamlMapper, ResourceLoader resourceLoader, String location) {
        this(yamlMapper, new PathMatchingResourcePatternResolver(resourceLoader), location);
    }

    private ToolDefinitionRepository(ObjectMapper yamlMapper, ResourcePatternResolver resolver, String location) {
        this.definitions = Collections.unmodifiableMap(load(yamlMapper, resolver, location));
    }

    public Optional<ToolDefinition> findByName(String name) {
        return Optional.ofNullable(definitions.get(name));
    }

    public Map<String, ToolDefinition> findAll() {
        return definitions;
    }

    private Map<String, ToolDefinition> load(ObjectMapper mapper, ResourcePatternResolver resolver, String location) {
        Map<String, ToolDefinition> loaded = new LinkedHashMap<>();
        try {
            for (Resource resource : resolver.getResources(location)) {
                ToolDefinition definition = mapper.readValue(resource.getInputStream(), ToolDefinition.class);
                String source = resource.getDescription();
                ToolDefinitionValidator.validate(definition, source);
                ToolDefinition previous = loaded.putIfAbsent(definition.name(), definition);
                if (previous != null) {
                    throw new IllegalStateException("Duplicate Tool definition name: " + definition.name());
                }
            }
            return loaded;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load Tool definitions from " + location, exception);
        }
    }
}
