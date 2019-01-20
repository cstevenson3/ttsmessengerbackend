package src;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.ServerSocket;
import java.util.Base64;

public class HTTPServer {

	static final int PORT = 80;
	
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
	
	public static void testReader(){
		String test = "test1\n\ntest2";
		Reader inputString = new StringReader(test);
		BufferedReader reader = new BufferedReader(inputString);
		try {
			System.out.println(reader.readLine());
			String middleLine = reader.readLine();
			if (middleLine.isEmpty()){
				System.out.println("works");
			}
			System.out.println(reader.readLine());
			System.out.println(reader.readLine());
			System.out.println(reader.readLine());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void bytesStringTest(){
		String input = "abc\ndef\rghi\r\njkl";
		byte[] inputBytes = input.getBytes();
		System.out.println(new String(inputBytes));
	}
	
	public static void main(String[] args) {
		//bytesStringTest();
		//testReader();
		server();
		//Room room = new Room();
		//room.test();
	}
}
