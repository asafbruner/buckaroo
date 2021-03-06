package com.sap.buckaroo.jpasetup;


/**
 * Roo add-on for deployment on SAP HANA Cloud Platform
 * ------------------------------------------------------
 * 
 * This class defines the interface of the commands our add-on should provide.
 * 
 * @see JpaSetupOperationsImpl
 * @see JpaSetupCommands
 * This command hides the running of jpa setup for dummy database type, by running it with a Hana command
 */
public interface JpaSetupOperations {
	
	public boolean isJpaSetupAvailable();

	public void run();	
	

}
