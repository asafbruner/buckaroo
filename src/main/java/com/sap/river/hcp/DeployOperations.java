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
	
	/**
	 * deploy command is available when a war file is available under the target directory
	 * 
	 * @return true if the conditions for enabling deploy command are met
	 */
	boolean isDeployRemoteAvailable();
	
    /**
     * 
     * @return true/false, depending on whether or not setup for deploy is available
     */
    boolean isSetupDeployLocalAvailable();
	
	/**
	 * Configure the maven setup needed for activating deploy to HCP
	 * 
     * @param host - The host of the HCP
     * @param account - The id of your HCP account 
     * @param userName - Your SCN login name which serves as your user name on your HCP account
     * @param password - Your SCN login password which serves as your password to your HCP account
	 */
	void setupDeployRemote(final String host, final String account, final String userName, final String password);
	
	/**
     * Activate deploy of project's output to HCP
     * 
     * @param command - goal for the neo web plugin
     * @param host - The id of the HCP
     * @param account - The id of your HCP account 
     * @param userName - Your SCN login name which serves as your user name on your HCP account
     * @param password - Your SCN login password which serves as your password to your HCP account
     */
	void deployRemoteCommand(final String command, final String host, final String account, final String userName, final String password);

    /**
     * do the setup
     * @param account
     * @param userName
     * @param password
     */
    void setupDeployLocal(String root);
}
