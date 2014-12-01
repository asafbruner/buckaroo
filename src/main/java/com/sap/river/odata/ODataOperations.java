package com.sap.river.odata;

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
	void setupOlingo(final JavaType factoryClass, final String serviceBasePath);


	/**
	 * 
	 * @param serviceBasePath - the URI of the external service
	 * @param username
	 * @param password
	 * @param serviceProxyName 
	 */
	void setupExternalService(final String serviceBasePath, final String username, final String password, final String serviceProxyName);
	
	
}
