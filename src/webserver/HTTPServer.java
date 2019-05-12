package src.webserver;

import java.io.IOException;
import java.net.ServerSocket;

public class HTTPServer {

	static final int PORT = 25565;
	
	public static void server(){
		ServerSocket serverConnect = null;
		try {
			serverConnect = new ServerSocket(PORT);
			System.out.println("Server started.\nListening for connections on port : " + PORT + " ...\n");
			
			while (true) {
				RequestHandler handler = new RequestHandler(serverConnect.accept());
				Thread thread = new Thread(handler);
				thread.start();
			}
			
		} catch (IOException e) {
			System.err.println("Server Connection error : " + e.getMessage());
		} finally {
			try {
				serverConnect.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) {
		
		ServerState.clearLocalStorage();
		ServerState.createRoom("welcome");
		ServerState.retrieveRoom("welcome").setAccessByURL(true);
		server();
	}
}
