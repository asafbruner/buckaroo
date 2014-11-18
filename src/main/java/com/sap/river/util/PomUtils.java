package com.sap.river.util;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

public class PomUtils {
	/**
	 * @return - POM object of the current project or the root project if no
	 *         other projects are in focus
	 */
	public static Pom getCurrentPOM(ProjectOperations projectOperations) {
		return projectOperations.getFocusedModule();
	}
	
	// generate the required plugin list base on configuration file
	private static List<Plugin> generateRequiredPlugins(String pluginPath, Element configuration) {
		final List<Plugin> requiredPlugins = new ArrayList<Plugin>();

		final List<Element> buildPlugins = XmlUtils.findElements(pluginPath,
				configuration);
		for (final Element pluginElement : buildPlugins) {
			requiredPlugins.add(new Plugin(pluginElement));
		}

		return requiredPlugins;
	}
	
	// replace plugins in POM
	public static void addPlugins(ProjectOperations projectOperations, String pluginPath, Element configuration,
			String moduleName) {
		// get the plugins required for this configuration
		final List<Plugin> requiredPlugins = generateRequiredPlugins(pluginPath, configuration);

		// projectOperations.removeBuildPlugins(moduleName, requiredPlugins);
		projectOperations.addBuildPlugins(moduleName, requiredPlugins);
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
	public static void addDependencies(ProjectOperations projectOperations, String dependencyPath,
			Element configuration, String moduleName) {
		// get the dependencies required for this configuration
		final List<Dependency> requiredDependencies = generateDependencies(
				dependencyPath, configuration);

		// projectOperations.removeDependencies(moduleName, requiredDependencies);
		projectOperations.addDependencies(moduleName, requiredDependencies);
	}

}
