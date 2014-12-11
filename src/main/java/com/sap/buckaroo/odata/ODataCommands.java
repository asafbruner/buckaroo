package com.sap.buckaroo.odata;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Roo add-on for setup OData content for SAP Applications
 * ------------------------------------------------------
 * 
 * The command class is registered by the Roo shell following an automatic CLASSPATH scan,
 * and enables the desired commands in Roo by implementing the CommandMarker interface.
 * 
 * The commands will be bound to methods defined in interface ODataOperations.
 * The actual implementation of the interface is done by class ODataOperationsImpl.
 * 
 * @see ODataOperations
 * @see ODataOperationsImpl
 */
@Component
@Service
public class ODataCommands implements CommandMarker {
	
	/**
	 * Get a reference to the ODataOperations from the underlying OSGi container
	 */
	@Reference private ODataOperations operations;
	
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
	// OData commands
	//////////////////////////////////////////////
	/**
     * This method is optional. It allows automatic command hiding in situations when the command should not be visible.
     *
     * You can define multiple methods annotated with {@link CliAvailabilityIndicator} if your commands have differing
     * visibility requirements.
     * 
     * @return true (default) if the command should be visible at this stage, false otherwise
     */
	@CliAvailabilityIndicator("odata setup")
	boolean isSetupOlingoODataAvailable() {
		return operations.isSetupOlingoOdataAvailable();
	}
	
	/**
     * Configure the olingo dependencies in maven and 
     * 
     */
    @CliCommand(value = "odata setup", help = "setup the configuration for embedding Olingo OData provider")
    public void setupOlingo(
    		@CliOption(key = "service", mandatory=false, help = "The service name in the URI") final String serviceBasePath,
    		@CliOption(key = "class", mandatory=true, help = "The name of the factory class") final JavaType factoryClassName){
    	operations.setupOlingo(factoryClassName, serviceBasePath);
    }
    
    @CliCommand(value = "webapp setup", help = "setup the configuration for web application")
    public void webAppSetupp() {
    	operations.setupWebAppProj();
    }
    
    /**
     * Configure the external service to consume
     * 
     */
    @CliCommand(value = "odata external service", help = "setup the configuration consuming an external service")
    public void setupOlingo(
    		@CliOption(key = "service", mandatory=false, help = "The service name in the URI") final String serviceBasePath,
    		@CliOption(key = "username", mandatory=false, help = "The username to access the service") final String username,
    		@CliOption(key = "password", mandatory=false, help = "The password to access the service") final String password,
    		@CliOption(key = "serviceProviderClassName", mandatory=false, help = "The name of the output service proxy class") final String serviceProxyName,
    		@CliOption(key = "testAutomatically", mandatory=false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false", help = "Create automatic integration tests for entities") final boolean isTestAutomatically){
    	operations.setupExternalService(serviceBasePath, username, password, serviceProxyName, isTestAutomatically);
    }
    
}
