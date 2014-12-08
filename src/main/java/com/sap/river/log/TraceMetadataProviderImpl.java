package com.sap.river.log;

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.configurable.ConfigurableMetadataProvider;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

@Component(immediate = true)
@Service
public class TraceMetadataProviderImpl extends
AbstractItdMetadataProvider implements TraceMetadataProvider {
	
	@Reference private ConfigurableMetadataProvider configurableMetadataProvider;
	
	protected final Map<JavaType, String> managedEntityTypes = new HashMap<JavaType, String>();
	
	protected void activate(final ComponentContext context) {
        metadataDependencyRegistry.addNotificationListener(this);
        metadataDependencyRegistry.registerDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        configurableMetadataProvider.addMetadataTrigger(TraceMetadata.RIVER_TRACE);
        addMetadataTrigger(TraceMetadata.RIVER_TRACE);
    }
	
	protected void deactivate(final ComponentContext context) {
        metadataDependencyRegistry.removeNotificationListener(this);
        metadataDependencyRegistry.deregisterDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        configurableMetadataProvider
                .removeMetadataTrigger(TraceMetadata.RIVER_TRACE);
        removeMetadataTrigger(TraceMetadata.RIVER_TRACE);
    }

	@Override
	public String getItdUniquenessFilenameSuffix() {
		return "River_Trace";
	}

	@Override
	public String getProvidesType() {
		// TODO Auto-generated method stub
		return TraceMetadata.getMetadataIdentifierType();
	}

	@Override
	protected String createLocalIdentifier(JavaType javaType, LogicalPath path) {
		return TraceMetadata.createIdentifier(javaType, path);
	}

	@Override
	protected String getGovernorPhysicalTypeIdentifier(
			String metadataIdentificationString) {
		return TraceMetadata.getPhysicalTypeIdentifier(metadataIdentificationString);
	}

	@Override
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(
			String metadataIdentificationString, JavaType aspectName,
			PhysicalTypeMetadata governorPhysicalTypeMetadata,
			String itdFilename) {
		final RiverTraceAnnotationValues annotationValues = new RiverTraceAnnotationValues(
                governorPhysicalTypeMetadata);
        final JavaType entity = annotationValues.getEntity();
        if (!annotationValues.isAnnotationFound() || entity == null) {
            return null;
        }
        
        //TODO: what to do with inheritance
        
        // Maintain a list of entities that are being tested
        managedEntityTypes.put(entity, metadataIdentificationString);
		return new TraceMetadata(metadataIdentificationString, entity.getSimpleTypeName(), aspectName, governorPhysicalTypeMetadata);
	}

}
