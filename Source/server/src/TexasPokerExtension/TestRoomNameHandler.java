package TexasPokerExtension;

import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;

public class TestRoomNameHandler extends BaseClientRequestHandler
{
	@Override
	public void handleClientRequest(User user, ISFSObject params)
	{
		TestExtension gameExt = (TestExtension) getParentExtension();
		if (user.isPlayer()) {
			ISFSObject obj = new SFSObject();
			obj.putUtfString("name", gameExt.getParentRoom().getName());
			send("get_test_room_name", obj, user);
			
			System.out.println("Get Room Name Request:" + gameExt.getParentRoom().getName());
			
			new Thread(new Runnable() {
			     public void run() {
					while(true)
					{
					}
			     }
			}).start();
		}
	}
}

