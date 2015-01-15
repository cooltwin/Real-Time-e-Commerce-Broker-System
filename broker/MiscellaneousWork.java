import java.util.*;
import java.io.*;
import java.net.*;
import java.util.logging.*;

class logging extends SimpleFormatter {

	public String format(LogRecord record){
		if(record.getLevel() == Level.INFO){
			return record.getMessage() + "\r\n";
		}else{
			return super.format(record);
		}
	}
}

class MiscellaneousWork {
	File password_file;
	File config_file;
	Logger mylogger;

	public MiscellaneousWork () {
		config_file = new File("config.txt");
		password_file = new File("broker_password_file.txt");
	}

	public String read_config_file(String search_string) {
		mylogger = Broker.mylogger; 
		String currentLine = null;
		Scanner scanner;
		try {
			scanner = new Scanner(config_file);
			while(scanner.hasNextLine()) {
				currentLine = scanner.nextLine();
				if(currentLine.indexOf(search_string) >= 0) {
					mylogger.info("read_config_file :  "+currentLine);
					return (currentLine.substring(currentLine.indexOf(':')+2));
				}
			}
			mylogger.info("read_config_file : "+search_string+" not found in config file");
			scanner.close();
		}catch(FileNotFoundException e)  {
			e.printStackTrace();
		}
		return null;
	}
	
	public String read_password_file(String search_string, String field) {
		mylogger = Broker.mylogger; 
		String currentLine = null,str = null;
		Scanner scanner;
		try {
			scanner = new Scanner(password_file);
			while(scanner.hasNextLine()) {
				currentLine = scanner.nextLine();
				if(currentLine.indexOf(search_string) >= 0) {
					while(currentLine.indexOf(field) == -1)
						currentLine = scanner.nextLine();
					mylogger.info("read_password_file :  "+currentLine);
					return (currentLine.substring(currentLine.indexOf(':')+2));
				}
			}
			mylogger.info("read_config_file : "+search_string+" not found in config file");
			scanner.close();
		}catch(FileNotFoundException e)  {
			e.printStackTrace();
		}
		return null;
	}

	public Logger set_logging(String mymachinename) {
		mylogger = Logger.getLogger("MyLog");
		FileHandler fh;
		try {
			fh = new FileHandler("logfile_"+mymachinename+".log");
			mylogger.addHandler(fh);
			logging formatter = new logging();
			fh.setFormatter(formatter);
			mylogger.setUseParentHandlers(false);
		}catch (IOException e) {
			e.printStackTrace();
		}
		return mylogger;
	}
	
	public byte[] intToByteArray(int value)
	{
		byte[] data = new byte[4];

		// int -> byte[]
		for (int i = 0; i < 4; ++i)
		{
			int shift = i << 3; // i * 8
			data[3 - i] = (byte) ((value & (0xff << shift)) >>> shift);
		}
		return data;
	}

	public int byteArrayToInt(byte[] data)
	{
		// byte[] -> int
		int number = 0;
		for (int i = 0; i < 4; ++i)
		{
			number |= (data[3-i] & 0xff) << (i << 3);
		}
		return number;
	}
	public byte[] concat(byte[] a, byte[] b)
	{
		byte[] c = new byte[a.length + b.length];
		System.arraycopy(a, 0, c, 0, a.length);
		System.arraycopy(b, 0, c, a.length, b.length);

		return c;
	}
	public String convert_byte_to_string(byte[] cipher){
		String cipher_string=null;
		for (int i=0; i<cipher.length; i++)
			cipher_string += new Integer(cipher[i])+" ";
		cipher_string += "";
		return cipher_string;
	}
}

