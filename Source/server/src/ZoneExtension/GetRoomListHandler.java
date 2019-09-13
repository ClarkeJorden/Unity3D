package ZoneExtension;

import java.util.List;

import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

public class GetRoomListHandler extends BaseClientRequestHandler
{

	@Override
	public void handleClientRequest(User user, ISFSObject params)
	{
		// debug by jbj 20180904
		ZoneExtension zoneExt = (ZoneExtension)getParentExtension();
		zoneExt.whereis();
		//////////////////////////
		
		int type = 0;
		if(params.containsKey("type"))
			type = params.getInt("type");
		long blind = 0;
		if(params.containsKey("blind"))
			blind = params.getLong("blind");
		if(type == 0)
			TexasPokerExtension.RoomExtension.updateRoomList(zoneExt.getParentZone(), blind, user);
		else if(type == 2)
			RouletteExtension.RoomExtension.updateRoomList(zoneExt.getParentZone(), blind, user);
	}
}
