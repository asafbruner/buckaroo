package com.sap.buckaroo.util;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class EncryptionUtils {

	private static Logger LOGGER = Logger.getLogger(EncryptionUtils.class
			.getName());
	SecretKey secretKey = null;
	Cipher cipher = null;
	final String CHARSET = "ISO-8859-1";

	public EncryptionUtils() {
		try {
			KeyGenerator keyGenerator = KeyGenerator.getInstance("DESede");
			keyGenerator.init(168);
			secretKey = keyGenerator.generateKey();
			cipher = Cipher.getInstance("DESede");
		} catch (Exception e) {
			LOGGER.info("EncryptionUtils exception: " + e.toString());
		}
	}

	/*
	public byte[] encrypt(String input) {
		try {
			byte[] inputBytes = input.getBytes("UTF8");

			// Initialize the cipher and encrypt this byte array

			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			byte[] cipherBytes = cipher.doFinal(inputBytes);
			String decodedCipherBytes = new String(cipherBytes, "UTF-8");
			System.out
					.println("The string representation of the encrypted bytes:\n [[["
							+ decodedCipherBytes + "]]]");
			return cipherBytes;
		} catch (Exception e) {
			LOGGER.info("encrypt exception: " + e.toString());
			return null;
		}
	}

	public String decrypt(byte[] cipherBytes) {
		try {
			cipher.init(Cipher.DECRYPT_MODE, secretKey);
			byte[] decryptedBytes = cipher.doFinal(cipherBytes);
			String decryptedText = new String(decryptedBytes, "UTF8");
			return decryptedText;
		} catch (Exception e) {
			LOGGER.info("encrypt exception: " + e.toString());
			return null;
		}
	}*/

	// ///////////////////////////////////////////////

	public String encrypt2(String inputStr) {
		try {
			// convert string to bytes
			byte[] inputBytes = convertStringToByteArray(inputStr);
			if (inputBytes == null)
				return null;

			// Initialize the cipher and encrypt this byte array

			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			byte[] encryptedBytes = cipher.doFinal(inputBytes);
			return convertByteArrayToString(encryptedBytes);
		} catch (Exception e) {
			LOGGER.info("Error encrypting value " + inputStr + ": "
					+ e.toString());
			return null;
		}
	}

	public String decrypt2(String encryptedStr) {
		// convert string to bytes
		byte[] encryptedBytes = convertStringToByteArray(encryptedStr);
		if (encryptedBytes == null)
			return null;

		try {
			cipher.init(Cipher.DECRYPT_MODE, secretKey);
			byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
			return convertByteArrayToString(decryptedBytes);
		} catch (Exception e) {
			LOGGER.info("Error decrypting value " + encryptedStr + ": "
					+ e.toString());
			return null;
		}
	}
	
	//////////////////

	private byte[] convertStringToByteArray(String input) {
		try {
			return input.getBytes(CHARSET);
			//return input.getBytes();
		} catch (UnsupportedEncodingException e) {
			LOGGER.info("convertStringToByteArray: Error converting " + input
					+ " to byte array:  " + e.toString());
			return null;
		}
	}

	private String convertByteArrayToString(byte[] input) {
		try {
			return new String(input, CHARSET);
			//return new String(input);
		} catch (UnsupportedEncodingException e) {
			LOGGER.info("convertByteArrayToString: Error converting " + input
					+ " to string array:  " + e.toString());
			return null;
		}
	}

}
