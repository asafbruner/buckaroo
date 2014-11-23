package com.sap.river.hcp;


/**
 * Roo add-on for deployment on SAP HANA Cloud Platform
 * ------------------------------------------------------
 * 
 * This class defines the interface of the commands our add-on should provide.
 * 
 * @see WebAppOperationsImpl
 * @see WebAppCommands
 */
public interface WebAppOperations {
	
	/**
	 */
	public boolean isSetupWebAppAvailable();
	
	/**
	 */
	public void setupWebApp();
}
