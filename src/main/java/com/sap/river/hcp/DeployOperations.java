package com.sap.river.hcp;


/**
 * Roo add-on for deployment on SAP HANA Cloud Platform
 * ------------------------------------------------------
 * 
 * This class defines the interface of the commands our add-on should provide.
 * 
 * @see DeployOperationsImpl
 * @see DeployCommands
 */
public interface DeployOperations {
	
	/**
	 * deploy command is available when a war file is available under the target directory
	 * 
	 * @return true if the conditions for enabling deploy command are met
	 */
	boolean isSetupDeployRemoteAvailable();
	
	void setupDeployRemote();
	
	/**
	 * deploy command is available when a war file is available under the target directory
	 * 
	 * @return true if the conditions for enabling deploy command are met
	 */
	boolean isSetupDeployLocalAvailable();

	void setupDeployLocal(String account, String userName, String password);
}
