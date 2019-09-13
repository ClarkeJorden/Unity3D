package TexasPokerExtension;

import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.extensions.SFSExtension;

public class TestExtension extends SFSExtension {

	@Override
	public void init() {
		addEventHandler(SFSEventType.USER_JOIN_ROOM, TestJoinHandler.class);
		addEventHandler(SFSEventType.USER_LEAVE_ROOM, TestLeaveHandler.class);
		addEventHandler(SFSEventType.USER_DISCONNECT, TestLeaveHandler.class);

		addRequestHandler("get_test_room_name", TestRoomNameHandler.class);
	}
}
