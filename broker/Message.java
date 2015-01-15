import java.io.*;
import java.util.*;

public class Message implements Serializable{
	public String sender;
	public String msg_type;
	protected byte[] cipher;
	protected byte[] mac;
	
	public Message(String sender, String msg_type, byte[] cipher, byte[] mac){
		this.sender =sender;
		this.msg_type = msg_type;
		this.cipher =cipher;
		this.mac = mac;
	}


}
