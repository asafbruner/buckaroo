package com.sap.river.util;

import java.util.ArrayList;
import java.util.List;
//import java.util.logging.Logger;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.DependencyScope;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Property;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class PomUtils {
//	private static Logger LOGGER = Logger.getLogger(PomUtils.class.getName());	

	// ////////////////////////////////////////////Plugins//////////////////////////////

	/**
	 * @return - POM object of the current project or the root project if no
	 *         other projects are in focus
	 */
	public static Pom getCurrentPOM(ProjectOperations projectOperations) {
		return projectOperations.getFocusedModule();
	}

	// generate the required plugin list base on configuration file
	private static List<Plugin> generateRequiredPlugins(String pluginPath,
			Element configuration) {
		final List<Plugin> requiredPlugins = new ArrayList<Plugin>();

		final List<Element> buildPlugins = XmlUtils.findElements(pluginPath,
				configuration);
		for (final Element pluginElement : buildPlugins) {
			requiredPlugins.add(new Plugin(pluginElement));
		}

		return requiredPlugins;
	}

	//Add plugins to the POM in profile section
	public static void addPluginsToProfile(ProjectOperations projectOperations,
			String pluginPath, Element configuration, String pomProfileIDVal) {		
		// get the plugins required for this configuration
		final List<Plugin> requiredPlugins = generateRequiredPlugins(
				pluginPath, configuration);
		
		//get the full path/name of the pom
		final String fullFilePathName = projectOperations.getPathResolver().getRoot() + "\\pom.xml";

		// get the pom document and its root element
		Document document = XMLUtils.getPOMDocument(fullFilePathName);
		if (document == null){
			return;//log has already been written
		}
		Element root = document.getDocumentElement();
		Element plugins = XMLUtils.initiateProfile(document, root, pomProfileIDVal);
		if (plugins == null){
			return;//log with error message already given
		}
		
		//for each plugin, generate
		for (Plugin onePlugin : requiredPlugins){
			//convert the plugin into an XML element
			Element plugin = onePlugin.getElement(document);
			
			//add this plugin to the plugins element
			XMLUtils.addPluginToProfile(plugins, plugin);
		}	
		
		//add the failsafe plugin
		plugins.appendChild(XMLUtils.generateMvnFailsafePlugin(document));

		//save all changes made above into file
		XMLUtils.saveXMLDocIntoFile(document, fullFilePathName);
	}

	// ////////////////////////////////////////////Dependencies//////////////////////////////

/**
 * Disable a dependency identified by it's groupId and artifactId.
 * Currently - remove the dependency.
 * 
 * @param projectOperations
 * @param moduleName
 * @param groupId
 * @param artifactId
 */
	public static void removeDependency(ProjectOperations projectOperations, String moduleName, String groupId, String artifactId) {
		Dependency dep = getDependency(projectOperations, groupId, artifactId);
		if (dep != null) {
			projectOperations.removeDependency(moduleName, dep);
		}
	}

/**
 * Set the scope of a dependency identified by it's groupId and artifactId.

 * @param projectOperations
 * @param moduleName
 * @param groupId
 * @param artifactId
 * @param scope
 */
	public static void setDependencyScope(ProjectOperations projectOperations, String moduleName, String groupId, String artifactId, DependencyScope scope) {
		Dependency dep = getDependency(projectOperations, groupId, artifactId);
		if (dep != null) {
			projectOperations.updateDependencyScope(moduleName, dep, scope);
		}
	}

/**
 * Add an exclusion of a dependency identified by it's groupId and artifactId.
 * 	
 * @param projectOperations
 * @param moduleName
 * @param groupId
 * @param artifactId
 * @param exGroupId
 * @param exArtifactId
 */
	public static void excludeDependency(ProjectOperations projectOperations, String moduleName, String groupId, String artifactId, String exGroupId, String exArtifactId) {
		Dependency dep = getDependency(projectOperations, groupId, artifactId);
		if (dep != null) {
			dep.addExclusion(exGroupId, exArtifactId);
			// TODO - fix a roo bug - addDependency does not support an update and projectOperations.removeDependency does not clean the set
			projectOperations.getPomFromModuleName(moduleName).getDependencies().remove(dep);  
			projectOperations.addDependency(moduleName, dep);
		}
	}

/**
 * TODO - consider returning a collection of dependencies with these id's.
 * @param projectOperations
 * @param groupId
 * @param artifactId
 * @return
 */
	private static Dependency getDependency(ProjectOperations projectOperations, String groupId, String artifactId) {
		final Set<Dependency> dependencies = projectOperations.getFocusedModule().getDependencies();

		for (final Dependency dependency : dependencies) {
			String gId = dependency.getGroupId();
			String aId = dependency.getArtifactId();
			if (gId.equals(groupId) && aId.equals(artifactId)) {
				return dependency;
			}
        }
		return null;
	}

	// generate dependency list based on this configuration
	private static List<Dependency> generateDependencies(String dependencyPath,
			Element configuration) {
		final List<Dependency> requiredDependencies = new ArrayList<Dependency>();

		final List<Element> dependencies = XmlUtils.findElements(
				dependencyPath, configuration);
		for (final Element dependencyElement : dependencies) {
			requiredDependencies.add(new Dependency(dependencyElement));
		}

		return requiredDependencies;
	}

	// replace dependencies in POM
	public static void addDependencies(ProjectOperations projectOperations,
			String dependencyPath, Element configuration, String moduleName) {
		// get the dependencies required for this configuration
		final List<Dependency> requiredDependencies = generateDependencies(
				dependencyPath, configuration);

		projectOperations.addDependencies(moduleName, requiredDependencies);
	}

	// ////////////////////////////////////////////Properties//////////////////////////////

	// add Properties To Project Operations
	public static void addPropertiesToProjectOp(
			ProjectOperations projectOperations, String pomPropertiesPath,
			Element configuration, String moduleName) {
		final List<Element> pomProperties = XmlUtils.findElements(
				pomPropertiesPath, configuration);
		for (final Element property : pomProperties) {
			projectOperations.addProperty(moduleName, new Property(property));
		}
	}

	// remove plug-in entry from the POM Document
	public static void updateInputRemoteProperties(
			ProjectOperations projectOperations, final String moduleName,
			final String host, final String account, final String userName,
			final String password, final String hostPropName,
			final String acctPropName, final String userPropName,
			final String pswdPropName) {
		if (!StringUtils.isBlank(host)) {
			projectOperations.addProperty(moduleName, new Property(
					hostPropName, host));
		}
		if (!StringUtils.isBlank(account)) {
			projectOperations.addProperty(moduleName, new Property(
					acctPropName, account));
		}
		if (!StringUtils.isBlank(userName)) {
			projectOperations.addProperty(moduleName, new Property(
					userPropName, userName));
		}
		if (!StringUtils.isBlank(password)) {
			projectOperations.addProperty(moduleName, new Property(
					pswdPropName, password));
		}
	}

	//update a property to the root
	public static void updateInputLocalProperties(
			ProjectOperations projectOperations, final String moduleName,
			final String root, final String rootPropName) {
		
		if (!StringUtils.isBlank(root)) {
			projectOperations.addProperty(moduleName, new Property(
					rootPropName, root));
		}
	}

}
