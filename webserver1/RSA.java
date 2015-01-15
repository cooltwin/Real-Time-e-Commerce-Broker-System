import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.GeneralSecurityException;
import java.security.SignatureException;
import java.security.Signature;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import javax.crypto.Cipher;
import java.util.Arrays;

public class RSA {
	String client;

	public RSA(String client){
		this.client = client;
	}

	private byte[] encryptData(String data) throws IOException {

		System.out.println("Data Before Encryption :" + data);
		byte[] dataToEncrypt = data.getBytes();
		byte[] encryptedData = null;
		try {
			PublicKey pubKey = readPublicKeyFromFile();
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.ENCRYPT_MODE, pubKey);
			encryptedData = cipher.doFinal(dataToEncrypt);
			System.out.println("Encryted Data: " + encryptedData);

		} catch (Exception e) {
			e.printStackTrace();
		}	

		return encryptedData;
	}

	private String decryptData(byte[] data) throws IOException {
		byte[] decryptedData = null;

		try {
			PrivateKey privateKey = readPrivateKeyFromFile();
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.DECRYPT_MODE, privateKey);
			decryptedData = cipher.doFinal(data);
			return  new String(decryptedData);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}
	public byte[] sign_message(String message)
	{
		MiscellaneousWork mw = new MiscellaneousWork();
		try
		{
			// Initialize a container for our signedMessage
			byte[] signedMessage = new byte[0];

			// Get private key 
			PrivateKey privateKey = readPrivateKeyFromFile();

			// Calculate the signature with an SHA1 hash function signed by the RSA private key
			Signature sig = Signature.getInstance("SHA1withRSA");
			sig.initSign(privateKey);
			sig.update(message.getBytes());
			byte[] signature = sig.sign();

			// Add the length of the signature and the signature itself in front of the message
			signedMessage = mw.concat(signedMessage,mw.intToByteArray(signature.length));
			signedMessage = mw.concat(signedMessage,signature);

			return mw.concat(signedMessage,message.getBytes());
		}
		catch (GeneralSecurityException exception)
		{
			exception.printStackTrace();
		}catch(IOException e){
			e.printStackTrace();
		}
		return null;
	}
	private byte[] unsign_message(byte[] signedMessage)
	{
		MiscellaneousWork mw = new MiscellaneousWork();
		try
		{
			// Read the signature from the signedMessage (and its length)
			int length = mw.byteArrayToInt(Arrays.copyOf(signedMessage,4));
			byte[] sentSignature = Arrays.copyOfRange(signedMessage,4,4+length);

			// Get public key 
			PublicKey publicKey = readPublicKeyFromFile();

			// Determine the signed hash sum of the message
			byte[] message = Arrays.copyOfRange(signedMessage, 4+length, signedMessage.length);
			Signature sig = Signature.getInstance("SHA1withRSA");
			sig.initVerify(publicKey);
			sig.update(message);

			// Verify the signature
			if (!sig.verify(sentSignature))
				throw new SignatureException("Signature invalid");

			return message;
		}
		catch (GeneralSecurityException exception)
		{
			exception.printStackTrace();
		}catch(IOException e){
			e.printStackTrace();
		}
		return null;
	}

	public PublicKey readPublicKeyFromFile() throws IOException{
		MiscellaneousWork mw = new MiscellaneousWork();
		try {
			BigInteger modulus =  new BigInteger(mw.read_password_file(client, "PubKey Modulus"));
			BigInteger exponent = new BigInteger(mw.read_password_file(client, "PubKey Exponent")); 

			//Get Public Key
			RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(modulus, exponent);
			KeyFactory fact = KeyFactory.getInstance("RSA");
			PublicKey publicKey = fact.generatePublic(rsaPublicKeySpec);

			return publicKey;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public PrivateKey readPrivateKeyFromFile() throws IOException{
		MiscellaneousWork mw = new MiscellaneousWork();
		try {

			BigInteger modulus = new BigInteger(mw.read_password_file("My_Machine_Details", "MyPrivKey Modulus"));
			BigInteger exponent = new BigInteger(mw.read_password_file("My_Machine_Details", "MyPrivKey Exponent")); 

			//Get Private Key
			RSAPrivateKeySpec rsaPrivateKeySpec = new RSAPrivateKeySpec(modulus, exponent);
			KeyFactory fact = KeyFactory.getInstance("RSA");
			PrivateKey privateKey = fact.generatePrivate(rsaPrivateKeySpec);

			return privateKey;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}	
