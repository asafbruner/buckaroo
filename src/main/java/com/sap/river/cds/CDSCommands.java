package com.sap.river.cds;


import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Roo add-on for deployment on SAP HANA Cloud Platform
 * ------------------------------------------------------
 * 
 * The command class is registered by the Roo shell following an automatic CLASSPATH scan,
 * and enables the desired commands in Roo by implementing the CommandMarker interface.
 * 
 * The commands will be bound to methods defined in interface DeployOperations.
 * The actual implementation of the interface is done by class DeployOperationsImpl.
 * 
 * @see CDSOperations
 * @see CDSOperationsImpl
 */
@Component
@Service
public class CDSCommands implements CommandMarker {
	
	/**
	 * Get a reference to the DeployOperations from the underlying OSGi container
	 */
	@Reference private CDSOperations operations;
	
	/**
	 * The activate method for this OSGi component, which will be called by the OSGi container upon bundle activation.
	 *  
	 * @param context The component context can be used to get access to the OSGi container (i.e. find out if certain bundles are active)
	 */
	protected void activate(ComponentContext context) {
		// Nothing to do here
    }

	/**
	 * The deactivate method for this OSGi component, which will be called by the OSGi container upon bundle deactivation.
	 * 
	 * @param context The component context can be used to get access to the OSGi container (i.e. find out if certain bundles are active)
	 */
	protected void deactivate(ComponentContext context) {
		// Nothing to do here
	}
	
	//////////////////////////////////////////////
	// CDS commands
	//////////////////////////////////////////////

	
	
	
	/**
	 * parse a cds file, build roo commands, and run them
	 * @param cdsFilePathName
	 */
    @CliCommand(value = "cds parsefile", help = "parse a cds file, build roo commands, and run them")
    public void parseRunCDS(@CliOption(key = "filepathname", mandatory = true, 	help = "The path/name of the CDS file on local file system") final String cdsFilePathName) {
    	operations.parseRunCDS(cdsFilePathName);
    }
 
}
