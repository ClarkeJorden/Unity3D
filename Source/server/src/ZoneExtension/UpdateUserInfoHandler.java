package ZoneExtension;

import java.sql.SQLException;
import java.util.List;

import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;

public class UpdateUserInfoHandler extends BaseClientRequestHandler {
	@Override
	public void handleClientRequest(User user, ISFSObject params) {
		// debug by jbj 20180904
		ZoneExtension zoneExt = (ZoneExtension)getParentExtension();
		zoneExt.whereis();
		//////////////////////////
		
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		String sql = "UPDATE user" + " SET name=\"" + params.getUtfString("name") + "\"" + ", description=\""
				+ params.getUtfString("description") + "\"" + ", location=\"" + params.getUtfString("location") + "\""
				+ ", photo=" + params.getInt("photo") + " WHERE email=\"" + params.getUtfString("email") + "\"";
		System.out.println(sql);
		try {
			dbManager.executeUpdate(sql, new Object[] {});
			List<User> userList = (List<User>) getParentExtension().getParentZone().getUserList();
			if (userList.size() > 0)
				send("update_userinfo", params, userList);
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
	}
}
