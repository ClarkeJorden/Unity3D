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

public class TransferActionHandler extends BaseClientRequestHandler 
{	
	@Override
	public void handleClientRequest(User user, ISFSObject params)
	{
		List<User> userList = new ArrayList<>();	
		ISFSArray array = params.getSFSArray("array");
		String device_id = params.getUtfString("device_id");
		if(array != null) {
			boolean bFirst = true;
			String strWhere = "";
			for(int i=0; i < array.size(); i++) {
				int id = array.getInt(i);
				ISFSObject transferInfo = getTransferInfo(id);
				if(transferInfo != null) {
					if(bFirst)
						strWhere = "id in (" + id ;
					else
						strWhere += "," + id ;
					bFirst = false;
					
					int type = transferInfo.getInt("type");
					String from_email = transferInfo.getUtfString("from_email");
					String to_email = transferInfo.getUtfString("to_email");
					
					if(type == 0) { // chip
						User user1 = getParentExtension().getParentZone().getUserByName(from_email);
						if(user1 != null && userList.contains(user1) == false)
							userList.add(user1);
					}				
				}				
			}
			
			if(strWhere != "") {
				strWhere += ")";
				
				updateTransferRecord(strWhere, device_id, 1);
				
				if(!userList.isEmpty()) {
					ISFSObject obj = new SFSObject();
					obj.putInt("type", 0);
					send("transfer_update", obj, userList);
				}
				
			}
			
		}
	}

	public ISFSObject getTransferInfo(int id)
	{
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		String sql = "SELECT * FROM transfer_gift WHERE id=\"" + id + "\"";
		try {
			ISFSArray res = dbManager.executeQuery(sql, new Object[] {});
			return res.getSFSObject(0);
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
		return null;
	}
		
	public void updateTransferRecord(String strWhere, String device_id, int status)
	{
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		String sql = "UPDATE transfer_gift SET status=" + status + ", to_device_id=\"" + device_id + "\" WHERE " + strWhere;
        try {
            dbManager.executeUpdate(sql, new Object[] {});
        }
        catch (SQLException e) {
            trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
        }
	}	
}