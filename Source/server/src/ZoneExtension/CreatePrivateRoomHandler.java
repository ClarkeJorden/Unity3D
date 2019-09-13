package ZoneExtension;

import java.util.ArrayList;
import java.util.List;

import ZoneExtension.ZoneExtension.DynamicRoomType;

import com.smartfoxserver.v2.api.CreateRoomSettings;
import com.smartfoxserver.v2.api.CreateRoomSettings.RoomExtensionSettings;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.entities.variables.RoomVariable;
import com.smartfoxserver.v2.entities.variables.SFSRoomVariable;
import com.smartfoxserver.v2.exceptions.SFSCreateRoomException;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.entities.SFSZone;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.SFSRoomRemoveMode;

public class CreatePrivateRoomHandler extends BaseClientRequestHandler
{
	@Override
	public void handleClientRequest(User user, ISFSObject params)
	{
 		String tableName = params.getUtfString("table_name");
		int blind = params.getInt("blind");
		int seat = params.getInt("size");
		boolean speed = params.getBool("speed");
		
		SFSZone zone = (SFSZone) ((ZoneExtension)getParentExtension()).getParentZone();
		ZoneExtension.mutex.lock();
		try
		{
			Room room = zone.getRoomByName(tableName);
			if(room != null && room.isDynamic())
				getApi().removeRoom(room);
	
			room = TexasPokerExtension.RoomExtension.CreateTexasRoom(getParentExtension().getParentZone(), blind, seat, speed, true, DynamicRoomType.RT_PRIVATE, tableName);
	
			ISFSObject response = new SFSObject();
			if(room != null){
				response.putBool("success", true);
				response.putUtfString("table_name", tableName);
				response.putInt("size", seat);
				send("create_private_table", response, user);
				
			} else
			{
				response.putBool("success", false);
				send("create_private_table", response, user);
			}
		}
		finally
		{
			ZoneExtension.mutex.unlock();
		}
	}
}

