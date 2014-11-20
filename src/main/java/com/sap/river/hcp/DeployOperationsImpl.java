package com.sap.river.hcp;

import java.io.IOException;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.project.MavenOperations;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

import com.sap.river.util.PomUtils;

@Component
@Service
public class DeployOperationsImpl implements DeployOperations {

	private static Logger LOGGER = Logger.getLogger(DeployOperationsImpl.class
			.getName());

	private final String SAP_CLOUD_HOST_PROP = "sap.cloud.host";
	private final String SAP_CLOUD_ACCOUNT_PROP = "sap.cloud.account";
	private final String SAP_CLOUD_USERNAME_PROP = "sap.cloud.username";
	private final String SAP_CLOUD_PASSWORD_PROP = "sap.cloud.password";
	private final String SAP_LOCAL_SERVER_ROOT = "local.server.root";
	
	private final String REMOTE_CONFIGURATION_BASE_PATH = "/configuration/river/deploy";
	
	private final String LOCAL_CONFIGURATION_BASE_PATH = "/configuration/river/deploy-local";
	private final String LOCAL_CONFIGURATION_PLUGIN_PATH = LOCAL_CONFIGURATION_BASE_PATH + "/build/plugins/plugin";
	private final String LOCAL_CONFIGURATION_PROPERTIES_PATH = LOCAL_CONFIGURATION_BASE_PATH + "/properties/*";
	
	private final String DEPENDENCY_SUBPATH = "/dependencies/dependency";
	private final String PLUGIN_SUBPATH = "/build/plugins/plugin";
	private final String PROPERTIES_SUBPATH = "/properties/*";
	
	private static final String POM_PROFILE_ID_VAL = "profile-install-hcp";
	
	private static final String NEO_PLUGIN_NAME = "neo-java-web";
	private static final String NEO_PLUGIN_COMMAND_INSTALL_LOCAL = "install-local";
	private static final String NEO_PLUGIN_DEPLOY_LOCAL = "deploy-local";
	private static final String NEO_PLUGIN_START_LOCAL = "start-local";

	/** river specific details in the project setup files (such as pom.xml etc.) */
	@Reference
	private ProjectOperations projectOperations;
	@Reference
	private MavenOperations mavenOperations;

	// /////////////////////////////////////////
	// API
	///////////////////////////////////////////

	@Override
	public boolean isSetupDeployRemoteAvailable() {	
		return true;
	}
	
	@Override
	//TODO, return true/false, depending on whether or not the .war file exists
	public boolean isDeployRemoteAvailable() {			
		return true;
	}
	
	@Override
	public boolean isSetupDeployLocalAvailable() {
		return true;
	}
	
	@Override
	//TODO, return true/false, depending on whether or not the .war file exists
	public boolean isDeployLocalAvailable() {
		return true;
	}

	@Override
	public void setupDeployRemote(final String host, final String account, final String userName,
			final String password) {
		Pom currentPom = PomUtils.getCurrentPOM(projectOperations);

		// read the configuration file from the current CLASSPATH
		Element configuration = XmlUtils.getConfiguration(getClass());
		
		String moduleName = currentPom.getModuleName();

        // update the plugins in the POM, based on configuration
		PomUtils.addPluginsToProfile(projectOperations, REMOTE_CONFIGURATION_BASE_PATH + PLUGIN_SUBPATH, configuration, POM_PROFILE_ID_VAL);

        // update the dependencies in the POM, based on configuration
		PomUtils.addDependencies(projectOperations, REMOTE_CONFIGURATION_BASE_PATH + DEPENDENCY_SUBPATH, configuration, moduleName);
        
        // add properties to project operation
		PomUtils.addPropertiesToProjectOp(projectOperations, REMOTE_CONFIGURATION_BASE_PATH + PROPERTIES_SUBPATH, configuration, moduleName);

        // if optd, create custom properties for them
        PomUtils.updateInputRemoteProperties(projectOperations, moduleName, host, account, userName, password, SAP_CLOUD_HOST_PROP, SAP_CLOUD_ACCOUNT_PROP, 
        		SAP_CLOUD_USERNAME_PROP, SAP_CLOUD_PASSWORD_PROP);
        
	}

	@Override
	public void deployRemoteCommand(String command, String host, String account,
			String userName, String password) {		
		Validate.notNull(command, "Plugin command required");
		StringBuffer sb = (new StringBuffer(NEO_PLUGIN_NAME)).append(":").append(command);
		if (!StringUtils.isBlank(host)) {
			sb.append(" -D").append(SAP_CLOUD_HOST_PROP).append("=").append(host);
		} 
		if (!StringUtils.isBlank(account)) {
			sb.append(" -D").append(SAP_CLOUD_ACCOUNT_PROP).append("=").append(account);
		}
		if (!StringUtils.isBlank(userName)) {
			sb.append(" -D").append(SAP_CLOUD_USERNAME_PROP).append("=").append(userName);
		}
		if (!StringUtils.isBlank(password)) {
			sb.append(" -D").append(SAP_CLOUD_PASSWORD_PROP).append("=").append(password);
		}
		sb.append(" -P " + POM_PROFILE_ID_VAL);
		try {			
			mavenOperations.executeMvnCommand(sb.toString());
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}
	}

	@Override
	public void setupDeployLocal(String root) {
		// read the configuration file from the current CLASSPATH
		Element configuration = XmlUtils.getConfiguration(getClass());

		String moduleName = projectOperations.getFocusedModule().getModuleName();

		// update the plugins in the POM, based on configuration
		PomUtils.addPluginsToProfile(projectOperations, LOCAL_CONFIGURATION_PLUGIN_PATH, configuration, POM_PROFILE_ID_VAL);

		// update the dependencies in the POM, based on configuration
		PomUtils.addDependencies(projectOperations, LOCAL_CONFIGURATION_BASE_PATH + DEPENDENCY_SUBPATH, configuration, moduleName);

		// add properties to project operation
		PomUtils.addPropertiesToProjectOp(projectOperations, LOCAL_CONFIGURATION_PROPERTIES_PATH, configuration, moduleName);
		
		// if optd, create custom properties for them
		PomUtils.updateInputLocalProperties(projectOperations, moduleName, root, SAP_LOCAL_SERVER_ROOT);
	}
	
	@Override
	public void deployLocalCommand() {		
		//install
		StringBuffer sb = (new StringBuffer(NEO_PLUGIN_NAME)).append(":").append(NEO_PLUGIN_COMMAND_INSTALL_LOCAL).append(" -P " + POM_PROFILE_ID_VAL);
		try {			
			mavenOperations.executeMvnCommand(sb.toString());
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}
		
		//deploy
		sb = (new StringBuffer(NEO_PLUGIN_NAME)).append(":").append(NEO_PLUGIN_DEPLOY_LOCAL).append(" -P " + POM_PROFILE_ID_VAL);
		try {			
			mavenOperations.executeMvnCommand(sb.toString());
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}
		
		//start
		sb = (new StringBuffer(NEO_PLUGIN_NAME)).append(":").append(NEO_PLUGIN_START_LOCAL).append(" -P " + POM_PROFILE_ID_VAL);
		try {			
			mavenOperations.executeMvnCommand(sb.toString());
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}
	}

}
