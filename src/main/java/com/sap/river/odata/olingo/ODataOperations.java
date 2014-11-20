package com.sap.river.odata.olingo;

import org.springframework.roo.model.JavaType;


/**
 * Roo add-on for Exposing OData Service
 * ------------------------------------------------------
 * 
 * This class defines the interface of the commands our add-on should provide.
 * 
 * @see ODataOperationsImpl
 * @see ODataCommands
 */
public interface ODataOperations {
	
	/**
	 *
	 */
	boolean isSetupOlingoOdataAvailable();
	
	/**
	 * 
	 */
	void setupOlingo();
	
	/**
	 * 
	 */
	boolean isSetupOlingoOdataJPAFactoryAvailable();
	
	/**
	 * 
     * @param factoryClassName - The name of the JPA OData factory class
     * @param serviceBasePath - The name of the service in the URI  
	 */
	void setupOlingoJPAFactory(final JavaType factoryClass, final String serviceBasePath);
	
	
}
