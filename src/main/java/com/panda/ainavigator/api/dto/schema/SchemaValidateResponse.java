package com.panda.ainavigator.api.dto.schema;

import java.util.List;

public record SchemaValidateResponse(String stage, boolean valid, List<String> errors) {
}
