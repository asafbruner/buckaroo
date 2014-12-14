package com.sap.buckaroo.hcp;

//import java.util.logging.Logger;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.DependencyScope;
import org.springframework.roo.project.MavenOperations;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

import com.sap.buckaroo.util.Constants;
import com.sap.buckaroo.util.FileUtil;
import com.sap.buckaroo.util.PomUtils;


@Component
@Service
public class DeployOperationsImpl implements DeployOperations {

// TODO - used logs
	private static Logger LOGGER = Logger.getLogger(DeployOperationsImpl.class
			.getName());
	@Reference private FileManager fileManager;
	@Reference private PathResolver pathResolver;


	private final String SAP_CLOUD_HOST_PROP = "sap.cloud.host";
	private final String SAP_CLOUD_ACCOUNT_PROP = "sap.cloud.account";
	private final String SAP_CLOUD_USERNAME_PROP = "sap.cloud.username";
	private final String SAP_CLOUD_PASSWORD_PROP = "sap.cloud.password";
	private final String SAP_LOCAL_SERVER_ROOT = "local.server.root";

	private final String REMOTE_CONFIGURATION_BASE_PATH = "/configuration/buckaroo/deploy";

	private final String LOCAL_CONFIGURATION_BASE_PATH = "/configuration/buckaroo/deploy-local";
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

	private static final String LOG4J_PROPERTIES = "log4j.properties";
	/** buckaroo specific details in the project setup files (such as pom.xml etc.) */
	@Reference
	private ProjectOperations projectOperations;
	@Reference
	private MavenOperations mavenOperations;
	@Reference private Shell shell;
	
	//TODO, following is a very unelegant solution that should be fixed later
	//Following is used to ensure that internal commands are not exposed externally (the flag is toggled just before running the command, and then toggled again)
	private boolean isAllowCommandExternally = false;

	// /////////////////////////////////////////
	// API
	// /////////////////////////////////////////

	@Override
	public boolean isSetupDeployRemoteAvailable() {	
		return isAllowCommandExternally;
	}
	
	@Override
	public boolean isDeployRemoteAvailable() {			
		return true;
	}

	@Override
	//TODO, return true/false, depending on whether or not the .war file exists
	public boolean isSetupHCPConfigAvailable() {			
		return true;
	}

	@Override
	public boolean isSetupDeployLocalAvailable() {
		return true;
	}

	@Override
	// TODO, return true/false, depending on whether or not the .war file exists
	public boolean isDeployLocalAvailable() {
		return true;
	}

	@Override
	//This method runs three things:
	//1) adjust the POM
	//2) install the SDK
	//3) setup the webapp
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
		PomUtils.updateInputRemoteProperties(projectOperations, moduleName, host, account, userName, password, SAP_CLOUD_HOST_PROP,
				SAP_CLOUD_ACCOUNT_PROP, SAP_CLOUD_USERNAME_PROP, SAP_CLOUD_PASSWORD_PROP);

//		supportRemoteLogger(projectOperations, moduleName);
	}

	/**
	 * To avoid conflicts with the HCP side logging implementation, all logging dependencies have to be discarded.
	 *
	 * Organize the slf4j dependencies in a deploy-able manner: 1. Remove unnecessary dependencies + log4j.properties 2. Set "slf4j-api"
	 * scope to be "provided" (i.e. provided by the framework") 3. Add exclusion of the jcl-pver-slf4j to dependent modules. 4. Remove the
	 * log4j.properties
	 *
	 *
	 * TODO - use the correct method Method : direct to test scope + create tests folder + move log4j.properties to the tests\resources
	 * folder Get dependent modules programmatically
	 *
	 */
	private void supportRemoteLogger(ProjectOperations projectOperations, String moduleName) {
		PomUtils.setDependencyScope(projectOperations, moduleName, "org.slf4j", "slf4j-api", DependencyScope.PROVIDED);
		PomUtils.removeDependency(projectOperations, moduleName, "log4j", "log4j");
		PomUtils.removeDependency(projectOperations, moduleName, "org.slf4j", "jcl-over-slf4j");
		PomUtils.removeDependency(projectOperations, moduleName, "org.slf4j", "slf4j-log4j12");
		PomUtils.removeDependency(projectOperations, moduleName, "commons-pool", "commons-pool");
		PomUtils.removeDependency(projectOperations, moduleName, "commons-dbcp", "commons-dbcp");
		PomUtils.excludeDependency(projectOperations, moduleName, "org.apache.tiles", "tiles-jsp", "org.slf4j", "jcl-over-slf4j");

		final String log4jPropsPath = projectOperations.getPathResolver().getFocusedIdentifier(Path.SRC_MAIN_RESOURCES, LOG4J_PROPERTIES);

		if (fileManager.exists(log4jPropsPath)) {
			fileManager.delete(log4jPropsPath);
		}

	}
	
	//if curVal is populated, return it.  Otherwise, return the value from the properties file (or null if it didn't exist)
	private String getPropValFromFileIfNecessary(Properties props, String curVal, String propKey){
		if ((curVal != null) && (!curVal.equals("")))
			return curVal;

		return FileUtil.getPropertyByKey(curVal, propKey, props, LOGGER);
	}

	@Override
	public void deployRemoteCommand(String command, String host, String account, String userName, String password) {
		Validate.notNull(command, "Plugin command required");

		//We look for parameters only if it is a deploy command
		Map<String, String> propKeyValues = new HashMap<String, String>();
		if (command.equals(Constants.DEPLOY)){
			
			//get the configuration file's path/name
			final String configPropertiesFilePathName= FileUtil.getPropertiesPath(fileManager, pathResolver, Constants.RESOURCE_DIR + Constants.CONFIG_PROPERTIES_FILE, false, LOGGER);
			
			//use the following to determine whether or not we need to resave data (save only if properties were received)
			final boolean isPropsReceived = (host != null) || (account != null) || (userName != null) || (password != null)? true:false; 
			
			if ((account==null) || (host==null) || (userName==null) || (password==null)){
				//try to get the values from the configuration file
				Properties props = new Properties();
				InputStream inputStr = null;
				try{
					inputStr = new FileInputStream(configPropertiesFilePathName);
					props.load(inputStr);
					
					//Get whatever was missing, return if it was missing from properties
					if ((account = getPropValFromFileIfNecessary(props, account, Constants.HCP_REMOTE_ACCOUNT)) == null)
						return;
					if ((host = getPropValFromFileIfNecessary(props, host, Constants.HCP_REMOTE_HOST)) == null)
						return;
					if ((userName = getPropValFromFileIfNecessary(props, userName, Constants.HCP_REMOTE_USER)) == null)
						return;
					if ((password = getPropValFromFileIfNecessary(props, password, Constants.HCP_REMOTE_PSWD)) == null)
						return;
//					if ((account = FileUtil.getPropertyByKey(account, Constants.HCP_REMOTE_ACCOUNT, props, LOGGER)) == null)
//						return;
//					if ((host = FileUtil.getPropertyByKey(host, Constants.HCP_REMOTE_HOST, props, LOGGER)) == null)
//						return;				
//					if ((userName = FileUtil.getPropertyByKey(userName, Constants.HCP_REMOTE_USER, props, LOGGER)) == null)
//						return;
//					if ((password = FileUtil.getPropertyByKey(password, Constants.HCP_REMOTE_PSWD, props, LOGGER)) == null)
//						return;
				}catch (IOException e){
					LOGGER.info("Exception received when opening file " + configPropertiesFilePathName + ": " + e.toString());//TODO KM
					return;
				}
				finally{
					FileUtil.closeInput(inputStr, LOGGER);
				}
			}
			//populate the properties map for saving
			propKeyValues.put(Constants.HCP_REMOTE_ACCOUNT, account);
			propKeyValues.put(Constants.HCP_REMOTE_HOST, host);
			propKeyValues.put(Constants.HCP_REMOTE_USER, userName);
			propKeyValues.put(Constants.HCP_REMOTE_PSWD, password);
			
			//regardless of where the parameters came from (input or from properties file), save them now
			if (isPropsReceived){
				//If there was an error within, don't continue on
				if (FileUtil.createUpdateConfigPropertiesFile(propKeyValues, configPropertiesFilePathName, LOGGER) == false)
					return;//if we couldn't save the parameters for whatever reason, don't continue on (log was already written)
			}
		}

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
	public void setupHCPConfig(){
		//update the pom
		//temporarily toggle this flag, in order to allow running of the following command
		isAllowCommandExternally = true;//set this true temporarily, just in order to run the following
		shell.executeCommand("hcp setup remote-deploy");
		isAllowCommandExternally = false;
		
		//install the HCP sdk locally
		shell.executeCommand("hcp remote-deploy --goal " + Constants.INSTALL_SDK);
		
		//Some more configuration changes needed to support web and UI5
		//temporarily toggle this flag, in order to allow running of the following command
		WebAppOperationsImpl.setIsAllowCommandExternally(true);
		shell.executeCommand("hcp setup webapp");
		WebAppOperationsImpl.setIsAllowCommandExternally(false);
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
		// install
		StringBuffer sb = (new StringBuffer(NEO_PLUGIN_NAME)).append(":").append(NEO_PLUGIN_COMMAND_INSTALL_LOCAL)
				.append(" -P " + POM_PROFILE_ID_VAL);
		try {
			mavenOperations.executeMvnCommand(sb.toString());
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}

		// deploy
		sb = (new StringBuffer(NEO_PLUGIN_NAME)).append(":").append(NEO_PLUGIN_DEPLOY_LOCAL).append(" -P " + POM_PROFILE_ID_VAL);
		try {
			mavenOperations.executeMvnCommand(sb.toString());
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}

		// start
		sb = (new StringBuffer(NEO_PLUGIN_NAME)).append(":").append(NEO_PLUGIN_START_LOCAL).append(" -P " + POM_PROFILE_ID_VAL);
		try {
			mavenOperations.executeMvnCommand(sb.toString());
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}
	}
}
