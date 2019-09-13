package ZoneExtension;

import java.sql.SQLException;

import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;

public class GetFriendRequestListHandler extends BaseClientRequestHandler
{

	@Override
	public void handleClientRequest(User user, ISFSObject params)
	{
		// debug by jbj 20180904
		ZoneExtension zoneExt = (ZoneExtension)getParentExtension();
		zoneExt.whereis();
		//////////////////////////

		String device_id = params.getUtfString("device_id");
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		String sql = "SELECT friend_request.id, friend_request.time, friend_request.status, user.email, user.name, user.chip, user.location FROM friend_request"
				+ " INNER JOIN user ON friend_request.email=user.email"
				+ " WHERE friend_request.friend_email=\"" + user.getName()+ "\""
				+ " AND (friend_request.friend_device_id=\"" + device_id + "\" OR (friend_request.status=0 AND (friend_request.friend_device_id IS NULL OR friend_request.friend_device_id='')))"
				+ " ORDER BY time ASC";
		try {
			ISFSObject response = new SFSObject();
			ISFSArray res = dbManager.executeQuery(sql, new Object[] {});
			response.putSFSArray("array", res);
			send("get_friend_request_list", response, user);
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
	}
}
