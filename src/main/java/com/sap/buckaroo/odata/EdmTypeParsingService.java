package com.sap.buckaroo.odata;

import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.edm.EdmParameter;
import org.apache.olingo.odata2.api.edm.EdmTyped;
import org.springframework.roo.model.JavaType;

public interface EdmTypeParsingService {
	
	/** 
	 * @return JavaType construct that represent Entry Feed return type
	 */
	public JavaType getFeedReturnType();
	
	/** Converts Edm function parameter into a corresponding JavaType construct
	 * 
	 * @param edmParameter dm function parameter
	 * @return JavaType that correspond to the input parameter
	 * @throws EdmException
	 */
	public JavaType getParameterType(final EdmParameter edmParameter) throws EdmException;
	
	/** Converts Edm function return type into a corresponding JavaType construct
	 * @param edmTyped
	 * @return
	 * @throws EdmException
	 */
	public JavaType getReturnType(EdmTyped edmTyped) throws EdmException;
	
	/**
	 * @param className
	 * @return JavaType construct that represent a given Java class Feed return type
	 */
	public JavaType getFeedReturnType(JavaType className);
	
}
