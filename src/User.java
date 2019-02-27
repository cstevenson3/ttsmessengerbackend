package src;

import java.io.Serializable;

public class User implements Serializable{
	public String name;
	public Authentication.PasswordHashSystem passwordHashSystem;
	
	public static String getDirectory(String username) {
		return "../ttsdata/users/" + username + ".ser";
	}
	
	public String getDirectory(){
		return User.getDirectory(name);
	}
}
