package src.webserver;

import java.util.HashMap;

public class ServerState {
	private static HashMap<String, Room> rooms;
	
	static{
		rooms = new HashMap<String, Room>();
	}
	
	public static boolean roomExists(String urlName){
		if(rooms.containsKey(urlName)){
			return true;
		}
		if(VirtualFileSystem.fileExists(Room.getDirectory(urlName))){
			return true;
		}
		return false;
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

	public static void createRoom(String urlName, String username){
		Room room = new Room();
		room.setUrlName(urlName);
		room.setDisplayName("New room");
		room.addFirstUser(username);
		rooms.put(urlName, room);
		room.writeToFile();
	}

	public static void clearLocalStorage(){
		VirtualFileSystem.deleteDirectory("../ttsmessenger/rooms/");
		VirtualFileSystem.deleteDirectory("../ttsdata/users/");
		VirtualFileSystem.deleteDirectory("../ttsdata/ttscache");
	}
}