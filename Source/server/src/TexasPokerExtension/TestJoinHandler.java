package TexasPokerExtension;

import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;

public class TestJoinHandler extends BaseServerEventHandler
{
	@Override
	public void handleServerEvent(ISFSEvent event) throws SFSException {
		TestExtension gameExt = (TestExtension) getParentExtension();
		User user = (User) event.getParameter(SFSEventParam.USER);
		System.out.println(gameExt.getParentRoom().getName() + ": " + user.getName() + " joined room");
	}
}

