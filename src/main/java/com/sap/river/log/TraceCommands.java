package com.sap.river.log;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

import com.sap.river.odata.ODataOperations;
import com.sap.river.odata.ODataOperationsImpl;

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
public class TraceCommands implements CommandMarker {
	
	/**
	 * Get a reference to the ODataOperations from the underlying OSGi container
	 */
	@Reference private TraceOperations operations;
	
	
	//////////////////////////////////////////////
	// Trace commands
	//////////////////////////////////////////////
	/**
     * This method is optional. It allows automatic command hiding in situations when the command should not be visible.
     *
     * You can define multiple methods annotated with {@link CliAvailabilityIndicator} if your commands have differing
     * visibility requirements.
     * 
     * @return true (default) if the command should be visible at this stage, false otherwise
     */
	
	@CliAvailabilityIndicator("trace setup")
	boolean isSetupTraceCommandAvailable() {
		return operations.isSetupTraceCommandAvailable();
	}
	
	/**
     * Configure the olingo dependencies in maven and 
     * 
     */
    @CliCommand(value = "trace setup", help = "setup the configuration for tracing a class")
    public void setupTrace(
    		@CliOption(key = "class", mandatory=true, help = "The name of the class to trace") final JavaType className){
    	operations.setupTrace(className);
    }
    
}