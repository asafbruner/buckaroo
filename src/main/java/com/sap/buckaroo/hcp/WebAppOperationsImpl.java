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

	private static final String WEB_MVC_XML = "WEB-INF/spring/webmvc-config.xml";
	private static final String VIEW_XML = "WEB-INF/views/views.xml";
	private static final String LOCATION_ATTR = "location";
	private static final String MAPPING_ATTR = "mapping";
	private static final String LOCATION_ATTR_VALUE = "/, classpath:/META-INF/web-ui5/";
	private static final String MAPPING_ATTR_VALUE = "/**";
	private static final String MVC_RESOURCE = "mvc:resources";
	private static final String BEANS_XPATH = "/beans";
	private static final String RESOURCES_XPATH = "/beans/resources";
	private static final String ERROR_MSG_XML_NOT_CREATED_FORMAT = "%s has not yet been generated. Make sure to run add-on again.";
	private static final String ERROR_MSG_LOCATION_CONFLICT = "Confilict in webmvc-config.xml. Resource in location " + LOCATION_ATTR_VALUE
			+ " is already mapped to a different value.";
	private static final String ERROR_MSG_BEANS_NODE_NOT_FOUND = "beans not found.";

	/** buckaroo specific details in the project setup files (such as pom.xml etc.) */

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
	// /////////////////////////////////////////

	@Override
	public boolean isSetupWebAppAvailable() {
		return true;
	}

	/**
	 * 1) in webmvc-config.xml add the line: <mvc:resources location="/, classpath:/META-INF/web-ui5/" mapping="/**"/> 2) in views.xml:
	 * under this tag: <definition name="index" extends="default"> replace the exsiting tag with this one: <put-attribute name="body"
	 * value="index.html" /> 3) copy directory web-ui5 from resources to META-INF
	 */
	@Override
	public void setupWebApp() {
		setupWebmvcConfigXml();
		setupViewsXml();
		installUI5Resources();
	}

	/**
	 * * 1) in webmvc-config.xml add the line: <mvc:resources location="/, classpath:/META-INF/web-ui5/" mapping="/**"/>
	 */
	private void installUI5Resources() {
		final String webappPath = pathResolver.getFocusedIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/web-ui5");
		File webappDir = new File(webappPath);
		File sourceDir = new File("\\\\iltlvp01.tlv.sap.corp\\public\\asaf\\web-ui5");
		FileUtils.copyRecursively(sourceDir, webappDir, false);
	}

	/**
	 * 2) in views.xml: under this tag: <definition name="index" extends="default"> replace the exsiting tag with this one: <put-attribute
	 * name="body" value="index.html" />
	 */
	private void setupViewsXml() {
		// TODO replace the attribute in a more elegant way
		final String viewsPath = pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, VIEW_XML);
		final String old_value = "/WEB-INF/views/index.jspx";
		final String new_value = "index.html";
		InputStream is = fileManager.getInputStream(viewsPath);

		String allClassContent = "";
		try {
			allClassContent = IOUtils.toString(is);
		} catch (IOException e) {
			LOGGER.info("Could not read from input stream");
			e.printStackTrace();
		}
		allClassContent = allClassContent.replaceAll(old_value, new_value);
		fileManager.createOrUpdateTextFileIfRequired(viewsPath, allClassContent, true);
	}

	/**
	 * Set the path to the ui5 library. In webmvc-config.xml add the line: <mvc:resources location="/, classpath:/META-INF/web-ui5/"
	 * mapping="/**"/>
	 * 
	 * Make sure it is after any other <resources> node. If no other <resources> node exists, append to the end of the <beans> node.
	 */
	private void setupWebmvcConfigXml() {
		final String webMvcXMLPath = pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, WEB_MVC_XML);
		Validate.isTrue(fileManager.exists(webMvcXMLPath), String.format(ERROR_MSG_XML_NOT_CREATED_FORMAT, webMvcXMLPath));

		final Document webXMLDoc = XmlUtils.readXml(fileManager.getInputStream(webMvcXMLPath));
		final Element root = webXMLDoc.getDocumentElement();
		final Element beansElement = XmlUtils.findFirstElement(BEANS_XPATH, root);
		Validate.isTrue(beansElement != null, ERROR_MSG_BEANS_NODE_NOT_FOUND);

		final List<Element> resources = XmlUtils.findElements(RESOURCES_XPATH, root);

		// check current resources under <beans>-
		// if new node to be added exist- do nothing
		// if conflict - throw
		// else - call utility method to plant the node in the right place.
		boolean needToAddResource = checkResourcesList(resources);

		if (needToAddResource) {
			addNewResourcesNode(webXMLDoc, beansElement, resources);
			fileManager.createOrUpdateTextFileIfRequired(webMvcXMLPath, XmlUtils.nodeToString(webXMLDoc), false);
		}
	}

	/**
	 * Validate the resources nodes under beans
	 * 
	 * - If the addon's location exists, and mapping is the same - return false - no need to add a node - If the addon's location exists,
	 * and mapping is different - break with a proper message. We don't want to override existing nodes. - Else - return true indicating a
	 * node should be inserted
	 * 
	 * @param resources - list of the resources under <beans> node
	 * @return true is new node needs to be inserted
	 * @throws if resources have a conflict with the target location
	 */
	private boolean checkResourcesList(List<Element> resources) {
		if (resources != null && !(resources.isEmpty())) {
			for (final Element resourcesElement : resources) {
				if (resourcesElement.getAttribute(LOCATION_ATTR).equals(LOCATION_ATTR_VALUE)) {
					Validate.isTrue(resourcesElement.getAttribute(MAPPING_ATTR).equals(MAPPING_ATTR_VALUE), ERROR_MSG_LOCATION_CONFLICT);
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Add the new resources node to the right place Assume there are no conflicts If other resources nodes exist - append the new node to
	 * the last one. Else - append as the last child of the beans node
	 * @param webXMLDoc
	 * @param beansElement
	 * @param resources
	 */
	private void addNewResourcesNode(Document webXMLDoc, Element beansElement, List<Element> resources) {
		/**
		 * Insert the new node after the last <resources> node found. Unfortunately, There is no insertAfter utility, so we:
		 * 
		 * - Clone the last resource node - Insert the clone before the original - Update the original nodes attributes to "our" values
		 * 
		 */
		if (resources != null && !(resources.isEmpty())) {
			Element lastResourceElement = resources.get(resources.size() - 1);
			Node resourceRefNode = lastResourceElement.cloneNode(false);
			beansElement.insertBefore(resourceRefNode, lastResourceElement);
			lastResourceElement.setAttribute(LOCATION_ATTR, LOCATION_ATTR_VALUE);
			lastResourceElement.setAttribute(MAPPING_ATTR, MAPPING_ATTR_VALUE);
		}
		/**
		 * There are no resources node - append the node as the last child of <beans>
		 */
		else {
			Element resourceRef = webXMLDoc.createElement(MVC_RESOURCE);
			resourceRef.setAttribute(LOCATION_ATTR, LOCATION_ATTR_VALUE);
			resourceRef.setAttribute(MAPPING_ATTR, MAPPING_ATTR_VALUE);
			beansElement.appendChild(resourceRef);
		}
	}
}
