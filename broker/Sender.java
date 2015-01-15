import java.util.*;
import java.io.*;
import java.net.*;
import java.util.logging.*;


public class Sender extends Thread {
	MiscellaneousWork mw;
	Logger mylogger;
	protected static String client;
	protected static String webserver;

	public Sender(MiscellaneousWork mw, Logger mylogger) {
		this.mw = mw;
		this.mylogger = mylogger;
	}
	public void run(){
		String input;
		Scanner sc = new Scanner(System.in);
		mylogger.info("Sender Thread : Sender started");
		System.out.println("Welcome to the E-commerce Secure System");
		
		while(true){
			while(!(input = sc.nextLine()).equals("")){
				// input type based on whom to send the message
				client = input;	
				webserver = input;
			}
		}
	}

	public synchronized void send_msg(Message msg, String host) throws InterruptedException { //sends a new msg
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

			mylogger.info("Sender Thread : Message sent to host : "+host+" hostname :"+hostname);
			mylogger.info("\nSender Thread : Sent Message is :");
			mylogger.info("Sender : "+msg.sender+" msg_type : "+msg.msg_type+" cipher : "+msg.cipher);
			mylogger.info("-------------------------------------------------------------");
			System.out.println("Sender Thread : Message sent to host : "+host+" hostname :"+hostname);
			System.out.println("\nSender Thread : Sent Message is :");
			System.out.println("Sender : "+msg.sender+" msg_type : "+msg.msg_type+" cipher : "+msg.cipher);
			System.out.println("-------------------------------------------------------------");
			socket.close();

		}catch(IOException e) {
			System.out.println("Sender Thread : "+e+"\n");
			mylogger.info("Sender Thread : "+e+"\n");
			mylogger.info("----x--------x----------x------------x-----------x--------x--------");
		}
	}
}
