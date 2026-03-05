package com.panda.ainavigator.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.panda.ainavigator.api.dto.schema.SchemaFetchResponse;
import com.panda.ainavigator.api.dto.schema.SchemaValidateResponse;
import com.panda.ainavigator.infrastructure.schema.LlmSchemaProvider;
import com.panda.ainavigator.infrastructure.schema.LlmSchemaStage;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SchemaApplicationService {

    private final LlmSchemaProvider schemaProvider;

    public SchemaApplicationService(LlmSchemaProvider schemaProvider) {
        this.schemaProvider = schemaProvider;
    }

    public SchemaFetchResponse getSchema(LlmSchemaStage stage) {
        String schema = schemaProvider.loadSchema(stage);
        return new SchemaFetchResponse(stage.name(), stage.getFileName(), schema);
    }

    public SchemaValidateResponse validate(LlmSchemaStage stage, JsonNode payload) {
        List<String> errors = schemaProvider.validate(stage, payload);
        return new SchemaValidateResponse(stage.name(), errors.isEmpty(), errors);
    }
}
