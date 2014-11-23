package com.sap.river.hcp;


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
 * @see DeployOperations
 * @see DeployOperationsImpl
 */
@Component
@Service
public class WebAppCommands implements CommandMarker {
	
	/**
	 * Get a reference to the DeployOperations from the underlying OSGi container
	 */
	@Reference private WebAppOperations operations;
	
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
	// HCP commands
	//////////////////////////////////////////////
	/**
     */
	@CliAvailabilityIndicator("hcp setup webapp")
	public boolean isSetupWebAppAvailable() {
		return operations.isSetupWebAppAvailable();
	}
	
	
	/**
     */
    @CliCommand(value = "hcp setup webapp", help = "setup the web app")
    public void setupWebApp()
    {
    	operations.setupWebApp();
    }
    
}
