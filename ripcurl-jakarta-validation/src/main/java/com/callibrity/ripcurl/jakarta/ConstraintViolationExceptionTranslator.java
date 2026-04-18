/*
 * Copyright © 2025 Callibrity, Inc. (contactus@callibrity.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.callibrity.ripcurl.jakarta;

import com.callibrity.ripcurl.core.JsonRpcErrorDetail;
import com.callibrity.ripcurl.core.JsonRpcProtocol;
import com.callibrity.ripcurl.core.spi.JsonRpcExceptionTranslator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Translates a Jakarta Bean Validation {@link ConstraintViolationException} — typically raised by
 * methodical's {@code JakartaMethodValidator} when a {@code @JsonRpcMethod} parameter fails a
 * {@code @NotNull}/{@code @Size}/{@code @Pattern}/etc. check — into a {@code -32602 Invalid params}
 * JSON-RPC error.
 *
 * <p>Per-violation detail is emitted in the error's {@code data} field as a JSON array of {@code
 * {field, message}} objects:
 *
 * <pre>{@code
 * {
 *   "code": -32602,
 *   "message": "Invalid params",
 *   "data": [
 *     {"field": "name",    "message": "must not be blank"},
 *     {"field": "age",     "message": "must be greater than or equal to 0"}
 *   ]
 * }
 * }</pre>
 *
 * <p>The {@code invalidValue} is deliberately omitted. Reflecting the rejected input back at the
 * client is convenient for debugging but dangerous for sensitive parameters (passwords, tokens,
 * PII) — clients that want that detail should capture it at the call site rather than rely on the
 * server leaking it.
 */
public class ConstraintViolationExceptionTranslator
    implements JsonRpcExceptionTranslator<ConstraintViolationException> {

  @Override
  public JsonRpcErrorDetail translate(ConstraintViolationException exception) {
    ArrayNode violations = JsonNodeFactory.instance.arrayNode();
    for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
      ObjectNode item = JsonNodeFactory.instance.objectNode();
      item.put("field", violation.getPropertyPath().toString());
      item.put("message", violation.getMessage());
      violations.add(item);
    }
    return new JsonRpcErrorDetail(JsonRpcProtocol.INVALID_PARAMS, "Invalid params", violations);
  }
}
