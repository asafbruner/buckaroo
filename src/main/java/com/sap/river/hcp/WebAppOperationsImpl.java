package com.sap.river.hcp;

import java.io.File;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.MavenOperations;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Component
@Service
public class WebAppOperationsImpl implements WebAppOperations {

	private static Logger LOGGER = Logger.getLogger(WebAppOperationsImpl.class
			.getName());

	private static final String WEB_MVC_XML = "WEB-INF/spring/webmvc-config.xml";
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

/**
 *  * 1) in webmvc-config.xml add the line:
<mvc:resources location="/, classpath:/META-INF/web-ui5/" mapping="/**"/>
 */
	private void installUI5Resources() 
	{
		final String webappPath = pathResolver.getFocusedIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/web-ui5");
		File webappDir = new File(webappPath);
		File sourceDir = new File("\\\\iltlvp01.tlv.sap.corp\\public\\asaf\\web-ui5");
		FileUtils.copyRecursively(sourceDir, webappDir,false);
	}

/**
2) in views.xml:
under this tag:
<definition name="index" extends="default">
replace the exsiting tag with this one:
*/
	private void setupViewsXml() 
	{
	}

	/**
	 1) in webmvc-config.xml add the line:
	<mvc:resources location="/, classpath:/META-INF/web-ui5/" mapping="/**"/>
	*/
	private void setupWebmvcConfigXml() 
	{
        final String webMvcXMLPath = pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, WEB_MVC_XML);
        Validate.isTrue(fileManager.exists(webMvcXMLPath), webMvcXMLPath + " has not yet been generated. Make sure to run add-on again.");
        final Document webXMLDoc = XmlUtils.readXml(fileManager.getInputStream(webMvcXMLPath));
        final Element root = webXMLDoc.getDocumentElement();
        final Element webAppElement = XmlUtils.findFirstElement("/beans", root);
        Validate.isTrue(webAppElement!=null, "beans not found.");
        Element resourceRef = webXMLDoc.createElement("mvc:resources");
        resourceRef.setAttribute("location", "/, classpath:/META-INF/web-ui5/");
        resourceRef.setAttribute("mapping", "/**");
        webAppElement.appendChild(resourceRef);
        fileManager.createOrUpdateTextFileIfRequired(webMvcXMLPath, XmlUtils.nodeToString(webXMLDoc), false);
	}
}

