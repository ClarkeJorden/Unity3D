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

public class UpdateLevelHandler extends BaseClientRequestHandler {
	@Override
	public void handleClientRequest(User user, ISFSObject params) {	
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		String sql = "UPDATE user"
				+ " SET level=" + params.getInt("level")
				+ " WHERE email=\"" + user.getName() + "\"";
		try {
			dbManager.executeUpdate(sql, new Object[] {});
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
		
		sql = "INSERT INTO news_level(email, level, time)"
				+ " VALUES(\"" + user.getName() + "\""
				+ "," + params.getInt("level")
				+ "," + System.currentTimeMillis() + ")";
		try {
			dbManager.executeUpdate(sql, new Object[] {});
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
		
		send("update_level", params, user);
	}
}
