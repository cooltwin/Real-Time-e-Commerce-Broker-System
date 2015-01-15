import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;

import java.security.MessageDigest;
import java.util.Arrays;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.Mac;
import javax.crypto.KeyGenerator;


import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.math.BigInteger;



class AES {
	static String IV;
	MiscellaneousWork mw;

	public  AES(String IV){
		this.IV = IV;
		this.mw = new MiscellaneousWork();
	}

	public SecretKey generate_session_key(){
		SecretKey sessionKey=null;
		try {
			KeyGenerator keygen = KeyGenerator.getInstance("AES");
			keygen.init(128,new SecureRandom());
			sessionKey = keygen.generateKey();
			}catch (NoSuchAlgorithmException e){
				e.printStackTrace();
			}catch(Exception e){
				e.printStackTrace();
			}
		
		return sessionKey;
	}
	
	public static byte[] encrypt_using_ltsk(String plainText, String encryptionKey) throws Exception {
		byte[] encoded = new BigInteger(encryptionKey, 16).toByteArray();
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "SunJCE");
		SecretKeySpec key = new SecretKeySpec(encoded, "AES");
		cipher.init(Cipher.ENCRYPT_MODE, key,new IvParameterSpec(IV.getBytes("UTF-8")));
		return cipher.doFinal(plainText.getBytes("UTF-8"));
	}

	public static String decrypt_using_ltsk(byte[] cipherText, String encryptionKey) throws Exception{
		byte[] encoded = new BigInteger(encryptionKey, 16).toByteArray();
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "SunJCE");
		SecretKeySpec key = new SecretKeySpec(encoded, "AES");
		cipher.init(Cipher.DECRYPT_MODE, key,new IvParameterSpec(IV.getBytes("UTF-8")));
		return new String(cipher.doFinal(cipherText),"UTF-8");
	}
	
	public static byte[] encrypt_using_sessionKey(String plainText, SecretKey encryptionKey) throws Exception {
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "SunJCE");
		byte[] encryptionKey_bytes = encryptionKey.getEncoded();
		SecretKeySpec key = new SecretKeySpec(encryptionKey_bytes, "AES");
		cipher.init(Cipher.ENCRYPT_MODE, key,new IvParameterSpec(IV.getBytes("UTF-8")));
		return cipher.doFinal(plainText.getBytes("UTF-8"));
	}

	public static String decrypt_using_sessionKey(byte[] cipherText, SecretKey encryptionKey) throws Exception{
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "SunJCE");
		byte[] encryptionKey_bytes = encryptionKey.getEncoded();
		SecretKeySpec key = new SecretKeySpec(encryptionKey_bytes, "AES");
		cipher.init(Cipher.DECRYPT_MODE, key,new IvParameterSpec(IV.getBytes("UTF-8")));
		return new String(cipher.doFinal(cipherText),"UTF-8");
	}


	public byte[] generate_mac(String msg){
		byte[] digest = null;
		try {
			// get a key generator for the HMAC-MD5 keyed-hashing algorithm
			KeyGenerator keyGen = KeyGenerator.getInstance("HmacMD5");
			// generate a key from the generator
			SecretKey key = keyGen.generateKey();

			// create a MAC and initialize with the above key
			Mac mac = Mac.getInstance(key.getAlgorithm());
			mac.init(key);
			String message = "This is a confidential message";
			// get the string as UTF-8 bytes
			byte[] b = message.getBytes("UTF-8");
			// create a digest from the byte array
			digest = mac.doFinal(b);
			return digest;
		}
		catch (NoSuchAlgorithmException e) {
			System.out.println("No Such Algorithm:" + e.getMessage());
			return digest;
		}
		catch (UnsupportedEncodingException e) {
			System.out.println("Unsupported Encoding:" + e.getMessage());
			return digest;
		}
		catch (InvalidKeyException e) {
			System.out.println("Invalid Key:" + e.getMessage());
			return digest;
		}

	}

}

