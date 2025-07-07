package com.callibrity.ripcurl.autoconfigure;

import com.callibrity.ripcurl.core.JsonRpcService;
import com.callibrity.ripcurl.core.def.DefaultJsonRpcMethodProvider;
import com.callibrity.ripcurl.core.def.DefaultJsonRpcService;
import com.callibrity.ripcurl.core.spi.JsonRpcMethodHandler;
import com.callibrity.ripcurl.core.spi.JsonRpcMethodHandlerProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;

@AutoConfiguration
public class RipCurlAutoConfiguration {

// -------------------------- OTHER METHODS --------------------------

    @Bean
    public DefaultJsonRpcMethodProvider defaultJsonRpcMethodProvider(List<JsonRpcMethodHandler> handlers) {
        return new DefaultJsonRpcMethodProvider(handlers);
    }

    @Bean
    public JsonRpcService defaultJsonRpcService(List<JsonRpcMethodHandlerProvider> providers) {
        return new DefaultJsonRpcService(providers);
    }

    @Bean
    public RipCurlController ripCurlController(JsonRpcService service) {
        return new RipCurlController(service);
    }

}
