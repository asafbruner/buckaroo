package com.sap.river.log;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.annotations.populator.AbstractAnnotationValues;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.model.JavaType;

public class RiverTraceAnnotationValues extends AbstractAnnotationValues {
	
	@AutoPopulate private JavaType entity;

	protected RiverTraceAnnotationValues(
			final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
        super(governorPhysicalTypeMetadata, TraceMetadata.RIVER_TRACE);
        AutoPopulationUtils.populate(this, annotationMetadata);
	}
	
	public JavaType getEntity() {
        return entity;
    }

}
