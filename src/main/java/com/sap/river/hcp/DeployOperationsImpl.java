package com.sap.river.hcp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.MavenOperations;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Property;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

@Component
@Service
public class DeployOperationsImpl implements DeployOperations {
	
	private static Logger LOGGER = Logger.getLogger(DeployOperationsImpl.class.getName());
	
	private final String SAP_CLOUD_ACCOUNT_PROP = "sap.cloud.account";
	private final String SAP_CLOUD_USERNAME_PROP = "sap.cloud.username";
	private final String SAP_CLOUD_PASSWORD_PROP = "sap.cloud.password";
	private final String CONFIGURATION_BASE_PATH = "/configuration/river/deploy";
	
	/**river specific details in the project seup files (such as pom.xml etc.) */	
	@Reference private ProjectOperations projectOperations;
	@Reference private MavenOperations mavenOperations;
	
	//@Reference private FileManager fileManager;
	//@Reference private PathResolver pathResolver;
	
	///////////////////////////////////////////
	// API
	///////////////////////////////////////////

	@Override
	public boolean isSetupDeployAvailable() {
		//TODO: check if the war exists under target(?) directory
		return true;
	}

	@Override
	public void setupDeploy(final String account, final String userName, final String password) {
		Pom currentPom = getCurrentPOM();
		LOGGER.info("deploy "+ currentPom.getPath());
		
		//read the configuration file from the current CLASSPATH
		Element configuration = XmlUtils.getConfiguration(getClass());
		
		// Identify the required plugins
        final List<Plugin> requiredPlugins = new ArrayList<Plugin>();
		
		final List<Element> buildPlugins = XmlUtils.findElements(CONFIGURATION_BASE_PATH + "/build/plugins/plugin", configuration);
        for (final Element pluginElement : buildPlugins) {
            requiredPlugins.add(new Plugin(pluginElement));
        }
        
        //update the POM the new configuration
        projectOperations.removeBuildPlugins(currentPom.getModuleName(), requiredPlugins); //TODO: needed?
        projectOperations.addBuildPlugins(currentPom.getModuleName(), requiredPlugins);
        
        //Identify the dependencies
        final List<Dependency> requiredDependencies = new ArrayList<Dependency>();
        
        final List<Element> dependencies = XmlUtils.findElements(CONFIGURATION_BASE_PATH + "/dependencies/dependency", configuration);
        for (final Element dependencyElement : dependencies) {
            requiredDependencies.add(new Dependency(dependencyElement));
        }
        
        //update the POM the new configuration
        projectOperations.removeDependencies(currentPom.getModuleName(), requiredDependencies);
        projectOperations.addDependencies(currentPom.getModuleName(), requiredDependencies);
        
        //Identify the fixed properties
        final List<Element> pomProperties = XmlUtils.findElements(CONFIGURATION_BASE_PATH + "/properties/*", configuration);
        for (final Element property : pomProperties) {
            projectOperations.addProperty(currentPom.getModuleName(), new Property(property));
        }
        
        //if optd, create custom properties for them
        updateInputProperties(currentPom.getModuleName(), account, userName, password);
        
	}

	@Override
	public boolean isDeployAvailable() {
		//TODO: implement context-aware logic
		return true;
	}

	@Override
	public void deployCommand(String command, String account, String userName,
			String password) {
		Validate.notNull(command, "Plugin command required");
		StringBuffer sb = (new StringBuffer("neo-java-web:")).append(command);
		if (!StringUtils.isBlank(account)) {
			sb.append(" -D").append(SAP_CLOUD_ACCOUNT_PROP).append("=").append(account);
        }
        if (!StringUtils.isBlank(userName)) {
        	sb.append(" -D").append(SAP_CLOUD_USERNAME_PROP).append("=").append(userName);
        }
        if (!StringUtils.isBlank(password)) {
        	sb.append(" -D").append(SAP_CLOUD_PASSWORD_PROP).append("=").append(password);
        }
		try {
			mavenOperations.executeMvnCommand(sb.toString());
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}
	}

	///////////////////////////////////////////
	//Auxiliary Functions
	///////////////////////////////////////////
	/**
	 * @return - POM object of the current project or the root project if no other projects are in focus
	 */
	private Pom getCurrentPOM() {
		return projectOperations.getFocusedModule();
	}
	
	/** remove plug-in entry from the POM Document */
	private void updateInputProperties(final String moduleName, final String account, final String userName, final String password) {
		if (!StringUtils.isBlank(account)) {
        	projectOperations.addProperty(moduleName, new Property(SAP_CLOUD_ACCOUNT_PROP, account));
        }
        if (!StringUtils.isBlank(userName)) {
        	projectOperations.addProperty(moduleName, new Property(SAP_CLOUD_USERNAME_PROP, userName));
        }
        if (!StringUtils.isBlank(password)) {
        	projectOperations.addProperty(moduleName, new Property(SAP_CLOUD_PASSWORD_PROP, password));
        }
	}
	
	/** remove plug-in entry from the POM Document
	/**
	 * remove plug-in entry from the POM Document
	 * @param buildPlugin - the entry to remove
	 */
//	private void removeBuildPlugin(Pom pom, Plugin buildPlugin) {
//		//remove the input plugin entry from the pom.xml Document
//		Document document = XmlUtils.readXml(fileManager.getInputStream(pom.getPath()));
//		Element root = document.getDocumentElement();
//		Element pluginsElement = XmlUtils.findFirstElement(PROJECT_BUILD_PLUGIN_PATH, root);
//		for (Element pluginXml : XmlUtils.findElements(PROJECT_BUILD_PLUGIN_PATH, root)) {
//			Plugin candidate = new Plugin(pluginXml);
//			if (arePluginsCompatible(buildPlugin, candidate)) {
//				LOGGER.info("removing plugin "+ candidate.getGAV().toString() + " from pom.xml");
//				pluginsElement.removeChild(pluginXml);
//			}
//		}
//		DomUtils.removeTextNodes(pluginsElement);
//		
//		//update pom.xml file with the new content
//		fileManager.createOrUpdateTextFileIfRequired(pom.getPath(), XmlUtils.nodeToString(document), true);
//	}
	
	/**
	 * add plug-in entry from the POM Document
	 * @param buildPluginElement - the element entry to add
	 */
//	private void addBuildPluginElement(Pom pom, Element buildPluginElement) {
//		//add the input plugin entry from the pom.xml Document
//		Document document = XmlUtils.readXml(fileManager.getInputStream(pom.getPath()));
//		Element root = document.getDocumentElement();
//		Element pluginsElement = XmlUtils.findFirstElement(PROJECT_BUILD_PLUGINS_PATH, root);
//		if (pluginsElement != null) {
//			// Append the build plugin passed via parameter pluginXML to the build plugins element of POM
//			Node plugin  = document.importNode(buildPluginElement, true);
//			pluginsElement.appendChild(plugin);
//			LOGGER.info("add build plugin ");
//		}
//		
//		//update pom.xml file with the new content
//		fileManager.createOrUpdateTextFileIfRequired(pom.getPath(), XmlUtils.nodeToString(document), true);
//	}
	
//	private boolean arePluginsCompatible(Plugin src, Plugin other) {
//		if (src == null || other == null) {
//			return false;
//		}
//		
//		int result = src.getGroupId().compareTo(other.getGroupId());
//        if (result == 0) {
//            result = src.getArtifactId().compareTo(other.getArtifactId());
//        }
//        
//        return (result == 0);
//	}

}
