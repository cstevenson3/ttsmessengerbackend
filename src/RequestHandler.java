package src;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import javax.swing.text.DefaultStyledDocument.ElementSpec;
import javax.xml.ws.http.HTTPException;

import src.Authentication.UserAlreadyExistsException;
import src.SampleEffect.EffectName;

public class RequestHandler implements Runnable{
	
	private Socket connection;
	
	static final File WEB_ROOT = new File("../ttsmessenger/");
	static final String DEFAULT_FILE = "index.html";
	
	final String lineSeparatorRegex = "\r\n?|\n";
	
	static Room testRoom = new Room();
	
	private String verboseOutput;
	
	public RequestHandler(Socket c){
		connection = c;
		verboseOutput = "";
	}

	private String contentType(String fileRequested){
		if (fileRequested.endsWith(".htm")  ||  fileRequested.endsWith(".html"))
			return "text/html";
		else if (fileRequested.endsWith(".css"))
			return "text/css";
		else if (fileRequested.endsWith(".js"))
			return "application/javascript";
		else if (fileRequested.endsWith(".ico"))
			return "image/x-icon";
		else if (fileRequested.endsWith(".png"))
			return "image/png";
		else if (fileRequested.endsWith(".wav"))
			return "audio/wav";
		else
			return "text/plain";
		
	}
	
	private byte[] readFileData(File file, int fileLength) throws IOException {
		if (pathInWebRoot(file.getPath())){
			FileInputStream fileIn = null;
			byte[] fileData = new byte[fileLength];
			
			try {
				fileIn = new FileInputStream(file);
				fileIn.read(fileData);
			} finally {
				if (fileIn != null) 
					fileIn.close();
			}
			
			return fileData;
		}else{
			throw new HTTPException(403);
		}
	}
	
	public boolean pathInWebRoot(String path) throws IOException{
		//check path isn't trying to escape the web root
		String canonicalRootPath = WEB_ROOT.getCanonicalPath();
		
		File file = new File(path);
		String canonicalPath = file.getCanonicalPath();
		return canonicalPath.startsWith(canonicalRootPath);
	}

	
	public String readFile(String path) throws Exception {
		if (pathInWebRoot(path)){
			byte[] encoded = Files.readAllBytes(Paths.get(path));
			
			//test if encoded string is same as bytes
			String result = new String(encoded);
			
			verboseOutput += ("byte[] to String and back is same as original: " + ((result.getBytes()==encoded)?"true":"false") + "\n");
			
			return result;
		}else{
			throw new HTTPException(403);
		}
	}
	
	private HttpResponse handleGetRequest(HttpRequest request){
		String fileRequested = request.fileRequested;
		if (fileRequested.endsWith("/")) {
			fileRequested += DEFAULT_FILE;
		}
		
		verboseOutput += ("Attempting to load file from " + fileRequested + "\n");
		String contentType = contentType(fileRequested);
		verboseOutput += ("File has type " + contentType + "\n");

		HttpResponse response = new HttpResponse();
		
		try {
			//String fileBody = readFile(WEB_ROOT + fileRequested);
			//response.body = fileBody;
			File file = new File(WEB_ROOT, fileRequested);
			
			byte[] fileBody = readFileData(file, (int)file.length());
			response.fileBody = fileBody;
			response.setCode(200);
			response.httpStatusMessage = "OK";
		} catch (Exception e) {
			if (e instanceof HTTPException){
				HTTPException ex = ((HTTPException) e);
				int code = ex.getStatusCode();
				response.setCode(code);
				response.autofillMessage();
			}
			e.printStackTrace();
		}
		
		response.addHeader("Server", "cstevenson3");
		response.addHeader("Date", (new Date()).toString());
		response.addHeader("Content-type", contentType);
		
		verboseOutput += ("File " + fileRequested + " of type " + contentType + " will be returned\n");
		
		return response;
	}
	
	private void createMessage(HttpRequest request, HttpResponse response){
		String roomName = request.headers.getProperty("Room");
		Room room = null;
		room = ServerState.retrieveRoom(roomName);
		
		String username = request.headers.getProperty("User");
		String roomPassword = request.headers.getProperty("Room-Password");
		
		if(room.accessAllowed(roomPassword, username)){
			Message message = new Message();
			message.basicText = request.body;
			message.username = username;
			SampledAudio output = null;

			try {
				output = TtsWithEffects.ttsWithEffects(message.basicText);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			room.addMessage(message, output);
			room.writeToFile();

			response.setCode(200);
			response.httpStatusMessage = "success";

			verboseOutput += "Room: " + roomName +  "updated, is now \n";
			verboseOutput += room.toString();
		}else{
			response.setCode(401);
			response.httpStatusMessage = "room access denied";
		}
	}
	
	private void createRoom(HttpRequest request, HttpResponse response){
		
		String roomName = request.headers.getProperty("Room");
		String roomPassword = request.headers.getProperty("Room-Password");
		String username = request.headers.getProperty("User");

		String accessByURLString = request.headers.getProperty("Room-Access-By-URL");
		String accessByPasswordString = request.headers.getProperty("Room-Access-By-Password");

		response.addHeader("User", username);
		response.addHeader("Room", roomName);
		response.addHeader("Room-Password", roomPassword);
		response.addHeader("Room-Access-By-URL", accessByURLString);
		response.addHeader("Room-Access-By-Password", accessByPasswordString);
		
		if(ServerState.roomExists(roomName)){
			response.setCode(401);
			response.httpStatusMessage = "room already exists";
		}else{
			if(username == null || username.equals("")){
				ServerState.createRoom(roomName);
			}else{
				ServerState.createRoom(roomName, username);
			}
			
			//optional settings
			Room room = ServerState.retrieveRoom(roomName);
			
			boolean accessByURL = Boolean.parseBoolean(accessByURLString);
			room.setAccessByURL(accessByURL);
			
			boolean accessByPassword = Boolean.parseBoolean(accessByPasswordString);
			room.setAccessByPassword(accessByPassword);
			room.setPassword(roomPassword);
			
			response.setCode(200);
			response.httpStatusMessage = "success";
		}
	}

	private void createUser(HttpRequest request, HttpResponse response){
		String username = request.headers.getProperty("New-User");
		String password = request.headers.getProperty("User-Password");
		try{
			Authentication.addUser(username, password);
			response.setCode(200);
			response.httpStatusMessage = "success";
			response.addHeader("User", username);
			response.addHeader("User-Password", password);
		}catch(UserAlreadyExistsException e){
			response.setCode(401);
			response.httpStatusMessage = "user already exists";
		}
	}

	private void addUserToRoom(HttpRequest request, HttpResponse response){
		String roomString = request.headers.getProperty("Room");
		String userString = request.headers.getProperty("User");
		String newUser = request.headers.getProperty("newUser");
		Room room = ServerState.retrieveRoom(roomString);
		try{
			room.addUser(userString, newUser);
			response.setCode(200);
			response.httpStatusMessage = "OK Room " + roomString + " accepted adding user " + newUser;
		}catch(Exception e){
			response.setCode(401);
			response.httpStatusMessage = "Room " + roomString + " denied adding user " + newUser;
		}
	}
	
	private void getMessage(HttpRequest request, HttpResponse response){
		String roomString = request.headers.getProperty("Room");
		if (roomString == null){
			response.setCode(400);
			verboseOutput += "getMessage Room header not found\n";
			response.httpStatusMessage = "getMessage Room header not found";
		}else{

			Room room = null;
			room = ServerState.retrieveRoom(roomString);
			String user = request.headers.getProperty("User");
			String roomPassword = request.headers.getProperty("Room-Password");
			//System.out.println(roomPassword);
			if(!room.accessAllowed(roomPassword, user)){
				response.setCode(401);
				response.httpStatusMessage = "forbidden";
				response.headers.setProperty("Get-Message-Response", "forbidden");
				return;
			}
					
			String messageIndexString = request.headers.getProperty("Message-Index");
			if (messageIndexString == null){
				//return 400
				response.setCode(400);
				verboseOutput += "getMessage Message-Index header not found\n";
				response.httpStatusMessage = "getMessage Message-Index header not found";
			}else{
				int messageIndex = Integer.parseInt(messageIndexString);
				Message message = room.getMessage(messageIndex);
				if (message == null){
					response.setCode(200);
					verboseOutput += "getMessage not yet available on this index\n";
					response.httpStatusMessage = "getMessage not yet available on this index";
					response.headers.setProperty("Get-Message-Response", "unavailable");
				}else{
					String text = message.basicText;
					response.body = text;
					response.useFileBody = false;
					response.setCode(200);
					verboseOutput += "getMessage found a message\n";
					response.httpStatusMessage = "success";
					response.headers.setProperty("Get-Message-Response", "success");
					response.headers.setProperty("Audio", message.audioPath);
					response.headers.setProperty("Message-Index", messageIndexString);
				}
			}
		}
	}
	
	private void loginUser(HttpRequest request, HttpResponse response){
		String username = request.headers.getProperty("User");
		String password = request.headers.getProperty("User-Password");
		if(Authentication.verifyPassword(username, password)){
			response.setCode(200);
			response.httpStatusMessage = "success";
			response.addHeader("User", username);
			response.addHeader("User-Password", password);
		}else{
			response.setCode(401);
			response.httpStatusMessage = "user authentication failure";
			response.addHeader("User", username);
			response.addHeader("User-Password", password);
		}
	}

	private void addRoomToUser(HttpRequest request, HttpResponse response){
		String username = request.headers.getProperty("User");
		String password = request.headers.getProperty("User-Password");
		String roomName = request.headers.getProperty("Room");
		String roomPassword = request.headers.getProperty("Room-Password");
		User user = (User)VirtualFileSystem.retrieve(User.getDirectory(username));
		if(user == null){
			response.setCode(401);
			response.httpStatusMessage = "user not found";
		}else{
			Room room = ServerState.retrieveRoom(roomName);
			if(room.accessAllowed(roomPassword, username)){
				user.roomsJoined.add(roomName);
				response.setCode(200);
				response.httpStatusMessage = "success";
			}else{
				response.setCode(401);
				response.httpStatusMessage = "room access denied";
			}
		}
		response.addHeader("User", username);
		response.addHeader("User-Password", password);
		response.addHeader("Room", roomName);
		if(roomPassword!=null) {
			response.addHeader("Room-Password", roomPassword);
		}else{
			response.addHeader("Room-Password", "");
		}
	}

	private void runCommand(String command, HttpRequest request, HttpResponse response){
		switch(command){
			case "createMessage":
				createMessage(request, response);
				break;
			case "getMessage":
				getMessage(request, response);
				break;
			case "createRoom":
				createRoom(request, response);
				break;
			case "createUser":
				createUser(request, response);
				break;
			case "addUserToRoom":
				addUserToRoom(request, response);
				break;
			case "addRoomToUser":
				addRoomToUser(request, response);
				break;
			case "loginUser":
				loginUser(request, response);
				break;
			default:
				throw new HTTPException(404);
		}
	}
	
	private HttpResponse handlePostRequest(HttpRequest request){
		verboseOutput += "handlePostRequest\n";
		HttpResponse response = new HttpResponse();
		String command = request.headers.getProperty("Command");
		if (command == null){
			//malformed request 400
			response.setCode(400);
			verboseOutput += "POST command header not found\n";
			response.httpStatusMessage = "POST command header not found";
		}else{
			try{
				runCommand(command, request, response);
			}catch(HTTPException e){
				response.setCode(404);
				verboseOutput += "Unknown post command\n";
				response.httpStatusMessage = "Unknown post command";
			}
		}
		return response;
	}
	
	private void handleRequest(HttpRequest request, PrintWriter headerOut, BufferedOutputStream dataOut){

		HttpResponse response = new HttpResponse();

		String username = request.headers.getProperty("User");
		String password = request.headers.getProperty("User-Password");

		//System.out.println(username + " and " + password);

		boolean correctPassword = false;
		if (username==null||username.equals("no-user")||username.equals("")){
			correctPassword = true;
		}else{
			if (password == null){
				correctPassword = false;
			}else{
				correctPassword = Authentication.verifyPassword(username, password);
			}
		}
		if (!correctPassword){
			response.setCode(401);
			response.httpStatusMessage = "user authentication failure";
			response.addHeader("User", username);
			response.addHeader("User-Password", password);
		}else{
			switch(request.httpMethod){
				case "GET":
					response = handleGetRequest(request);
					break;
				case "POST":
					response = handlePostRequest(request);
					break;
				default:
					verboseOutput += "Unsupported header requested\n";
					break;
			}
		}
		
		try {
			response.outputResponse(headerOut, dataOut);
		} catch (IOException e) {
			verboseOutput += "Response could not be written out\n";
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		
		verboseOutput += ("Connection opened on " + new Date() + "\n");
		BufferedReader in = null;
		PrintWriter headerOut = null;
		BufferedOutputStream dataOut = null;
		try {
			in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			headerOut = new PrintWriter(connection.getOutputStream());
			dataOut = new BufferedOutputStream(connection.getOutputStream());

			HttpRequest request = null;
			
			try {
				request = new HttpRequest(in, verboseOutput);
			} catch (Exception e) {
				if (e instanceof HTTPException){
					//generate response according to code
				}else{
					//generate response with server error code
				}
				e.printStackTrace();
			}

			//make changes according to request, and respond
			handleRequest(request, headerOut, dataOut);
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				in.close();
				headerOut.close();
				dataOut.close();
				connection.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			verboseOutput += ("Connection closed\n\n");
			//System.out.println(verboseOutput);
			//System.out.println();
			System.out.flush();
		}
	}

}
