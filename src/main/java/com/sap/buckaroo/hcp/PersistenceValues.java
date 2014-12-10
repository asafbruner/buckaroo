package com.sap.buckaroo.hcp;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * enum used for tab-completion of persistence properties.
 * 
 * @since 1.1.1
 */
public enum PersistenceValues {
    HANA("HANA"),
    MAX_DB("MAX_DB"),
    DERBY("DERBY");
    
    private String propertyName;
    
    private PersistenceValues(String propertyName) {
        Validate.notBlank(propertyName, "Property name required");
        this.propertyName = propertyName;
    }
    
    public String getPropertyName() {
        return propertyName;
    }

    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
         builder.append("propertyName", propertyName);
        return builder.toString();
    }
}