package com.sap.river.hcp;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CommandMarker;

/**
 * Roo add-on for deployment on SAP HANA Cloud Platform ------------------------------------------------------
 * 
 * The command class is registered by the Roo shell following an automatic CLASSPATH scan, and enables the desired commands in Roo by
 * implementing the CommandMarker interface.
 * 
 * The commands will be bound to methods defined in interface DeployOperations. The actual implementation of the interface is done by class
 * DeployOperationsImpl.
 * 
 * @see DeployLocalOperations
 * @see DeployLocalOperationsImpl
 */
@Component
@Service
public class DeployLocalCommands implements CommandMarker {

	/**
	 * Get a reference to the DeployOperations from the underlying OSGi container
	 */
	@Reference
	private DeployLocalOperations operations;

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

	// ////////////////////////////////////////////
	// HCP commands
	// ////////////////////////////////////////////
	/**
	 * This method is optional. It allows automatic command hiding in situations when the command should not be visible.
	 *
	 * You can define multiple methods annotated with {@link CliAvailabilityIndicator} if your commands have differing visibility
	 * requirements.
	 * 
	 * @return true (default) if the command should be visible at this stage, false otherwise
	 */
	@CliAvailabilityIndicator("hcp setup local-deploy")
	boolean isDeployAvailable() {
		return operations.isSetupDeployLocalAvailable();
	}

	/**
	 * Register hcp-deploy command with roo shell.
	 * 
	 * @param accountName - The id of your HCP account
	 * @param userName - Your SCN login name which serves as your user name on your HCP account
	 * @param password - Your SCN login password which serves as your password to your HCP account
	 */
	@CliCommand(value = "hcp setup local-deploy", help = "setup the configuration for local deployment of an HCP server")
	public void setupDeployLocal() {
		operations.setupDeployLocal();
	}
}
