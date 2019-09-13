package RouletteExtension;

import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;

public class DisconnectHandler extends BaseServerEventHandler
{
	@Override
	public void handleServerEvent(ISFSEvent event) throws SFSException {
//		System.out.println("Leave");
		RoomExtension gameExt = (RoomExtension) getParentExtension();
		User user = (User) event.getParameter(SFSEventParam.USER);
		gameExt.leavePlayer(user.getName());
		if (gameExt.getParentRoom().getUserList().size() == 0) {
			gameExt.autoDeleteEmptyRoom();
		}
	}
}

