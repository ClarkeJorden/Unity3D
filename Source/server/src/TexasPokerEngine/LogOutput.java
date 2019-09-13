
////////////////////////// 	class for debug		///////////// 

package TexasPokerEngine;

import com.smartfoxserver.v2.extensions.ExtensionLogLevel;
import com.smartfoxserver.v2.extensions.SFSExtension;
//import java.lang.InterruptedException;
import java.lang.Exception;
import java.lang.NullPointerException;
import java.lang.InterruptedException;
//import java.sql.SQLException;

public class LogOutput extends  SFSExtension{
	
	private static LogOutput inst = null;
	public LogOutput() {
		// TODO Auto-generated method stub
		inst = this;
	}
	
	public static LogOutput instance()
	{
		if(inst == null)
			inst = new LogOutput();
		return inst;
	}
	
	public static void traceLog(String output)
	{
		//instance().outputString(output);
		//instance().trace(ExtensionLogLevel.WARN,output);
	}
	
	public void outputString(String output) 
	{
		System.out.println(output);
	}
	
	@Override
	public void init() {
		// TODO Auto-generated method stub
		
	}
}
