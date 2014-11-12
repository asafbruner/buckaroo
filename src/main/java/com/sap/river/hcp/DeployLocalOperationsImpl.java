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
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

@Component
@Service
public class DeployLocalOperationsImpl implements DeployLocalOperations {

	private static Logger LOGGER = Logger.getLogger(DeployLocalOperationsImpl.class.getName());

	private final String CONFIGURATION_BASE_PATH = "/configuration/river/deploy-local";
	private final String CONFIGURATION_PLUGIN_PATH = CONFIGURATION_BASE_PATH + "/build/plugins/plugin";
	private final String CONFIGURATION_PROPERTIES_PATH = CONFIGURATION_BASE_PATH + "/properties/*";

	@Reference
	private ProjectOperations projectOperations;

	@Override
	public boolean isSetupDeployLocalAvailable() {
		return true;
	}

	@Override
	public void setupDeployLocal() {
		LOGGER.info("deploy " + projectOperations.getFocusedModule().getPath());

		// read the configuration file from the current CLASSPATH
		Element configuration = XmlUtils.getConfiguration(getClass());

		// Identify the required plugins
		final List<Plugin> requiredPlugins = new ArrayList<Plugin>();

		final List<Element> buildPlugins = XmlUtils.findElements(CONFIGURATION_PLUGIN_PATH, configuration);
		for (final Element pluginElement : buildPlugins) {
			requiredPlugins.add(new Plugin(pluginElement));
		}

		String moduleName = projectOperations.getFocusedModule().getModuleName();
		// update the POM the new configuration
		projectOperations.removeBuildPlugins(moduleName, requiredPlugins);
		projectOperations.addBuildPlugins(moduleName, requiredPlugins);

		// Identify the dependencies
		final List<Dependency> requiredDependencies = new ArrayList<Dependency>();

		final List<Element> dependencies = XmlUtils.findElements(CONFIGURATION_BASE_PATH + "/dependencies/dependency", configuration);
		for (final Element dependencyElement : dependencies) {
			requiredDependencies.add(new Dependency(dependencyElement));
		}

		// update the POM the new configuration
		projectOperations.removeDependencies(moduleName, requiredDependencies);
		projectOperations.addDependencies(moduleName, requiredDependencies);

		// Identify the properties
		final List<Element> pomProperties = XmlUtils.findElements(CONFIGURATION_PROPERTIES_PATH, configuration);
		for (final Element property : pomProperties) {
			projectOperations.addProperty(moduleName, new Property(property));
		}
	}
}
