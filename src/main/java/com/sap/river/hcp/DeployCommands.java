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
public class DeployCommands implements CommandMarker {
	
	/**
	 * Get a reference to the DeployOperations from the underlying OSGi container
	 */
	@Reference private DeployOperations operations;
	
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
     * This method is optional. It allows automatic command hiding in situations when the command should not be visible.
     *
     * You can define multiple methods annotated with {@link CliAvailabilityIndicator} if your commands have differing
     * visibility requirements.
     *
     * @return true (default) if the command should be visible at this stage, false otherwise
     */
	@CliAvailabilityIndicator("hcp setup remote-deploy")
	public boolean isSetupDeployRemoteAvailable() {
		return operations.isSetupDeployRemoteAvailable();
	}
	
	/**
     * This method is optional. It allows automatic command hiding in situations when the command should not be visible.
     *
     * You can define multiple methods annotated with {@link CliAvailabilityIndicator} if your commands have differing
     * visibility requirements.
     *
     * @return true (default) if the command should be visible at this stage, false otherwise
     */
	@CliAvailabilityIndicator("hcp remote-deploy")
	public boolean isDeployRemoteAvailable() {		
		return operations.isDeployRemoteAvailable();
	}
	
    /**
    * This method is optional. It allows automatic command hiding in situations when the command should not be visible.
    *
    * You can define multiple methods annotated with {@link CliAvailabilityIndicator} if your commands have differing visibility
    * requirements.
    * 
     * @return true (default) if the command should be visible at this stage, false otherwise
    */
    @CliAvailabilityIndicator("hcp setup local-deploy")
    public boolean isSetupDeployLocalAvailable() {
		return operations.isSetupDeployLocalAvailable();
    }	
	
	/**
     * Configure the maven setup needed for activating deploy to HCP
     * 
     * @param accountName - The id of your HCP account 
     * @param userName - Your SCN login name which serves as your user name on your HCP account
     * @param password - Your SCN login password which serves as your password to your HCP account
     */
    @CliCommand(value = "hcp setup remote-deploy", help = "setup the configuration for deployment of web application to HANA Cloud Platform")
    public void setupDeployRemote(
    		@CliOption(key = "host", mandatory = false, help = "The host of the HANA cloud platform") final String host,
    		@CliOption(key = "account", mandatory = false, help = "The id of the account on HANA cloud platform") final String account,
    		@CliOption(key = "user", mandatory = false, help = "The user name to log into the account") final String userName,
    		@CliOption(key = "password", mandatory = false, help = "The login password of the user") final String password) {
    	operations.setupDeployRemote(host, account, userName, password);
    }
    
    /**
     * Activate deploy of project's output to HCP
     * 
     * @param command - goal for the neo web plugin
     * @param accountName - The id of your HCP account 
     * @param userName - Your SCN login name which serves as your user name on your HCP account
     * @param password - Your SCN login password which serves as your password to your HCP account
     */
    @CliCommand(value = "hcp remote-deploy", help = "Deploy the web application to HANA Cloud Platform")
    public void deployRemoteCommand(
    		@CliOption(key = "goal", unspecifiedDefaultValue = "deploy", help = "goal for the neo web plugin") final String command,
    		@CliOption(key = "host", unspecifiedDefaultValue = "neo.ondemand.com", help = "The host of the HANA cloud platform") final String host,
    		@CliOption(key = "account", help = "The id of the account on HANA cloud platform") final String account,
    		@CliOption(key = "user", help = "The user name to log into the account") final String userName,
    		@CliOption(key = "password", help = "The login password of the user") final String password) {
    	operations.deployRemoteCommand(command, host, account, userName, password);
    }
    
    /**
     * 
     * @param root - the root of the server on localhost
     */
    @CliCommand(value = "hcp setup local-deploy", help = "setup the configuration for local deployment of an HCP server")
    public void setupDeployLocal(
            @CliOption(key = "root", mandatory = false, help = "The file system root") final String root) {
    	operations.setupDeployLocal(root);
    }
    
    
}
