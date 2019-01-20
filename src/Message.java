package src;

public class Message {
	public long timeCreated;
	public String basicText;
	
	public Message(){
		timeCreated = System.currentTimeMillis() / 1000L;
		basicText = "";
	}
	
	public Message(long _timeCreated, String _basicText){
		timeCreated = _timeCreated;
		basicText = _basicText;
	}

	@Override
	public String toString() {
		return "Message [timeCreated=" + timeCreated + ", basicText=\n"
				+ basicText + "]";
	}
	
}