package com.sap.buckaroo.odata;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.edm.EdmMultiplicity;
import org.apache.olingo.odata2.api.edm.EdmParameter;
import org.apache.olingo.odata2.api.edm.EdmSimpleType;
import org.apache.olingo.odata2.api.edm.EdmTypeKind;
import org.apache.olingo.odata2.api.edm.EdmTyped;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaType;

@Component
@Service
public class EdmTypeParsingServiceImpl implements EdmTypeParsingService {

	@Override
	public JavaType getFeedReturnType() {
		final List<JavaType> returnTypeGenericParams = new ArrayList<JavaType>();
		returnTypeGenericParams.add(new JavaType("ODataEntry"));
		JavaType returnType = new JavaType(List.class.getName(), 0,
		        DataType.TYPE, null, returnTypeGenericParams);
		return returnType;
	}
	
	@Override
	public JavaType getFeedReturnType(JavaType className) {
		final List<JavaType> returnTypeGenericParams = new ArrayList<JavaType>();
		returnTypeGenericParams.add(className);
		JavaType returnType = new JavaType(List.class.getName(), 0,
		        DataType.TYPE, null, returnTypeGenericParams);
		return returnType;
	}

	@Override
	public JavaType getParameterType(EdmParameter edmParameter)
			throws EdmException {
		return convertEdmTypeToJavaType(edmParameter);
	}

	@Override
	public JavaType getReturnType(EdmTyped edmTyped) throws EdmException {
		return convertEdmTypeToJavaType(edmTyped);
	}
	
	private JavaType convertEdmTypeToJavaType(EdmTyped edmTyped) throws EdmException {
		
		JavaType returnType = null;
		
		if (edmTyped == null) {
			returnType = JavaType.VOID_PRIMITIVE;
		} else if (edmTyped.getType() != null && EdmTypeKind.SIMPLE.equals(edmTyped.getType().getKind())) {
			EdmSimpleType stype = (EdmSimpleType) (edmTyped.getType());
			if (byte[].class.equals(stype.getDefaultType())) {
				returnType = getFeedReturnType(new JavaType(java.lang.Byte.class.getName()));
			} else {
				returnType = new JavaType(stype.getDefaultType().getName());
			}
		} else { //
			returnType = new JavaType(java.lang.Object.class.getName());
		}
		
		//The edm type has n multiplicity -- need to convert to java list
		if (edmTyped.getMultiplicity().equals(EdmMultiplicity.MANY)) {
			returnType = getFeedReturnType(returnType);
		}
		return returnType;
	}

}
