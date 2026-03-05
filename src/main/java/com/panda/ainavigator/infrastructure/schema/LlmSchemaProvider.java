package com.panda.ainavigator.infrastructure.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.InputFormat;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

@Component
public class LlmSchemaProvider {

    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory jsonSchemaFactory;

    public LlmSchemaProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.jsonSchemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    public String loadSchema(LlmSchemaStage stage) {
        Path schemaPath = Path.of("spec", "04_llm_schemas", stage.getFileName());
        try {
            return Files.readString(schemaPath, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load schema file: " + schemaPath, ex);
        }
    }

    public List<String> validate(LlmSchemaStage stage, JsonNode payload) {
        String schemaText = loadSchema(stage);
        JsonSchema schema = jsonSchemaFactory.getSchema(schemaText, InputFormat.JSON);
        Set<ValidationMessage> messages = schema.validate(payload);
        return messages.stream().map(ValidationMessage::getMessage).sorted().toList();
    }

    public JsonNode parse(String schemaText) {
        try {
            return objectMapper.readTree(schemaText);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to parse schema text", ex);
        }
    }
}
