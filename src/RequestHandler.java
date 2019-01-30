package src;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;

import javax.sound.sampled.AudioInputStream;
import javax.xml.ws.http.HTTPException;

import marytts.LocalMaryInterface;
import marytts.MaryInterface;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;
import marytts.util.data.audio.MaryAudioUtils;

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
	
	public static void createNecessaryDirectories(String filePath){
		File file = new File(filePath);
		File directory = new File(file, "..");
		if (!(directory.isDirectory())){
			directory.mkdirs();
		}
	}
	
	public static void textToSpeechFile(String path, String text){
		MaryInterface marytts = null;
		AudioInputStream audio = null;
		try {
			marytts = new LocalMaryInterface();
		} catch (MaryConfigurationException e) {
			e.printStackTrace();
		}
		try {
			audio = marytts.generateAudio(text);
		} catch (SynthesisException e1) {
			e1.printStackTrace();
		}
		double[] samples = MaryAudioUtils.getSamplesAsDoubleArray(audio);
		double[] output = new double[2*samples.length];
		for (int i = 0; i < samples.length; i++){
			//output[2*i] = samples[i];
			//output[2*i+1] = samples[i];
			if (i > samples.length/2){
				//double sign = Math.signum(samples[i]);
				//samples[i] *= sign * Math.sqrt(Math.abs(samples[i])) * 2;
			}
		}
		createNecessaryDirectories(path);
		try {
			MaryAudioUtils.writeWavFile(samples, path, audio.getFormat());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void createMessage(HttpRequest request, HttpResponse response){
		int roomIndex = Integer.parseInt(request.headers.getProperty("Room"));
		Room room = null;
		if (roomIndex == 0){
			room = testRoom;
		}else{
			//TODO actually get the right room
			room = testRoom;
		}
		int messageIndex = room.messages.size();
		
		Message testMessage = new Message();
		testMessage.basicText = request.body;
		
		String ttstext = testMessage.basicText;
		
		String audioPath = "audio/" + roomIndex + "/" + messageIndex + ".wav";
		File dir = new File(WEB_ROOT, audioPath);
		try {
			textToSpeechFile(dir.getCanonicalPath(), ttstext);
		} catch (IOException e) {
			e.printStackTrace();
		}
		testMessage.audioPath = audioPath;
		
		testRoom.addMessage(testMessage);
		verboseOutput += "testRoom updated, is now \n";
		verboseOutput += testRoom.toString();
		response.setCode(200);
		response.httpStatusMessage = "Create message ok";
	}
	
	private void getMessage(HttpRequest request, HttpResponse response){
		String roomString = request.headers.getProperty("Room");
		if (roomString == null){
			response.setCode(400);
			verboseOutput += "getMessage Room header not found\n";
			response.httpStatusMessage = "getMessage Room header not found";
		}else{
			int roomIndex = Integer.parseInt(roomString);
			Room room = null;
			if (roomIndex == 0){
				room = testRoom;
			}else{
				//TODO get actual room from hash table
				room = testRoom;
			}
			String messageIndexString = request.headers.getProperty("Message-Index");
			if (messageIndexString == null){
				//return 400
				response.setCode(400);
				verboseOutput += "getMessage Message-Index header not found\n";
				response.httpStatusMessage = "getMessage Message-Index header not found";
			}else{
				int messageIndex = Integer.parseInt(messageIndexString);
				if (messageIndex >= room.messages.size()){
					response.setCode(200);
					verboseOutput += "getMessage not yet available on this index\n";
					response.httpStatusMessage = "getMessage not yet available on this index";
					response.headers.setProperty("Get-Message-Response", "unavailable");
				}else{
					Message message = room.messages.get(messageIndex);
					String text = message.basicText;
					response.body = text;
					response.useFileBody = false;
					response.setCode(200);
					verboseOutput += "getMessage found a message\n";
					response.httpStatusMessage = "getMessage found a message";
					response.headers.setProperty("Get-Message-Response", "success");
					response.headers.setProperty("Audio", message.audioPath);
					response.headers.setProperty("Message-Index", messageIndexString);
				}
			}
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
		/*
		String file = request.fileRequested;
		switch(file){
		case "/command":
			verboseOutput += "POST command\n";
			String[] body = request.body.split("\r\n?|\n");
			if (body.length == 0){
				//malformed request 400
				response.setCode(400);
				verboseOutput += "POST indicated command but no body was found\n";
				response.httpStatusMessage = "POST indicated command but no body was found";
			}else{
				try{
					runCommand(body);
					response.setCode(200);
					verboseOutput += "POST ran command with no exceptions\n";
					response.httpStatusMessage = "POST ran command with no exceptions";
				}catch(HTTPException e){
					response.setCode(e.getStatusCode());
					response.autofillMessage();
				}
			}
			break;
		default:
			System.out.println(request.fileRequested);
			verboseOutput += "POST file not recognised\n";
			//return 404
			response.setCode(404);
			response.httpStatusMessage = "POST file not recognised";
		}
		response.addHeader("Server", "cstevenson3");
		response.addHeader("Date", (new Date()).toString());
		*/
	}
	
	private void handleRequest(HttpRequest request, PrintWriter headerOut, BufferedOutputStream dataOut){
		HttpResponse response = null;
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
			System.out.println(verboseOutput);
			System.out.println();
			System.out.flush();
		}
	}

}
