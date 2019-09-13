package ZoneExtension;

import java.sql.SQLException;

import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;

public class GetFriendSearchListHandler extends BaseClientRequestHandler
{

	@Override
	public void handleClientRequest(User user, ISFSObject params)
	{
		// debug by jbj 20180904
		ZoneExtension zoneExt = (ZoneExtension)getParentExtension();
		zoneExt.whereis();
		//////////////////////////
		
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		String sql = "SELECT * FROM user WHERE"
					+ " lower(name) LIKE \"" + params.getUtfString("search").toLowerCase() + "%\""
					+ " OR email=\"" + params.getUtfString("search") + "\""
					+ " OR user_id=\"" + params.getUtfString("search") + "\"";
		try {
			ISFSObject response = new SFSObject();
			ISFSArray res = dbManager.executeQuery(sql, new Object[] {});
			for(int i = 0; i < res.size(); i ++) {
				ISFSObject obj = res.getSFSObject(i);
				((ZoneExtension)getParentExtension()).AddUserInfo(obj);
			}
			response.putSFSArray("array", res);
			send("get_friend_search_list", response, user);
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
	}

}
