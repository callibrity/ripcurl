# RipCurl

![Maven Central Version](https://img.shields.io/maven-central/v/com.callibrity.ripcurl/ripcurl)
![GitHub License](https://img.shields.io/github/license/callibrity/ripcurl)

[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=callibrity_ripcurl&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=callibrity_ripcurl)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=callibrity_ripcurl&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=callibrity_ripcurl)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=callibrity_ripcurl&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=callibrity_ripcurl)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=callibrity_ripcurl&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=callibrity_ripcurl)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=callibrity_ripcurl&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=callibrity_ripcurl)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=callibrity_ripcurl&metric=coverage)](https://sonarcloud.io/summary/new_code?id=callibrity_ripcurl)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=callibrity_ripcurl&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=callibrity_ripcurl)


A [JSON-RPC 2.0](https://www.jsonrpc.org/specification) compliant framework built for Spring Boot.

## Getting Started

RipCurl includes a Spring Boot starter, making it easy to get started by simply adding a dependency:

```xml
<dependency>
    <groupId>com.callibrity.ripcurl</groupId>
    <artifactId>ripcurl-spring-boot-starter</artifactId>
    <version>${ripcurl.version}</version>
</dependency>
```

By default RipCurl will listen for JSON-RPC requests on the `/jsonrpc` endpoint. You can change this by setting the `ripcurl.endpoint` property in your `application.properties` or `application.yml` file:

```properties
ripcurl.endpoint=/your-custom-endpoint
```

## Using RipCurl

To use RipCurl, you need to annotate a bean method with `@JsonRpc` and the bean itself with `@JsonRpcService`:

```java
import com.callibrity.ripcurl.core.annotation.JsonRpc;
import org.springframework.stereotype.Component;

@Component
@JsonRpcService
public class HelloRpc {

    @JsonRpc("hello")
    public String sayHello(String name) {
        return String.format("Hello, %s!", name);
    }
}
```

RipCurl will scan the Spring `ApplicationContext` for all beans annotated with the `@JsonRpcService` annotation and 
register each of their methods annotated with `@JsonRpc` with the `JsonRpcDispatcher`.

You can then send a JSON-RPC request to the `/jsonrpc` endpoint with the following payload:

```json
{
  "jsonrpc": "2.0",
  "id": "12345",
  "method": "hello",
  "params": {
    "name": "RipCurl"
  }
}
```

The response will be:

```json
{
  "jsonrpc": "2.0",
  "id": "12345",
  "result": "Hello, RipCurl!"
}
```