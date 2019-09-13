package ZoneExtension;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;

public class GetStoreEventHandler extends BaseClientRequestHandler
{
	@Override
	public void handleClientRequest(User user, ISFSObject params)
	{
		String email = user.getName();
		ISFSObject userInfo = getUserInfo(email);
		if(userInfo == null)
			return;

		ISFSObject obj = new SFSObject();
		obj.putBool("sales_day", isSalesDay(email));
		obj.putBool("new_account", isNewAccount(email));
		obj.putUtfString("location", userInfo.getUtfString("location"));
		if(user != null)
			send("get_store_event", obj, user);
	}
	
	private boolean isSalesDay(String email)
	{
        return false;		
	}
	
	private boolean isNewAccount(String email)
	{
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		String sql = "SELECT * FROM store_buy WHERE email=\"" + email + "\" AND type=0";
		try {
			ISFSArray res = dbManager.executeQuery(sql, new Object[] {});
			return (res.size() < 2);
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
       
        return false;
	}
	
	private ISFSObject getUserInfo(String email)
	{
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		String sql = "SELECT * FROM user WHERE email=\"" + email + "\"";
		try {
			ISFSArray res = dbManager.executeQuery(sql, new Object[] {});
			return res.getSFSObject(0);
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
		return null;
	}
}

