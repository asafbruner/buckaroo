package com.sap.river.jpasetup;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;

public enum JpaSetupPropertyName {

	HANA("HANA"), MAX_DB("MAX_DB"), DERBY("DERBY");

	private String propertyName;

	private JpaSetupPropertyName(String propertyName) {
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
