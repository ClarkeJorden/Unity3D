package ZoneExtension;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;

class UserInfo
{
	public String name;
	public String email;
}

public class GetFriendRoomHandler extends BaseClientRequestHandler
{

	@Override
	public void handleClientRequest(User user, ISFSObject params)
	{
		// debug by jbj 20180904
		ZoneExtension zoneExt = (ZoneExtension)getParentExtension();
		zoneExt.whereis();
		//////////////////////////
		int cmd = params.getInt("cmd");
		int gameType = 0;
		String friendEmail = "";
		if(cmd == 0){ // all, email's friend first
			friendEmail = params.getUtfString("email");
			gameType = GetUserGameType(friendEmail);
			if(gameType < 0)
				return;
		}
		else if(cmd == 1){ // all with game type
			gameType = params.getInt("game_type");
		}
		
		ISFSArray allFriends = GetAllFriends(user.getName());
		List<ISFSObject> friendRoomList = GetFriendRoomArray(gameType, allFriends);
		
		// sort friend room list by min_buyin
		friendRoomList.sort(new Comparator<ISFSObject>() {
			@Override
			public int compare(ISFSObject o1, ISFSObject o2) {
				long min1 = o1.getLong("min_buyin");
				long min2 = o2.getLong("min_buyin");
				
				if(min1 > min2)
					return 1;
				else if(min1 < min2)
					return -1;
				else
					return 0;
			}
		});
		
		// if cmd is 0, email's room first in room list
		if(cmd == 0){
			int friendRoomIndex = -1;
			for(int i=0; i<friendRoomList.size(); i++){
				ISFSObject roomInfo = friendRoomList.get(i);
				ISFSArray players = roomInfo.getSFSArray("player_list");
				if(IsEmailExist(friendEmail, players)) {
					friendRoomIndex = i;
					break;
				}
			}
			
			if(friendRoomIndex != -1){
				ISFSObject friendRoom = friendRoomList.get(friendRoomIndex);
				friendRoomList.remove(friendRoomIndex);
				friendRoomList.add(0, friendRoom);
			}
		}
		
		ISFSObject response = new SFSObject();
		ISFSArray roomArray = new SFSArray();
		ISFSArray friendArray = new SFSArray();
		
		// if cmd is 0, email's friend first in friend list		
		if(cmd == 0){
			ISFSObject friend = GetElementByEmail(friendEmail, allFriends);
			if(friend != null)
				friendArray.addSFSObject(friend);
		}
		
		for(ISFSObject room: friendRoomList){
			roomArray.addSFSObject(room);
			
			ISFSArray playerList = room.getSFSArray("player_list");
			for(int i = 0; i < playerList.size(); i ++) {
				ISFSObject player = playerList.getSFSObject(i);
				String playerEmail = player.getUtfString("email");
				ISFSObject friendInfo = GetElementByEmail(playerEmail, allFriends);
				if(friendInfo != null){
					if(!IsEmailExist(playerEmail, friendArray))
						friendArray.addSFSObject(friendInfo);
				}
			}			
		}
		
		response.putInt("type", gameType);
		response.putSFSArray("room_array", roomArray);
		response.putSFSArray("friend_array", friendArray);
		
		send("get_friend_room", response, user);
		return;
	}
	
	private int GetUserGameType(String email)
	{
		ZoneExtension.mutex.lock();
		try
		{
			List<Room> roomList = getParentExtension().getParentZone().getRoomList();
			for(Room room : roomList) {
				if(!room.isGame() || room.getGroupId().compareTo("default") == 0)
					continue;
				ISFSObject obj = (ISFSObject) room.getExtension().handleInternalMessage("get_room_info", null);
				if(obj == null)
					continue;		
				ISFSArray playerList = obj.getSFSArray("player_list");
				if(IsEmailExist(email, playerList)){
					return obj.getInt("type");
				}
			}
		}
		finally
		{
			ZoneExtension.mutex.unlock();
		}
		
		return -1;
	}
	
	private List<ISFSObject> GetFriendRoomArray(int gameType, ISFSArray friendArray)
	{
		List<ISFSObject> roomArray = new ArrayList<>();		
		ZoneExtension.mutex.lock();
		try
		{
			List<Room> roomList = getParentExtension().getParentZone().getRoomList();
			for(Room room : roomList) {
				if(!room.isGame() || room.getGroupId().compareTo("default") == 0)
					continue;
				ISFSObject obj = (ISFSObject) room.getExtension().handleInternalMessage("get_room_info", null);
				if(obj == null)
					continue;
				if(obj.getInt("type") != gameType)
					continue;
				ISFSArray playerList = obj.getSFSArray("player_list");
				for(int i = 0; i < playerList.size(); i ++) {
					ISFSObject player = playerList.getSFSObject(i);
					String playerEmail = player.getUtfString("email");
					if(IsEmailExist(playerEmail, friendArray)){
						roomArray.add(obj);
						break;
					}
				}
			}
		}
		finally
		{
			ZoneExtension.mutex.unlock();
		}

		return roomArray;
	}
	
	private boolean IsEmailExist(String email, ISFSArray array)
	{
		if(GetElementByEmail(email, array) == null)
			return false;
		
		return true;
	}
	
	private ISFSObject GetElementByEmail(String email, ISFSArray array)
	{
		if(array == null)
			return null;
		
		int size = array.size();
		for(int i=0; i<size; i++) {
			ISFSObject obj = array.getSFSObject(i);
			String objEmail = obj.getUtfString("email");
			if(objEmail.compareTo(email) == 0)
				return obj;
		}
		
		return null;
	}
	
	private ISFSArray GetAllFriends(String email)
	{
		ISFSArray res = null;
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		String sql = "SELECT friend.email AS email, user.name AS name FROM friend INNER JOIN user ON friend.email=user.email WHERE friend.friend_email=\"" + email + "\"";
		try {
			res = dbManager.executeQuery(sql, new Object[] {});
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
			res = null;
		}
		
		return res;		
	}
}
