package com.sap.buckaroo.util;

public class Constants {
	//general constants
	public static final String CONFIG_PROPERTIES_FILE = "configuration.properties";
	public static final String RESOURCE_DIR = "META-INF/spring/";
	
	//Properties
	//OData and connection
	public static final String ODATA_SERVICE_ENDPOINT = "odata.service.endpoint";
	public static final String ODATA_SERVICE_VERSION = "odata.service.version";
	public static final String CONNECTION_AUTHENTICATION_TYPE = "connection.authentication.type";
	public static final String CONNECTION_AUTHENTICATION_USER = "connection.authentication.user";
	public static final String CONNECTION_AUTHENTICATION_PASSWORD = "connection.authentication.password";
	public static final String CONNECTION_HTTP_HEADERS_ACCEPT = "connection.http.headers.accept";
	public static final String CONNECTION_HTTP_HEADERS_CONTENTTYPE = "connection.http.headers.contentType";
	//HCP deploy
	public static final String HCP_REMOTE_ACCOUNT = "hcp.remotedeploy.account";
	public static final String HCP_REMOTE_HOST = "hcp.remotedeploy.host";
	public static final String HCP_REMOTE_USER = "hcp.remotedeploy.user";
	public static final String HCP_REMOTE_PSWD= "hcp.remotedeploy.password";
	
	//Commands
	public static final String DEPLOY = "deploy";
	public static final String INSTALL_SDK = "install-sdk";

}
