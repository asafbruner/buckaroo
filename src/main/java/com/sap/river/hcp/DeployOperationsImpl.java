package com.sap.river.hcp;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.project.Dependency;
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

	private final String REMOTE_CONFIGURATION_BASE_PATH = "/configuration/river/deploy";
	private final String LOCAL_CONFIGURATION_BASE_PATH = "/configuration/river/deploy-local";
	private final String LOCAL_CONFIGURATION_PLUGIN_PATH = LOCAL_CONFIGURATION_BASE_PATH + "/build/plugins/plugin";
	private final String LOCAL_CONFIGURATION_PROPERTIES_PATH = LOCAL_CONFIGURATION_BASE_PATH + "/properties/*";

	/**
	 * OSGi helpers that allows the plug-in to configure river specific details in the project setup files (such as pom.xml etc.)
	 */
	@Reference
	private ProjectOperations projectOperations;

	// @Reference private FileManager fileManager;
	// @Reference private PathResolver pathResolver;

	// /////////////////////////////////////////
	// API
	// /////////////////////////////////////////

	@Override
	public boolean isSetupDeployRemoteAvailable() {
		return true;
	}

	@Override
	public void setupDeployRemote() {
		Pom currentPom = getCurrentPOM();
		LOGGER.info("deploy " + currentPom.getPath());

		// read the configuration file from the current CLASSPATH
		Element configuration = XmlUtils.getConfiguration(getClass());
		
		String moduleName = currentPom.getModuleName();

		// update the plugins in the POM, based on configuration
		replacePlugins(REMOTE_CONFIGURATION_BASE_PATH + "/build/plugins/plugin", configuration, moduleName);

		// update the dependencies in the POM, based on configuration
		replaceDependencies(REMOTE_CONFIGURATION_BASE_PATH + "/dependencies/dependency", configuration, moduleName);

		// Identify the properties
		addPropertiesToProjectOp(REMOTE_CONFIGURATION_BASE_PATH + "/properties/*", configuration, moduleName);
	}
	
	
	@Override
	public boolean isSetupDeployLocalAvailable() {
		return true;
	}
	
	@Override
	public void setupDeployLocal(String account, String userName, String password) {
		LOGGER.info("deploy " + projectOperations.getFocusedModule().getPath());

		// read the configuration file from the current CLASSPATH
		Element configuration = XmlUtils.getConfiguration(getClass());

		String moduleName = projectOperations.getFocusedModule().getModuleName();
		
		// update the plugins in the POM, based on configuration
		replacePlugins(LOCAL_CONFIGURATION_PLUGIN_PATH, configuration, moduleName);

		// update the dependencies in the POM, based on configuration
		replaceDependencies(LOCAL_CONFIGURATION_BASE_PATH + "/dependencies/dependency", configuration, moduleName);

		// Identify the properties
		addPropertiesToProjectOp(LOCAL_CONFIGURATION_PROPERTIES_PATH, configuration, moduleName);
	}

	// /////////////////////////////////////////
	// Auxiliary Functions
	// /////////////////////////////////////////
	/**
	 * @return - POM object of the current project or the root project if no other projects are in focus
	 */
	private Pom getCurrentPOM() {
		return projectOperations.getFocusedModule();
	}
	
	//generate the required plugin list base on configuration file
	private List<Plugin> generateRequiredPlugins(String pluginPath, Element configuration){
		final List<Plugin> requiredPlugins = new ArrayList<Plugin>();
		
		final List<Element> buildPlugins = XmlUtils.findElements(pluginPath, configuration);
		for (final Element pluginElement : buildPlugins) {
			requiredPlugins.add(new Plugin(pluginElement));
		}
		
		return requiredPlugins;
	}
	
	//replace plugins in POM
	private void replacePlugins(String pluginPath, Element configuration, String moduleName){
		//get the plugins required for this configuration
		final List<Plugin> requiredPlugins = generateRequiredPlugins(pluginPath, configuration);
		
		//projectOperations.removeBuildPlugins(moduleName, requiredPlugins);
		projectOperations.addBuildPlugins(moduleName, requiredPlugins);
	}
	
	//generate dependency list based on this configuration
	private List<Dependency> generateDependencies(String dependencyPath, Element configuration){
		final List<Dependency> requiredDependencies = new ArrayList<Dependency>();
		
		final List<Element> dependencies = XmlUtils.findElements(dependencyPath, configuration);
		for (final Element dependencyElement : dependencies) {
			requiredDependencies.add(new Dependency(dependencyElement));
		}
		
		return requiredDependencies;
	}
	
	//replace dependencies in POM
	private void replaceDependencies(String dependencyPath, Element configuration, String moduleName){			
		//get the dependencies required for this configuration
		final List<Dependency> requiredDependencies = generateDependencies(dependencyPath, configuration);
		
		//projectOperations.removeDependencies(moduleName, requiredDependencies);
		projectOperations.addDependencies(moduleName, requiredDependencies);
	}
	
	//add Properties To Project Operations
	private void addPropertiesToProjectOp(String pomPropertiesPath, Element configuration, String moduleName){
		final List<Element> pomProperties = XmlUtils.findElements(pomPropertiesPath, configuration);
		for (final Element property : pomProperties) {
			projectOperations.addProperty(moduleName, new Property(property));
		}
	}
	
	

	/**
	 * remove plug-in entry from the POM Document
	 * @param buildPlugin - the entry to remove
	 */
	// private void removeBuildPlugin(Pom pom, Plugin buildPlugin) {
	// //remove the input plugin entry from the pom.xml Document
	// Document document = XmlUtils.readXml(fileManager.getInputStream(pom.getPath()));
	// Element root = document.getDocumentElement();
	// Element pluginsElement = XmlUtils.findFirstElement(PROJECT_BUILD_PLUGIN_PATH, root);
	// for (Element pluginXml : XmlUtils.findElements(PROJECT_BUILD_PLUGIN_PATH, root)) {
	// Plugin candidate = new Plugin(pluginXml);
	// if (arePluginsCompatible(buildPlugin, candidate)) {
	// LOGGER.info("removing plugin "+ candidate.getGAV().toString() + " from pom.xml");
	// pluginsElement.removeChild(pluginXml);
	// }
	// }
	// DomUtils.removeTextNodes(pluginsElement);
	//
	// //update pom.xml file with the new content
	// fileManager.createOrUpdateTextFileIfRequired(pom.getPath(), XmlUtils.nodeToString(document), true);
	// }

	/**
	 * add plug-in entry from the POM Document
	 * @param buildPluginElement - the element entry to add
	 */
	// private void addBuildPluginElement(Pom pom, Element buildPluginElement) {
	// //add the input plugin entry from the pom.xml Document
	// Document document = XmlUtils.readXml(fileManager.getInputStream(pom.getPath()));
	// Element root = document.getDocumentElement();
	// Element pluginsElement = XmlUtils.findFirstElement(PROJECT_BUILD_PLUGINS_PATH, root);
	// if (pluginsElement != null) {
	// // Append the build plugin passed via parameter pluginXML to the build plugins element of POM
	// Node plugin = document.importNode(buildPluginElement, true);
	// pluginsElement.appendChild(plugin);
	// LOGGER.info("add build plugin ");
	// }
	//
	// //update pom.xml file with the new content
	// fileManager.createOrUpdateTextFileIfRequired(pom.getPath(), XmlUtils.nodeToString(document), true);
	// }

	// private boolean arePluginsCompatible(Plugin src, Plugin other) {
	// if (src == null || other == null) {
	// return false;
	// }
	//
	// int result = src.getGroupId().compareTo(other.getGroupId());
	// if (result == 0) {
	// result = src.getArtifactId().compareTo(other.getArtifactId());
	// }
	//
	// return (result == 0);
	// }

}
