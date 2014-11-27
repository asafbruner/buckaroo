package com.sap.river.cds;


/**
 * Roo add-on for deployment on SAP HANA Cloud Platform
 * ------------------------------------------------------
 * 
 * This class defines the interface of the commands our add-on should provide.
 * 
 * @see CDSOperationsImpl
 * @see CDSCommands
 */
public interface CDSOperations {
	

	/**
	 * parse a cds file, build roo commands, and run them
	 * @param cdsFilePathName  - the full path/name of the cds file to be parsed and deployed
	 */
	public void parseRunCDS(String cdsFilePathName);
	
	
}
