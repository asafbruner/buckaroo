package com.sap.river.hcp;


/**
 * Roo add-on for deployment on SAP HANA Cloud Platform
 * ------------------------------------------------------
 * 
 * This class defines the interface of the commands our add-on should provide.
 * 
 * @see PersistenceOperationsImpl
 * @see PersistenceCommands
 */
public interface PersistenceOperations {
	
	/**
	 * deploy command is available when a war file is available under the target directory
	 * 
	 * @return true if the conditions for enabling deploy command are met
	 */
	public boolean isSetupPersistenceRemoteAvailable();
	
    /**
     * 
     * @return true/false, depending on whether or not setup for deploy is available
     */
	public boolean isSetupPersistenceLocalAvailable();
	
	/**
	 * Configure the maven setup needed for activating deploy to HCP
	 * @param db - the db type of the persistence layer 
	 */
	public void setupPersistenceRemote(PersistenceValues db);
	
    /**
     * do the setup
     * @param root
     */
	public void setupPersistenceLocal();
}
