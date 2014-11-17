package com.sap.river.odata;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.JavaType;

import com.sap.river.model.RiverJavaType;

/**
 * Represents a parsed {@link ODataIntegrationTest} annotation.
 * 
 */
public class ODataIntegrationTestAnnotationValues extends AbstractAnnotationValues {

    public static final String ENTITY_SET_METHOD = "entitySet";
    
	@AutoPopulate private JavaType entity;
    @AutoPopulate private boolean entitySet = true;

    public ODataIntegrationTestAnnotationValues(
            final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
        super(governorPhysicalTypeMetadata, RiverJavaType.RIVER_ODATA_INTEGRATION_TEST);
        AutoPopulationUtils.populate(this, annotationMetadata);
    }

    public JavaType getEntity() {
        return entity;
    }

    public boolean isEntitySet() {
    	return entitySet;
    }
}
