# LLM Observability Troubleshooting

## How to confirm a request really called LLM

Search logs by `traceId` or `requestId`.

Successful calls will emit:

- `LLM_CALL_START`
- `LLM_CALL_SUCCESS`

If only fallback logs appear and there is no success log, the request did not complete a usable LLM call.

## How to search one request chain

All web requests now write `traceId` and `requestId` into MDC and response headers:

- `X-Trace-Id`
- `X-Request-Id`

Use either value to grep server logs.

## Log meanings

- `LLM_CALL_START`: stage and model were selected and the call is about to start
- `LLM_CALL_SUCCESS`: the provider returned usable content; includes `latencyMs`, `promptTokens`, `completionTokens`, `totalTokens`
- `LLM_CALL_FALLBACK`: business flow switched to fallback logic
- `LLM_CALL_FAILURE`: technical failure classification for the LLM call itself

## Fallback reasons

- `LLM_TIMEOUT`: provider timeout or read timeout
- `LLM_API_ERROR`: remote provider returned HTTP/API failure
- `JSON_PARSE_ERROR`: provider response was not valid JSON or downstream JSON parsing failed
- `EMPTY_RESPONSE`: provider returned empty body or empty content
- `MISSING_REQUIRED_FIELDS`: response JSON existed but required fields/schema validation failed
- `FORCE_FALLBACK`: `app.llm.force-fallback=true` skipped the real LLM call
- `UNKNOWN_ERROR`: uncategorized failure

## Failure types

- `TIMEOUT`: low-level timeout
- `API_ERROR`: remote API failure
- `JSON_PARSE_ERROR`: JSON decoding/parsing failure
- `EMPTY_RESPONSE`: empty provider response
- `VALIDATION_ERROR`: required fields/schema/business validation failure
- `UNKNOWN_ERROR`: uncategorized error

## How to read token and latency logs

- `promptTokens`: provider prompt token usage, `-1` when upstream does not provide it
- `completionTokens`: provider completion token usage, `-1` when unavailable
- `totalTokens`: provider total token usage, `-1` when unavailable
- `latencyMs`: measured from just before outbound call until success/failure/fallback path completes

## Force fallback

Configuration:

- `app.llm.force-fallback=true`

Behavior:

- real LLM call is skipped
- `LLM_CALL_FALLBACK reason=FORCE_FALLBACK` is logged
- business flow uses existing template/rule fallback where available

## Common troubleshooting flow

1. Find `traceId` or `requestId` from response header.
2. Search matching `LLM_CALL_*` logs.
3. Confirm `stage` and `model`.
4. If `LLM_CALL_SUCCESS` exists, inspect `latencyMs` and token fields.
5. If `LLM_CALL_FAILURE` exists, inspect `errorType`.
6. If `LLM_CALL_FALLBACK` exists, inspect `reason` and check whether response metadata returned `fallbackApplied=true`.
