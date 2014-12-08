package com.sap.river.log;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RiverTrace {
	/**
     * @return the type of class that will have an entity test created
     *         (required; must offer entity services)
     */
    Class<?> entity();

}
