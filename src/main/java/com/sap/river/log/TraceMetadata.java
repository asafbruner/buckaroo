package com.sap.river.log;

import java.lang.reflect.Modifier;

import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

public class TraceMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

	protected static final String PROVIDES_TYPE_STRING = TraceMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils
            .create(PROVIDES_TYPE_STRING);
	final public static JavaType RIVER_TRACE = new JavaType(RiverTrace.class);
	
	protected String entityName;
	
	protected TraceMetadata(String identifier, String entityName, JavaType aspectName,
			PhysicalTypeMetadata governorPhysicalTypeMetadata) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		this.entityName = entityName;
		builder.addField(getLoggerStaticField());
		itdTypeDetails = builder.build();
	}
	
	private FieldMetadata getLoggerStaticField() {
		//private final Logger logger = LoggerFactory.getLogger(BaseController.class);
		// TODO Auto-generated method stub
		final JavaSymbolName fieldName = new JavaSymbolName("LOGGER"); 
		final JavaType fieldType = new JavaType("org.slf4j.Logger");
		final String fieldInitializer = "org.slf4j.LoggerFactory.getLogger("+ entityName + ".class)"; 
		return new FieldMetadataBuilder(getId(), 
				Modifier.FINAL + Modifier.STATIC + Modifier.PUBLIC, 
				fieldName, fieldType, fieldInitializer).build();
	}

	public static String getMetadataIdentifierType() {
        return PROVIDES_TYPE;
    }

	public static String createIdentifier(final JavaType javaType, final LogicalPath path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	public static String getPhysicalTypeIdentifier(
			String metadataIdentificationString) {
		final LogicalPath path = PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING,
                metadataIdentificationString);
        final JavaType javaType = PhysicalTypeIdentifierNamingUtils.getJavaType(
                PROVIDES_TYPE_STRING, metadataIdentificationString);
        return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

}
