package com.callibrity.ripcurl.autoconfigure;

import com.ripcurl.core.JsonRpcService;
import com.ripcurl.core.def.DefaultJsonRpcMethodProvider;
import com.ripcurl.core.def.DefaultJsonRpcService;
import com.ripcurl.core.spi.JsonRpcMethodHandler;
import com.ripcurl.core.spi.JsonRpcMethodHandlerProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;

@AutoConfiguration
public class RipCurlAutoconfiguration {

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
