package TexasPokerExtension;

import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;

public class LeaveHandler extends BaseServerEventHandler
{
	@Override
	public void handleServerEvent(ISFSEvent event) throws SFSException {
//		System.out.println("Leave");
		RoomExtension gameExt = (RoomExtension) getParentExtension();
		User user = (User) event.getParameter(SFSEventParam.USER);
		if(gameExt.leavePlayer(user.getName(), 0)) {
			if (gameExt.getParentRoom().getUserList().size() == 0) {
				if(gameExt.isPrivate()){
					getApi().removeRoom(gameExt.getParentRoom());
				}
				else{
					gameExt.autoDeleteEmptyRoom();
				}
			}
		}
	}
}

