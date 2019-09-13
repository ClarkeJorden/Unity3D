package ZoneExtension;

import java.sql.SQLException;

import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;

public class GetNewMessageHandler extends BaseClientRequestHandler
{
	@Override
	public void handleClientRequest(User user, ISFSObject params)
	{
		// debug by jbj 20180904
		ZoneExtension zoneExt = (ZoneExtension)getParentExtension();
		zoneExt.whereis();
		//////////////////////////
		
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		String sql = "SELECT from_email FROM message WHERE to_email=\"" + user.getName() + "\" AND message_read=0 AND deleted_to=0";
		try {
			ISFSObject response = new SFSObject();
			ISFSArray res = dbManager.executeQuery(sql, new Object[] {});
			response.putSFSArray("array", res);
			send("get_new_message", response, user);
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
	}
}
