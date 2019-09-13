package ZoneExtension;

import java.sql.SQLException;

import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;

public class FriendChatActionHandler extends BaseClientRequestHandler
{

	@Override
	public void handleClientRequest(User user, ISFSObject params)
	{
		// debug by jbj 20180904
		ZoneExtension zoneExt = (ZoneExtension)getParentExtension();
		zoneExt.whereis();
		//////////////////////////

		String email = params.getUtfString("email");
		String friend_email = params.getUtfString("friend_email");
		int chat = params.getInt("chat");
		
		setFriendChat(email, friend_email, chat);
		if(chat == 1)
			setFriendChat(friend_email, email, chat);

		User user1 = getParentExtension().getParentZone().getUserByName(email);
		User user2 = getParentExtension().getParentZone().getUserByName(friend_email);
		if(user1 != null)
			send("friend_update", new SFSObject(), user1);
		if(user2 != null)
			send("friend_update", new SFSObject(), user2);
	}
	
	public void setFriendChat(String email, String friend_email, int chat)
	{
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		String sql = "";
		if(chat == 1)
			sql = "UPDATE friend SET chat=" + chat + ", chat_time=" + System.currentTimeMillis() + " WHERE email=\"" + email + "\" AND friend_email=\"" + friend_email + "\"";
		else
			sql = "UPDATE friend SET chat=" + chat + " WHERE friend_email=\"" + email + "\" AND email=\"" + friend_email + "\"";
		System.out.println(sql);
        try {
            dbManager.executeUpdate(sql, new Object[] {});
        }
        catch (SQLException e) {
            trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
        }
        
        if(chat == 0)
        {
			sql = "UPDATE message SET deleted_from=1 WHERE"
					+ " from_email=\"" + email + "\""
					+ " AND to_email=\"" + friend_email + "\"";
            try {
                dbManager.executeUpdate(sql, new Object[] {});
            }
            catch (SQLException e) {
                trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
            }

			sql = "UPDATE message SET deleted_to=1 WHERE"
					+ " to_email=\"" + email + "\""
					+ " AND from_email=\"" + friend_email + "\"";
            try {
                dbManager.executeUpdate(sql, new Object[] {});
            }
            catch (SQLException e) {
                trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
            }
        }
	}

}
