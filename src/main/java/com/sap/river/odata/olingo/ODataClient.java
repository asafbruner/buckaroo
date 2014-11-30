package com.sap.river.odata.olingo;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.olingo.odata2.api.commons.HttpStatusCodes;
import org.apache.olingo.odata2.api.edm.Edm;
import org.apache.olingo.odata2.api.ep.EntityProvider;
import org.apache.olingo.odata2.api.exception.ODataException;

public class ODataClient {
	public static final String HTTP_METHOD_PUT = "PUT";
	public static final String HTTP_METHOD_POST = "POST";
	public static final String HTTP_METHOD_GET = "GET";
	private static final String HTTP_METHOD_DELETE = "DELETE";

	public static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";
	public static final String HTTP_HEADER_ACCEPT = "Accept";

	public static final String APPLICATION_JSON = "application/json";
	public static final String APPLICATION_XML = "application/xml";
	public static final String APPLICATION_ATOM_XML = "application/atom+xml";
	public static final String APPLICATION_FORM = "application/x-www-form-urlencoded";
	public static final String METADATA = "$metadata";
	public static final String INDEX = "/index.jsp";
	public static final String SEPARATOR = "/";


	private String serviceUrl;
	private String authToken;
	
	public ODataClient(String serviceUrl, String authToken) {
		this.serviceUrl = serviceUrl;
		this.authToken = authToken;
	}

	public String getEndpoint() {
		return serviceUrl;
	}

	public void setEndpoint(String endpoint) {
		this.serviceUrl = endpoint;
	}
	
	public String getAuthToken() {
		return authToken;
	}

	public void setAuthToken(String authToken) {
		this.authToken = authToken;
	}
	

	public Edm readEdm() throws IOException, ODataException {
		InputStream content = execute(serviceUrl + SEPARATOR + METADATA, APPLICATION_XML, HTTP_METHOD_GET);
		return EntityProvider.readMetadata(content, false);
	}

	
	private HttpStatusCodes checkStatus(HttpURLConnection connection) throws IOException {
		HttpStatusCodes httpStatusCode = HttpStatusCodes.fromStatusCode(connection.getResponseCode());
		if (400 <= httpStatusCode.getStatusCode() && httpStatusCode.getStatusCode() <= 599) {
			throw new RuntimeException("Http Connection failed with status " + httpStatusCode.getStatusCode() + " "
					+ httpStatusCode.toString() + " " + connection.getResponseMessage());
		}
		return httpStatusCode;
	}

	

	private InputStream execute(String absolutUri, String contentType, String httpMethod) throws IOException {
		HttpURLConnection connection = initializeConnection(absolutUri, contentType, httpMethod);

		connection.connect();
		checkStatus(connection);

		InputStream content = connection.getInputStream();
		return content;
	}

	
	private HttpURLConnection initializeConnection(String absolutUri, String contentType, String httpMethod) throws MalformedURLException,
			IOException {
		URL url = new URL(absolutUri);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		connection.setRequestMethod(httpMethod);
		connection.setRequestProperty("Authorization", authToken); /*"Basic UDE5NDA3NzkzODE6MVFhMldzM0Vk"*/
		connection.setRequestProperty(HTTP_HEADER_CONTENT_TYPE, contentType);
		if (contentType.equals(ODataClient.APPLICATION_JSON)) {
			connection.setRequestProperty(HTTP_HEADER_ACCEPT, contentType);
		}
		if (HTTP_METHOD_POST.equals(httpMethod) || HTTP_METHOD_PUT.equals(httpMethod)) {
			connection.setDoOutput(true);
			connection.setRequestProperty(HTTP_HEADER_CONTENT_TYPE, contentType);
		}

		return connection;
	}
}