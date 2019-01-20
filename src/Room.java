package src;

import java.util.ArrayList;
import java.util.List;

public class Room {
	public ArrayList<Message> messages;
	public Room(){
		messages = new ArrayList<Message>();
	}
	
	public void addMessage(Message message){
		//TODO use a better data structure and algorithm
		int compareIndex = messages.size() - 1;
		messages.add(message);
		//shuffle into time order
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
			addMessage(message);
		}
		
		for (Message output : messages){
			System.out.println(output.toString());
		}
	}
}
