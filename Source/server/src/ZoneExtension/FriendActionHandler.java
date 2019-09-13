package ZoneExtension;

import java.sql.SQLException;

import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;

public class FriendActionHandler extends BaseClientRequestHandler
{

	@Override
	public void handleClientRequest(User user, ISFSObject params)
	{
		// debug by jbj 20180904
		ZoneExtension zoneExt = (ZoneExtension)getParentExtension();
		zoneExt.whereis();
		//////////////////////////
		
		int action = params.getInt("action");
		String email = params.getUtfString("email");
		String friend_email = params.getUtfString("friend_email");

		User user1 = getParentExtension().getParentZone().getUserByName(email);
		User user2 = getParentExtension().getParentZone().getUserByName(friend_email);

		if(email.compareTo(user.getName()) == 0) {			// Request
			if(action == 0)		// Add Request
			{
				String name = params.getUtfString("name");
				addFriendRequest(email, name, friend_email);
				if(user2 != null)
					send("friend_request", params, user2);
			}
			else					// Remove
			{
				removeFriend(email, friend_email);
				removeFriend(friend_email, email);
			}
		}
		else {												// Reply
			String device_id = params.getUtfString("device_id");
			if(action == 0)		// Accept
			{
				updateFriendRequest(email, friend_email, device_id, 1);
//				removeFriendRequest(email, friend_email);
				addFriend(email, friend_email);
				addFriend(friend_email, email);
			}
			else					// Decline
			{
				updateFriendRequest(email, friend_email, device_id, 2);
//				removeFriendRequest(email, friend_email);
			}
		}
		
		if(user1 != null)
			send("friend_update", new SFSObject(), user1);
		if(user2 != null)
			send("friend_update", new SFSObject(), user2);
	}

	public void addFriendRequest(String email, String name, String friend_email)
	{
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		String sql = "INSERT INTO friend_request(email, friend_email, time)"
				+ " VALUES (\""
				+ email + "\",\""
				+ friend_email + "\","
				+ System.currentTimeMillis() + ")";
        try {
            dbManager.executeUpdate(sql, new Object[] {});
        }
        catch (SQLException e) {
            trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
        }
	}
	
	public void removeFriendRequest(String email, String friend_email)
	{
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		String sql = "DELETE FROM friend_request WHERE email=\"" + email + "\" AND friend_email=\"" + friend_email + "\"";
        try {
            dbManager.executeUpdate(sql, new Object[] {});
        }
        catch (SQLException e) {
            trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
        }
	}
	
	public void updateFriendRequest(String email, String friend_email, String friend_device_id, int status)
	{
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		String sql = "UPDATE friend_request SET status=" + status + ", time=" + System.currentTimeMillis() + ", friend_device_id=\"" + friend_device_id + "\" WHERE email=\"" + email + "\" AND friend_email=\"" + friend_email + "\" AND status=0";
        try {
            dbManager.executeUpdate(sql, new Object[] {});
        }
        catch (SQLException e) {
            trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
        }
	}
	
	public void addFriend(String email, String friend_email)
	{
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		String sql = "SELECT * FROM friend WHERE email=\"" + email + "\" AND friend_email=\"" + friend_email + "\"";
		try {
			ISFSArray res = dbManager.executeQuery(sql, new Object[] {});
			if(res.size() == 0) {
				sql = "INSERT INTO friend(email, friend_email)"
						+ " VALUES (\""
						+ email + "\",\""
						+ friend_email + "\")";
                try {
                    dbManager.executeUpdate(sql, new Object[] {});
                }
                catch (SQLException e) {
                    trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
                }
			}
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
	}

	public void removeFriend(String email, String friend_email)
	{
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		String sql = "DELETE FROM friend WHERE email=\"" + email + "\" AND friend_email=\"" + friend_email + "\"";
        try {
            dbManager.executeUpdate(sql, new Object[] {});
        }
        catch (SQLException e) {
            trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
        }
	}
	
}
