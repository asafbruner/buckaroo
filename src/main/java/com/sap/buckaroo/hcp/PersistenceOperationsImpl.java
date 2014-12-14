package com.sap.buckaroo.hcp;

import java.io.InputStream;
//import java.util.logging.Logger;


import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.MavenOperations;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.DomUtils;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Component
@Service
public class PersistenceOperationsImpl implements PersistenceOperations {

	// private static Logger LOGGER = Logger.getLogger(PersistenceOperationsImpl.class.getName());
	private static final String PERSISTENCE_XML = "META-INF/persistence.xml";
	private static final String PERSISTENCE_UNIT = "persistence-unit";
	private static final String DEFAULT_PERSISTENCE_UNIT = "persistenceUnit";
	private static final String APPLICATION_CONTEXT_XML = "applicationContext.xml";
	private static final String DATABASE_PROPERTIES_FILE = "database.properties";
	private static final String WEB_XML = "WEB-INF/web.xml";
	private static final String RESOURCE_REF = "resource-ref";

	@Reference
	private FileManager fileManager;
	@Reference
	private PathResolver pathResolver;

	/** buckaroo specific details in the project setup files (such as pom.xml etc.) */
	@Reference
	private ProjectOperations projectOperations;

	@Reference
	private MavenOperations mavenOperations;
	
	//TODO, following is a very unelegant solution that should be fixed later
	//Following is used to ensure that internal commands are not exposed externally (the flag is toggled just before running the command, and then toggled again)
	private static boolean isAllowCommandExternally = false;
	
	public static void setIsAllowCommandExternally(boolean isAllowCommandExternally){
		PersistenceOperationsImpl.isAllowCommandExternally = isAllowCommandExternally;
	}

	// /////////////////////////////////////////
	// API
	// /////////////////////////////////////////

	@Override
	public boolean isSetupPersistenceRemoteAvailable() {
		return isAllowCommandExternally;
	}

	@Override
	public boolean isSetupPersistenceLocalAvailable() {
		return true;
	}

	@Override
	public void setupPersistenceRemote(PersistenceValues db) {
		updatePersistenceXml();
		updateApplicationContext();
		updateWebXML();
	}

	@Override
	public void setupPersistenceLocal() {
	}

	private void updatePersistenceXml() {
		Validate.notNull(fileManager, "FileManager is required");
		Validate.notNull(pathResolver, "PathResolver is required");
		Validate.notNull(projectOperations, "ProjectOperations is required");

		final InputStream inputStream;
		final Document persistence;

		inputStream = FileUtils.getInputStream(getClass(), "persistence-template.xml");
		persistence = XmlUtils.readXml(inputStream);

		final Element root = persistence.getDocumentElement();
		final Element persistenceElement = XmlUtils.findFirstElement("/persistence", root);
		Validate.notNull(persistenceElement, "No persistence element found");

		Element persistenceUnitElement;
		persistenceUnitElement = XmlUtils.findFirstElement(PERSISTENCE_UNIT + "[@name = '" + DEFAULT_PERSISTENCE_UNIT + "']",
				persistenceElement);

		if (persistenceUnitElement != null) {
			while (persistenceUnitElement.getFirstChild() != null) {
				persistenceUnitElement.removeChild(persistenceUnitElement.getFirstChild());
			}
		} else {
			persistenceUnitElement = persistence.createElement(PERSISTENCE_UNIT);
			persistenceElement.appendChild(persistenceUnitElement);
		}

		// Add provider element
		final Element provider = persistence.createElement("provider");
		persistenceUnitElement.setAttribute("name", StringUtils.defaultIfEmpty(DEFAULT_PERSISTENCE_UNIT, DEFAULT_PERSISTENCE_UNIT));
		persistenceUnitElement.setAttribute("transaction-type", "RESOURCE_LOCAL");
		provider.setTextContent("org.eclipse.persistence.jpa.PersistenceProvider");

		persistenceUnitElement.appendChild(provider);
		// Add properties
		final Element properties = persistence.createElement("properties");
		properties.appendChild(createPropertyElement("eclipselink.target-database",
				"org.eclipse.persistence.platform.database.HANAPlatform", persistence));
		properties
				.appendChild(persistence
						.createComment(" value=\"drop-and-create-tables\" to build a new database on each run; value=\"create-tables\" creates new tables if needed; value=\"none\" makes no changes to the database ")); // ROO-627
		properties.appendChild(createPropertyElement("eclipselink.ddl-generation", "create-tables", persistence));
		properties.appendChild(createPropertyElement("eclipselink.ddl-generation.output-mode", "database", persistence));
		properties.appendChild(createPropertyElement("eclipselink.weaving", "static", persistence));

		persistenceUnitElement.appendChild(properties);
		// TODO - Move string to constants
		final String persistencePath = pathResolver.getFocusedIdentifier(Path.SRC_MAIN_RESOURCES, PERSISTENCE_XML);
		fileManager.createOrUpdateTextFileIfRequired(persistencePath, XmlUtils.nodeToString(persistence), false);
	}

	private void updateApplicationContext() {
		// TODO - see if needed and if so generalize
		Validate.notNull(fileManager, "FileManager is required");
		Validate.notNull(pathResolver, "PathResolver is required");
		Validate.notNull(projectOperations, "ProjectOperations is required");

		final String contextPath = projectOperations.getPathResolver().getFocusedIdentifier(Path.SPRING_CONFIG_ROOT,
				APPLICATION_CONTEXT_XML);
		final Document appCtx = XmlUtils.readXml(fileManager.getInputStream(contextPath));
		final Element root = appCtx.getDocumentElement();

		Element dataSourceJndi = XmlUtils.findFirstElement("/beans/jndi-lookup[@id = 'dataSource']", root);

		if (dataSourceJndi == null) {
			dataSourceJndi = appCtx.createElement("jee:jndi-lookup");
			dataSourceJndi.setAttribute("id", "dataSource");
			root.appendChild(dataSourceJndi);
		}
		dataSourceJndi.setAttribute("jndi-name", "jdbc/DefaultDB");
		dataSourceJndi.setAttribute(RESOURCE_REF, "true");

		Element dataSource = XmlUtils.findFirstElement("/beans/bean[@id = 'dataSource']", root);

		if (dataSource != null) {
			dataSource.getParentNode().removeChild(dataSource);
		}

		final String dbPropsPath = projectOperations.getPathResolver().getFocusedIdentifier(Path.SPRING_CONFIG_ROOT,
				DATABASE_PROPERTIES_FILE);

		if (fileManager.exists(dbPropsPath)) {
			fileManager.delete(dbPropsPath);
		}

		DomUtils.removeTextNodes(root);

		fileManager.createOrUpdateTextFileIfRequired(contextPath, XmlUtils.nodeToString(appCtx), false);
	}

	private void updateWebXML() {
		final String webXMLPath = pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, WEB_XML);
		Validate.isTrue(fileManager.exists(webXMLPath), webXMLPath + " has not yet been generated. Make sure to run add-on again.");
		final Document webXMLDoc = XmlUtils.readXml(fileManager.getInputStream(webXMLPath));
		final Element root = webXMLDoc.getDocumentElement();
		final Element webAppElement = XmlUtils.findFirstElement("/web-app", root);

		Element resourceRef = XmlUtils.findFirstElement(RESOURCE_REF, webAppElement);

		if (resourceRef != null) {
			resourceRef.getParentNode().removeChild(resourceRef);
		}

		resourceRef = webXMLDoc.createElement(RESOURCE_REF);
		Element resourceRefName = webXMLDoc.createElement("res-ref-name");
		resourceRefName.setTextContent("jdbc/DefaultDB");
		resourceRef.appendChild(resourceRefName);
		Element resourceRefType = webXMLDoc.createElement("res-type");
		resourceRefType.setTextContent("javax.sql.DataSource");
		resourceRef.appendChild(resourceRefType);
		webAppElement.appendChild(resourceRef);

		fileManager.createOrUpdateTextFileIfRequired(webXMLPath, XmlUtils.nodeToString(webXMLDoc), false);
	}

	private Element createPropertyElement(final String name, final String value, final Document document) {
		final Element property = document.createElement("property");
		property.setAttribute("name", name);
		property.setAttribute("value", value);
		return property;
	}

}
