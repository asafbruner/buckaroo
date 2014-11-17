package com.sap.river.odata;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.configurable.ConfigurableMetadataProvider;
import org.springframework.roo.addon.test.IntegrationTestMetadataProvider;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.layers.LayerService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;

import com.sap.river.model.RiverJavaType;
//import static org.springframework.roo.model.RooJavaType.ROO_INTEGRATION_TEST;

/**
 * Implementation of {@link IntegrationTestMetadataProvider}.
 *
 */
@Component(immediate = true)
@Service
public class ODataIntegrationTestMetadataProviderImpl extends
        AbstractItdMetadataProvider implements IntegrationTestMetadataProvider {

    @Reference private ConfigurableMetadataProvider configurableMetadataProvider;
    @Reference private LayerService layerService;
    @Reference private ProjectOperations projectOperations;

    private final Map<JavaType, String> managedEntityTypes = new HashMap<JavaType, String>();
    private final Set<String> producedMids = new LinkedHashSet<String>();
    private Boolean wasGaeEnabled;

    protected void activate(final ComponentContext context) {
        metadataDependencyRegistry.addNotificationListener(this);
        metadataDependencyRegistry.registerDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        
        // Integration test classes are @Configurable because they may need DI
        // of other DOD classes that provide M:1 relationships
        configurableMetadataProvider.addMetadataTrigger(RiverJavaType.RIVER_ODATA_INTEGRATION_TEST);
        addMetadataTrigger(RiverJavaType.RIVER_ODATA_INTEGRATION_TEST);
    }

    @Override
    protected String createLocalIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return ODataIntegrationTestMetadata.createIdentifier(javaType, path);
    }

    protected void deactivate(final ComponentContext context) {
        metadataDependencyRegistry.removeNotificationListener(this);
        metadataDependencyRegistry.deregisterDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());

        configurableMetadataProvider.removeMetadataTrigger(RiverJavaType.RIVER_ODATA_INTEGRATION_TEST);
        removeMetadataTrigger(RiverJavaType.RIVER_ODATA_INTEGRATION_TEST);
    }

//    private ClassOrInterfaceTypeDetails getEntitySuperclass(
//            final JavaType entity) {
//        final String physicalTypeIdentifier = PhysicalTypeIdentifier
//                .createIdentifier(entity,
//                        typeLocationService.getTypePath(entity));
//        final PhysicalTypeMetadata ptm = (PhysicalTypeMetadata) metadataService
//                .get(physicalTypeIdentifier);
//        Validate.notNull(ptm, "Java source code unavailable for type %s",
//                PhysicalTypeIdentifier.getFriendlyName(physicalTypeIdentifier));
//        final PhysicalTypeDetails ptd = ptm.getMemberHoldingTypeDetails();
//        Validate.notNull(ptd,
//                "Java source code details unavailable for type %s",
//                PhysicalTypeIdentifier.getFriendlyName(physicalTypeIdentifier));
//        Validate.isInstanceOf(ClassOrInterfaceTypeDetails.class, ptd,
//                "Java source code is immutable for type %s",
//                PhysicalTypeIdentifier.getFriendlyName(physicalTypeIdentifier));
//        final ClassOrInterfaceTypeDetails cid = (ClassOrInterfaceTypeDetails) ptd;
//        return cid.getSuperclass();
//    }

    @Override
    protected String getGovernorPhysicalTypeIdentifier(
            final String metadataIdentificationString) {
        final JavaType javaType = ODataIntegrationTestMetadata
                .getJavaType(metadataIdentificationString);
        final LogicalPath path = ODataIntegrationTestMetadata
                .getPath(metadataIdentificationString);
        return PhysicalTypeIdentifier.createIdentifier(javaType, path);
    }

    public String getItdUniquenessFilenameSuffix() {
        return "ODataIntegrationTest";
    }

    @Override
    protected ItdTypeDetailsProvidingMetadataItem getMetadata(
            final String metadataIdentificationString,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final String itdFilename) {
        // We need to parse the annotation, which we expect to be present
        final ODataIntegrationTestAnnotationValues annotationValues = new ODataIntegrationTestAnnotationValues(
                governorPhysicalTypeMetadata);
        final JavaType entity = annotationValues.getEntity();
        if (!annotationValues.isAnnotationFound() || entity == null) {
            return null;
        }

        // In order to handle switching between GAE and JPA produced MIDs need
        // to be remembered so they can be regenerated on JPA <-> GAE switch
        producedMids.add(metadataIdentificationString);

        // Maintain a list of entities that are being tested
        managedEntityTypes.put(entity, metadataIdentificationString);

//        final String moduleName = PhysicalTypeIdentifierNamingUtils.getPath(
//                metadataIdentificationString).getModule();
//        final boolean isGaeEnabled = projectOperations
//                .isProjectAvailable(moduleName)
//                && projectOperations.isFeatureInstalledInModule(
//                        FeatureNames.GAE, moduleName);

        return new ODataIntegrationTestMetadata(metadataIdentificationString,
                aspectName, governorPhysicalTypeMetadata, annotationValues);
    }

    public String getProvidesType() {
        return ODataIntegrationTestMetadata.getMetadataIdentiferType();
    }

    private void handleChangesToLayeringForTestedEntities(
            final JavaType physicalType) {
        final MemberHoldingTypeDetails memberHoldingTypeDetails = typeLocationService
                .getTypeDetails(physicalType);
        if (memberHoldingTypeDetails != null) {
            for (final JavaType type : memberHoldingTypeDetails
                    .getLayerEntities()) {
                handleChangesToTestedEntities(type);
            }
        }
    }

    private void handleChangesToTestedEntities(final JavaType physicalType) {
        final String localMid = managedEntityTypes.get(physicalType);
        if (localMid != null) {
            // One of the entities for which we produce metadata has changed;
            // refresh that metadata
            metadataService.get(localMid);
        }
    }

    /**
     * Handles a generic change (i.e. with no explicit downstream dependency) to
     * the given physical type
     * 
     * @param physicalType the type that changed (required)
     */
    private void handleGenericChangeToPhysicalType(final JavaType physicalType) {
        handleChangesToTestedEntities(physicalType);
        handleChangesToLayeringForTestedEntities(physicalType);
    }

    /**
     * Handles a generic change (i.e. with no explicit downstream dependency) to
     * the project metadata
     */
    private void handleGenericChangeToProject(final String moduleName) {
        final ProjectMetadata projectMetadata = projectOperations
                .getProjectMetadata(moduleName);
        if (projectMetadata != null && projectMetadata.isValid()) {
            final boolean isGaeEnabled = projectOperations
                    .isFeatureInstalledInModule(FeatureNames.GAE, moduleName);
            // We need to determine if the persistence state has changed, we do
            // this by comparing the last known state to the current state
            final boolean hasGaeStateChanged = wasGaeEnabled == null
                    || isGaeEnabled != wasGaeEnabled;
            if (hasGaeStateChanged) {
                wasGaeEnabled = isGaeEnabled;
                for (final String producedMid : producedMids) {
                    metadataService.evictAndGet(producedMid);
                }
            }
        }
    }

    @Override
    protected void notifyForGenericListener(final String upstreamDependency) {
        if (PhysicalTypeIdentifier.isValid(upstreamDependency)) {
            handleGenericChangeToPhysicalType(PhysicalTypeIdentifier
                    .getJavaType(upstreamDependency));
        }
        if (ProjectMetadata.isValid(upstreamDependency)) {
            handleGenericChangeToProject(ProjectMetadata
                    .getModuleName(upstreamDependency));
        }
    }
}
