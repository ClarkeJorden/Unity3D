package ZoneExtension;

import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

public class InviteFriendHandler extends BaseClientRequestHandler
{

	@Override
	public void handleClientRequest(User user, ISFSObject params)
	{
		// debug by jbj 20180904
		ZoneExtension zoneExt = (ZoneExtension)getParentExtension();
		zoneExt.whereis();
		//////////////////////////

		
		String friend_email = params.getUtfString("friend_email");

		User friend = getParentExtension().getParentZone().getUserByName(friend_email);
		if(friend != null)
			send("invite_friend", params, friend);
	}
}
