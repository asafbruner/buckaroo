package com.sap.river.util;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XMLUtils {
	
	private static Logger LOGGER = Logger.getLogger(XMLUtils.class.getName());
	private static final String PROFILES_NODE_NAME = "profiles";
	private static final String PROFILE_NODE_NAME = "profile";
	private static final String PROFILE_ID_NAME = "id";
	private static final String ACTIVATION_NAME = "activation";
	private static final String ACTIVEBYDEFAULT_NAME = "activeByDefault";
	private static final String ACTIVEBYDEFAULT_VAL = "false";
	private static final String BUILD_NAME = "build";
	private static final String PLUGINS_NAME = "plugins";
	private static final String PLUGIN_NAME = "plugin";
	private static final String GROUPID_NAME = "groupId";
	private static final String ARTIFACTID_NAME = "artifactId";
	private static final String VERSION_NAME = "version";
	private static final String EXECUTIONS_NAME = "executions";
	private static final String EXECUTION_NAME = "execution";
	
	private static final String ID_NAME = "id";
	private static final String GOALS_NAME = "goals";
	private static final String GOAL_NAME = "goal";
	
	/**
	 * return the root element of the POM
	 */
	public static Document getPOMDocument(String fullFilePathName){
		//get java File element
		File pomFile = new File(fullFilePathName);
		
		//Build DocumentBuilder
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = null;	
		try {
			dBuilder = dbFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			LOGGER.info("ParserConfigurationException in building new document:  " + e.getMessage());
			return null;
		}
		
		//Build and return XML document
		try {
			return dBuilder.parse(pomFile);
		} catch (SAXException e) {
			LOGGER.info("SAXException in parsing:  " + e.getMessage());
			return null;
		}
		catch (IOException e) {
			LOGGER.info("IOException in parsing:  " + e.getMessage());
			return null;
		}
	}
	
	/**
	 * If there is no <profiles> section, create one with relevant profile with given profileId
	 * If <profiles> already contains a <profile> with given profileId, delete all of its plugins
	 * @param document
	 * @param pomRoot
	 * @param profileIdVal
	 * @return the <plugins> node (under profiles/profile/build), or null for any error
	 */
	public static Element initiateProfile(Document document, Element pomRoot, String profileIdVal){
		//If <profiles> section does not exist, then create and get out
		NodeList childrenRoot = pomRoot.getChildNodes();
		int numProfileNodes = childrenRoot.getLength();
		Node profiles = null;
		for (int index=0; index<numProfileNodes; index++){
			Node currNode = childrenRoot.item(index);
			if (currNode.getNodeName().equals(PROFILES_NODE_NAME)){
				profiles = currNode;
				break;
			}
		}
		if (profiles == null){
			//no <profiles> section was found.
			//create <profiles>
			Element profilesNew = addProfilesSection(document, pomRoot);
			
			//Create <profile>, add to <profiles>, and get out
			return addNewProfileSection(document, profilesNew, profileIdVal);
		}
		
		//the <profiles> section already exists, go over all profiles to determine if the specific profile (with given profileIdVal) exists
		NodeList profileNodes = profiles.getChildNodes();
		numProfileNodes = profileNodes.getLength();
		boolean isProblem = false;
		Element profile = null;
		Element plugins = null;
		for (int profileIndex=0; profileIndex<numProfileNodes; profileIndex++){
			//Convert to Element if possible, otherwise it is not relevant
			Node profileNode = profileNodes.item(profileIndex);
			if (!(profileNode instanceof Element)){
				continue;
			}
			Element currProfile = (Element)profileNode;
			
			NodeList profileChildren = currProfile.getChildNodes();
			int numProfileChildren = profileChildren.getLength();
			Element profileId = null;
			for (int indexProfileChildren=0; indexProfileChildren<numProfileChildren; indexProfileChildren++){
				if (!(profileChildren.item(indexProfileChildren) instanceof Element)){
					continue;
				}
				
				if (profileChildren.item(indexProfileChildren).getNodeName().equals(PROFILE_ID_NAME)){
					profileId = (Element) profileChildren.item(indexProfileChildren);
					break;
				}
			}
			if (profileId == null){
				//we have a profile without an id, ignore it and move to the next one
				continue;
			}
			
			//Check if its id is the one we are interested in
			String currProfileIdVal = profileId.getTextContent();
			if (currProfileIdVal.equals(profileIdVal)){
				profile = currProfile;
				//A profile was found with id = profileId
				//get/create the <build> element
				Element build = getChildElement(document, currProfile, BUILD_NAME);
				if (build == null){
					isProblem = true;
					break;
				}
				
				//get/create the <plugins> element
				plugins = getChildElement(document, build, PLUGINS_NAME);
				if (plugins == null){
					isProblem = true;
					break;
				}
				
				//remove all children from the <plugins> element (whether or not they are <plugin>'s)
				NodeList pluginNodes = plugins.getChildNodes();
				int numPluginsChildren = pluginNodes.getLength();
				for (int pluginIndex=numPluginsChildren-1; pluginIndex>=0; pluginIndex--){
					plugins.removeChild(pluginNodes.item(pluginIndex));
				}
				
				//whatever happened above, we are done now, don't check any other profiles
				break;
			}
			//else continue to the next profile
		}
		
		if (isProblem)
			return null;
		
		if (profile == null){
			//the required profile was never found.  Create it now, add to profiles, and get out
			return addNewProfileSection(document, (Element)profiles, profileIdVal);
		} 
		else{
			//we did find the profile, so return the plugins element
			return plugins;
		}
	}
	
	/**
	 * save the document as is, into xml file given by parameter
	 * @param document
	 * @param fullFilePathName
	 */
	public static void saveXMLDocIntoFile(Document document, String fullFilePathName){
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = null;
		try{
			transformer = transformerFactory.newTransformer();
		}
		catch(TransformerConfigurationException e){
			LOGGER.info("TransformerConfigurationException in generating new transformer:  " + e.getMessage());
			return;
		}
		
		DOMSource source = new DOMSource(document);
		StreamResult result = new StreamResult(new File(fullFilePathName));
		
		try{
			transformer.transform(source, result);
		}catch(TransformerException e){
			LOGGER.info("TransformerException in doing transform:  " + e.getMessage());
			return;
		}
	}
	
	//this method gets an element representing a plugin
	//it needs to be built into a profile
	public static void addPluginToProfile(Element plugins, Element plugin){
		plugins.appendChild(plugin);
	}
	
	//under the profiles section, create a new profile with correct profileId, and return the plugins element
	private static Element addNewProfileSection(Document document, Element profiles, String profileIdVal){
		//start with profile node
		Element profile = document.createElement(PROFILE_NODE_NAME);
		profiles.appendChild(profile);
		
		//create the id tag
		Node id = document.createElement(PROFILE_ID_NAME);
		//id.setNodeValue(profileIdVal);
		id.setTextContent(profileIdVal);
		//add it to the profile
		profile.appendChild(id);
		
		//create activation tag
		Node activeByDefault = document.createElement(ACTIVEBYDEFAULT_NAME);
		//activeByDefault.setNodeValue(ACTIVEBYDEFAULT_VAL);
		activeByDefault.setTextContent(ACTIVEBYDEFAULT_VAL);
		Node activation = document.createElement(ACTIVATION_NAME);
		activation.appendChild(activeByDefault);
		//add it to profile
		profile.appendChild(activation);
		
		//create build tag
		Node build = document.createElement(BUILD_NAME);
		//add it to profile
		profile.appendChild(build);
		
		//create plugins tag, add to build
		Node plugins = document.createElement(PLUGINS_NAME);
		build.appendChild(plugins);		
		
		return (Element)plugins;
	}
	
	public static Element generateMvnFailsafePlugin(Document document){
		//generate plugin element
		Element plugin = document.createElement(PLUGIN_NAME);
		
		//generate groupId tag, add to plugin
		Element groupId = document.createElement(GROUPID_NAME);
		groupId.setTextContent("org.apache.maven.plugins");
		plugin.appendChild(groupId);
		
		//artifactId
		Element artifactId = document.createElement(ARTIFACTID_NAME);
		artifactId.setTextContent("maven-failsafe-plugin");
		plugin.appendChild(artifactId);
		
		//version
		Element version = document.createElement(VERSION_NAME);
		version.setTextContent("2.13");
		plugin.appendChild(version);
		
		//Executions
		Element executions = document.createElement(EXECUTIONS_NAME);
		plugin.appendChild(executions);
		
		//There are two Execute elements
		
		//Execute1
		Element execution1 = document.createElement(EXECUTION_NAME);
		executions.appendChild(execution1);
		//Its id
		Element id1 = document.createElement(ID_NAME);
		id1.setTextContent("integration-test");
		execution1.appendChild(id1);
		//Its goals
		Element goals1 = document.createElement(GOALS_NAME);
		execution1.appendChild(goals1);
		//The goals' goal
		Element goal1 = document.createElement(GOAL_NAME);
		goal1.setTextContent("integration-test");
		goals1.appendChild(goal1);
		
		//Execute2
		Element execution2 = document.createElement(EXECUTION_NAME);
		executions.appendChild(execution2);
		//Its id
		Element id2 = document.createElement(ID_NAME);
		id2.setTextContent("verify");
		execution2.appendChild(id2);
		//Its goals
		Element goals2 = document.createElement(GOALS_NAME);
		execution2.appendChild(goals2);
		//The goals' goal
		Element goal2 = document.createElement(GOAL_NAME);
		goal2.setTextContent("verify");
		goals2.appendChild(goal2);
				
		return plugin;
	}
	
	/**
	 * add profiles section under pomRoot
	 * @param pomRoot
	 * @return the profiles node
	 */
	private static Element addProfilesSection(Document document, Element root){
		Node profiles = document.createElement(PROFILES_NODE_NAME);
		root.appendChild(profiles);
		return (Element)profiles;
	}
	
	//expect that the original element has a single child element named subElementName
	//return the single child element of the original, or create it if it doesn't exist
	//either way, return it (or null if there are more than one)
	private static Element getChildElement(Document document, Element origElement, String subElementName){
		NodeList buildNodes = origElement.getChildNodes();
		int numChildrenOfOrig = buildNodes.getLength();
		for (int indexChildOfOrig=0; indexChildOfOrig<numChildrenOfOrig; indexChildOfOrig++){
			Node child = buildNodes.item(indexChildOfOrig);
			if (child.getNodeName().equals(subElementName)){
				return (Element)child;
			}
		}
		
		//if we are here, the child node was never found, so create it now
		Element newElem = document.createElement(subElementName);
		origElement.appendChild(newElem);
		return newElem;

	}

}
