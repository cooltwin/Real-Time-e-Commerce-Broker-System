import java.util.*;
import java.io.*;
import java.net.*;
import java.util.logging.*;

class Receiver extends Thread {
	MiscellaneousWork mw;
	Logger mylogger;

	public Receiver(MiscellaneousWork mw, Logger mylogger) {
		this.mw = mw;
		this.mylogger = mylogger;
	}
	public void m_rcv() {
		int port = 0;
		port = Integer.parseInt(mw.read_config_file("port"));
		SyncMsgs sync = new SyncMsgs(); 
		mylogger.info("Receiver Thread : Receiver Started .........");
		try {
			ServerSocket serverSocket = new ServerSocket(port);
			while(true) {
				Message msgbody = null;
				Socket socket = serverSocket.accept();
				InputStream is = socket.getInputStream();
				ObjectInputStream ois = new ObjectInputStream(is);
				msgbody = (Message)ois.readObject();
				mylogger.info("Receiver Thread : Received a Message : ");
				mylogger.info("Sender :"+msgbody.sender+" Msg_type :"+msgbody.msg_type+" Cipher :"+msgbody.cipher);
				System.out.println("Receiver Thread : Received a Message : ");
				System.out.println("Sender :"+msgbody.sender+" Msg_type :"+msgbody.msg_type+" Cipher :"+msgbody.cipher);
				System.out.println("-------------------------------------------------------------");
				sync.set_msg(msgbody);
			}
		}
		catch(Exception e) {
			mylogger.info("Receive thread : "+e+"\n");
			mylogger.info("----x--------x----------x------------x-----------x--------x--------");
		}/*catch(InterruptedException e) {
		   mylogger.info("Sender Thread : Interrupted Exception\n");
		   mylogger.info("----x--------x----------x------------x-----------x--------x--------");
		   }*/
	}

	public void run() {
		m_rcv();
	}

}

