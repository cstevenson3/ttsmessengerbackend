package src;

import java.util.HashMap;

public class ServerState {
	private static HashMap<String, Room> rooms;
	
	static{
		rooms = new HashMap<String, Room>();
	}
	
	public static Room retrieveRoom(String urlName){
		if(rooms.containsKey(urlName)){
			return rooms.get(urlName);
		}else{
			//try filesystem
			Room room = new Room();
			room.setUrlName(urlName);
			String directory = room.getDirectory();
			Room roomFile = (Room) VirtualFileSystem.retrieve(directory);
			if(roomFile == null){
				rooms.put(urlName, room);
				VirtualFileSystem.store(directory, room);
			}else{
				rooms.put(urlName, roomFile);
			}
			return rooms.get(urlName);
		}
	}
	
	public static void saveRoom(String urlName){
		if(rooms.containsKey(urlName)){
			rooms.get(urlName).writeToFile();
		}
	}
	
	public static void createRoom(String urlName){
		Room room = new Room();
		room.setUrlName(urlName);
		room.setDisplayName("New room");
		rooms.put(urlName, room);
		room.writeToFile();
	}
}
