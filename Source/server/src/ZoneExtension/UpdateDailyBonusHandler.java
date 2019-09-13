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

public class UpdateDailyBonusHandler extends BaseClientRequestHandler
{

	@Override
	public void handleClientRequest(User user, ISFSObject params)
	{
		// debug by jbj 20180904
		ZoneExtension zoneExt = (ZoneExtension)getParentExtension();
		zoneExt.whereis();
		//////////////////////////

		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		String sql = "UPDATE daily_bonus SET"
				+ " day=" + params.getInt("day")
				+ ", updated_at=" + System.currentTimeMillis()
				+ " WHERE email=\"" + user.getName() + "\"";
		try {
			dbManager.executeUpdate(sql, new Object[] {});
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}

		long chip = getPlayerChipcount(user.getName());
		chip += params.getLong("chip");
		setPlayerChipcount(user.getName(), chip);

		ISFSObject obj = new SFSObject();
		obj.putLong("chip", chip);
		send("update_daily_bonus", obj, user);
	}
	
	public void addPlayerChipcount(String email, long value) {
		long chip = getPlayerChipcount(email);
		setPlayerChipcount(email, chip + value);
	}

	public void payPlayerChipcount(String email, long value) {
		long chip = getPlayerChipcount(email);
		chip -= value;
		if (chip < 0)
			chip = 0;
		setPlayerChipcount(email, chip);
	}

	public void setPlayerChipcount(String email, long value) {
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		String sql = "UPDATE user SET chip=" + value + " WHERE email=\"" + email + "\"";
		try {
			dbManager.executeUpdate(sql, new Object[] {});
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
	}

	public long getPlayerChipcount(String email) {
		long chipcount = 0;
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		// System.out.println(email);
		String sql = "SELECT chip FROM user WHERE email=\"" + email + "\"";
		try {
			ISFSArray res = dbManager.executeQuery(sql, new Object[] {});
			if (res.size() > 0)
				chipcount = res.getSFSObject(0).getLong("chip");
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
		return chipcount;
	}
	
}
