package com.sap.river.odata;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link ODataIntegrationTest}.
 * 
 */
public class ODataIntegrationTestMetadata extends
        AbstractItdTypeDetailsProvidingMetadataItem {

    private static final JavaType WEB_APP_CONFIGURATION = new JavaType(
    		"org.springframework.test.context.web.WebAppConfiguration");
    
    private static final JavaType TEST = new JavaType("org.junit.Test");
    
    private static final String PROVIDES_TYPE_STRING = ODataIntegrationTestMetadata.class
            .getName();

    public static String createIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return PhysicalTypeIdentifierNamingUtils.createIdentifier(
                PROVIDES_TYPE_STRING, javaType, path);
    }

    public static JavaType getJavaType(final String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.getJavaType(
                PROVIDES_TYPE_STRING, metadataIdentificationString);
    }

    public static String getMetadataIdentiferType() {
        return MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
    }

    public static LogicalPath getPath(final String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING,
                metadataIdentificationString);
    }

    public static boolean isValid(final String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING,
                metadataIdentificationString);
    }

    private ODataIntegrationTestAnnotationValues annotationValues;
    
    //private boolean entityHasSuperclass;
    //private boolean hasEmbeddedIdentifier;
    // private boolean isGaeSupported = false;
    // private String transactionManager;

    public ODataIntegrationTestMetadata(final String identifier,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final ODataIntegrationTestAnnotationValues annotationValues
            //final DataOnDemandMetadata dataOnDemandMetadata,
            //final String transactionManager,
            //final boolean hasEmbeddedIdentifier,
            //final boolean entityHasSuperclass
            ) {
        super(identifier, aspectName, governorPhysicalTypeMetadata);
        Validate.isTrue(
                isValid(identifier),
                "Metadata identification string '%s' does not appear to be a valid",
                identifier);
        Validate.notNull(annotationValues, "Annotation values required");

        if (!isValid()) {
            return;
        }

        this.annotationValues = annotationValues;
        //this.transactionManager = transactionManager;
        //this.hasEmbeddedIdentifier = hasEmbeddedIdentifier;
        //this.entityHasSuperclass = entityHasSuperclass;

        addRequiredIntegrationTestClassIntroductions();        
        builder.addMethod(getEntitySetMethodTest());
        itdTypeDetails = builder.build();
    }

    /**
     * Adds the JUnit and Spring type level annotations if needed
     * OData aspect just add injected code on top of the standard integration test aspects 
     */
    protected void addRequiredIntegrationTestClassIntroductions() {
        // Add an @WebAppConfiguration annotation to the type, if 
        // the user did not define it on the governor directly
        if (MemberFindingUtils.getAnnotationOfType(
                governorTypeDetails.getAnnotations(), WEB_APP_CONFIGURATION) == null) {
            final AnnotationMetadataBuilder webAppConfigurationBuilder = new AnnotationMetadataBuilder(
            		WEB_APP_CONFIGURATION);
            builder.addAnnotation(webAppConfigurationBuilder);
        }
    }

    /**
     * @return a test for the count method, if available and requested (may
     *         return null)
     */
    private MethodMetadataBuilder getEntitySetMethodTest() {
        if (!annotationValues.isEntitySet()) {
            // User does not want this method
            return null;
        }

        // Prepare method signature
        final JavaSymbolName methodName = new JavaSymbolName("test"
                + StringUtils.capitalize(ODataIntegrationTestAnnotationValues.ENTITY_SET_METHOD));
        if (governorHasMethod(methodName)) {
            return null;
        }

        final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
        annotations.add(new AnnotationMetadataBuilder(TEST));

        final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        bodyBuilder.appendFormalLine("org.junit.Assert.assertTrue(true);");

        final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(
                getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE,
                bodyBuilder);
        methodBuilder.setAnnotations(annotations);
        return methodBuilder;
    }


    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("identifier", getId());
        builder.append("valid", valid);
        builder.append("aspectName", aspectName);
        builder.append("destinationType", destination);
        builder.append("governor", governorPhysicalTypeMetadata.getId());
        builder.append("itdTypeDetails", itdTypeDetails);
        return builder.toString();
    }
}