package com.sap.river.log;

import org.springframework.roo.model.JavaType;

public interface TraceOperations {
	boolean isSetupTraceCommandAvailable();
	
	public void setupTrace(final JavaType className);

}
