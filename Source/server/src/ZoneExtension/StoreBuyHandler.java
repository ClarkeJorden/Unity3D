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

public class StoreBuyHandler extends BaseClientRequestHandler
{
	@Override
	public void handleClientRequest(User user, ISFSObject params)
	{
		String email = user.getName();
		String device_id = params.getUtfString("device_id");
		int type = params.getInt("type");
		double price = params.getDouble("price");
		long value = params.getLong("value");
		
		if(insertStoreBuy(email, device_id, type, price, value)) {
			updateUserInfo(email, type, price, value);
			
			// make user update his profile
			ISFSObject obj = new SFSObject();
			if(user != null) {
				send("purchase_update", obj, user);
				
				if(type == 5) // welcome bonus
					send("welcome_bonus", obj, user);
			}
		}
	}
	
	private boolean insertStoreBuy(String email, String device_id, int type, double price, long value)
	{
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		
		String sql = "INSERT INTO store_buy(email, device_id, type, price, value, created_at)"
				+ " VALUES (\""
				+ email + "\",\""
				+ device_id + "\","
				+ type + ","
				+ price + ","
				+ value + "," 
				+ System.currentTimeMillis() + ")";
        try {
			dbManager.executeInsert(sql, new Object[] {});
            return true;
        }
        catch (SQLException e) {
            trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
        }
        return false;		
	}
	
	private boolean updateUserInfo(String email, int type, double price, long value)
	{	
		ISFSObject userInfo = getUserInfo(email);
		if(userInfo == null)
			return false;
		
		String sql = "";
		switch(type){
		case 0: // chip
			{
				long chip = userInfo.getLong("chip");
				chip += value;
				sql = "UPDATE user SET chip=" + chip + " WHERE email=\"" + email + "\"";	
			}
			break;
		case 1: // coin
			{
				long coin = userInfo.getLong("coin");
				coin += value;
				sql = "UPDATE user SET coin=" + coin+ " WHERE email=\"" + email + "\"";
			}
			break;
		}
		
		if(sql.isEmpty())
			return false;
		
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
        try {
            dbManager.executeUpdate(sql, new Object[] {});
            return true;
        }
        catch (SQLException e) {
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
