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
	boolean isSetupDeployAvailable();
	
	/**
	 * deploy command is available when a war file is available under the target directory
	 * 
	 * @return true if the conditions for enabling deploy command are met
	 */
	boolean isDeployAvailable();
	
	/**
	 * Configure the maven setup needed for activating deploy to HCP
	 * 
     * @param accountName - The id of your HCP account 
     * @param userName - Your SCN login name which serves as your user name on your HCP account
     * @param password - Your SCN login password which serves as your password to your HCP account
	 */
	void setupDeploy(final String account, final String userName, final String password);
	
	/**
     * Activate deploy of project's output to HCP
     * 
     * @param command - goal for the neo web plugin
     * @param accountName - The id of your HCP account 
     * @param userName - Your SCN login name which serves as your user name on your HCP account
     * @param password - Your SCN login password which serves as your password to your HCP account
     */
	void deployCommand(final String command, final String account, final String userName, final String password);
}
