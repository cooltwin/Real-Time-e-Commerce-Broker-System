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

class Webserver extends Thread{
	static String IV;
	AES aesobj;
	private static String wb_longTimeSharedKey;
	private static SecretKey wb_session_key;
	private static SecretKey cw_session_key;
	public static Logger mylogger;
	public static String broker;
	public static String item;
	public static String msg_string;
	MiscellaneousWork mw;
	public enum MsgType{
		Hellomsg,Forwardmsg,Itemrequest,Paymentconfirmation
	}

	public Webserver(MiscellaneousWork mw, Logger mylogger){
		broker = "paypal";
		//Scanner sc = new Scanner(System.in);
		//System.out.println("Enter the broker name :");
		//broker = sc.nextLine();
		this.mw = mw;
		this.mylogger = mylogger;
		IV = mw.read_config_file("IV");
		aesobj = new AES(IV);
		wb_longTimeSharedKey = mw.read_password_file(broker, "longtimeSharedkey");
		//cw_session_key = aesobj.generate_session_key();
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
	public void send_hello_msg(){
		String msg=null,broker=null;
		Message encrypted_msg=null;
		byte[] mac ;
		msg = "Hello"+broker;
		mylogger.info("Webserver : Sending message :"+msg);
		System.out.println("Webserver : Sent message :"+msg);
		try {
			byte[] cipher = msg.getBytes();
			mac = aesobj.generate_mac("Webserver"+"Hello_Msg"+msg);
			encrypted_msg = new Message("Webserver","Hello_Msg",cipher,mac);
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
			mylogger.info("Webserver Thread : Message sent to host : "+host+" hostname :"+hostname);
			mylogger.info("Webserver Thread : Sent Message is :");
			mylogger.info("sender : "+msg.sender+" msg_type : "+msg.msg_type+" cipher : "+msg.cipher);
			mylogger.info("-------------------------------------------------------------");
			System.out.println("-------------------------------------------------------------");
			System.out.println("Webserver Thread : Message sent to host : "+host+" hostname :"+hostname);
			System.out.println("Webserver Thread : Sent Message is :");
			System.out.println("sender : "+msg.sender+" msg_type : "+msg.msg_type+" cipher : "+msg.cipher);
			System.out.println("-------------------------------------------------------------");
			socket.close();

		}catch(IOException e) {
			System.out.println("Webserver Thread : "+e+"\n");
			mylogger.info("Webserver Thread : "+e+"\n");
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
		String catalogue=null,prdt=null;
		Message msgbody=null;
		byte[] cipher, mac;
		RSA rsaObj=null;
		PublicKey publicKey=null;
		Scanner sc = new Scanner(System.in);

		try {		
			switch(MsgType.valueOf(rcvd_msgbody.msg_type)){
				case Hellomsg:
					rcvd_msg = rcvd_msgbody.cipher.toString();
					/*if(!check_mac(rcvd_msgbody, rcvd_msg)){
					  System.out.println("Webserver Thread : Error!! Messsag Integrity Hampered, exiting");
					  return true;
					  }*/	
					System.out.println("Webserver Thread : After decryption, received msg is :"+rcvd_msg);
					mylogger.info("Webserver Thread : After decryption, received msg is :"+rcvd_msg);
					wb_session_key = aesobj.generate_session_key();
					System.out.println("Webserver Thread : Sending wb session key : "+wb_session_key);
					mylogger.info("Webserver Thread : Sending wb session key: "+ wb_session_key);
					byte[] encoded = new BigInteger(wb_longTimeSharedKey, 16).toByteArray();
					SecretKey wb_ltsk = new SecretKeySpec(encoded, "AES");
					Cipher session_key_cipher = Cipher.getInstance("AES/ECB/NoPadding");
					session_key_cipher.init(Cipher.ENCRYPT_MODE, wb_ltsk);
					byte[] wrappedKey = session_key_cipher.doFinal(wb_session_key.getEncoded());
					// here wrapped key is the cipher
					mac = aesobj.generate_mac("Webserver"+"WB_Session_Key"+wrappedKey);
					msgbody = new Message("Webserver", "WB_Session_Key", wrappedKey, mac); 
					send_msg(msgbody,broker);
					break;


				case Forwardmsg:
					rsaObj = new RSA("client");
					PrivateKey privateKey = rsaObj.readPrivateKeyFromFile();
					Cipher session_key_cipher1 = Cipher.getInstance("RSA/ECB/PKCS1Padding");
					session_key_cipher1.init(Cipher.DECRYPT_MODE, privateKey);
					cw_session_key = new SecretKeySpec(session_key_cipher1.doFinal(rcvd_msgbody.cipher), "AES");
					System.out.println("Webserver Thread : Received cw session key :"+cw_session_key);
					mylogger.info("Webserver Thread : Received cw session key :"+cw_session_key);

					msg_string = "Webserver \tCatalog \nItems\tCost \nBook1\t$10 " +
						"\nBook2\t$11\nBook3\t$12\nBook4\t$13\nBook5\t$14\nBook6\t$15\nBook8\t$16" +
						"\nBook9\t$17\nSong1\t$20\nSong2\t$21\nSong3\t$22\nSong4\t$23" +
						"\nSong5\t$24\nSong6\t$25\nSong7\t$26\nSong8\t$27";
					cipher = aesobj.encrypt_using_sessionKey(msg_string,cw_session_key);
					mac = null;
					System.out.println("Webserver Thread : Sending catalogue ");
					mylogger.info("Webserver Thread : Sending catalogue ");
					msgbody = new Message("Webserver", "Catalogforward", cipher, mac);
					send_msg(msgbody, broker);
					break;

				case Itemrequest:
					rcvd_msg = aesobj.decrypt_using_sessionKey(rcvd_msgbody.cipher, cw_session_key);
					item = rcvd_msg;
					System.out.println("Webserver Thread : Received Item request for :"+item);
					mylogger.info("Webserver Thread : Received Item request for :"+item);
					/*if(!check_mac(rcvd_msgbody,rcvd_msg)){
					  System.out.println("Webserver Thread : Error!! Messsag Integrity Hampered, exiting");
					  return true;
					  }*/
					break;

				case Paymentconfirmation:
					mac = null;	
					rcvd_msg = aesobj.decrypt_using_sessionKey(rcvd_msgbody.cipher, wb_session_key);
					System.out.println("Webserver Thread : After decryption, received msg is :"+rcvd_msg);
					mylogger.info("Webserver Thread : After decryption, received msg is :"+rcvd_msg);
					/*if(!check_mac(rcvd_msgbody,rcvd_msg)){
					  System.out.println("Webserver Thread : Error!! Messsag Integrity Hampered, exiting");
					  return true;
					  }*/
					Thread.sleep(500);
					msg = "Product : "+item;
					RandomAccessFile prdt_file = new RandomAccessFile("/home/004/t/tx/txg131030/netsec_demo/netsec_prj_webserver1/Product.txt", "rw");
					prdt_file.setLength(0);
					prdt_file.seek(0);
					prdt_file.write(msg_string.getBytes());
					prdt_file.close();
					String amt_paid_by_broker = rcvd_msg.substring(rcvd_msg.indexOf('$')+1);
					int ind =msg_string.indexOf(item)+7;
					String amt_to_be_paid = msg_string.substring(ind,ind+2);
					if(amt_paid_by_broker.equals(amt_to_be_paid)){
						System.out.println("Webserver Thread : Payment Successful");
					}
					else {
						System.out.println("Webserver Thread : Client is supposed to pay : "+amt_to_be_paid+
						" but amt transferred was : "+amt_paid_by_broker);
						msg = "Invalid Payment Amount, Payment failed";
						cipher = aesobj.encrypt_using_sessionKey(msg,cw_session_key);
						msgbody = new Message("Webserver", "Transcation_Failed", cipher, mac);
						send_msg(msgbody, broker);
						System.out.println("Webserver Thread : Payment failed, Transaction failed, Closing Connection");
						return true;
					}

					cipher = aesobj.encrypt_using_sessionKey(msg,cw_session_key);
					System.out.println("Webserver Thread : Sending Product ");
					mylogger.info("Webserver Thread : Sending Product");
					msgbody = new Message("Webserver", "Sendproduct", cipher, mac);
					send_msg(msgbody, broker);
					System.out.println("Done Sending product, Now Closing the Connection .......");
					break;

				default :
					System.out.println("Webserver Thread : Error!! Wrong Msg type");
					mylogger.info("Webserver Thread : Error!! Wrong Msg type");
					return true;
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return false;
	}

}

public class ecommerce_webserver{
	public static Queue<Message> msgqueue;
	public static Logger mylogger;

	public ecommerce_webserver(){
		msgqueue = new LinkedList<Message>();
	}

	public static void main(String args[]){
		Scanner sc = new Scanner(System.in);
		MiscellaneousWork mw = new MiscellaneousWork();
		mylogger = mw.set_logging(args[0]);
		ecommerce_webserver obj = new ecommerce_webserver();

		Thread rcvmsg= new Receiver(mw, mylogger);
		rcvmsg.start();
		Thread Webserver= new Webserver(mw, mylogger);
		Webserver.start();
		try {
			rcvmsg.join();
			Webserver.join();
		} catch (InterruptedException e) {
			mylogger.info("Problem in joining the threads\n");
		}

	}
}
