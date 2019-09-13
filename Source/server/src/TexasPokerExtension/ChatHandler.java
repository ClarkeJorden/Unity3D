package TexasPokerExtension;

import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

public class ChatHandler extends BaseClientRequestHandler
{
	@Override
	public void handleClientRequest(User user, ISFSObject params)
	{
		RoomExtension gameExt = (RoomExtension) getParentExtension();
		if (user.isPlayer()) {
			gameExt.sendMessage(params.getUtfString("email"), params.getUtfString("name"), params.getUtfString("message"), false);
		}
	}
}
