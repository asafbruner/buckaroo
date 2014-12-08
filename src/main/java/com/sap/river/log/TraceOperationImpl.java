package com.sap.river.log;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;

@Component
@Service
public class TraceOperationImpl implements TraceOperations {
	
	@Reference private MetadataService metadataService;
	@Reference private TypeLocationService typeLocationService;
	@Reference private TypeManagementService typeManagementService;

	@Override
	public boolean isSetupTraceCommandAvailable() {
		return true;
	}

	@Override
	public void setupTrace(JavaType className) {
		// TODO Auto-generated method stub

		//append @RiverTrace to the test file so it would trigger the creation of OData tests aspect
		final ClassOrInterfaceTypeDetails cid = getEntity(className);
		Validate.isTrue(!Modifier.isAbstract(cid.getModifier()),
        "Type %s is abstract", className.getFullyQualifiedTypeName());
		final LogicalPath path = PhysicalTypeIdentifier.getPath(cid.getDeclaredByMetadataId());
		final String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(className,
                Path.SRC_MAIN_JAVA.getModulePathId(path.getModule()));
        
        final PhysicalTypeMetadata ptm = (PhysicalTypeMetadata) metadataService
                .get(declaredByMetadataId);
 
        if (ptm != null) {
        	final PhysicalTypeDetails ptd = ptm.getMemberHoldingTypeDetails();
        	final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                    (ClassOrInterfaceTypeDetails) ptd);
        	final List<AnnotationAttributeValue<?>> config = new ArrayList<AnnotationAttributeValue<?>>();
            config.add(new ClassAttributeValue(new JavaSymbolName("entity"), className));
        	cidBuilder.addAnnotation(new AnnotationMetadataBuilder(new JavaType(RiverTrace.class),
                    config));
        	typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());
        }
	}

	protected ClassOrInterfaceTypeDetails getEntity(final JavaType entity) {
        final ClassOrInterfaceTypeDetails cid = typeLocationService
                .getTypeDetails(entity);
        Validate.notNull(cid,
                "Java source code details unavailable for type %s", cid);
        return cid;
    }

}
