package com.sap.river.odata;

import static org.springframework.roo.shell.OptionContexts.UPDATE_PROJECT;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.BeanInfoUtils;
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
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

import com.sap.river.model.RiverJavaType;

@Component
@Service
public class ODataIntegrationTestsCommands implements CommandMarker {
	
	//TODO: delegate this to Operation interface
	@Reference private MetadataService metadataService;
	@Reference private TypeLocationService typeLocationService;
	@Reference private TypeManagementService typeManagementService;
	
	/////////////////////////////////////
	// ODATA Commands
	/////////////////////////////////////
	@CliAvailabilityIndicator({"odata test integration" })
	public boolean isODataAvailable() {
		return true;
	}
	
	@CliCommand(value = "odata test integration", help = "Creates a new OData integration test for the specified entity")
    public void newODataIntegrationTest(
            @CliOption(key = "entity", mandatory = false, unspecifiedDefaultValue = "*", optionContext = UPDATE_PROJECT, help = "The name of the entity to create an integration test for") final JavaType entity) {

        Validate.isTrue(
                BeanInfoUtils.isEntityReasonablyNamed(entity),
                "Cannot create an integration test for an entity named 'Test' or 'TestCase' under any circumstances");
        //append @RiverODataTestIntegration to the test file so it would trigger the creation of OData tests aspect
        final ClassOrInterfaceTypeDetails cid = getEntity(entity);
        Validate.isTrue(!Modifier.isAbstract(cid.getModifier()),
                "Type %s is abstract", entity.getFullyQualifiedTypeName());
        final LogicalPath path = PhysicalTypeIdentifier.getPath(cid
                .getDeclaredByMetadataId());
        final JavaType name = new JavaType(entity + "IntegrationTest");
        final String declaredByMetadataId = PhysicalTypeIdentifier
                .createIdentifier(name,
                        Path.SRC_TEST_JAVA.getModulePathId(path.getModule()));

        
        final PhysicalTypeMetadata ptm = (PhysicalTypeMetadata) metadataService
                .get(declaredByMetadataId);
        if (ptm != null) {
        	final PhysicalTypeDetails ptd = ptm.getMemberHoldingTypeDetails();
        	final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                    (ClassOrInterfaceTypeDetails) ptd);
        	final List<AnnotationAttributeValue<?>> config = new ArrayList<AnnotationAttributeValue<?>>();
            config.add(new ClassAttributeValue(new JavaSymbolName("entity"), entity));
        	cidBuilder.addAnnotation(new AnnotationMetadataBuilder(RiverJavaType.RIVER_ODATA_INTEGRATION_TEST,
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
