package com.sap.buckaroo.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;

/**
 * General utility class, including for handling copying of resource in the file system
 *
 */
public class FileUtil {
	/**
	 * Suffix that will be appended to filename when making backups before modifications.
	 */
	private static final String BAKUP_SUFFIX = ".buckaroo.backup";

	/** Logger */
	private static Logger log = Logger.getLogger(FileUtil.class.getName());

	/**
	 * backup the source file
	 * @param fileManager - File Manager which handle OS operations
	 * @param sourceFile - The name of the file to backup
	 */
	public static void backupFile(FileManager fileManager, String sourceFile) {
		String targetFile = sourceFile + BAKUP_SUFFIX;
		FileUtil.copyFile(fileManager, sourceFile, targetFile);
	}

	/**
	 * revert the source file to the state as saved in the corresponding backup file
	 * @param fileManager - File Manager which handle OS operations
	 * @param sourceFile - The name of the file to revert
	 */
	public static void revertFile(FileManager fileManager, String sourceFile) {
		String backupFile = sourceFile + BAKUP_SUFFIX;
		FileUtil.copyFile(fileManager, backupFile, sourceFile);
		fileManager.delete(backupFile);
	}

	/**
	 * copy the source file to the destination file name
	 * @param fileManager - File Manager which handle OS operations
	 * @param sourceFile - The name of the file to copy
	 * @param targetFile - The name of new file
	 */
	public static void copyFile(FileManager fileManager, String sourceFile, String targetFile) {
		if (sourceFile == null || !fileManager.exists(sourceFile)) {
			log.warning("resource for copy is null or does not exist: " + sourceFile);
			return;
		}

		java.io.InputStream inputStream = null;
		java.io.OutputStream outputStream = null;
		MutableFile mutableFile = fileManager.exists(targetFile) ? fileManager.updateFile(targetFile) : fileManager.createFile(targetFile);

		try {
			inputStream = fileManager.getInputStream(sourceFile);
			outputStream = mutableFile.getOutputStream();
			IOUtils.copy(inputStream, outputStream);
		} catch (IOException ioe) {
			throw new IllegalStateException("Could not copy '" + sourceFile + "' to '" + targetFile + "'.", ioe);
		} finally {
			IOUtils.closeQuietly(inputStream);
			IOUtils.closeQuietly(outputStream);
		}
	}

	//Get a value from the properties file
	//If the value already exists, then return it.
	//Otherwise, try to get it,  If successful, return it.  Otherwise log an error and return null
	public static String getPropertyByKey(String currVal, String key, Properties prop, Logger LOGGER){
		if (currVal != null)
			return currVal;
		currVal = prop.getProperty(key);
		if (currVal == null){
			LOGGER.info("Failed to obtain property value for key="+key);//TODOKM
			return null;
		}
		return currVal;
	}
	
	/**
	 * get the file path/name.
	 * If it doesn't exist, create the file
	 * @param fileManager
	 * @param pathResolver
	 * @param resourcePath
	 * @param forTests
	 * @param LOGGER
	 * @return
	 */
	public static String getPropertiesPath(FileManager fileManager, PathResolver pathResolver, String resourcePath, boolean forTests, Logger LOGGER) {
		final String propertiesPath = pathResolver
				.getFocusedIdentifier(forTests ? Path.SRC_TEST_RESOURCES : Path.SRC_MAIN_RESOURCES, resourcePath);
		final boolean propertiesPathExists = fileManager
				.exists(propertiesPath);

		if (!propertiesPathExists) {
			OutputStream outputStream = fileManager.createFile(
					propertiesPath).getOutputStream();
			if (outputStream == null) {
				LOGGER.info("Could not create properties file " + resourcePath);
			}
		}
		
		return propertiesPath;
	}
	
	/**
	 * save all values in input map into property file
	 * if the values already exist, REPLACE them.  If there are already values in the property file that
	 * do not appear in the map, keep them (but order of file may be changed)
	 * @param propKeyValues - set of key/values to be entered into the property file
	 * @param configPropertiesFilePathName - path/name of the configuration file
	 * @param LOGGER
	 * @return
	 */
	public static boolean createUpdateConfigPropertiesFile(final Map<String, String> propKeyValues, final String configPropertiesFilePathName, Logger LOGGER) {
		Properties props = new Properties();
		InputStream inputStr = null;
		OutputStream outputStr = null;
		boolean isSuccess = true;
		try{
			//get the existing file, in properties format
			inputStr = new FileInputStream(configPropertiesFilePathName);
			props.load(inputStr);
			
			Iterator<Entry<String, String>> entries = propKeyValues.entrySet().iterator();
			while (entries.hasNext()){
				Entry<String, String> oneEntry = entries.next();
				props.setProperty(oneEntry.getKey(), oneEntry.getValue());
			}
			
			//now save back into file
			outputStr = new FileOutputStream(configPropertiesFilePathName);	
			props.store(outputStr, "");
		}
		catch(FileNotFoundException e){
			LOGGER.info("Exception received in trying to open input/output stream on file: " + configPropertiesFilePathName);//TODO KM
			isSuccess = false;
		}
		catch(IOException e){
			LOGGER.info("Exception received in trying to read or store properties from the property file: " + configPropertiesFilePathName);//TODO KM
			isSuccess = false;
		}
		finally{
			if ((!closeInput(inputStr, LOGGER)) || (!closeOutput(outputStr, LOGGER)))
				isSuccess = false;
		}
		
		return isSuccess;
	}
	
	public static boolean closeInput(InputStream inputStr, Logger LOGGER){
		if (inputStr != null){
			try {
				inputStr.close();
			} catch (IOException e) {
				LOGGER.info("Exception received in trying to close input stream:  " + e.toString());//TODO KM
				return false;
			}
		}
		return true;
	}
	public static boolean closeOutput(OutputStream outputStr, Logger LOGGER){
		if (outputStr != null){
			try {
				outputStr.close();
			} catch (IOException e) {
				LOGGER.info("Exception received in trying to close output stream:  " + e.toString());//TODO KM
				return false;
			}
		}
		return true;
	}
}
