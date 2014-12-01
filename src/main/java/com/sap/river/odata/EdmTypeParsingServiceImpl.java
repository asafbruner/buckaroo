package com.sap.river.odata;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.olingo.odata2.api.edm.EdmException;
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
	public JavaType getParameterType(EdmParameter edmParameter)
			throws EdmException {
		JavaType paramType;
		if (EdmTypeKind.SIMPLE.equals(edmParameter.getType().getKind())) { //If parameter is simple type
			EdmSimpleType simpleParam = (EdmSimpleType)edmParameter.getType();
			paramType = new JavaType(simpleParam.getDefaultType());
		}
		else { //parameter is an Object
			paramType = new JavaType("java.lang.Object");
		}
		return paramType;
	}

	@Override
	public JavaType getReturnType(EdmTyped edmTyped) throws EdmException {
		JavaType returnType;
		if (edmTyped == null) {
			returnType = JavaType.VOID_PRIMITIVE;
		} else if (edmTyped.getType() != null && EdmTypeKind.SIMPLE.equals(
				edmTyped.getType().getKind())) {
			EdmSimpleType stype = (EdmSimpleType)(edmTyped.getType());
			returnType = new JavaType(stype.getDefaultType().getName());
		} else {
			returnType = new JavaType(java.lang.Object.class.getName());
		}
		return returnType;
	}

}
