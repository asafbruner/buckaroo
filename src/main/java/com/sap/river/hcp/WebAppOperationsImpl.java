package com.sap.river.hcp;

import java.io.File;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.MavenOperations;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Resource;
import org.springframework.roo.support.util.FileUtils;

@Component
@Service
public class WebAppOperationsImpl implements WebAppOperations {

	private static Logger LOGGER = Logger.getLogger(WebAppOperationsImpl.class
			.getName());

	/** river specific details in the project setup files (such as pom.xml etc.) */
	@Reference
	private ProjectOperations projectOperations;
	@Reference
	private MavenOperations mavenOperations;
	@Reference 
	private FileManager fileManager;
    @Reference 
    private PathResolver pathResolver;

	// /////////////////////////////////////////
	// API
	///////////////////////////////////////////

	@Override
	public boolean isSetupWebAppAvailable() {	
		return true;
	}
/**
 * 1) in webmvc-config.xml add the line:
<mvc:resources location="/, classpath:/META-INF/web-ui5/" mapping="/**"/>
2) in views.xml:
under this tag:
<definition name="index" extends="default">
replace the exsiting tag with this one:
<put-attribute name="body" value="index.html" />
3) copy directory web-ui5 from resources to META-INF

 */
	@Override
	public void setupWebApp()
	{
		setupWebmvcConfigXml();
		setupViewsXml();
		installUI5Resources();
	}

	private void installUI5Resources() 
	{
		final String webappPath = pathResolver.getFocusedIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/web-ui5");
		File webappDir = new File(webappPath);
		File sourceDir = new File("\\\\iltlvp01.tlv.sap.corp\\public\\asaf\\web-ui5");
		FileUtils.copyRecursively(sourceDir, webappDir,false);
	}

	private void setupViewsXml() 
	{
	}

	private void setupWebmvcConfigXml() 
	{
	}
}

