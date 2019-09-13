package ZoneExtension;

import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

public class UpdateGlobalInfoHandler extends BaseClientRequestHandler {
	@Override
	public void handleClientRequest(User user, ISFSObject params) {
		// debug by jbj 20180904
		ZoneExtension zoneExt = (ZoneExtension)getParentExtension();
		zoneExt.whereis();
		//////////////////////////

		int userCount = getParentExtension().getParentZone().getUserCount();
		
		ISFSObject obj = new SFSObject();
		obj.putInt("user_count", userCount);
		send("update_global_info", obj, user);
	}
}
