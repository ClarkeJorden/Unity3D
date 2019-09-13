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

public class GetGiftTransferListHandler extends BaseClientRequestHandler
{	
	@Override
	public void handleClientRequest(User user, ISFSObject params)
	{
		ISFSObject obj = new SFSObject();
		int type = params.getInt("type");
		String device_id = params.getUtfString("device_id");
		
		long curTime = System.currentTimeMillis();
		Date curDate = new Date(curTime);
		Date beginDate = new Date(curDate.getYear(), curDate.getMonth(), curDate.getDate(), 0, 0, 0);
		Date endDate = new Date(curDate.getYear(), curDate.getMonth(), curDate.getDate() + 1, 0, 0, 0);
		SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		String beginDateStr = dateFormatGmt.format(beginDate);
		String endDateStr = dateFormatGmt.format(endDate);
			
		ISFSArray res = getTransferGiftList(user.getName(), device_id, type, beginDateStr, endDateStr); 
		obj.putSFSArray("array", res);
		
		send("get_gift_transfer_list", obj, user);
	}
	
	public ISFSArray getTransferGiftList(String email, String device_id, int type, String beginDateStr, String endDateStr)
	{
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();

		String sql = "SELECT transfer_gift.id as id, type, value, status, time, user.email as email, user.name as name FROM transfer_gift INNER JOIN user ON transfer_gift.from_email=user.email"
					+ " WHERE transfer_gift.to_email=\"" + email + "\""
					+ " AND (transfer_gift.to_device_id=\"" + device_id + "\" OR (transfer_gift.status=0 AND (transfer_gift.to_device_id IS NULL OR transfer_gift.to_device_id='')))"
					+ " AND transfer_gift.type=" + type 
					+ " AND (time>\"" + beginDateStr + "\" OR time=\"" + beginDateStr + "\") AND time<\"" + endDateStr + "\""
					+ " ORDER BY transfer_gift.time ASC";
		try {
			ISFSArray res = dbManager.executeQuery(sql, new Object[] {});
			return res;
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
		return new SFSArray();		
	}
}