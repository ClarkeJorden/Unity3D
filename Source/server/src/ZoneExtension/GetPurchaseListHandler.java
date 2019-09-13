package ZoneExtension;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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


public class GetPurchaseListHandler extends BaseClientRequestHandler {
	public void handleClientRequest(User user, ISFSObject params)
	{
		ISFSObject obj = new SFSObject();
		
		String device_id = params.getUtfString("device_id");
		long curTime = System.currentTimeMillis();
		Date now = new Date(curTime);
		Date today = new Date(now.getYear(), now.getMonth(), now.getDate());
		
		Calendar cal = Calendar.getInstance();	
		cal.setTime(today);
		cal.add(Calendar.DATE, -14);
		long twoWeeksAgo = cal.getTimeInMillis();
				
		ISFSArray res0 = getPurchaseList(user.getName(), device_id, twoWeeksAgo, curTime);
		for(int i=0; i<res0.size(); i++){
			ISFSObject obj0 = res0.getSFSObject(i);
			if(obj0.getInt("type")== 5) { // bonus for first purchase
				obj0.putLong("coin", 2000); // 2K coins
				obj0.putLong("ticket", 10); // 10 lottery tickets
				int status = obj0.getInt("status");
				if(status == 2){
					obj0.removeElement("status");
					obj0.putInt("status", 0);
				}
			}
		}
		obj.putSFSArray("array0", res0);
		ISFSArray res1 = getAwardList(user.getName(), twoWeeksAgo, curTime); 
		obj.putSFSArray("array1", res1);
		
		ISFSArray res2 = getWelcomeBonusList(user.getName(), twoWeeksAgo, curTime);
		for(int i=0; i<res2.size(); i++){
			ISFSObject obj2 = res2.getSFSObject(i);
			obj2.putLong("coin", 2000); // 2K coins
			obj2.putLong("ticket", 10); // 10 lottery tickets
		}		
		obj.putSFSArray("array2", res2);
		
		send("get_purchase_list", obj, user);
	}
	
	public ISFSArray getPurchaseList(String email, String device_id, long begin, long end)
	{
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();

		String sql = "SELECT id, type, value, status, created_at AS time FROM store_buy WHERE (type=0 OR type=1 OR (type=5 AND status>0))";
			
		sql += 	" AND email=\"" + email + "\""
				+ " AND device_id=\"" + device_id + "\""
				+ " AND (created_at>" + begin + " OR created_at=" + begin + ")"
				+ " AND created_at<" + end
				+ " ORDER BY created_at DESC";
		try {
			ISFSArray res = dbManager.executeQuery(sql, new Object[] {});
			return res;
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
		return new SFSArray();		
	}
	
	public ISFSArray getAwardList(String email, long begin, long end)
	{
		return new SFSArray();
	}
	
	public ISFSArray getWelcomeBonusList(String email, long begin, long end)
	{
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();

		String sql = "SELECT id, type, value, status, created_at AS time FROM store_buy WHERE type=5 AND status=0";
			
		sql += 	" AND email=\"" + email + "\""
				+ " AND (created_at>" + begin + " OR created_at=" + begin + ")"
				+ " AND created_at<" + end
				+ " ORDER BY created_at DESC";
		try {
			ISFSArray res = dbManager.executeQuery(sql, new Object[] {});
			return res;
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
		return new SFSArray();		
	}	
}
