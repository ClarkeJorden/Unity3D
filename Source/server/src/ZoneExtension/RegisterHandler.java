package ZoneExtension;

import java.sql.SQLException;

import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;

public class RegisterHandler extends BaseClientRequestHandler
{
	private static int USER_START_ID = 1203948735;
	@Override
	public void handleClientRequest(User user, ISFSObject params)
	{
		// debug by jbj 20180904
		ZoneExtension zoneExt = (ZoneExtension)getParentExtension();
		zoneExt.whereis();
		//////////////////////////
		
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		String sql = "SELECT * FROM user WHERE email=\"" + params.getUtfString("email") + "\"";
		try {
			ISFSArray res = dbManager.executeQuery(sql, new Object[] {});
			ISFSObject response = new SFSObject();            
			if(res.size() > 0) {
				response.putBool("success", false);
				response.putUtfString("reason", "email");
			}
			else {
				sql = "INSERT INTO user(email, password, name, chip, coin, level, user_id, started_playing, description, location)"
						+ " VALUES (\""
						+ params.getUtfString("email") + "\",\""
						+ params.getUtfString("password") + "\",\""
						+ params.getUtfString("name") + "\","
						+ 10000 + ","
						+ 10 + ","
						+ 1 + ",\""
						+ "\","
						+ System.currentTimeMillis() + ",\""
						+ "\",\""
						+ "UNKNOWN" + "\")";
				try {
					Long rowId = (Long)dbManager.executeInsert(sql, new Object[] {});
					setUserId(rowId.intValue());
					response.putBool("success", true);
				}
				catch (SQLException e) {
					trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
				}
			}
			send("register", response, user);
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
	}
	
	public void setUserId(int rowId)
	{
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		String idStr = String.format("%02d-%04d-%04d", ((USER_START_ID + rowId) / 10000) / 10000, ((USER_START_ID + rowId) / 10000) % 10000, (USER_START_ID + rowId) % 10000);
		String sql = "UPDATE user SET user_id=\"" + idStr + "\" WHERE id=" + rowId;
		try {
			dbManager.executeUpdate(sql, new Object[] {});
		}
		catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
	}
}
