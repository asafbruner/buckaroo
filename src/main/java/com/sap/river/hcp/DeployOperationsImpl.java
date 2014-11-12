package com.sap.river.hcp;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

@Component
@Service
public class DeployOperationsImpl implements DeployOperations {
	
	private static Logger LOGGER = Logger.getLogger(DeployOperationsImpl.class.getName());
	
	/** plugins XPath in pox.xml */
	@SuppressWarnings("unused")
	private static final String PROJECT_BUILD_PLUGIN_PATH = "/project/build/plugins/plugin";
	/** plugin XPath in pom.xml */
	@SuppressWarnings("unused")
	private static final String PROJECT_BUILD_PLUGINS_PATH = "/project/build/plugins";
	/** plugin XPath in configuration.xml */
	private static final String CONFIGURATION_PLUGIN_PATH = "/configuration/river/build/plugins/plugin";
	
	/**
	 * OSGi helpers that allows the plug-in to configure river specific details in the project
	 * setup files (such as pom.xml etc.)
	 */
	@Reference private ProjectOperations projectOperations;
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
	public void setupDeploy() {
		Pom currentPom = getCurrentPOM();
		LOGGER.info("deploy "+ currentPom.getPath());
		
		//read the configuration file from the current CLASSPATH
		Element configuration = XmlUtils.getConfiguration(getClass());
		
		// Identify the required plugins
        final List<Plugin> requiredPlugins = new ArrayList<Plugin>();
		
		final List<Element> buildPlugins = XmlUtils.findElements(CONFIGURATION_PLUGIN_PATH, configuration);
        for (final Element pluginElement : buildPlugins) {
            requiredPlugins.add(new Plugin(pluginElement));
        }
        
        //update the POM the new configuration
        projectOperations.removeBuildPlugins(currentPom.getModuleName(), requiredPlugins); //TODO: needed?
        projectOperations.addBuildPlugins(currentPom.getModuleName(), requiredPlugins);
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
	
	/** remove plug-in entry from the POM Document
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
	
	/** add plug-in entry from the POM Document
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
