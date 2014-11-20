package com.sap.river.odata.olingo;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.TypeParsingService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.MavenOperations;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Property;
import org.springframework.roo.support.util.DomUtils;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.springframework.roo.model.SpringJavaType;

import com.sap.river.hcp.DeployCommands;
import com.sap.river.util.ConfigurationUtil;

@Component
@Service
public class ODataOperationsImpl implements ODataOperations {
	
	private static Logger LOGGER = Logger.getLogger(ODataOperationsImpl.class.getName());
	
	private final String CONFIGURATION_BASE_PATH = "/configuration/river/olingo";
	private final String XML_NS_ODATA = "xmlns:odata";
	private final String XSI_SCHEMA_LOCATION = "xsi:schemaLocation";
	
	/**river specific details in the project setup files (such as pom.xml etc.) */	
	@Reference private ProjectOperations projectOperations;
	@Reference private MavenOperations mavenOperations;
	@Reference private FileManager fileManager;
	@Reference private PathResolver pathResolver;
	@Reference private TypeManagementService typeManagementService;
	@Reference private TypeLocationService typeLocationService;
	@Reference private TypeParsingService typeParsingService;
	
	///////////////////////////////////////////
	// API
	///////////////////////////////////////////

	@Override
	public boolean isSetupOlingoOdataAvailable() {
		return true;
	}

	
	@Override
	public void setupOlingo(final JavaType factoryClass, final String serviceBasePath) {
		updateProjectPom();
		updateServletConfiguration();
		setupOlingoJPAFactory(factoryClass);
	}

	
	///////////////////////////////////////////
	//Auxiliary Functions
	///////////////////////////////////////////
		
	protected void updateServletConfiguration() {
		
		//-------------------------------------------------------------------------------------------------------------//
		//TODO - there is a general bug that if the tags already exists it doesn't really find them in the web.xml
		//hence they are not removed and id causes duplicates in the second run.
		//-------------------------------------------------------------------------------------------------------------//
		
		final String configPath = projectOperations.getPathResolver().
				getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF\\web.xml");

		Document document = XmlUtils.readXml(fileManager.getInputStream(configPath));
		Element webXml = document.getDocumentElement();
		final Element webAppElement = XmlUtils.findFirstElement("/web-app", webXml);
		
		//read the configuration file from the current CLASSPATH
		final Element configuration = XmlUtils.getConfiguration(DeployCommands.class);
		
		//Inject the context-param
		Element configLocationContextParam = XmlUtils.findFirstElement(
	                        "/context-param[param-name = 'contextConfigLocation']", webAppElement);
		if (configLocationContextParam != null) {
			configLocationContextParam.getParentNode().removeChild(configLocationContextParam);
		}
		
		configLocationContextParam = XmlUtils.findFirstElement(CONFIGURATION_BASE_PATH + "/webConfig" +
                "/context-param[param-name = 'contextConfigLocation']", configuration);
		Node configContextNode = document.importNode(configLocationContextParam, true);
		webXml.appendChild(configContextNode);
		
		//Inject the conext loader listener
		Element contextLoaderListener = XmlUtils.findFirstElement(
                "/listener[listener-class = 'org.springframework.web.context.ContextLoaderListener']", webXml);
		if (contextLoaderListener != null) {
			webXml.removeChild(contextLoaderListener);
		}
		
		contextLoaderListener = XmlUtils.findFirstElement(CONFIGURATION_BASE_PATH + "/webConfig" +
		    "/listener[listener-class = 'org.springframework.web.context.ContextLoaderListener']", configuration);
		Node contextLoaderListenerNode = document.importNode(contextLoaderListener, true);
		webXml.appendChild(contextLoaderListenerNode);

		//Inject the CXF servlet 
		Element cxfServletConfig = XmlUtils.findFirstElement(
                "/servlet[servlet-class = 'org.apache.cxf.transport.servlet.CXFServlet']", webXml);
		if (cxfServletConfig != null) {
			webXml.removeChild(cxfServletConfig);
		}
		
		cxfServletConfig = XmlUtils.findFirstElement(CONFIGURATION_BASE_PATH + "/webConfig" +
		    "/servlet[servlet-class = 'org.apache.cxf.transport.servlet.CXFServlet']", configuration);
		Node cxfServletNode = document.importNode(cxfServletConfig, true);
		webXml.appendChild(cxfServletNode);
		
		//Inject the CXF servlet mapping
		//TODO - Need to remove CXFNonSpringJaxrsServlet - the name might not be unique...
		Element cxfServletMappingConfig = XmlUtils.findFirstElement(
                "/servlet-mapping[servlet-name = 'CXFServlet']", webXml);
		if (cxfServletMappingConfig != null) {
			webXml.removeChild(cxfServletMappingConfig);
		}
		
		cxfServletMappingConfig = XmlUtils.findFirstElement(CONFIGURATION_BASE_PATH + "/webConfig" +
		    "/servlet-mapping[servlet-name = 'CXFServlet']", configuration);
		Node cxfServletMappingNode = document.importNode(cxfServletMappingConfig, true);
		webXml.appendChild(cxfServletMappingNode);
		
		//Remove the CXFNonSpringServlet servlet + servlet mapping definition if was found
		Element cxfNonSpringServletConfig = XmlUtils.findFirstElement(
                "/servlet[servlet-class = 'org.apache.cxf.jaxrs.servlet.CXFNonSpringJaxrsServlet']", webXml);
		if (cxfNonSpringServletConfig != null) {
			webXml.removeChild(cxfNonSpringServletConfig);
		}
		Element cxfNonSpringServletMappingConfig = XmlUtils.findFirstElement(
                "/servlet-mapping[servlet-name = 'CXFNonSpringJaxrsServlet']", webXml);
		if (cxfNonSpringServletMappingConfig != null) {
			webXml.removeChild(cxfNonSpringServletMappingConfig);
		}
		
		DomUtils.removeTextNodes(webXml);
		// LOGGER.info(XmlUtils.nodeToString(webXml));
		fileManager.createOrUpdateTextFileIfRequired(configPath, XmlUtils.nodeToString(webXml), false);
		
	}
	
	protected void updateProjectPom() {
		String module = ConfigurationUtil.getCurrentPOM(projectOperations).getModuleName();
				
		//read the configuration file from the current CLASSPATH
		Element configuration = XmlUtils.getConfiguration(DeployCommands.class);
		
        //Identify the dependencies
        final List<Dependency> requiredDependencies = new ArrayList<Dependency>();
        
        final List<Element> dependencies = XmlUtils.findElements(CONFIGURATION_BASE_PATH + "/dependencies/dependency", configuration);
        
        for (final Element dependencyElement : dependencies) {
            requiredDependencies.add(new Dependency(dependencyElement));
        }
        
        //update the POM the new configuration
        projectOperations.removeDependencies(module, requiredDependencies);
        projectOperations.addDependencies(module, requiredDependencies);
        
        //Identify the fixed properties
        final List<Element> pomProperties = XmlUtils.findElements(CONFIGURATION_BASE_PATH + "/properties/*", configuration);
        for (final Element property : pomProperties) {
            projectOperations.addProperty(module, new Property(property));
        }
	}
	

	public void setupOlingoJPAFactory(final JavaType factoryClass) {
		updateApplicationContextConfig();
		createServiceFactoryClass(factoryClass);
	}
	
	/**
	 * This method update the spring application context configuration file
	 */
	private void updateApplicationContextConfig() {
		
		final String configPath = projectOperations.getPathResolver().
				getFocusedIdentifier(Path.SPRING_CONFIG_ROOT, "/applicationContext.xml");
		Document document = XmlUtils.readXml(fileManager.getInputStream(configPath));
		Element appContextXml = document.getDocumentElement();
		
		//read the configuration file from the current CLASSPATH
		final Element configuration = XmlUtils.getConfiguration(DeployCommands.class);
		
		//Identify odata olingo namespace to be added to Spring configuration file
		 String olingoNamespace = XmlUtils.findFirstElement(CONFIGURATION_BASE_PATH + "/appContextConfig" +
                "/namespaces/olingo-ns", configuration).getTextContent();
		 String olingoSchemaLocation = XmlUtils.findFirstElement(CONFIGURATION_BASE_PATH + "/appContextConfig" +
	                "/schemaLocation", configuration).getTextContent();
		 
		Element beansElement = XmlUtils.findFirstElement("/beans",appContextXml);
		if (!beansElement.hasAttribute(XML_NS_ODATA)){			
			beansElement.setAttribute(XML_NS_ODATA, olingoNamespace);
		}
		
		//Schema location
		String schemaLocationValue = beansElement.getAttribute(XSI_SCHEMA_LOCATION);
		if (schemaLocationValue.indexOf(olingoSchemaLocation) == -1) { //schema location does not exists
			schemaLocationValue = schemaLocationValue + " " + olingoSchemaLocation;
			beansElement.setAttribute(XSI_SCHEMA_LOCATION, schemaLocationValue);
		}
		
		//Inject the CXF imports
		Element configCXFImport = XmlUtils.findFirstElement(
	                        "/beans/import[@resource = 'classpath:META-INF/cxf/cxf.xml']", appContextXml);
		if (configCXFImport != null) {
			appContextXml.removeChild(document.importNode(configCXFImport, true));
		}
		configCXFImport = XmlUtils.findFirstElement(CONFIGURATION_BASE_PATH + "/appContextConfig" +
                "/imports/import[@resource = 'classpath:META-INF/cxf/cxf.xml']", configuration);
		Node configCXFImportNode = document.importNode(configCXFImport, true);
		appContextXml.appendChild(configCXFImportNode);
		
		Element configCXFServletImport = XmlUtils.findFirstElement(
                "/beans/import[@resource = 'classpath:META-INF/cxf/cxf-servlet.xml']", appContextXml);
		if (configCXFServletImport != null) {
			appContextXml.removeChild(document.importNode(configCXFServletImport, true));
		}
		configCXFServletImport = XmlUtils.findFirstElement(CONFIGURATION_BASE_PATH + "/appContextConfig" +
		    "/imports/import[@resource = 'classpath:META-INF/cxf/cxf-servlet.xml']", configuration);
		Node configCXFImportServletNode = document.importNode(configCXFServletImport, true);
		appContextXml.appendChild(configCXFImportServletNode);
		
		//Define the OData service
		Element configODataService = XmlUtils.findFirstElement(
                "/beans/*[local-name()='server' and @address='/odata.svc']", appContextXml);
		if (configODataService != null) {
			appContextXml.removeChild(document.importNode(configODataService, true));
		}
		configODataService = XmlUtils.findFirstElement(CONFIGURATION_BASE_PATH + "/appContextConfig" +
		    "/serviceConfig/*", configuration);
		Node configOdataServiceNode = document.importNode(configODataService, true);
		appContextXml.appendChild(configOdataServiceNode);

		DomUtils.removeTextNodes(appContextXml);
		fileManager.createOrUpdateTextFileIfRequired(configPath, XmlUtils.nodeToString(appContextXml), false);
		//LOGGER.info(XmlUtils.nodeToString(appContextXml));
	}

	/**
	 * Creates the generated OData service factory class
	 * 
	 * @param factoryClassName - the name of the factory class
	 */
	private void createServiceFactoryClass(JavaType factoryClass) {
		
		final String declaredByMetadataId = PhysicalTypeIdentifier
				.createIdentifier(factoryClass,
						pathResolver.getFocusedPath(Path.SRC_MAIN_JAVA));
		final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
				declaredByMetadataId, Modifier.PUBLIC, factoryClass,
				PhysicalTypeCategory.CLASS);

		//Add @Service class annotation
		final List<AnnotationMetadataBuilder> annotationBuilder = new ArrayList<AnnotationMetadataBuilder>();
		annotationBuilder.add(new AnnotationMetadataBuilder(SpringJavaType.SERVICE));
		cidBuilder.setAnnotations(annotationBuilder);

		//Adds extends 
		cidBuilder.setExtendsTypes(Arrays.asList(new JavaType("ODataJPAServiceFactory")));
		
		final ClassOrInterfaceTypeDetails cid  = cidBuilder.build();
		
		//TODO ----- This part should be improved, currently doing string manipulation from another persisted file ---//
		String newContents = typeParsingService.getCompilationUnitContents(cid);
        
        //Adding rest of the imports
        final InputStream inputStream = FileUtils.getInputStream(DeployCommands.class, "JPAServiceFactory.txt");
        String allClassContent = "";
        try {
			allClassContent = IOUtils.toString(inputStream);
		} catch (IOException e) {
			LOGGER.info("Could not read from input stream");
			e.printStackTrace();
		}
        
        String imports = allClassContent.substring(allClassContent.indexOf("import org.springframework.stereotype.Service;"),allClassContent.indexOf("@Service"));
    
        String classContent = allClassContent.substring(allClassContent.indexOf("{")+1, allClassContent.lastIndexOf("}")+1);
        
        newContents = newContents.replace("import org.springframework.stereotype.Service;", imports);
        newContents = newContents.replace("}", classContent);
        newContents = newContents.replaceAll("@Service", "@Service(value=\"jpaServiceFactory\")");
        
        //Adding the content of the class
        final String fileCanonicalPath = typeLocationService.getPhysicalTypeCanonicalPath(cid.getDeclaredByMetadataId());
        fileManager.createOrUpdateTextFileIfRequired(fileCanonicalPath,
                newContents, true);
	
	}

}
