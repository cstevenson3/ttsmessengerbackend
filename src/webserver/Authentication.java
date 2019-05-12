package src.webserver;

import src.webserver.PasswordHashing.CannotPerformOperationException;
import src.webserver.PasswordHashing.InvalidHashException;

public class Authentication {	
	public class UserAlreadyExistsException extends Exception{
		
	}
	
	
	
	public static void addUser(String name, String password) throws UserAlreadyExistsException{
		//check if user already exists
		if (VirtualFileSystem.fileExists(User.getDirectory(name))){
			throw new Authentication().new UserAlreadyExistsException();
		}else{
			//make user
			User user = new User();
			user.name = name;
			
			String hash = null;
			try {
				hash = PasswordHashing.createHash(password);
			} catch (CannotPerformOperationException e) {
				e.printStackTrace();
			}
			user.PBKDF2_Hash = hash;
			VirtualFileSystem.store(user.getDirectory(), user);
			System.out.println("Added user: " + user.name);
		}
	}
	
	public static boolean verifyPassword(String username, String password){
		User user = (User) VirtualFileSystem.retrieve(User.getDirectory(username));
		boolean success = false;
		try {
			success = PasswordHashing.verifyPassword(password, user.PBKDF2_Hash);
		} catch (CannotPerformOperationException | InvalidHashException e) {
			e.printStackTrace();
		}
		return success;
	}
}
