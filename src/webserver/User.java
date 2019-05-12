package src.webserver;

import java.io.Serializable;
import java.util.ArrayList;

public class User implements Serializable{
	public String name;
	public String PBKDF2_Hash;

	public ArrayList<String> roomsJoined = new ArrayList<String>();
	
	public static String getDirectory(String username) {
		return "../ttsdata/users/" + username + ".ser";
	}
	
	public String getDirectory(){
		return User.getDirectory(name);
	}
}
