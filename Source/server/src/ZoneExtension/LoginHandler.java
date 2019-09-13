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
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;

public class LoginHandler extends BaseClientRequestHandler
{
	private static int USER_START_ID = 1203948735;
	@Override
	public void handleClientRequest(User user, ISFSObject params)
	{
		boolean isFirstLogIn = false;
		// debug by jbj 20180904
		ZoneExtension zoneExt = (ZoneExtension)getParentExtension();
		zoneExt.whereis();
		//////////////////////////
		
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		String sql = "SELECT * FROM user WHERE email=\"" + params.getUtfString("email") + "\"";
		try {
			ISFSArray res = dbManager.executeQuery(sql, new Object[] {});
			ISFSObject response = new SFSObject();            
			if(res.size() == 0) {
				if(params.getBool("facebook") != null && params.getBool("facebook"))
				{
					ISFSObject obj = registerFacebookUser(params);
					isFirstLogIn = isFirstLogin(obj);
					response.putBool("first_login", isFirstLogIn);
					if(isFirstLogIn)
					{
						obj = setBonusNewAccount(obj);
					}					
					UpdateLastLoginTime(obj);
					sendNotificationToFriends(params.getUtfString("email"), obj.getUtfString("name"));
					response.putBool("success", true);
					response.putSFSObject("info", getUserInfo(params.getUtfString("email")));
					response.putSFSObject("same_user_playing_info", getUserPlayingInfo(params.getUtfString("email"), false));
				}
				else if(params.getBool("guest") != null && params.getBool("guest"))
				{
					ISFSObject obj = registerGuest(params);
					isFirstLogIn = isFirstLogin(obj);
					response.putBool("first_login", isFirstLogIn);
					if(isFirstLogIn)
					{
						obj = setBonusNewAccount(obj);
					}
					UpdateLastLoginTime(obj);
					sendNotificationToFriends(params.getUtfString("email"), obj.getUtfString("name"));
					response.putBool("success", true);
					response.putSFSObject("info", getUserInfo(params.getUtfString("email")));
					response.putSFSObject("same_user_playing_info", getUserPlayingInfo(params.getUtfString("email"), false));
				}
				else
				{
					response.putBool("success", false);
					response.putUtfString("reason", "email");
				}
			}
			else {
				ISFSObject obj = (ISFSObject)(res.getElementAt(0));
				String pwd = obj.getUtfString("password");
				if(params.getBool("facebook") != null && params.getBool("facebook"))
				{
					ISFSObject obj1 = updateFacebookUser(params);
					isFirstLogIn = isFirstLogin(obj1);
					response.putBool("first_login", isFirstLogIn);
					if(isFirstLogIn)
					{
						obj1 = setBonusNewAccount(obj1);
					}
					UpdateLastLoginTime(obj1);
					sendNotificationToFriends(params.getUtfString("email"), obj1.getUtfString("name"));
					response.putBool("success", true);
					response.putSFSObject("info", getUserInfo(params.getUtfString("email")));
					response.putSFSObject("same_user_playing_info", getUserPlayingInfo(params.getUtfString("email"), false));
				}
				else if(pwd.compareTo(params.getUtfString("password")) == 0) {
					isFirstLogIn = isFirstLogin(obj);
					response.putBool("first_login", isFirstLogIn);
					if(isFirstLogIn)
					{
						obj = setBonusNewAccount(obj);
					}
					UpdateLastLoginTime(obj);
					sendNotificationToFriends(params.getUtfString("email"), res.getSFSObject(0).getUtfString("name"));
					response.putBool("success", true);
					response.putSFSObject("info", getUserInfo(params.getUtfString("email")));
					response.putSFSObject("same_user_playing_info", getUserPlayingInfo(params.getUtfString("email"), false));
				}
				else {
					response.putBool("success", false);
					response.putUtfString("reason", "password");
				}
			}
			send("login", response, user);
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
	}
	
	public boolean isFirstLogin(ISFSObject dataObject) {
		if(dataObject == null)
			return false;
		
		if(dataObject.getLong("last_login") == 0)
		{			
			return true;
		}	
		
		return false;
	}
	
	public ISFSObject setBonusNewAccount (ISFSObject dataObject)
	{
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		String sql = "UPDATE user SET chip=" + (dataObject.getLong("chip") + 750) + ", coin=" + (dataObject.getLong("coin") + 100) + " WHERE email=\"" + dataObject.getUtfString("email") + "\"";
		try {
			dbManager.executeUpdate(sql, new Object[] {});
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
		return getUserInfo(dataObject.getUtfString("email"));
	}
	
	private void UpdateLastLoginTime(ISFSObject dataObject)
	{
		long curTime = System.currentTimeMillis();
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		String sql = "UPDATE user SET last_login=" + curTime + " WHERE email=\"" + dataObject.getUtfString("email") + "\"";
		try {
			dbManager.executeUpdate(sql, new Object[] {});
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
	}
	
	public boolean setDailyBonus(User user, ISFSObject dataObject)
	{
		long curTime = System.currentTimeMillis();
		Date date1 = new Date(curTime);
		Date date2 = new Date(dataObject.getLong("last_login"));

		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		String sql = "UPDATE user SET last_login=" + curTime + " WHERE email=\"" + dataObject.getUtfString("email") + "\"";
		
		SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd");
		dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));

//		System.out.println(dateFormatGmt.format(date1));
//		System.out.println(dateFormatGmt.format(date2));

		if(dateFormatGmt.format(date1).compareTo(dateFormatGmt.format(date2)) != 0)
			sql = "UPDATE user SET last_login=" + curTime + ", chip=" + (dataObject.getLong("chip") + 100000) + " WHERE email=\"" + dataObject.getUtfString("email") + "\"";
		
		try {
			dbManager.executeUpdate(sql, new Object[] {});
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
		
		return (dateFormatGmt.format(date1).compareTo(dateFormatGmt.format(date2)) != 0);
	}

	public void sendNotificationToFriends(String email, String name)
	{
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		String sql = "SELECT friend.email FROM friend INNER JOIN user ON friend.email=user.email WHERE friend.friend_email=\"" + email + "\"";
		try {
			ISFSObject response = new SFSObject();
			response.putUtfString("email", email);
			response.putUtfString("name", name);
			ISFSArray res = dbManager.executeQuery(sql, new Object[] {});
			for(int i = 0; i < res.size(); i ++) {
				User user = getParentExtension().getParentZone().getUserByName(res.getSFSObject(i).getUtfString("email"));
				if(user != null)
					send("friend_login", response, user);
			}
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
	}

	public ISFSObject getUserPlayingInfo(String email, boolean userCanLeave)
	{
		ISFSObject obj = new SFSObject();
		ZoneExtension.mutex.lock();
		try
		{
			List<Room> roomList = getParentExtension().getParentZone().getRoomList();
			for(Room room : roomList)
			{
				if(!room.isGame() || room.getGroupId().compareTo("default") == 0)
					continue;
				ISFSObject roomInfo = (ISFSObject) room.getExtension().handleInternalMessage("get_user_room_info", email);
				if(roomInfo != null){
					obj.putBool("is_same_user_playing", true);
					obj.putSFSObject("playing_info", roomInfo);
					if(userCanLeave == false){
						ISFSObject param = new SFSObject();
						param.putUtfString("email", email);
						param.putBool("can_leave", userCanLeave);					
						room.getExtension().handleInternalMessage("set_user_can_leave", param);
					}
					return obj;
				}
			}			
		}
		finally
		{
			ZoneExtension.mutex.unlock();
		}

		
		obj.putBool("is_same_user_playing", false);
		return obj;
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
	
	public ISFSObject registerGuest(ISFSObject params)
	{
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		String sql = "INSERT INTO user(email, password, name, chip, coin, user_id, started_playing, description, location)"
				+ " VALUES (\""
				+ params.getUtfString("email") + "\",\""
				+ params.getUtfString("password") + "\",\""
				+ params.getUtfString("name") + "\","
				+ 10000 + ","
				+ 10 + ",\""
				+ "\","
				+ System.currentTimeMillis() + ",\""
				+ "\",\""
				+ "UNKNOWN" + "\")";
		try {
			Long rowId = (Long)dbManager.executeInsert(sql, new Object[] {});
			setUserId(rowId.intValue());
		}
		catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}

		return getUserInfo(params.getUtfString("email"));
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
	
	public ISFSObject registerFacebookUser(ISFSObject params)
	{
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		String sql = "INSERT INTO user(email, password, name, photo, chip, coin, user_id, started_playing, description, location)"
				+ " VALUES (\""
				+ params.getUtfString("email") + "\",\""
				+ "" + "\",\""
				+ params.getUtfString("name") + "\","
				+ params.getInt("photo") + ","
				+ 10000 + ","
				+ 10 + ",\""
				+ "\","
				+ System.currentTimeMillis() + ",\""
				+ "\",\""
				+ "UNKNOWN" + "\")";
		try {
			Long rowId = (Long)dbManager.executeInsert(sql, new Object[] {});
			setUserId(rowId.intValue());
		}
		catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}

		return getUserInfo(params.getUtfString("email"));
	}
	
	public ISFSObject updateFacebookUser(ISFSObject params)
	{
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		String sql = "UPDATE user"
				+ " SET name=\"" + params.getUtfString("name") + "\""
				+ " AND photo=" + params.getInt("photo")
				+ " AND chip=" + 10000
				+ " AND coin=" + 10
				+ " AND user_id=\"" + "" + "\""
				+ " AND started_playing=" + System.currentTimeMillis()
				+ " AND description=\"" + "" + "\""
				+ " AND location=\"" + "UNKNOWN" + "\""
				+ " WHERE email=\"" + params.getUtfString("email") + "\"";
		try {
			dbManager.executeUpdate(sql, new Object[] {});
		}
		catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}

		return getUserInfo(params.getUtfString("email"));
	}

}
