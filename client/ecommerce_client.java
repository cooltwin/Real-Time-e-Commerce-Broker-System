import java.util.*;
import java.io.*;
import java.net.*;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.logging.*;
import javax.crypto.SecretKey;
import javax.crypto.Cipher;
import java.math.BigInteger;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;

class Client extends Thread{
	static String IV;
	AES aesobj;
	private static String cb_longTimeSharedKey;
	private static SecretKey cb_session_key;
	private static SecretKey cw_session_key;
	public static Logger mylogger;
	public static String broker;
	public static String webserver;
	public static RSA rsaObj=null;
	MiscellaneousWork mw;

	public enum MsgType{
		Challenge_Msg,BC_Session_Key,Connection_Established_With_WebServer,Catalogue_Msg,Sign_Document,Product,Transaction_Failed;
	}


	public Client(MiscellaneousWork mw, Logger mylogger){
		Scanner sc = new Scanner(System.in);
		System.out.println("Enter the broker name :");
		broker = sc.nextLine();
		this.mw = mw;
		this.mylogger = mylogger;
		IV = mw.read_config_file("IV");
		aesobj = new AES(IV);
		cw_session_key = aesobj.generate_session_key();
	}

	public void run(){
		Message rcvd_msg=null;
		boolean shutdown = false;
		SyncMsgs sync = new SyncMsgs();
		try {
			Thread.sleep(2000);
			cb_longTimeSharedKey = mw.read_password_file(broker, "longtimeSharedkey_cb");
			send_hello_msg();
			while(true){
				rcvd_msg = sync.get_msg();
				if(rcvd_msg != null){
					shutdown = handle_msg(rcvd_msg);
					if(shutdown == true)
						break; // will kill the thread
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}

	}
	public void send_hello_msg(){
		String msg=null;
		Message encrypted_msg=null;
		byte[] mac ;
		msg = "Hello "+broker;
		mylogger.info("Client : Sending message :"+msg);
		System.out.println("Client : Sent message :"+msg);
		try {
			byte[] cipher = msg.getBytes();
			mac = aesobj.generate_mac("Client"+"Hello_Msg"+msg);
			encrypted_msg = new Message("Client","Hello_Msg",cipher,mac);
			send_msg(encrypted_msg, broker);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public void send_msg(Message msg, String host) throws InterruptedException { //sends a new msg
		int j, port =0;
		String hostname=null;
		Socket socket;
		InetAddress address;
		port = Integer.parseInt(mw.read_config_file("port"));
		try {
			address = (InetAddress)null;
			hostname = mw.read_config_file(host);
			address = InetAddress.getByName(hostname);
			socket = new Socket(address, port);

			OutputStream os = socket.getOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(os);
			oos.writeObject(msg);
			oos.flush();

			mylogger.info("-------------------------------------------------------------");
			mylogger.info("Client Thread : Message sent to host : "+host+" hostname :"+hostname);
			mylogger.info("Client Thread : Sent Message is :");
			mylogger.info("Client : "+msg.sender+" msg_type : "+msg.msg_type+" cipher : "+msg.cipher);
			mylogger.info("-------------------------------------------------------------");
			System.out.println("-------------------------------------------------------------");
			System.out.println("Client Thread : Message sent to host : "+host+" hostname :"+hostname);
			System.out.println("Client Thread : Sent Message is :");
			System.out.println("Client : "+msg.sender+" msg_type : "+msg.msg_type+" cipher : "+msg.cipher);
			System.out.println("-------------------------------------------------------------");
			socket.close();

		}catch(IOException e) {
			System.out.println("Client Thread : "+e+"\n");
			mylogger.info("Client Thread : "+e+"\n");
			mylogger.info("----x--------x----------x------------x-----------x--------x--------");
		}
	}

	public boolean check_mac(Message rcvd_msgbody, String unecrypted_data){
		String mac_str = rcvd_msgbody.sender+rcvd_msgbody.msg_type+unecrypted_data;
		byte[] my_mac = aesobj.generate_mac(mac_str);
		if(Arrays.equals(my_mac,rcvd_msgbody.mac))
			return true;
		else
			return false;
	}

	public boolean handle_msg(Message rcvd_msgbody){
		int item_cost=0;
		String rcvd_msg = null, str[],msg=null;
		String catalogue=null,item=null,prdt=null;
		Message msgbody=null;
		byte[] cipher, mac;
		PublicKey publicKey=null;
		Scanner sc = new Scanner(System.in);

		try {		
			switch(MsgType.valueOf(rcvd_msgbody.msg_type)){
				case Challenge_Msg:
					rcvd_msg =aesobj.decrypt_using_ltsk(rcvd_msgbody.cipher,cb_longTimeSharedKey);
					System.out.println("Client Thread : After decryption, received msg is :"+rcvd_msg);
					mylogger.info("Client Thread : After decryption, received msg is :"+rcvd_msg);
					msg = String.valueOf((Integer.parseInt(rcvd_msg))+1);
					System.out.println("Client Thread : Sending msg : "+msg);
					mylogger.info("Client Thread : Sending msg : "+msg);
					mac = aesobj.generate_mac("Client"+"Challenge_Msg"+msg);
					cipher = aesobj.encrypt_using_ltsk(msg, cb_longTimeSharedKey);
					msgbody = new Message("Client", "Challenge_Msg", cipher, mac); 
					send_msg(msgbody,broker);
					break;

				case BC_Session_Key:
					/*if(!check_mac(rcvd_msgbody, rcvd_msg)){
					  System.out.println("Client Thread : Error!! Messsag Integrity Hampered, exiting");
					  return false;
					  }*/	
					byte[] encoded = new BigInteger(cb_longTimeSharedKey, 16).toByteArray();
					SecretKey cb_ltsk = new SecretKeySpec(encoded, "AES");
					Cipher session_key_cipher = Cipher.getInstance("AES/ECB/NoPadding");
					session_key_cipher.init(Cipher.DECRYPT_MODE, cb_ltsk);
					cb_session_key = new SecretKeySpec(session_key_cipher.doFinal(rcvd_msgbody.cipher), "AES");
					System.out.println("Client Thread : After decryption, received session_key is :"+cb_session_key);
					mylogger.info("Client Thread : After decryption, received msg is :"+cb_session_key);
					System.out.println("\nEnter the name of the webserver you want to connect to :");
					webserver = sc.nextLine();
					msg = "Connect me to "+webserver;
					System.out.println("Client Thread : Sending msg : "+msg);
					mylogger.info("Client Thread : Sending msg : "+msg);
					mac = aesobj.generate_mac("Client"+"Connect_Msg"+msg);
					cipher = aesobj.encrypt_using_sessionKey(msg, cb_session_key);
					msgbody = new Message("Client", "Connect_Msg", cipher, mac); 
					send_msg(msgbody,broker);
					break;

				case Connection_Established_With_WebServer:
					System.out.println("Client Thread : received msg that connection is established :");
					mylogger.info("Client Thread :  received msg that connection is established :");
					/*if(!check_mac(rcvd_msgbody,rcvd_msg)){
					  System.out.println("Client Thread : Error!! Messsag Integrity Hampered, exiting");
					  return false;
					  }*/	
					rsaObj = new RSA(webserver);
					publicKey = rsaObj.readPublicKeyFromFile();
					String msg_string = "Forward msg to webserver";
					Cipher session_key_cipher1 = Cipher.getInstance("RSA/ECB/PKCS1Padding");
					session_key_cipher1.init(Cipher.ENCRYPT_MODE, publicKey);
					byte[] wrappedKey1 = session_key_cipher1.doFinal(cw_session_key.getEncoded());

					mac = aesobj.generate_mac("Client"+"CW_Session_Key"+wrappedKey1);
					System.out.println("Client Thread : Sending session key to webserver: "+cw_session_key);
					mylogger.info("Client Thread : Sending session key to webserver: "+cw_session_key);
					msgbody = new Message("Client","CW_Session_Key", wrappedKey1, mac); 
					send_msg(msgbody, broker);
					break;

				case Catalogue_Msg:
					rcvd_msg = aesobj.decrypt_using_sessionKey(rcvd_msgbody.cipher, cw_session_key);
					RandomAccessFile catalogue_file = new RandomAccessFile("/home/004/t/tx/txg131030/netsec_demo/netsec_prj_client/Catalogue.txt", "rw");
					catalogue_file.setLength(0);
					catalogue_file.seek(0);
					catalogue_file.write(rcvd_msg.getBytes());
					catalogue_file.close();
					System.out.println("Client Thread : Received Catalogue file :\n"+rcvd_msg);
					mylogger.info("Client Thread : Received Catalogue file: \n"+rcvd_msg);
					/*if(!check_mac(rcvd_msgbody,rcvd_msg)){
					  System.out.println("Client Thread : Error!! Messsag Integrity Hampered, exiting");
					  return false;
					  }*/	
					System.out.println("\nEnter the item you want to purchase :");
					item = sc.nextLine();
					System.out.println("Enter the cost of the item:");
					item_cost = Integer.parseInt(sc.nextLine());
					// sending msg to web server via broker to purchase the item 
					msg = item;
					mac = aesobj.generate_mac("Client"+"Request_Product"+msg);
					cipher = aesobj.encrypt_using_sessionKey(msg,cw_session_key); 
					System.out.println("Client Thread : Sending msg :"+msg);
					mylogger.info("Client Thread : Sending msg :"+msg);
					msgbody = new Message("Client", "Request_Product", cipher, mac); 
					send_msg(msgbody, broker);
					// sending msg to broker to pay fro the product
					msg = "Pay web server $"+item_cost;
					mac = aesobj.generate_mac("Client"+"Pay_For_Product"+msg);
					cipher = aesobj.encrypt_using_sessionKey(msg,cb_session_key);
					System.out.println("Client Thread : Sending msg : "+msg);
					mylogger.info("Client Thread : Sending msg : "+msg);
					msgbody = new Message("Client", "Pay_For_Product", cipher,mac); 
					send_msg(msgbody, broker);
					break;

				case Sign_Document: 
					rcvd_msg =aesobj.decrypt_using_sessionKey(rcvd_msgbody.cipher,cb_session_key);
					System.out.println("Client Thread : After decryption, received msg is :\n"+rcvd_msg);
					mylogger.info("Client Thread : After decryption, received msg is :\n"+rcvd_msg);
					/*if(!check_mac(rcvd_msgbody,rcvd_msg)){
					  System.out.println("Client Thread : Error!! Messsag Integrity Hampered, exiting");
					  return false;
					  }*/
					cipher = rsaObj.sign_message(rcvd_msg);
					mac = null;
					System.out.println("Client Thread : Sending document to broker ");
					mylogger.info("Client Thread : Sending document to broker ");
					msgbody = new Message("Client", "Signed_Document", cipher, mac); 
					send_msg(msgbody, broker);
					break;

				case Transaction_Failed: 
					rcvd_msg =aesobj.decrypt_using_sessionKey(rcvd_msgbody.cipher,cw_session_key);
					System.out.println("Client Thread : After decryption, received msg is :\n"+rcvd_msg);
					mylogger.info("Client Thread : After decryption, received msg is :\n"+rcvd_msg);
					System.out.println("Client Thread : Transaction Failed, Now Closing the Connection .......");
					mylogger.info("Client Thread : Transaction Failed, Now Closing the Connection .......");
					return true;

				case Product:
					rcvd_msg = aesobj.decrypt_using_sessionKey(rcvd_msgbody.cipher, cw_session_key);
					System.out.println("Client Thread : Received Product");
					mylogger.info("Client Thread : Received Product");
					RandomAccessFile prdt_file = new RandomAccessFile("/home/004/t/tx/txg131030/netsec_demo/netsec_prj_client/Product.txt", "rw");
					prdt_file.setLength(0);
					prdt_file.seek(0);
					prdt_file.write(rcvd_msg.getBytes());
					prdt_file.close();

					/*if(!check_mac(rcvd_msgbody,rcvd_msg)){
					  System.out.println("Client Thread : Error!! Messsag Integrity Hampered, exiting");
					  return false;
					  }*/	
					System.out.println("Client Thread : Transaction Successful, Now Closing the Connection .......");
					mylogger.info("Client Thread : Transaction Successful, Now Closing the Connection .......");
					return true;

				default :
					System.out.println("Client Thread : Error!! Wrong Msg type");
					mylogger.info("Client Thread : Error!! Wrong Msg type");
					return true;
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return false;
	}

}

public class ecommerce_client{
	public static Queue<Message> msgqueue;
	public static Logger mylogger;

	public ecommerce_client(){
		msgqueue = new LinkedList<Message>();
	}

	public static void main(String args[]){
		Scanner sc = new Scanner(System.in);
		MiscellaneousWork mw = new MiscellaneousWork();
		mylogger = mw.set_logging(args[0]);
		ecommerce_client obj = new ecommerce_client();

		Thread client= new Client(mw, mylogger);
		client.start();
		Thread rcvmsg= new Receiver(mw, mylogger);
		rcvmsg.start();
		try {
			rcvmsg.join();
			client.join();
		} catch (InterruptedException e) {
			mylogger.info("Problem in joining the threads\n");
		}

	}
}
