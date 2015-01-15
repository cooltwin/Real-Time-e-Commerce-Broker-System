import java.util.*;
import java.io.*;
import java.net.*;
import java.util.logging.*;

class SyncMsgs {
	private static boolean lock=false;


	public synchronized void set_msg(Message msg) throws InterruptedException {
		Logger mylogger=ecommerce_webserver.mylogger;
		mylogger.info("set_msg : Dumping msg from receiver to syncmsg");
		lock = true;
		ecommerce_webserver.msgqueue.add(msg);
		mylogger.info("set_msg : Leaving set_msg");
	}
	
	public synchronized Message get_msg() throws InterruptedException {
		Logger mylogger=ecommerce_webserver.mylogger;
		Message msg=null;
		if(lock) {
			mylogger.info("get_msg : Received msg from syncmsgs: ");
			mylogger.info("---------------------------------------------------------------------");
			msg = ecommerce_webserver.msgqueue.poll();
			mylogger.info("Sender : "+msg.sender+" msg_type : "+msg.msg_type+" cipher : "+msg.cipher);
			mylogger.info("---------------------------------------------------------------------");
			lock = false;
			return msg;
		}
		return null;
	}
}




