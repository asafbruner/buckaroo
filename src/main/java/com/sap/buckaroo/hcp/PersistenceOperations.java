package com.sap.buckaroo.hcp;


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
	 * 
     * @return true/false, depending on whether or not setup for remote persistence is available
	 */
	public boolean isSetupPersistenceRemoteAvailable();
	
    /**
     * 
     * @return true/false, depending on whether or not setup for remote persistence is available
     */
	public boolean isSetupPersistenceLocalAvailable();
	
	/**
	 * Configure the maven setup needed for HCP persistence
	 * @param db - the db type of the persistence layer
	 * 
	 *  TODO Expand the API to fully support addon's options
	 */
	public void setupPersistenceRemote(PersistenceValues db);
	
    /**
	 * Configure the maven setup needed for local persistence
	 * 
	 *  TODO Expand the API to fully support addon's options
     */
	public void setupPersistenceLocal();
}
