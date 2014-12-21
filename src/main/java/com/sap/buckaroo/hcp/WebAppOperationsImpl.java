package com.sap.buckaroo.hcp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
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
import org.w3c.dom.Node;

@Component
@Service
public class WebAppOperationsImpl implements WebAppOperations {

	private static Logger LOGGER = Logger.getLogger(WebAppOperationsImpl.class.getName());
	private static String WEB_UI5_RESOURCES = "\\\\iltlvp01\\TIP_HPAI_PLATFORM\\DevX\\Buckaroo\\web-ui5";
	/** buckaroo specific details in the project setup files (such as pom.xml etc.) */

	@Reference
	private ProjectOperations projectOperations;
	@Reference
	private MavenOperations mavenOperations;
	@Reference
	private FileManager fileManager;
	@Reference
	private PathResolver pathResolver;
	
	//TODO, following is a very unelegant solution that should be fixed later
	//Following is used to ensure that internal commands are not exposed externally (the flag is toggled just before running the command, and then toggled again)
	private static boolean isAllowCommandExternally = false;
	
	public static void setIsAllowCommandExternally(boolean isAllowCommandExternally){
		WebAppOperationsImpl.isAllowCommandExternally = isAllowCommandExternally;
	}

	// /////////////////////////////////////////
	// API
	// /////////////////////////////////////////

	@Override
	public boolean isSetupWebAppAvailable() {
		return isAllowCommandExternally;
	}

	/**
	 * 1) in webmvc-config.xml add the line: <mvc:resources location="/, classpath:/META-INF/web-ui5/" mapping="/**"/> 2) in views.xml:
	 * under this tag: <definition name="index" extends="default"> replace the exsiting tag with this one: <put-attribute name="body"
	 * value="index.html" /> 3) copy directory web-ui5 from resources to META-INF
	 */
	@Override
	public void setupWebApp() {
		installUI5Resources();
	}

	/**
	 * * 1) in webmvc-config.xml add the line: <mvc:resources location="/, classpath:/META-INF/web-ui5/" mapping="/**"/>
	 */
	private void installUI5Resources() {
		final String webappPath = pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP,"");
		File webappDir = new File(webappPath);
		File sourceDir = new File(WEB_UI5_RESOURCES);
		FileUtils.copyRecursively(sourceDir, webappDir, false);
	}

}
