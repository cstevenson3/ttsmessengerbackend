package src;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class Room implements Serializable{
	
	@Override
	public String toString() {
		return "Room [displayName=" + displayName + ", urlName=" + urlName
				+ ", accessByPassword=" + accessByPassword + ", accessByURL="
				+ accessByURL + ", password=" + password + "]";
	}

	private String displayName;
	private String urlName;
	private ArrayList<Message> messages;
	private transient Semaphore editMutex;
	private ArrayList<String> users;
	private boolean accessByPassword;
	private boolean accessByURL;
	private String password;
	
	public Room(){
		editMutex = new Semaphore(1);
		messages = new ArrayList<Message>();
		users = new ArrayList<String>();
	}
	
	private void acquireMutex(Semaphore mutex){
		if (mutex == null){
			mutex = new Semaphore(1);
		}
		try {
			mutex.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void releaseMutex(Semaphore mutex){
		if (mutex == null){
			mutex = new Semaphore(1);
			return;
		}
		mutex.release();
	}
	
	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		acquireMutex(editMutex);
		this.displayName = displayName;
		releaseMutex(editMutex);
	}

	public String getUrlName() {
		return urlName;
	}

	public void setUrlName(String urlName) {
		acquireMutex(editMutex);
		this.urlName = urlName;
		releaseMutex(editMutex);
	}
	
	public boolean userAllowed(String username){
		return users.contains(username);
	}
	
	public boolean accessAllowed(String password, String user){
		//ßSystem.out.println(this.toString());
		
		if(accessByURL){
			return true;
		}else{
			if(accessByPassword){
				if(password == null){
					
				}else{
					if(password.equals(this.password)){
						return true;
					}
				}
			}
			if (users.contains(user)){
				return true;
			}
		}
		return false;
	}

	public Message getMessage(int index){
		if (index >= messages.size()){
			return null;
		}else{
			return messages.get(index);
		}
	}
	
	public String getDirectory(){
		return Room.getDirectory(this.urlName);
	}
	
	public synchronized void addMessage(Message message, SampledAudio sound){
		
		acquireMutex(editMutex);
		
		int index = messages.size();
		String internalPath = "../ttsmessenger/audio/" + urlName + "/" + index + ".wav"; //TODO use file routing system for this
		sound.writeToWav(internalPath);
		String webPath = "audio/" + urlName + "/" + index + ".wav";
		message.audioPath = webPath;
		messages.add(message);
		
		releaseMutex(editMutex);
		
		//TODO use a better data structure and algorithm

		//shuffle into time order
		/*
		while (compareIndex >= 0){
			if (message.timeCreated >= messages.get(compareIndex).timeCreated){
				break;
			}else{
				//switch compareIndex and compareIndex + 1
				Message temp = messages.get(compareIndex);
				messages.set(compareIndex, messages.get(compareIndex + 1));
				messages.set(compareIndex + 1, temp);
			}
			compareIndex --;
		}
		*/
		
	}
	
	public void writeToFile(){
		acquireMutex(editMutex);
		VirtualFileSystem.store(getDirectory(), this);
		releaseMutex(editMutex);
	}
	
	
	
	public void test(){
		Message message1 = new Message(5, "5.1");
		Message message2 = new Message(6, "6.1");
		Message message3 = new Message(8, "8.1");
		Message message4 = new Message(7, "7.1");
		Message message5 = new Message(6, "6.2");
		Message message6 = new Message(5, "5.2");
		
		Message[] testMessages = new Message[]{message1, message2, message3, message4, message5, message6};
		
		for (Message message : testMessages){
			//addMessage(message);
		}
		
		for (Message output : messages){
			System.out.println(output.toString());
		}
	}

	public void setAccessByURL(boolean accessByURL) {
		this.accessByURL = accessByURL;
	}

	public void setAccessByPassword(boolean accessByPassword) {
		this.accessByPassword = accessByPassword;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public static String getDirectory(String urlName) {
		try {
			return RequestHandler.WEB_ROOT.getCanonicalPath() + "/audio/" + urlName + "/" + urlName + ".ser";
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
