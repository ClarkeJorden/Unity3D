package RouletteExtension;

import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

public class ActionHandler extends BaseClientRequestHandler
{
	@Override
	public void handleClientRequest(User user, ISFSObject params)
	{
		RoomExtension gameExt = (RoomExtension) getParentExtension();
		if (user.isPlayer()) {
			gameExt.getAction(params);
//			gameExt.actionArray.addSFSObject(params);
//			if(!gameExt.isAction)
//				gameExt.processAction();
//			else
//				System.out.println("busy: " + gameExt.actionArray.size());
		}
	}
}
