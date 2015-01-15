import java.util.*;
import java.util.logging.Logger;
import java.io.*;
import java.net.*;



class SyncMsgs {
	private static boolean lock=false;


	public synchronized void set_msg(Message msg) throws InterruptedException {
		Logger mylogger=ecommerce_broker.mylogger;
		mylogger.info("set_msg : Dumping msg from receiver to syncmsg");
		lock = true;
		ecommerce_broker.msgqueue.add(msg);
		mylogger.info("set_msg : Leaving set_msg");
	}
	
	public synchronized Message get_msg() throws InterruptedException {
		Logger mylogger=ecommerce_broker.mylogger;
		Message msg=null;
		if(lock) {
			mylogger.info("get_msg : Received msg from syncmsgs: ");
			mylogger.info("---------------------------------------------------------------------");
			msg = ecommerce_broker.msgqueue.poll();
			mylogger.info("Broker : "+msg.sender+" msg_type : "+msg.msg_type+" cipher : "+msg.cipher);
			mylogger.info("---------------------------------------------------------------------");
			lock = false;
			return msg;
		}
		return null;
	}
}




