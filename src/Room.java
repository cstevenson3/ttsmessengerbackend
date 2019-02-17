package src;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class Room implements Serializable{
	
	private String displayName;
	private String urlName;
	private ArrayList<Message> messages;
	private transient Semaphore editMutex;
	
	public Room(){
		editMutex = new Semaphore(1);
		messages = new ArrayList<Message>();
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

	public Message getMessage(int index){
		if (index >= messages.size()){
			return null;
		}else{
			return messages.get(index);
		}
	}
	
	public String getDirectory(){
		try {
			return RequestHandler.WEB_ROOT.getCanonicalPath() + "/audio/" + urlName + "/" + urlName + ".ser";
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
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
	
	@Override
	public String toString(){
		String result = "Room:\n";
		for (Message output : messages){
			result += ((output.toString()) + "\n");
		}
		result += "End Room\n";
		return result;
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
}
