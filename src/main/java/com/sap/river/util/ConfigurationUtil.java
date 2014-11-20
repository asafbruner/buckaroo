package com.sap.river.util;

import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;

public class ConfigurationUtil {
	
	/**
	 * @return - POM object of the current project or the root project if no other projects are in focus
	 */
	public static Pom getCurrentPOM(ProjectOperations projectOperations) {
		return projectOperations.getFocusedModule(); 
	}

}
