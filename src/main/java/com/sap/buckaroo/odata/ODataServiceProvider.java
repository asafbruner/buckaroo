package com.sap.buckaroo.odata;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.olingo.odata2.api.commons.HttpContentType;
import org.apache.olingo.odata2.api.commons.HttpHeaders;
import org.apache.olingo.odata2.api.commons.ODataHttpMethod;
import org.apache.olingo.odata2.api.edm.Edm;
import org.apache.olingo.odata2.api.edm.EdmEntityContainer;
import org.apache.olingo.odata2.api.edm.EdmFunctionImport;
import org.apache.olingo.odata2.api.ep.EntityProvider;
import org.apache.olingo.odata2.api.ep.EntityProviderReadProperties;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.exception.ODataException;

public class ODataServiceProvider  {
	
	private ODataConnection connection;
	private String serviceUrl;
	
	public String getEndpoint() {
		return serviceUrl;
	}

	public void setEndpoint(String endpoint) {
		this.serviceUrl = endpoint;
	}
	
	public ODataConnection getConnection() {
		return connection;
	}
	
	public void setConnection(ODataConnection connection) {
		this.connection = connection;
	}

	public Edm readEdm() throws IOException, ODataException {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(HttpHeaders.ACCEPT, HttpContentType.APPLICATION_XML);
		headers.put(HttpHeaders.CONTENT_TYPE, HttpContentType.APPLICATION_XML);
		
		InputStream content = connection.sendRequest(serviceUrl + "/$metadata", ODataHttpMethod.GET.name(), headers);
		return EntityProvider.readMetadata(content, false);
	}
	
	public ODataEntry readEntry(Edm edm, String entitySetName, String keyValue, String expand) throws IOException, ODataException {
		String path = createURI(entitySetName, keyValue, expand);
		EdmEntityContainer entityContainer = edm.getDefaultEntityContainer();
		
		InputStream content = connection.sendRequest(path, ODataHttpMethod.GET.name(), null);
		return EntityProvider.readEntry(connection.getHttpHeaderConnectType(), entityContainer.getEntitySet(entitySetName), content, EntityProviderReadProperties
				.init().build());
	}
	
	public List<ODataEntry> readFeed(Edm edm, String entitySetName) throws IOException, ODataException{
		String path = createURI(entitySetName);
		EdmEntityContainer entityContainer = edm.getDefaultEntityContainer();
		
		InputStream content = connection.sendRequest(path, ODataHttpMethod.GET.name(), null);
		return EntityProvider.readFeed(connection.getHttpHeaderConnectType(), entityContainer.getEntitySet(entitySetName), content, EntityProviderReadProperties
				.init().build()).getEntries();
	}
	
	public Object invokeFunction(EdmFunctionImport f, Map<String, Object>parameters) throws IOException, ODataException {
		//String path = createFunctionURI(f.getName(), parameters);
		//InputStream content = connection.sendRequest(path, ODataHttpMethod.GET.name(), null);
		
		//Object result = EntityProvider.readFunctionImport(connection.getHttpHeaderConnectType(), f, content,  EntityProviderReadProperties.init().build());
		//return (result instanceof ODataFeed) ? ((ODataFeed)result).getEntries() : result;
		return new ArrayList<ODataEntry>();
	}
	
	protected String createURI(String entitySetName, String keyValue, String expand) {
		StringBuilder sb = new StringBuilder(createURI(entitySetName)).append("(").append(keyValue).append(")");
		if (expand != null) {
			sb.append("?expand=").append(expand);
		}
		return sb.toString();
	}
	
	protected String createURI(String entitySetName) {
		StringBuilder sb = new StringBuilder(serviceUrl).append("/").append(entitySetName);
		return sb.toString();
	}
	
	protected String createFunctionURI(String functionName, Map<String, Object>parameters) {
		StringBuilder sb = new StringBuilder(createURI(functionName));
		if (!parameters.isEmpty()) {
			List<String> parameterList = new LinkedList<String>();
			for (Map.Entry<String, Object> paramEntry : parameters.entrySet()) {
				final StringBuilder param = new StringBuilder(paramEntry.getKey()).append("=").append(getParamValue(paramEntry.getValue()));
				parameterList.add(param.toString());
			}
			
			sb.append("?").append(StringUtils.join(parameterList.toArray(new String[] {}), "&"));
		}
		
		return sb.toString();
	}
	
	protected String getParamValue(Object param) {
		if (param instanceof String) {
			return "'" + (String)param + "'";
		} 
		
		return String.valueOf(param);
	}

}
