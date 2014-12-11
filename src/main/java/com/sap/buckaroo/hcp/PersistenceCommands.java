package com.sap.buckaroo.hcp;


import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Roo add-on for setting HCP supported Databases
 * ------------------------------------------------------
 * 
 * The command class is registered by the Roo shell following an automatic CLASSPATH scan,
 * and enables the desired commands in Roo by implementing the CommandMarker interface.
 * 
 * The commands will be bound to methods defined in interface PersistenceOperations.
 * The actual implementation of the interface is done by class PersistenceOperationsImpl.
 * 
 * @see PersistenceOperations
 * @see PersistenceOperationsImpl
 */
@Component
@Service
public class PersistenceCommands implements CommandMarker {
	
	/**
	 * Get a reference to the PersistenceOperations from the underlying OSGi container
	 */
	@Reference private PersistenceOperations operations;
	
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
	@CliAvailabilityIndicator("hcp setup remote-persistence")
	public boolean isSetupPersistenceRemoteAvailable() {
		return operations.isSetupPersistenceRemoteAvailable();
	}
	
    /**
    * This method is optional. It allows automatic command hiding in situations when the command should not be visible.
    *
    * You can define multiple methods annotated with {@link CliAvailabilityIndicator} if your commands have differing visibility
    * requirements.
    * 
     * @return true (default) if the command should be visible at this stage, false otherwise
    */
    @CliAvailabilityIndicator("hcp setup local-persistence")
    public boolean isSetupPersistenceLocalAvailable() {
		return operations.isSetupPersistenceLocalAvailable();
    }	
	
	/**
     * Configure the maven setup needed for setting persistence in HCP
     * 
     */
    @CliCommand(value = "hcp setup remote-persistence", help = "setup the configuration for deployment of web application to HANA Cloud Platform")
    public void setupPersistenceRemote(
    		@CliOption(key = "database", mandatory = true, help = "The system's DB type") final PersistenceValues db) {
    	operations.setupPersistenceRemote(db);
    }
    
    /**
     * 
     */
    @CliCommand(value = "hcp setup local-persistence", help = "setup the configuration for local deployment of an HCP server")
    public void setupPersistenceLocal() {
    	operations.setupPersistenceLocal();
    }
}
