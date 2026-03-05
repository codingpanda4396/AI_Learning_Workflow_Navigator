package com.panda.ainavigator.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.panda.ainavigator.api.dto.schema.SchemaFetchResponse;
import com.panda.ainavigator.api.dto.schema.SchemaValidateResponse;
import com.panda.ainavigator.application.service.SchemaApplicationService;
import com.panda.ainavigator.infrastructure.schema.LlmSchemaStage;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/debug/schema")
public class SchemaDebugController {

    private final SchemaApplicationService schemaService;

    public SchemaDebugController(SchemaApplicationService schemaService) {
        this.schemaService = schemaService;
    }

    @Operation(summary = "Get LLM schema by stage")
    @GetMapping("/{stage}")
    public SchemaFetchResponse getSchema(@PathVariable LlmSchemaStage stage) {
        return schemaService.getSchema(stage);
    }

    @Operation(summary = "Validate payload against stage schema")
    @PostMapping("/{stage}/validate")
    public SchemaValidateResponse validate(@PathVariable LlmSchemaStage stage,
                                           @RequestBody JsonNode payload) {
        return schemaService.validate(stage, payload);
    }
}
