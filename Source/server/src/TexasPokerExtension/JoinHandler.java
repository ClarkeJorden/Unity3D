package TexasPokerExtension;

import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

public class JoinHandler extends BaseClientRequestHandler
{
	@Override
	public void handleClientRequest(User user, ISFSObject params)
	{
		RoomExtension gameExt = (RoomExtension) getParentExtension();
		if (user.isPlayer()) {
			if(gameExt.joinNewPlayer(params))
				gameExt.autoCreateTexasRooms();
//			gameExt.sendChipStatus(user.getName());
		}
	}
}
