package com.sap.river;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Example of an enum used for tab-completion of properties.
 * 
 * @since 1.1.1
 */
public enum RiverPropertyName {
	AUSTRALIA("Australia"), UNITED_STATES("United States"), GERMANY("Germany"), ISRAEL("Israel"), NOT_SPECIFIED("None of your business!");

	private String propertyName;

	private RiverPropertyName(String propertyName) {
		Validate.notBlank(propertyName, "Property name required");
		this.propertyName = propertyName;
	}

	public String getPropertyName() {
		return propertyName;
	}

	@Override
	public String toString() {
		final ToStringBuilder builder = new ToStringBuilder(this);
		builder.append("propertyName", propertyName);
		return builder.toString();
	}
}