package src.webserver;

import java.io.Serializable;

public class Message implements Serializable{
	public long timeCreated;
	public String basicText;
	public String audioPath;
	public String displayUserName; //for a non-anonymous message (either a temp nickname or a registered user)
	public String username; //for a message from a registered user (if this is set, displayUserName will be overridden with the user's current nickname)
	
	public Message(){
		timeCreated = System.currentTimeMillis() / 1000L;
		basicText = "";
		audioPath = "";
	}
	
	public Message(long _timeCreated, String _basicText){
		timeCreated = _timeCreated;
		basicText = _basicText;
	}

	@Override
	public String toString() {
		return "Message [timeCreated=" + timeCreated + ", basicText=\n"
				+ basicText + "\n" + "audioPath=" + audioPath + "]";
	}
	
}