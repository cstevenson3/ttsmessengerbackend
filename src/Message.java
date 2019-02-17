package src;

import java.io.Serializable;

public class Message implements Serializable{
	public long timeCreated;
	public String basicText;
	public String audioPath;
	
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