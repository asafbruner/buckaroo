package com.sap.buckaroo.odata;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.olingo.odata2.api.edm.Edm;
import org.apache.olingo.odata2.api.edm.EdmEntitySet;
import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.edm.EdmParameter;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.TypeParsingService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.ImportMetadata;
import org.springframework.roo.classpath.details.ImportMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.SpringJavaType;
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

import com.sap.buckaroo.hcp.DeployCommands;
import com.sap.buckaroo.util.ConfigurationUtil;

@Component
@Service
public class ODataOperationsImpl implements ODataOperations {

	private static Logger LOGGER = Logger.getLogger(ODataOperationsImpl.class.getName());

	private static final String CONFIGURATION_BASE_PATH = "/configuration/buckaroo/olingo";
	private static final String CONFIGURATION_APP_CONFIG_BASE_PATH = "/configuration/buckaroo/olingo/appContextConfig";
	private static final String XML_NS_ODATA = "xmlns:odata";
	private static final String XSI_SCHEMA_LOCATION = "xsi:schemaLocation";
	private static final String ODATA_SERVICE_PROPERTIES_FILE = "odata-service.properties";

	/** buckaroo specific details in the project setup files (such as pom.xml etc.) */
	@Reference
	private ProjectOperations projectOperations;
	@Reference
	private MavenOperations mavenOperations;
	@Reference
	private FileManager fileManager;
	@Reference
	private PathResolver pathResolver;
	@Reference
	private TypeManagementService typeManagementService;
	@Reference
	private TypeLocationService typeLocationService;
	@Reference
	private TypeParsingService typeParsingService;

	/** Edm type management */
	@Reference
	private EdmTypeParsingService edmParsingService;

	/** connection and OData provider */
	private ODataServiceProvider odataServiceProvider;

	public ODataOperationsImpl() {
		odataServiceProvider = new ODataServiceProvider();
		odataServiceProvider.setConnection(new ODataConnection());
	}

	// /////////////////////////////////////////
	// API
	// /////////////////////////////////////////

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

	// /////////////////////////////////////////
	// Auxiliary Functions
	// /////////////////////////////////////////

	private void createApplicationContextTest() {
		// The path of the new file to create or update
		final String appContextTestXml = projectOperations.getPathResolver().getFocusedIdentifier(Path.SRC_TEST_RESOURCES,
				"META-INF\\spring\\applicationContext-test.xml");

		// Take the content of the current applicationContext.xml
		final String configPath = projectOperations.getPathResolver().getFocusedIdentifier(Path.SPRING_CONFIG_ROOT,
				"/applicationContext.xml");
		Document document = XmlUtils.readXml(fileManager.getInputStream(configPath));
		Element appContextContent = document.getDocumentElement();

		fileManager.createOrUpdateTextFileIfRequired(appContextTestXml, XmlUtils.nodeToString(appContextContent), true);

		// After creation of applicationContext-test.xml - need to remove redundant elements
		Document documentTest = XmlUtils.readXml(fileManager.getInputStream(appContextTestXml));
		Element appContextTestContent = documentTest.getDocumentElement();

		com.sap.buckaroo.util.XMLUtils.removeElementFromXML(appContextTestContent,
				"/beans/import[@resource = 'classpath:META-INF/cxf/cxf.xml']");
		com.sap.buckaroo.util.XMLUtils.removeElementFromXML(appContextTestContent,
				"/beans/import[@resource = 'classpath:META-INF/cxf/cxf-servlet.xml']");
		com.sap.buckaroo.util.XMLUtils.removeElementFromXML(appContextTestContent,
				"/beans/*[local-name()='server' and @address='/odata.svc']");
		com.sap.buckaroo.util.XMLUtils.removeElementFromXML(appContextTestContent,
				"/beans/*[local-name()='annotation-driven' and @mode='aspectj']");

		com.sap.buckaroo.util.XMLUtils.removeElementFromXML(appContextTestContent, "/beans/bean[@id='dataSource']");
		com.sap.buckaroo.util.XMLUtils.removeElementFromXML(appContextTestContent, "/beans/bean[@id='transactionManager']");
		com.sap.buckaroo.util.XMLUtils.removeElementFromXML(appContextTestContent, "/beans/bean[@id='entityManagerFactory']");

		// add component-scan filter to filter out OData components that depends on JPA
		final Element configuration = XmlUtils.getConfiguration(DeployCommands.class);

		Element autoScanElement = XmlUtils.findFirstElement("/beans/*[local-name()='component-scan']", appContextTestContent);
		Element excludeFilter = XmlUtils.findFirstElement("*[local-name()='exclude-filter' and @regex='.*Factory.*']", autoScanElement);
		if (excludeFilter != null) {
			excludeFilter.getParentNode().removeChild(excludeFilter);
		}

		excludeFilter = XmlUtils.findFirstElement(CONFIGURATION_APP_CONFIG_BASE_PATH + "/component-scan/*[local-name()='exclude-filter']",
				configuration);
		Node excludeFilterNode = autoScanElement.getOwnerDocument().importNode(excludeFilter, true);
		autoScanElement.appendChild(excludeFilterNode);

		fileManager.createOrUpdateTextFileIfRequired(appContextTestXml, XmlUtils.nodeToString(appContextTestContent), true);
	}

	protected void updateServletConfiguration() {

		final String configPath = projectOperations.getPathResolver().getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF\\web.xml");

		Document document = XmlUtils.readXml(fileManager.getInputStream(configPath));
		Element webXml = document.getDocumentElement();
		final Element webAppElement = XmlUtils.findFirstElement("/web-app", webXml);

		// read the configuration file from the current CLASSPATH
		final Element configuration = XmlUtils.getConfiguration(DeployCommands.class);

		// Inject the context-param
		Element configLocationContextParam = XmlUtils.findFirstElement("/web-app/context-param[param-name = 'contextConfigLocation']",
				webAppElement);
		if (configLocationContextParam != null) {
			configLocationContextParam.getParentNode().removeChild(configLocationContextParam);
		}

		configLocationContextParam = XmlUtils.findFirstElement(CONFIGURATION_BASE_PATH + "/webConfig"
				+ "/context-param[param-name = 'contextConfigLocation']", configuration);
		Node configContextNode = document.importNode(configLocationContextParam, true);
		webXml.appendChild(configContextNode);

		// Inject the conext loader listener
		Element contextLoaderListener = XmlUtils.findFirstElement(
				"/web-app/listener[listener-class = 'org.springframework.web.context.ContextLoaderListener']", webXml);
		if (contextLoaderListener != null) {
			contextLoaderListener.getParentNode().removeChild(contextLoaderListener);
		}

		contextLoaderListener = XmlUtils.findFirstElement(CONFIGURATION_BASE_PATH + "/webConfig"
				+ "/listener[listener-class = 'org.springframework.web.context.ContextLoaderListener']", configuration);
		Node contextLoaderListenerNode = document.importNode(contextLoaderListener, true);
		webXml.appendChild(contextLoaderListenerNode);

		// Inject the CXF servlet
		Element cxfServletConfig = XmlUtils.findFirstElement(
				"/web-app/servlet[servlet-class = 'org.apache.cxf.transport.servlet.CXFServlet']", webXml);
		if (cxfServletConfig != null) {
			cxfServletConfig.getParentNode().removeChild(cxfServletConfig);
		}

		cxfServletConfig = XmlUtils.findFirstElement(CONFIGURATION_BASE_PATH + "/webConfig"
				+ "/servlet[servlet-class = 'org.apache.cxf.transport.servlet.CXFServlet']", configuration);
		Node cxfServletNode = document.importNode(cxfServletConfig, true);
		webXml.appendChild(cxfServletNode);

		// Inject the CXF servlet mapping
		// TODO - Need to remove CXFNonSpringJaxrsServlet - the name might not be unique...
		Element cxfServletMappingConfig = XmlUtils.findFirstElement("/web-app/servlet-mapping[servlet-name = 'CXFServlet']", webXml);
		if (cxfServletMappingConfig != null) {
			cxfServletMappingConfig.getParentNode().removeChild(cxfServletMappingConfig);
		}

		cxfServletMappingConfig = XmlUtils.findFirstElement(CONFIGURATION_BASE_PATH + "/webConfig"
				+ "/servlet-mapping[servlet-name = 'CXFServlet']", configuration);
		Node cxfServletMappingNode = document.importNode(cxfServletMappingConfig, true);
		webXml.appendChild(cxfServletMappingNode);

		// Remove the CXFNonSpringServlet servlet + servlet mapping definition if was found
		Element cxfNonSpringServletConfig = XmlUtils.findFirstElement(
				"/servlet[servlet-class = 'org.apache.cxf.jaxrs.servlet.CXFNonSpringJaxrsServlet']", webXml);
		if (cxfNonSpringServletConfig != null) {
			cxfNonSpringServletConfig.getParentNode().removeChild(cxfNonSpringServletConfig);
		}
		Element cxfNonSpringServletMappingConfig = XmlUtils.findFirstElement("/servlet-mapping[servlet-name = 'CXFNonSpringJaxrsServlet']",
				webXml);
		if (cxfNonSpringServletMappingConfig != null) {
			cxfNonSpringServletMappingConfig.getParentNode().removeChild(cxfNonSpringServletMappingConfig);
		}

		DomUtils.removeTextNodes(webXml);
		// LOGGER.info(XmlUtils.nodeToString(webXml));
		fileManager.createOrUpdateTextFileIfRequired(configPath, XmlUtils.nodeToString(webXml), false);

	}

	protected void updateProjectPom() {
		String module = ConfigurationUtil.getCurrentPOM(projectOperations).getModuleName();

		// read the configuration file from the current CLASSPATH
		Element configuration = XmlUtils.getConfiguration(DeployCommands.class);

		// Identify the dependencies
		final List<Dependency> requiredDependencies = new ArrayList<Dependency>();

		final List<Element> dependencies = XmlUtils.findElements(CONFIGURATION_BASE_PATH + "/dependencies/dependency", configuration);

		for (final Element dependencyElement : dependencies) {
			Dependency dependency = new Dependency(dependencyElement);
			// only if the dependency does not already exist
			if (!projectOperations.getFocusedModule().isDependencyRegistered(dependency)) {
				requiredDependencies.add(new Dependency(dependencyElement));
			}
		}

		// update the POM the new configuration
		projectOperations.addDependencies(module, requiredDependencies);

		// Identify the fixed properties
		final List<Element> pomProperties = XmlUtils.findElements(CONFIGURATION_BASE_PATH + "/properties/*", configuration);
		for (final Element property : pomProperties) {
			projectOperations.addProperty(module, new Property(property));
		}
	}

	protected void setupOlingoJPAFactory(final JavaType factoryClass) {
		updateApplicationContextConfig();
		createJPAServiceFactoryClass(factoryClass);
	}

	/**
	 * This method update the spring application context configuration file
	 */
	protected void updateApplicationContextConfig() {

		final String configPath = projectOperations.getPathResolver().getFocusedIdentifier(Path.SPRING_CONFIG_ROOT,
				"/applicationContext.xml");
		Document document = XmlUtils.readXml(fileManager.getInputStream(configPath));
		Element appContextXml = document.getDocumentElement();

		// read the configuration file from the current CLASSPATH
		final Element configuration = XmlUtils.getConfiguration(DeployCommands.class);

		// Identify odata olingo namespace to be added to Spring configuration file
		String olingoNamespace = XmlUtils.findFirstElement(CONFIGURATION_APP_CONFIG_BASE_PATH + "/namespaces/olingo-ns", configuration)
				.getTextContent();
		String olingoSchemaLocation = XmlUtils.findFirstElement(CONFIGURATION_APP_CONFIG_BASE_PATH + "/schemaLocation", configuration)
				.getTextContent();

		Element beansElement = XmlUtils.findFirstElement("/beans", appContextXml);
		if (!beansElement.hasAttribute(XML_NS_ODATA)) {
			beansElement.setAttribute(XML_NS_ODATA, olingoNamespace);
		}

		// Schema location
		String schemaLocationValue = beansElement.getAttribute(XSI_SCHEMA_LOCATION);
		if (schemaLocationValue.indexOf(olingoSchemaLocation) == -1) { // schema location does not exists
			schemaLocationValue = schemaLocationValue + " " + olingoSchemaLocation;
			beansElement.setAttribute(XSI_SCHEMA_LOCATION, schemaLocationValue);
		}

		// Inject the CXF imports
		Element configCXFImport = XmlUtils.findFirstElement("/beans/import[@resource = 'classpath:META-INF/cxf/cxf.xml']", appContextXml);
		if (configCXFImport != null) {
			configCXFImport.getParentNode().removeChild(configCXFImport);
		}
		configCXFImport = XmlUtils.findFirstElement(CONFIGURATION_APP_CONFIG_BASE_PATH
				+ "/imports/import[@resource = 'classpath:META-INF/cxf/cxf.xml']", configuration);
		Node configCXFImportNode = document.importNode(configCXFImport, true);
		appContextXml.appendChild(configCXFImportNode);

		Element configCXFServletImport = XmlUtils.findFirstElement("/beans/import[@resource = 'classpath:META-INF/cxf/cxf-servlet.xml']",
				appContextXml);
		if (configCXFServletImport != null) {
			configCXFServletImport.getParentNode().removeChild(configCXFServletImport);
		}
		configCXFServletImport = XmlUtils.findFirstElement(CONFIGURATION_APP_CONFIG_BASE_PATH
				+ "/imports/import[@resource = 'classpath:META-INF/cxf/cxf-servlet.xml']", configuration);
		Node configCXFImportServletNode = document.importNode(configCXFServletImport, true);
		appContextXml.appendChild(configCXFImportServletNode);

		// Define the OData service
		Element configODataService = XmlUtils.findFirstElement("/beans/*[local-name()='server' and @address='/odata.svc']", appContextXml);
		if (configODataService != null) {
			configODataService.getParentNode().removeChild(configODataService);
		}
		configODataService = XmlUtils.findFirstElement(CONFIGURATION_APP_CONFIG_BASE_PATH + "/serviceConfig/*", configuration);
		Node configOdataServiceNode = document.importNode(configODataService, true);
		appContextXml.appendChild(configOdataServiceNode);

		DomUtils.removeTextNodes(appContextXml);
		fileManager.createOrUpdateTextFileIfRequired(configPath, XmlUtils.nodeToString(appContextXml), false);
		// LOGGER.info(XmlUtils.nodeToString(appContextXml));
	}

	protected void createODataServiceProxyBean(JavaType serviceClass, Edm edm) throws Exception {

		final String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(serviceClass,
				pathResolver.getFocusedPath(Path.SRC_MAIN_JAVA));
		final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC,
				serviceClass, PhysicalTypeCategory.CLASS);

		// Build Imports
		final List<ImportMetadata> imports = new ArrayList<ImportMetadata>();
		imports.add(ImportMetadataBuilder.getImport(declaredByMetadataId, new JavaType("java.lang.Exception")));
		imports.add(ImportMetadataBuilder.getImport(declaredByMetadataId, new JavaType("java.util.LinkedHashMap")));
		imports.add(ImportMetadataBuilder.getImport(declaredByMetadataId, new JavaType("java.util.Map")));
		imports.add(ImportMetadataBuilder.getImport(declaredByMetadataId, new JavaType("org.apache.olingo.odata2.api.edm.Edm")));
		imports.add(ImportMetadataBuilder.getImport(declaredByMetadataId, new JavaType(
				"org.springframework.beans.factory.annotation.Autowired")));
		imports.add(ImportMetadataBuilder.getImport(declaredByMetadataId, new JavaType("org.apache.olingo.odata2.api.ep.entry.ODataEntry")));

		cidBuilder.addImports(imports);

		// Add @Service Annotation
		final List<AnnotationMetadataBuilder> annotationBuilder = new ArrayList<AnnotationMetadataBuilder>();
		annotationBuilder.add(new AnnotationMetadataBuilder(SpringJavaType.COMPONENT));
		annotationBuilder.add(new AnnotationMetadataBuilder(SpringJavaType.SERVICE));
		cidBuilder.setAnnotations(annotationBuilder);

		final List<FieldMetadataBuilder> fields = new ArrayList<FieldMetadataBuilder>();

		// Add @Autowired ODataServiceProvider
		FieldMetadataBuilder memberServiceProvider = new FieldMetadataBuilder(declaredByMetadataId, Modifier.PROTECTED, new JavaSymbolName(
				"serviceProvider"), new JavaType("ODataServiceProvider"), null);
		memberServiceProvider.addAnnotation(new AnnotationMetadataBuilder(SpringJavaType.AUTOWIRED));
		fields.add(memberServiceProvider);

		// Add @Autowired ODataServiceProvider
		FieldMetadataBuilder memberEdm = new FieldMetadataBuilder(declaredByMetadataId, Modifier.PROTECTED, new JavaSymbolName("edm"),
				new JavaType("Edm"), null);
		memberEdm.addAnnotation(new AnnotationMetadataBuilder(SpringJavaType.AUTOWIRED));
		fields.add(memberEdm);
		cidBuilder.setDeclaredFields(fields);

		// Add Methods
		// 1. Add Function Imports
		final List<org.apache.olingo.odata2.api.edm.EdmFunctionImport> edmFunctions = edm.getFunctionImports();
		for (org.apache.olingo.odata2.api.edm.EdmFunctionImport efi : edmFunctions) {

			// initialize method builder
			MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(declaredByMetadataId);
			methodBuilder.setModifier(Modifier.PUBLIC);
			methodBuilder.setMethodName(new JavaSymbolName(efi.getName()));

			// method return type
			JavaType returnType = edmParsingService.getReturnType(efi.getReturnType());
			methodBuilder.setReturnType(returnType);

			// method throws clause
			methodBuilder.addThrowsType(new JavaType("java.lang.Exception"));

			// method body and parameters
			final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
			bodyBuilder.appendFormalLine("Map<String, Object> params = new LinkedHashMap<String, Object>();");

			for (String paramName : efi.getParameterNames()) {

				// add the parameter definition to the method builder
				final EdmParameter edmParameter = efi.getParameter(paramName);
				methodBuilder.addParameter(paramName, edmParsingService.getParameterType(edmParameter));

				// add code to insert the parameter value to the map
				bodyBuilder.appendFormalLine("params.put(\"" + paramName + "\", " + paramName + ");");
			}

			String returnStmnt = "";
			String castStmnt = "";
			if (!returnType.equals(JavaType.VOID_PRIMITIVE)) { // The method has a return value
				returnStmnt = "return ";
				if (returnType.isCoreType()) {
					castStmnt = "(" + returnType + ")";
				}
			}
			bodyBuilder.newLine();
			bodyBuilder.appendFormalLine(returnStmnt + castStmnt
					+ "serviceProvider.invokeFunction(edm.getDefaultEntityContainer().getFunctionImport(\"" + efi.getName()
					+ "\"), params);");

			methodBuilder.setBodyBuilder(bodyBuilder);
			cidBuilder.addMethod(methodBuilder);
		}

		// 2. Add getters to entities
		final List<EdmEntitySet> entitySets = edm.getDefaultEntityContainer().getEntitySets();
		for (EdmEntitySet entitySet : entitySets) {

			// initialize method builder
			MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(declaredByMetadataId);
			methodBuilder.setModifier(Modifier.PUBLIC);
			methodBuilder.setMethodName(new JavaSymbolName("get" + generateJavaSymbolFromEdmName(entitySet.getName()) + "List"));

			// set return method
			methodBuilder.setReturnType(edmParsingService.getFeedReturnType());

			// method throws clause
			methodBuilder.addThrowsType(new JavaType("java.lang.Exception"));

			// method body and parameters
			final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
			bodyBuilder.appendFormalLine("return serviceProvider.readFeed(edm, \"" + entitySet.getName() + "\");");

			methodBuilder.setBodyBuilder(bodyBuilder);
			cidBuilder.addMethod(methodBuilder);

		}

		typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());
	}

	protected void createODataServiceProxyTestBean(JavaType serviceClass, Edm edm) throws Exception {

		JavaType serviceTestClass = new JavaType(serviceClass.getFullyQualifiedTypeName() + "Test");
		final String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(serviceTestClass,
				pathResolver.getFocusedPath(Path.SRC_TEST_JAVA));
		final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC,
				serviceTestClass, PhysicalTypeCategory.CLASS);

		// Build Imports
		final List<ImportMetadata> imports = new ArrayList<ImportMetadata>();
		imports.add(ImportMetadataBuilder.getImport(declaredByMetadataId, new JavaType("java.lang.Exception")));
		imports.add(ImportMetadataBuilder.getImport(declaredByMetadataId, new JavaType("java.util.List")));
		imports.add(ImportMetadataBuilder.getImport(declaredByMetadataId, new JavaType("org.junit.Assert")));
		imports.add(ImportMetadataBuilder.getImport(declaredByMetadataId, new JavaType("org.junit.runner.RunWith")));
		imports.add(ImportMetadataBuilder.getImport(declaredByMetadataId, new JavaType(
				"org.springframework.test.context.ContextConfiguration")));
		imports.add(ImportMetadataBuilder.getImport(declaredByMetadataId, new JavaType(
				"org.springframework.test.context.junit4.SpringJUnit4ClassRunner")));
		imports.add(ImportMetadataBuilder.getImport(declaredByMetadataId, new JavaType(
				"org.springframework.beans.factory.annotation.Autowired")));
		imports.add(ImportMetadataBuilder.getImport(declaredByMetadataId, new JavaType("org.apache.olingo.odata2.api.ep.entry.ODataEntry")));
		cidBuilder.addImports(imports);

		// Add @RunWith Annotation
		final List<AnnotationMetadataBuilder> annotationBuilder = new ArrayList<AnnotationMetadataBuilder>();
		final List<AnnotationAttributeValue<?>> runWithAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		runWithAttributes.add(new ClassAttributeValue(new JavaSymbolName("value"), new JavaType(
				"org.springframework.test.context.junit4.SpringJUnit4ClassRunner")));
		annotationBuilder.add(new AnnotationMetadataBuilder(new JavaType("org.junit.runner.RunWith"), runWithAttributes));

		// Add @ContextConfiguration Annotation
		final List<AnnotationAttributeValue<?>> ctxCfgAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		ctxCfgAttributes.add(new StringAttributeValue(new JavaSymbolName("locations"),
				"classpath*:META-INF/spring/applicationContext-test.xml"));
		annotationBuilder.add(new AnnotationMetadataBuilder(new JavaType("org.springframework.test.context.ContextConfiguration"),
				ctxCfgAttributes));

		cidBuilder.setAnnotations(annotationBuilder);

		final List<FieldMetadataBuilder> fields = new ArrayList<FieldMetadataBuilder>();

		// Add @Autowired test fixture
		FieldMetadataBuilder svc = new FieldMetadataBuilder(declaredByMetadataId, Modifier.PROTECTED, new JavaSymbolName("svc"),
				serviceClass, null);
		svc.addAnnotation(new AnnotationMetadataBuilder(SpringJavaType.AUTOWIRED));
		fields.add(svc);

		cidBuilder.setDeclaredFields(fields);

		// define @Test Annotation
		AnnotationMetadataBuilder testAnnotation = new AnnotationMetadataBuilder(new JavaType("org.junit.Test"));

		final List<EdmEntitySet> entitySets = edm.getDefaultEntityContainer().getEntitySets();
		for (EdmEntitySet entitySet : entitySets) {

			// initialize method builder
			MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(declaredByMetadataId);
			methodBuilder.setModifier(Modifier.PUBLIC);
			methodBuilder.setMethodName(new JavaSymbolName("test" + "Get" + generateJavaSymbolFromEdmName(entitySet.getName()) + "List"));

			// set return method
			methodBuilder.setReturnType(JavaType.VOID_PRIMITIVE);

			// method throws clause
			methodBuilder.addThrowsType(new JavaType("java.lang.Exception"));

			methodBuilder.addAnnotation(testAnnotation);

			// method body and parameters
			final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
			buildODataTestBody(entitySet, bodyBuilder);

			methodBuilder.setBodyBuilder(bodyBuilder);
			cidBuilder.addMethod(methodBuilder);
		}

		// add utility function to print results (TEMPORARY)
		MethodMetadataBuilder printMethodBuilder = createPrintUtilForTestBean(declaredByMetadataId);
		cidBuilder.addMethod(printMethodBuilder);

		typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());
	}

	private void buildODataTestBody(EdmEntitySet entitySet, final InvocableMemberBodyBuilder bodyBuilder) throws EdmException {
		bodyBuilder.appendFormalLine("List<ODataEntry> feed = null;");
		bodyBuilder.appendFormalLine("try {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("feed = svc." + "get" + generateJavaSymbolFromEdmName(entitySet.getName()) + "List" + "();");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("} catch(Exception ex) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("Assert.fail(\"test failed with cause: \"+ex.getMessage());");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("printFeed(feed);");
	}

	/**
	 * Creates the generated OData service factory class
	 * 
	 * @param factoryClassName - the name of the factory class
	 */
	protected void createJPAServiceFactoryClass(JavaType factoryClass) {

		final String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(factoryClass,
				pathResolver.getFocusedPath(Path.SRC_MAIN_JAVA));
		final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC,
				factoryClass, PhysicalTypeCategory.CLASS);

		// Add @Service class annotation
		final List<AnnotationMetadataBuilder> annotationBuilder = new ArrayList<AnnotationMetadataBuilder>();
		annotationBuilder.add(new AnnotationMetadataBuilder(SpringJavaType.SERVICE));
		cidBuilder.setAnnotations(annotationBuilder);

		// Adds extends
		cidBuilder.setExtendsTypes(Arrays.asList(new JavaType("ODataJPAServiceFactory")));

		final ClassOrInterfaceTypeDetails cid = cidBuilder.build();

		// TODO ----- This part should be improved, currently doing string manipulation from another persisted file ---//
		String newContents = typeParsingService.getCompilationUnitContents(cid);

		// Adding rest of the imports
		final InputStream inputStream = FileUtils.getInputStream(DeployCommands.class, "JPAServiceFactory.txt");
		String allClassContent = "";
		try {
			allClassContent = IOUtils.toString(inputStream);
		} catch (IOException e) {
			LOGGER.info("Could not read from input stream");
			e.printStackTrace();
		}

		String imports = allClassContent.substring(allClassContent.indexOf("import org.springframework.stereotype.Service;"),
				allClassContent.indexOf("@Service"));

		String classContent = allClassContent.substring(allClassContent.indexOf("{") + 1, allClassContent.lastIndexOf("}") + 1);

		newContents = newContents.replace("import org.springframework.stereotype.Service;", imports);
		newContents = newContents.replace("}", classContent);
		newContents = newContents.replaceAll("@Service", "@Service(value=\"jpaServiceFactory\")");

		// Adding the content of the class
		final String fileCanonicalPath = typeLocationService.getPhysicalTypeCanonicalPath(cid.getDeclaredByMetadataId());
		fileManager.createOrUpdateTextFileIfRequired(fileCanonicalPath, newContents, true);

	}

	protected String getPropertiesPath(String resourcePath, boolean forTests) {
		final String propertiesPath = pathResolver.getFocusedIdentifier(forTests ? Path.SRC_TEST_RESOURCES : Path.SRC_MAIN_RESOURCES,
				resourcePath);
		final boolean propertiesPathExists = fileManager.exists(propertiesPath);

		if (!propertiesPathExists) {
			OutputStream outputStream = fileManager.createFile(propertiesPath).getOutputStream();
			if (outputStream == null) {
				LOGGER.info("Could not create properties file " + resourcePath);
			}
		}

		return propertiesPath;
	}

	protected void createClientPropertiesFile(String serviceBasePath, String username, String password, boolean forTests) {
		final String externalServicePropertiesPath = getPropertiesPath("META-INF\\spring\\" + ODATA_SERVICE_PROPERTIES_FILE, forTests);

		// TODO this code replaces any already existed client properties file with the following.
		// Should consider merging!
		StringBuilder propertiesContent = new StringBuilder();

		// OData service properties
		propertiesContent.append("odata.service.endpoint=").append(serviceBasePath).append("\n");
		propertiesContent.append("odata.service.version=").append("2.0").append("\n");

		// Embed connection setup properties
		propertiesContent.append("connection.authentication.type=Basic\n");
		propertiesContent.append("connection.authentication.user=").append(username).append("\n");
		propertiesContent.append("connection.authentication.password=").append(password).append("\n");

		// Embed Http Headers properties for default behavior
		propertiesContent.append("connection.http.headers.accept=").append("application/json").append("\n");
		propertiesContent.append("connection.http.headers.contentType=").append("application/json").append("\n");

		fileManager.createOrUpdateTextFileIfRequired(externalServicePropertiesPath, propertiesContent.toString(), null, false);

	}

	/**
	 * Creates a new class named className, adding content from the given file
	 * @param filename - the filename to copy the content from
	 * @param className - the new class name to generate
	 */
	protected void createClassFromFile(String filename, String className) {
		// Creating a class builder
		JavaType EdmFactoryJT = new JavaType(projectOperations.getFocusedTopLevelPackage() + ".odata." + className);
		String packageName = EdmFactoryJT.getFullyQualifiedTypeName().substring(0,
				EdmFactoryJT.getFullyQualifiedTypeName().lastIndexOf('.'));

		final String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(EdmFactoryJT,
				pathResolver.getFocusedPath(Path.SRC_MAIN_JAVA));
		final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC,
				EdmFactoryJT, PhysicalTypeCategory.CLASS);

		final ClassOrInterfaceTypeDetails cid = cidBuilder.build();

		// First, adding the package name to the location of the target project
		String newContents = "package " + packageName + ";\n";

		// Creating the rest of the class content
		final InputStream inputStream = FileUtils.getInputStream(DeployCommands.class, filename);
		String allClassContent = "";
		try {
			allClassContent = IOUtils.toString(inputStream);
		} catch (IOException e) {
			LOGGER.info("Could not read from input stream");
			e.printStackTrace();
		}
		newContents += allClassContent;

		// Adding the content of the class
		final String fileCanonicalPath = typeLocationService.getPhysicalTypeCanonicalPath(cid.getDeclaredByMetadataId());
		fileManager.createOrUpdateTextFileIfRequired(fileCanonicalPath, newContents, true);
	}

	private void createEdmFactory() {
		createClassFromFile("EdmFactoryBean.txt", "EdmFactoryBean");
	}

	private void createConnectionBean() {
		createClassFromFile("ODataConnectionBean.txt", "ODataConnectionBean");
	}

	private void createServiceProvider() {
		createClassFromFile("ODataServiceProvider.txt", "ODataServiceProvider");
	}

	protected void updateBean(Document document, final Element configuration, Element appContextXml, final String xPath) {
		Element beanElement = XmlUtils.findFirstElement(xPath, appContextXml);
		if (beanElement != null) {
			beanElement.getParentNode().removeChild(beanElement);
		}
		beanElement = XmlUtils.findFirstElement(CONFIGURATION_APP_CONFIG_BASE_PATH + xPath, configuration);
		String className = beanElement.getAttribute("class");
		beanElement.setAttribute("class", projectOperations.getFocusedTopLevelPackage() + ".odata." + className);
		Node edmFactoryBeanNode = document.importNode(beanElement, true);
		appContextXml.appendChild(edmFactoryBeanNode);
	}

	protected void setupODataServiceApplicationContext() {

		final String configPath = projectOperations.getPathResolver().getFocusedIdentifier(Path.SPRING_CONFIG_ROOT,
				"/applicationContext.xml");
		Document document = XmlUtils.readXml(fileManager.getInputStream(configPath));
		Element appContextXml = document.getDocumentElement();

		// read the configuration file from the current CLASSPATH
		final Element configuration = XmlUtils.getConfiguration(DeployCommands.class);

		// configure EdmFactoryBean
		updateBean(document, configuration, appContextXml, "/beans/bean[@id = 'defaultEdm']");

		// configure ODataConnection
		updateBean(document, configuration, appContextXml, "/beans/bean[@id = 'defaultODataConn']");

		// configure ServiveProvider
		updateBean(document, configuration, appContextXml, "/beans/bean[@id = 'odataServiceProvider']");

		DomUtils.removeTextNodes(appContextXml);
		fileManager.createOrUpdateTextFileIfRequired(configPath, XmlUtils.nodeToString(appContextXml), false);
	}

	private String generateJavaSymbolFromEdmName(final String edmName) {
		return edmName.replaceAll("[-_@<> %~\\/\\*\\!\\?\\^\\(\\)\\{\\}\\[\\]\\|\\'\\+]", "");
	}

	@Override
	public void setupExternalService(final String serviceBasePath, final String username, final String password,
			final String serviceProxyName, final boolean isTestAutomatically) {

		// Create the properties file
		// TODO name of the file should not be hardcoded !!!
		createClientPropertiesFile(serviceBasePath, username, password, false);

		// Create some infrastructure beans that allow the users to plug odata service
		// consumption support into their application
		createEdmFactory();
		createConnectionBean();
		createServiceProvider();

		setupODataServiceApplicationContext();

		// consume external service and generate service proxy
		odataServiceProvider.getConnection().setUserName(username);
		odataServiceProvider.getConnection().setPassword(password);
		odataServiceProvider.setEndpoint(serviceBasePath);

		try {
			Edm edm = odataServiceProvider.readEdm();

			// find the name of the service
			// TODO have the command set hte name because we do not have access to the schema name here
			String serviceName = serviceProxyName != null ? serviceProxyName : edm.getDefaultEntityContainer().getName();
			JavaType serviceBeanType = new JavaType(projectOperations.getFocusedTopLevelPackage() + ".odata." + serviceName);

			createODataServiceProxyBean(serviceBeanType, edm);

			// If tests automatically is given - create the applicationContext-test.xml and the generated tests
			if (isTestAutomatically) {
				createClientPropertiesFile(serviceBasePath, username, password, true);
				createApplicationContextTest();
				createODataServiceProxyTestBean(serviceBeanType, edm);
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalStateException("failed to consume the external service", e);
		}

	}

	private MethodMetadataBuilder createPrintUtilForTestBean(String declaredByMetadataId) {
		// create utility function
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(declaredByMetadataId);
		methodBuilder.setModifier(Modifier.PRIVATE);
		methodBuilder.setMethodName(new JavaSymbolName("printFeed"));

		// create feed parameter (List<ODataEntry>) for the function
		List<JavaSymbolName> feedParamName = new ArrayList<JavaSymbolName>();
		feedParamName.add(new JavaSymbolName("feed"));
		methodBuilder.setParameterNames(feedParamName);
		List<AnnotatedJavaType> feedParamType = new ArrayList<AnnotatedJavaType>();
		feedParamType.add(AnnotatedJavaType.convertFromJavaType(edmParsingService.getFeedReturnType()));
		methodBuilder.setParameterTypes(feedParamType);

		// set return method
		methodBuilder.setReturnType(JavaType.VOID_PRIMITIVE);

		// method body
		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("for (ODataEntry entry : feed) {\n" + "Assert.assertNotNull(entry.getProperties());\n"
				+ "System.out.println((new StringBuilder(\"Entry: \")).append(entry.getMetadata().getUri()).toString());\n" + "}\n");
		methodBuilder.setBodyBuilder(bodyBuilder);
		return methodBuilder;
	}
}
