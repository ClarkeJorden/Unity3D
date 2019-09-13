package ZoneExtension;

import java.sql.SQLException;
import java.util.List;

import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;

public class UpdateBestFriendHandler extends BaseClientRequestHandler {
	@Override
	public void handleClientRequest(User user, ISFSObject params) {
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		String sql = "UPDATE friend INNER JOIN user ON friend.email=user.email"
				+ " SET friend.best_friend=IF(user.email=\"" + params.getUtfString("email") + "\", 1, 0)"
				+ " WHERE friend.friend_email=\"" + user.getName() + "\"";
		try {
			dbManager.executeUpdate(sql, new Object[] {});
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
		
		sql = "INSERT INTO best_friend_request(from_email, to_email, name)"
				+ " VALUES(\"" + user.getName() + "\""
				+ ",\"" + params.getUtfString("email") + "\""
				+ ",\"" + params.getUtfString("name") + "\")";
		try {
			dbManager.executeUpdate(sql, new Object[] {});
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}

		User best_friend = getParentExtension().getParentZone().getUserByName(params.getUtfString("email"));
		if (best_friend != null) {
			ISFSObject obj = new SFSObject();
			obj.putUtfString("email", user.getName());
			obj.putUtfString("name", params.getUtfString("name"));
			send("best_friend_request", obj, best_friend);
		}
	}
}
