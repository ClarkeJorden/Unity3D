package ZoneExtension;

import java.sql.SQLException;

import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;

public class SendMessageHandler extends BaseClientRequestHandler
{

	@Override
	public void handleClientRequest(User user, ISFSObject params)
	{	
		// debug by jbj 20180904
		ZoneExtension zoneExt = (ZoneExtension)getParentExtension();
		zoneExt.whereis();
		//////////////////////////
		
		saveMessage(params);
		
		User user1 = getParentExtension().getParentZone().getUserByName(params.getUtfString("from"));
		User user2 = getParentExtension().getParentZone().getUserByName(params.getUtfString("to"));
		if(user1 != null)
			send("message_update", params, user1);
		if(user2 != null)
			send("message_update", params, user2);
	}
	
	public void saveMessage(ISFSObject params)
	{
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		String sql = "INSERT INTO message(from_email, to_email, content, time)"
				+ " VALUES (\""
				+ params.getUtfString("from")+ "\",\""
				+ params.getUtfString("to")+ "\",\""
				+ params.getUtfString("content") + "\","
				+ System.currentTimeMillis() + ")";
        try {
            dbManager.executeUpdate(sql, new Object[] {});
        }
        catch (SQLException e) {
            trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
        }
        
        sql = "UPDATE friend SET chat_time=" + System.currentTimeMillis()
        	+ " WHERE email=\"" + params.getUtfString("from") + "\""
        	+ " OR email=\"" + params.getUtfString("to") + "\"";
        try {
            dbManager.executeUpdate(sql, new Object[] {});
        }
        catch (SQLException e) {
            trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
        }
	}
	
}
