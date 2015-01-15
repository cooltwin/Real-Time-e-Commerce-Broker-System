import java.text.SimpleDateFormat;
import java.util.*;
import java.io.*;
import java.net.*;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.logging.*;
import java.util.Random;
import javax.crypto.SecretKey;
import javax.crypto.Cipher;
import java.sql.Timestamp;
import java.util.Date;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigInteger;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.Security;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;

class Broker extends Thread{

	static String IV;
	AES aesobj;
	private static String cb_longTimeSharedKey;
	private static String bw_LongTimeSharedKey; 
	private static SecretKey cb_session_key;
	private static SecretKey bw_session_key;
	public static Logger mylogger;
	public static String client;
	public static String webserver;
	public static String purchase_info;

	public enum MsgType{
		Hello_Msg,Connect_Msg,WB_Session_Key,CW_Session_Key,Catalogforward,Request_Product,Pay_For_Product,Signed_Document,Sendproduct,Challenge_Msg,Transcation_Failed;
	}



	public Broker(MiscellaneousWork mw, Logger mylogger){
		this.mylogger = mylogger;
		IV = mw.read_config_file("IV");
		aesobj = new AES(IV);

		cb_longTimeSharedKey = mw.read_password_file("client", "longtimeSharedkey_cb");
		bw_session_key = aesobj.generate_session_key();
		cb_session_key = aesobj.generate_session_key();

	}

	public void run(){
		Message rcvd_msg=null;
		boolean shutdown = false;
		SyncMsgs sync = new SyncMsgs();
		try {
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




	public void send_hello_msg(String webserver){
		String msg=null,rcvd_msg=null,rcvd_encrypted_msg=null;
		Message encrypted_msg=null;

		msg = "Hello "+ webserver;
		mylogger.info("Broker : Sending message :"+msg);
		System.out.println("Broker : Sending message :"+msg);

		try {
			byte[] cipher = msg.getBytes();
			byte[] mac = aesobj.generate_mac("Broker"+"Hellomsg"+ msg);
			encrypted_msg = new Message("Broker","Hellomsg",cipher,mac);
			send_msg(encrypted_msg, webserver);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	class RandomInteger {
		int randomInt;
		public int random() {
			//note a single Random object is reused here
			Random randomGenerator = new Random();
			for (int idx = 1; idx <= 10; ++idx){
				randomInt = randomGenerator.nextInt(100);
			}
			return randomInt;

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

	public void send_msg(Message msg, String host) throws InterruptedException { //sends a new msg
		int port =0;
		String hostname=null;
		Socket socket;
		InetAddress address;
		MiscellaneousWork mw = new MiscellaneousWork();
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
			mylogger.info("Broker Thread : Message sent to host : "+host+" hostname :"+hostname);
			mylogger.info("Broker Thread : Sent Message is :");
			mylogger.info("sender : "+msg.sender+" msg_type : "+msg.msg_type+" cipher : "+msg.cipher);
			mylogger.info("-------------------------------------------------------------");
			System.out.println("-------------------------------------------------------------");
			System.out.println("Broker Thread : Message sent to host : "+host+" hostname :"+hostname);
			System.out.println("Broker Thread : Sent Message is :");
			System.out.println("sender : "+msg.sender+" msg_type : "+msg.msg_type+" cipher : "+msg.cipher);
			System.out.println("-------------------------------------------------------------");
			socket.close();

		}catch(IOException e) {
			System.out.println("Broker Thread : "+e+"\n");
			mylogger.info("Broker Thread : "+e+"\n");
			mylogger.info("----x--------x----------x------------x-----------x--------x--------");
		}
	}



	public boolean handle_msg(Message rcvd_msgbody){
		int item_cost=0, credit_amount=0,challenge=0;
		String rcvd_msg = null,received_message =null, str[],msg=null,msg1=null,client="client";
		String catalogue=null,item=null,prdt=null;
		String str1 = null;
		Message msgbody,message=null;
		String random_no=null;
		byte[] cipher,sent_message;
		RandomInteger ran_obj = new RandomInteger();
		RSA rsaObj=null;
		byte[] mac;
		PublicKey publicKey=null;
		Scanner sc = new Scanner(System.in);
		MiscellaneousWork mw = new MiscellaneousWork();
		try {		

			//switch case failing for java 1.6
			switch(MsgType.valueOf(rcvd_msgbody.msg_type)){

				case Hello_Msg:
					str1 = new String(rcvd_msgbody.cipher,"UTF-8");
					System.out.println("Broker Thread : received msg is :"+str1);
					mylogger.info("Broker Thread : received msg is :"+str1);
					challenge = ran_obj.random();
					random_no = Integer.toString(challenge);
					System.out.println("Broker Thread : Sending random_no : "+random_no);
					mylogger.info("Broker Thread : Sending random_no : "+random_no);
					cipher = aesobj.encrypt_using_ltsk(random_no,cb_longTimeSharedKey);
					mac = aesobj.generate_mac("Broker"+"Challenge_Msg"+random_no);
					message = new Message("Broker","Challenge_Msg", cipher , mac);
					send_msg(message,"client");
					break;

				case Challenge_Msg:
					rcvd_msg = aesobj.decrypt_using_ltsk(rcvd_msgbody.cipher, cb_longTimeSharedKey);
					System.out.println("Broker Thread : After decryption, received msg is :"+rcvd_msg);
					mylogger.info("Broker Thread : After decryption, received msg is :"+rcvd_msg);
					int expected_challenge = challenge+1;
					System.out.println("Broker Thread : Sending session key : "+cb_session_key);
					mylogger.info("Broker Thread : Sending session key : "+cb_session_key);
					byte[] encoded = new BigInteger(cb_longTimeSharedKey, 16).toByteArray();
					SecretKey cb_ltsk = new SecretKeySpec(encoded, "AES"); 
					Cipher session_key_cipher = Cipher.getInstance("AES/ECB/NoPadding");
					session_key_cipher.init(Cipher.ENCRYPT_MODE, cb_ltsk);
					byte[] wrappedKey = session_key_cipher.doFinal(cb_session_key.getEncoded());
					// here wrapped key is the cipher that we are passing in the message object
					mac = aesobj.generate_mac("Broker"+"BC_Session_Key"+wrappedKey);
					message = new Message("Broker","BC_Session_Key", wrappedKey , mac);
					send_msg(message,"client");	
					break;

				case Connect_Msg:
					rcvd_msg = aesobj.decrypt_using_sessionKey(rcvd_msgbody.cipher, cb_session_key);
					System.out.println("Broker Thread : After decryption, received msg is :"+rcvd_msg);
					mylogger.info("Broker Thread : After decryption, received msg is :"+rcvd_msg);
					webserver =rcvd_msg.substring(rcvd_msg.indexOf("to")+3);
					System.out.println("Broker Thread : Connecting to "+webserver+" ........");
					bw_LongTimeSharedKey = mw.read_password_file(webserver, "longtimeSharedKey_bw");
					/*if(!check_mac(rcvd_msgbody,rcvd_msg)){
					  System.out.println("Broker Thread : Error!! Messsage Integrity Hampered, exiting");
					  return true;
					  }*/
					send_hello_msg(webserver);
					break;

				case WB_Session_Key:
					byte[] encoded1 = new BigInteger(bw_LongTimeSharedKey, 16).toByteArray();
					SecretKey bw_ltsk = new SecretKeySpec(encoded1, "AES");
					Cipher session_key_cipher1 = Cipher.getInstance("AES/ECB/NoPadding");
					session_key_cipher1.init(Cipher.DECRYPT_MODE, bw_ltsk);
					bw_session_key = new SecretKeySpec(session_key_cipher1.doFinal(rcvd_msgbody.cipher), "AES");

					System.out.println("Broker Thread : After decryption, received wb session key is :"+bw_session_key);
					mylogger.info("Broker Thread : After decryption, received wb is :"+bw_session_key);
					message = new Message("Broker","Connection_Established_With_WebServer",null,null);
					send_msg(message,client);
					break;

				case CW_Session_Key:
					System.out.println("Broker Thread : received msg type :"+rcvd_msgbody.msg_type+" needs to be forwarded");
					mylogger.info("Broker Thread : received msg type :"+rcvd_msgbody.msg_type+" needs to be forwarded");
					message = new Message("Broker","Forwardmsg",rcvd_msgbody.cipher,null);
					send_msg(message,webserver);
					break;


				case Catalogforward:
					cipher = rcvd_msgbody.cipher;
					System.out.println("Broker Thread : received msg type :"+rcvd_msgbody.msg_type+" needs to be forwarded");
					mylogger.info("Broker Thread : received msg type :"+rcvd_msgbody.msg_type+" needs to be forwarded");
					message = new Message("Broker","Catalogue_Msg",cipher,null);
					send_msg(message,client);
					break;

				case Request_Product:
					cipher=rcvd_msgbody.cipher;
					System.out.println("Broker Thread : received msg type :"+rcvd_msgbody.msg_type+" needs to be forwarded");
					mylogger.info("Broker Thread : received msg type :"+rcvd_msgbody.msg_type+" needs to be forwarded");
					message = new Message("Broker","Itemrequest",cipher,null);
					send_msg(message,webserver);
					break;

				case Pay_For_Product:
					rcvd_msg = aesobj.decrypt_using_sessionKey(rcvd_msgbody.cipher,cb_session_key);
					System.out.println("Broker Thread : After decryption, received msg is :"+rcvd_msg);
					mylogger.info("Broker Thread : After decryption, received msg is :"+rcvd_msg);
					/*if(!check_mac(rcvd_msgbody,rcvd_msg)){
					  System.out.println("Broker Thread : Error!! Messsage Integrity Hampered, exiting");
					  return true;
					  }*/
					String purchase_amt = rcvd_msg.substring(rcvd_msg.indexOf('$')+1);
					msg = "Transfering "+"$"+purchase_amt;
					cipher = aesobj.encrypt_using_sessionKey(msg,bw_session_key);
					message = new Message("Broker","Paymentconfirmation",cipher,null);
					MiscellaneousWork mew = new MiscellaneousWork();
					credit_amount = Integer.parseInt(mew.read_password_file(client,"credit_amount"));
					credit_amount = credit_amount - item_cost;
					send_msg(message,webserver);

					// signed_document for non-repudiation
					Date date = new Date();
					SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a");
					String formattedDate = sdf.format(date);
					purchase_info = "Client ID : 1\n"+"Purchase ID : 1\n"+"Purchased Date : "+formattedDate +
						"\nWebserver : "+webserver+"\nPurchase Amount : "+purchase_amt+"\n";
					sent_message = aesobj.encrypt_using_sessionKey(purchase_info, cb_session_key);
					msg = sent_message.toString();
					mac = aesobj.generate_mac(msg);
					msgbody = new Message("Broker", "Sign_Document", sent_message,mac); 
					send_msg(msgbody,client);
					break;

				case Transcation_Failed:
					cipher=rcvd_msgbody.cipher;
					System.out.println("Broker Thread : received msg type :"+rcvd_msgbody.msg_type+" needs to be forwarded");
					mylogger.info("Broker Thread : received msg type :"+rcvd_msgbody.msg_type+" needs to be forwarded");
					message = new Message("Broker","Transaction_Failed",cipher,null);
					send_msg(message,client);
					System.out.println("Broker Thread : Transaction Failed, Now Closing the Connection .......");
					break;
	
				case Signed_Document:
					rsaObj = new RSA("client");
					System.out.println("Broker Thread : Received Signed document");
					mylogger.info("Broker Thread :  Received Signed document");
					System.out.println("Broker Thread : Verifying Client's signature......");
					String data = new String(rsaObj.unsign_message(rcvd_msgbody.cipher),"UTF-8");
					System.out.println("Broker Thread : After unsigning the doc  :\n"+data);
					if(data.equals(purchase_info))
						System.out.println("Broker Thread : Good!!! It's a Valid Signature");
					else {
						System.out.println("Broker Thread : Error!! Signature is not valid");
						return true;
					}
					RandomAccessFile signed_file = new RandomAccessFile("/home/004/t/tx/txg131030/netsec_demo/netsec_prj_broker/signed_doc.txt", "rw");
					signed_file.setLength(0);
					signed_file.seek(0);
					signed_file.write(rcvd_msgbody.cipher);
					signed_file.close();
					break;

				case Sendproduct:
					cipher = rcvd_msgbody.cipher;
					System.out.println("Broker Thread : received msg type :"+rcvd_msgbody.msg_type+" needs to be forwarded");
					mylogger.info("Broker Thread : received msg type :"+rcvd_msgbody.msg_type+" needs to be forwarded");
					message = new Message("Broker","Product",cipher,null);
					send_msg(message,client);
					System.out.println("Broker Thread : Sending Product to Client");
					System.out.println("Done Sending product, Now Closing the Connection .......");
					return true;

				default :
					System.out.println("Broker Thread : Error Msg type");
					mylogger.info("Broker Thread : Error Msg type");
					return true;
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return false;
	}
}




public class ecommerce_broker{
	public static Queue<Message> msgqueue;
	public static Logger mylogger;

	public ecommerce_broker(){
		msgqueue = new LinkedList<Message>();
	}

	public static void main(String args[]){
		Scanner sc = new Scanner(System.in);
		MiscellaneousWork mw = new MiscellaneousWork();
		mylogger = mw.set_logging(args[0]);
		ecommerce_broker obj = new ecommerce_broker();
		Thread rcvmsg= new Receiver(mw, mylogger);
		rcvmsg.start();
		Thread broker= new Broker(mw, mylogger);
		broker.start();
		try {
			rcvmsg.join();
			broker.join();
		} catch (InterruptedException e) {
			mylogger.info("Problem in joining the threads\n");
		}

	} 
}




