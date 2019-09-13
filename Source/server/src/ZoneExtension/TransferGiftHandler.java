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

public class TransferGiftHandler extends BaseClientRequestHandler 
{	
	@Override
	public void handleClientRequest(User user, ISFSObject params)
	{
		String from_email = user.getName();
		String to_email = params.getUtfString("to_email");
		int type = params.getInt("type");
		long value = params.getLong("value");
		
		if(addTransferRecord(from_email, to_email, type, value)) {
			transferGift(from_email, to_email, type, value);
			
			User user1 = getParentExtension().getParentZone().getUserByName(to_email);			
			// notify to user1
			if(user1 != null)
				notifyTransferGiftTo(user1, from_email, type, value);

			// make from_user and to user update there transfer list
			ISFSObject obj = new SFSObject();
			obj.putInt("type", type);
			
			if(user != null)
				send("transfer_update", obj, user);
			if(user1 != null)
				send("transfer_update", obj, user1);
		}
	}
	
	public void notifyTransferGiftTo(User toUser, String email, int type, long value)
	{
		ISFSObject userInfo = getUserInfo(email);
		String name = "";
		if(userInfo != null)
			name = userInfo.getUtfString("name");
		
		ISFSObject obj = new SFSObject();
		obj.putUtfString("email", email);
		obj.putUtfString("name", name);
		obj.putInt("type", type);
		obj.putLong("value", value);
		
		send("transfer_gift", obj, toUser);			
	}
	
	public ISFSObject getUserInfo(String email)
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
	
	public boolean addTransferRecord(String from_email, String to_email, int type, long value)
	{
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		long curTime = System.currentTimeMillis();
		Date curDate = new Date(curTime);
		SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		String dateTimeStr = dateFormatGmt.format(curDate);
		
		String sql = "INSERT INTO transfer_gift(from_email, to_email, type, value, time, status)"
				+ " VALUES (\""
				+ from_email + "\",\""
				+ to_email + "\","
				+ type + ","
				+ value + ",\""
				+ dateTimeStr + "\","
				+ 0 + ")";
        try {
			dbManager.executeInsert(sql, new Object[] {});
            return true;
        }
        catch (SQLException e) {
            trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
        }
        return false;
	}
	
	public void transferGift(String from_email, String to_email, int type, long value) {
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();

		ISFSObject user1 = getUserInfo(from_email);
		ISFSObject user2 = getUserInfo(to_email);
		if(type == 0) { // transfer chip
			if(user1 != null) {
				long chip = user1.getLong("chip");
				if(chip < value)
					value = chip;
				
				chip -= value;
				String sql = "UPDATE user SET chip=" + chip + " WHERE email=\"" + from_email + "\"";				
		        try {
		            dbManager.executeUpdate(sql, new Object[] {});
		        }
		        catch (SQLException e) {
		            trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		        }
			}
			
			if(user2 != null) {
				long chip = user2.getLong("chip");
				chip += value;
				
				String sql = "UPDATE user SET chip=" + chip + " WHERE email=\"" + to_email + "\"";
		        try {
		            dbManager.executeUpdate(sql, new Object[] {});
		        }
		        catch (SQLException e) {
		            trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		        }
			}
		}
	}	
}
