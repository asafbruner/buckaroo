package com.sap.river.util;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Property;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

public class PropertiesUtils {
	
	// add Properties To Project Operations
	public static void addPropertiesToProjectOp(ProjectOperations projectOperations, String pomPropertiesPath,
			Element configuration, String moduleName) {
		final List<Element> pomProperties = XmlUtils.findElements(pomPropertiesPath, configuration);
		for (final Element property : pomProperties) {
			projectOperations.addProperty(moduleName, new Property(property));
		}
	}

	/** remove plug-in entry from the POM Document */
	public static  void updateInputRemoteProperties(ProjectOperations projectOperations, final String moduleName, final String host, 
			final String account, final String userName, final String password, final String hostPropName, final String acctPropName, 
			final String userPropName, final String pswdPropName) {
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
	
	public static  void updateInputLocalProperties(ProjectOperations projectOperations, final String moduleName, final String root, final String rootPropName) {
		if (!StringUtils.isBlank(root)) {
			projectOperations.addProperty(moduleName, new Property(
					rootPropName, root));
		}
	}

}
