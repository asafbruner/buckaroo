package com.sap.river.util;

import java.io.IOException;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;

/**
 * Utility class for handling copying of resource in the file system
 *
 */
public class FileUtil {
	/**
	 * Suffix that will be appended to filename when making backups before modifications. 
	 */
	private static final String BAKUP_SUFFIX = ".river.backup";
	
	/** Logger */
	private static Logger log = Logger.getLogger(FileUtil.class.getName());
	
	/** backup the source file
	 * @param fileManager - File Manager which handle OS operations
	 * @param sourceFile - The name of the file to backup
	 */
	public static void backupFile(FileManager fileManager, String sourceFile) {
		String targetFile = sourceFile + BAKUP_SUFFIX;
		FileUtil.copyFile(fileManager, sourceFile, targetFile);
	}
	
	/** revert the source file to the state as saved in the corresponding backup file
	 * @param fileManager - File Manager which handle OS operations
	 * @param sourceFile - The name of the file to revert
	 */
	public static void revertFile(FileManager fileManager, String sourceFile) {
		String backupFile = sourceFile + BAKUP_SUFFIX;
		FileUtil.copyFile(fileManager, backupFile, sourceFile);
		fileManager.delete(backupFile);
	}
	
	/** copy the source file to the destination fiel name
	 * @param fileManager -  File Manager which handle OS operations
	 * @param sourceFile - The name of the file to copy
	 * @param targetFile - The name of new file
	 */
	public static void copyFile(FileManager fileManager, String sourceFile, String targetFile) {
		if (sourceFile == null || !fileManager.exists(sourceFile)) {
			log.warning("resource for copy is null or does not exist: "+sourceFile);
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
            throw new IllegalStateException("Could not copy '"+sourceFile+"' to '"+targetFile+"'.", ioe);
        } finally {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(outputStream);
        }
	}
	

}
