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
package com.callibrity.ripcurl.core;

/** JSON-RPC 2.0 protocol constants. */
public final class JsonRpcProtocol {

  /** The JSON-RPC protocol version string. */
  public static final String VERSION = "2.0";

  /** Parse error — invalid JSON was received. */
  public static final int PARSE_ERROR = -32700;

  /** Invalid Request — the JSON sent is not a valid Request object. */
  public static final int INVALID_REQUEST = -32600;

  /** Method not found — the method does not exist or is not available. */
  public static final int METHOD_NOT_FOUND = -32601;

  /** Invalid params — invalid method parameter(s). */
  public static final int INVALID_PARAMS = -32602;

  /** Internal error — internal JSON-RPC error. */
  public static final int INTERNAL_ERROR = -32603;

  private JsonRpcProtocol() {}
}
