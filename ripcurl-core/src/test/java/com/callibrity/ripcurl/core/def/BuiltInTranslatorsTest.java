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
package com.callibrity.ripcurl.core.def;

import static org.assertj.core.api.Assertions.assertThat;

import com.callibrity.ripcurl.core.JsonRpcErrorDetail;
import com.callibrity.ripcurl.core.JsonRpcProtocol;
import com.callibrity.ripcurl.core.exception.JsonRpcException;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.jwcarman.methodical.ParameterResolutionException;

/**
 * Unit coverage for the three translators ripcurl ships out of the box. Each is a one-liner, so the
 * tests are terse — the important invariants are the JSON-RPC error codes chosen for each exception
 * type (these are contract: clients code against them).
 *
 * <p>The catch-all {@code -32603 Internal error} behavior is owned by the registry itself (see
 * {@link DefaultJsonRpcExceptionTranslatorRegistryTest}), not a standalone translator class.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class BuiltInTranslatorsTest {

  @Nested
  class Default_json_rpc_exception_translator {

    @Test
    void preserves_the_exceptions_self_declared_code_and_message() {
      JsonRpcErrorDetail detail =
          new DefaultJsonRpcExceptionTranslator()
              .translate(new JsonRpcException(-32001, "not authorized"));
      assertThat(detail.code()).isEqualTo(-32001);
      assertThat(detail.message()).isEqualTo("not authorized");
    }
  }

  @Nested
  class Illegal_argument_exception_translator {

    @Test
    void maps_to_invalid_params() {
      JsonRpcErrorDetail detail =
          new IllegalArgumentExceptionTranslator()
              .translate(new IllegalArgumentException("age must be non-negative"));
      assertThat(detail.code()).isEqualTo(JsonRpcProtocol.INVALID_PARAMS);
      assertThat(detail.message()).isEqualTo("age must be non-negative");
    }
  }

  @Nested
  class Parameter_resolution_exception_translator {

    @Test
    void maps_to_invalid_params() {
      JsonRpcErrorDetail detail =
          new ParameterResolutionExceptionTranslator()
              .translate(new ParameterResolutionException("cannot deserialize params"));
      assertThat(detail.code()).isEqualTo(JsonRpcProtocol.INVALID_PARAMS);
      assertThat(detail.message()).isEqualTo("cannot deserialize params");
    }
  }
}
