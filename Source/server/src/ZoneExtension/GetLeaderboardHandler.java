package ZoneExtension;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;

public class GetLeaderboardHandler extends BaseClientRequestHandler {
	@SuppressWarnings("deprecation")
	@Override
	public void handleClientRequest(User user, ISFSObject params) {
		// debug by jbj 20180904
		ZoneExtension zoneExt = (ZoneExtension)getParentExtension();
		zoneExt.whereis();
		//////////////////////////
		
		String email = params.getUtfString("email");
		int type = params.getInt("type");

		IDBManager dbManager = getParentExtension().getParentZone()
				.getDBManager();
		ISFSObject response = new SFSObject();
		ISFSArray res = new SFSArray();
		ISFSArray res1 = new SFSArray();
		ISFSArray tempArray = new SFSArray();

		String sql = "";
		if (type == 0)
			sql = "SELECT * FROM user WHERE privilege=0 ORDER BY chip DESC LIMIT 10";
		else if (type == 1) {
			String countryCode = getCountryCode(email);
			if (countryCode.compareTo("UNKNOWN") != 0)
				sql = "SELECT * FROM user WHERE privilege=0 AND location LIKE \"%"
						+ countryCode + "%\" ORDER BY chip DESC LIMIT 10";
		} else if (type == 2) {
			sql = "SELECT * FROM user INNER JOIN friend ON user.email=friend.email"
					+ " WHERE friend.friend_email=\""
					+ email
					+ "\" AND privilege=0" + " ORDER BY chip DESC LIMIT 10";
		} else if (type == 3) {		
			long curTime = System.currentTimeMillis();
			Date curDate = new Date(curTime);
			Date endDate = new Date(curDate.getYear(), curDate.getMonth(), curDate.getDate(), 14, 0, 0); //2:00 PM
			if(curDate.after(endDate))
				curDate.setDate(curDate.getDate() + 1);
			SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd");
			dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
			String dateStr = dateFormatGmt.format(curDate);
			
			sql = "SELECT * FROM user INNER JOIN daily_hand ON user.email=daily_hand.email AND daily_hand.gain_date=\""
					+ dateStr 
					+ "\" WHERE privilege=0" + " ORDER BY daily_hand.gain DESC LIMIT 25";
		}

		if (sql.compareTo("") != 0) {
			try {
				res = dbManager.executeQuery(sql, new Object[] {});
			} catch (SQLException e) {
				trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
			}
		}

		if (type == 2) {
			sql = "SELECT * FROM user WHERE email=\"" + email + "\"";
			try {
				res1 = dbManager.executeQuery(sql, new Object[] {});
			} catch (SQLException e) {
				trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
			}
			ISFSObject myObj = res1.getSFSObject(0);
			boolean isAdd = false;
			for (int i = 0; i < res.size(); i++) {
				ISFSObject obj = res.getSFSObject(i);
				if (obj.getLong("chip") <= myObj.getLong("chip") && !isAdd) {
					tempArray.addSFSObject(myObj);
					isAdd = true;
				}
				if (tempArray.size() < 10)
					tempArray.addSFSObject(obj);
			}
			if (!isAdd && tempArray.size() < 10)
				tempArray.addSFSObject(myObj);
			response.putSFSArray("array", tempArray);
		} else
			response.putSFSArray("array", res);
		send("get_leaderboard", response, user);
	}

	public String getCountryCode(String email) {
		IDBManager dbManager = getParentExtension().getParentZone()
				.getDBManager();
		String sql = "SELECT location FROM user WHERE email=\"" + email + "\"";
		String code = "UNKNOWN";
		try {
			ISFSArray res = dbManager.executeQuery(sql, new Object[] {});
			if (res.size() > 0) {
				String str = res.getSFSObject(0).getUtfString("location");
				if (str.compareTo("UNKNOWN") != 0)
					code = str.split(",")[1];
			}
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
		return code;
	}
}
