package com.sap.river.odata;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates to produce an OData integration test class.
 * 
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface ODataIntegrationTest {
	
    boolean entitySet() default true;

    /**
     * @return the type of class that will have an entity test created
     *         (required; must offer entity services)
     */
    Class<?> entity();
}
