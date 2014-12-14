package com.sap.buckaroo.odata;

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

	void setupWebAppProj();

	/**
	 * 
	 * @param serviceBasePath - the URI of the external service
	 * @param username
	 * @param password
	 * @param serviceProxyName 
	 */
	void setupExternalService(String serviceBasePath, String username, String password, final String serviceProxyName, final boolean isTestAutomatically);
	
	
}
