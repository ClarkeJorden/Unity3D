package ZoneExtension;

import java.sql.SQLException;
import java.util.ArrayList;
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

public class PurchaseActionHandler extends BaseClientRequestHandler 
{	
	@Override
	public void handleClientRequest(User user, ISFSObject params)
	{
		int cmd = params.getInt("cmd");
		if(cmd == 0) { // welcome bonus collect
			int id = params.getInt("id");
			int type = 5;
			ISFSObject bonusInfo = getPurchaseInfo(id, user.getName(), type, 0);
			if(bonusInfo != null) {
				long dbChip = bonusInfo.getLong("value");
				long chip = params.getLong("chip");
				if(chip != dbChip)
					return;				
				long coin = params.getLong("coin");
				long ticket = params.getLong("ticket");
				updatePurchaseRecord(id, type, 2); // set status to 2
				
				ISFSObject userInfo = getUserInfo(user.getName());
				if(userInfo != null){
					long orgChip = userInfo.getLong("chip");
					long orgCoin = userInfo.getLong("coin");
					long orgTicket = userInfo.getLong("ticket");
					
					chip += orgChip;
					coin += orgCoin;
					ticket += orgTicket;
					
					updateUserInfo(user.getName(), type, chip, coin, ticket);
					
					// make user update his profile
					ISFSObject obj = new SFSObject();
					if(user != null)
						send("purchase_update", obj, user);
				}
			}
		}
		else if(cmd == 1){ // read purchase list
			ISFSArray array = params.getSFSArray("array");
			
			if(array != null) {
				String strWhere = "";
				for(int i=0; i < array.size(); i++) {
					int id = array.getInt(i);
					if(i==0)
						strWhere = "id in (" + id ;
					else
						strWhere += "," + id ;
				}
				
				if(strWhere != "") {
					strWhere += ") AND email=\"" + user.getName() + "\"";
					updatePurchaseRecords(strWhere, 1);
				}
			}
		}		
	}

	public ISFSObject getPurchaseInfo(int id, String email, int type, int status)
	{
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		String sql = "SELECT * FROM store_buy WHERE id=" + id + " AND email=\"" + email + "\" AND type=" + type + " AND status=" + status + "" ;
		try {
			ISFSArray res = dbManager.executeQuery(sql, new Object[] {});
			return res.getSFSObject(0);
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
		return null;
	}

	public void updatePurchaseRecord(int id, int type, int status)
	{
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		String sql = "UPDATE store_buy SET status=" + status + " WHERE id=" + id + " AND type=" + type;
        try {
            dbManager.executeUpdate(sql, new Object[] {});
        }
        catch (SQLException e) {
            trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
        }		
	}
	
	public void updatePurchaseRecords(String strWhere, int status)
	{
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		String sql = "UPDATE store_buy SET status=" + status + " WHERE " + strWhere;
        try {
            dbManager.executeUpdate(sql, new Object[] {});
        }
        catch (SQLException e) {
            trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
        }
	}
	
	private boolean updateUserInfo(String email, int type, long chip, long coin, long ticket)
	{	
		ISFSObject userInfo = getUserInfo(email);
		if(userInfo == null)
			return false;
		
		String sql = "";
		switch(type){
		case 5: // welcome bonus collect
			sql = "UPDATE user SET chip=" + chip + ", coin=" + coin + ", ticket=" + ticket + " WHERE email=\"" + email + "\"";	
			break;
		}
		
		if(sql.isEmpty())
			return false;
		
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
        try {
            dbManager.executeUpdate(sql, new Object[] {});
            return true;
        }
        catch (SQLException e) {
            trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
        }
        
        return false;
	}
	
	private ISFSObject getUserInfo(String email)
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
	
}