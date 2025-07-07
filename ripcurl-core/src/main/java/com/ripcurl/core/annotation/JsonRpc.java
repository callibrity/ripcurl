package com.ripcurl.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface JsonRpc {
    /**
     * Specifies the name associated with the JSON-RPC method.
     *
     * @return the name or identifier of the JSON-RPC method, default is an empty string (method name will be used)
     */
    String value() default "";
}
