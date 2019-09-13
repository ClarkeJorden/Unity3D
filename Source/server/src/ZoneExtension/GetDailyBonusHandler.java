package ZoneExtension;

import java.sql.SQLException;
import java.time.Duration;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;

public class GetDailyBonusHandler extends BaseClientRequestHandler
{

	@Override
	public void handleClientRequest(User user, ISFSObject params)
	{
		// debug by jbj 20180904
		ZoneExtension zoneExt = (ZoneExtension)getParentExtension();
		zoneExt.whereis();
		//////////////////////////

		ISFSObject obj = GetDailyBonus(user.getName());
		if(obj == null)
		{
			InsertDailyBonus(user.getName());
			obj = GetDailyBonus(user.getName());
		}
		
		Calendar cal = Calendar.getInstance();
		Date now = new Date();
		Date createdAt = new Date(obj.getLong("created_at"));
		Date updatedAt = new Date(obj.getLong("updated_at"));
		Date expectedDate = new Date(now.getYear(), now.getMonth(), now.getDate()
								, createdAt.getHours(), createdAt.getMinutes(), createdAt.getSeconds());
		

		if(expectedDate.compareTo(now) < 0)
		{
			cal.setTime(expectedDate);
			cal.add(Calendar.DATE, 1);
			expectedDate = cal.getTime();
		}
		if(expectedDate.compareTo(updatedAt) < 0)
		{
			cal.setTime(expectedDate);
			cal.add(Calendar.DATE, 1);
			expectedDate = cal.getTime();
		}
		
		int day = obj.getInt("day");
		boolean claim = false;
		long diff = expectedDate.getTime() - updatedAt.getTime();

//		System.out.println("now:" + now.toString());
//		System.out.println("expected:" + expectedDate.toString());
//		System.out.println("updated:" + updatedAt.toString());
//		System.out.println(diff / 1000 + ":" + Duration.ofHours(1).getSeconds() * 1000);
		
		if(diff < Duration.ofDays(1).getSeconds() * 1000)
			claim = false;
		else if(diff >= Duration.ofDays(2).getSeconds() * 1000)
		{
			day = 1;
			claim = true;
		}
		else
			claim = true;

		ISFSObject resp = new SFSObject();
		resp.putInt("day", day);
		resp.putLong("expected", expectedDate.getTime());
		resp.putBool("claim", claim);
		send("get_daily_bonus", resp, user);
	}
	
	public ISFSObject GetDailyBonus(String email)
	{
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		String sql = "SELECT * FROM daily_bonus WHERE email=\"" + email + "\"";
		try {
			ISFSArray res = dbManager.executeQuery(sql, new Object[] {});
			if(res.size() > 0)
				return res.getSFSObject(0);
			else
				return null;
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
		return null;
	}
	
	public void InsertDailyBonus(String email)
	{
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		String sql = "INSERT INTO daily_bonus(email, created_at, updated_at)"
				+ " VALUES(\"" + email + "\","
				+ System.currentTimeMillis() + ","
				+ 0 + ")";
		try {
			dbManager.executeUpdate(sql, new Object[] {});
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
	}
	
}
