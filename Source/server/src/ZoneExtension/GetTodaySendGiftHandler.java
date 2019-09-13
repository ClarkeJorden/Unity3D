package ZoneExtension;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.math.BigDecimal;

import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;

public class GetTodaySendGiftHandler extends BaseClientRequestHandler
{	
	@Override
	public void handleClientRequest(User user, ISFSObject params)
	{
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();

		long curTime = System.currentTimeMillis();
		Date curDate = new Date(curTime);
		Date beginDate = new Date(curDate.getYear(), curDate.getMonth(), curDate.getDate(), 0, 0, 0);
		Date endDate = new Date(curDate.getYear(), curDate.getMonth(), curDate.getDate() + 1, 0, 0, 0);
		SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		String beginDateStr = dateFormatGmt.format(beginDate);
		String endDateStr = dateFormatGmt.format(endDate);
		
		int type = params.getInt("type");
		String sql = "SELECT * FROM transfer_gift WHERE from_email=\"" + user.getName()
					+ "\" AND (time>\"" + beginDateStr + "\" OR time=\"" + beginDateStr
					+ "\") AND time<\"" + endDateStr
					+ "\" AND type=\"" + type + "\"";
		try {
			ISFSObject response = new SFSObject();
			ISFSArray res = dbManager.executeQuery(sql, new Object[] {});
			BigDecimal total = BigDecimal.ZERO;
			for(int i = 0; i < res.size(); i ++) {
				ISFSObject obj = res.getSFSObject(i);
				total = total.add(BigDecimal.valueOf(obj.getLong("value")));
			}
			response.putInt("type", type);
			response.putLong("value", total.longValue());
			send("get_today_send_gift", response, user);
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
	}	
}
