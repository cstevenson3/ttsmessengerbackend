package src;

import java.io.Serializable;

import src.PasswordHashing.CannotPerformOperationException;
import src.PasswordHashing.InvalidHashException;

public class Authentication {

	public class PasswordHashSystem implements Serializable{
		public PasswordHashFunction function;
		public String hash;
	}
	public enum PasswordHashFunction {
		PBKDF2
	}
	
	public class UserAlreadyExistsException extends Exception{
		
	}
	
	
	
	public void addUser(String name, String password) throws UserAlreadyExistsException{
		//check if user already exists
		if (VirtualFileSystem.fileExists(User.getDirectory(name))){
			throw new UserAlreadyExistsException();
		}else{
			//make user
			User user = new User();
			user.name = name;
			
			PasswordHashSystem pbkdf2 = new PasswordHashSystem();
			pbkdf2.function = PasswordHashFunction.PBKDF2;
			try {
				pbkdf2.hash = PasswordHashing.createHash(password);
			} catch (CannotPerformOperationException e) {
				e.printStackTrace();
			}
			user.passwordHashSystem = pbkdf2;
			VirtualFileSystem.store(user.getDirectory(), user);
		}
	}
	
	public boolean verifyPassword(String username, String password){
		User user = (User) VirtualFileSystem.retrieve(User.getDirectory(username));
		boolean success = false;
		try {
			success = PasswordHashing.verifyPassword(password, user.passwordHashSystem.hash);
		} catch (CannotPerformOperationException | InvalidHashException e) {
			e.printStackTrace();
		}
		return success;
	}
}
