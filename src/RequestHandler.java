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

import javax.xml.ws.http.HTTPException;

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
	
	public static double clamp(double val, double min, double max) {
	    return Math.max(min, Math.min(max, val));
	}
	
	private static String asteriskToMark(String input, int startIndex){
		for (int i = 0; i < input.length(); i++){
			if (input.charAt(i) == "*".charAt(0)){
				String before = input.substring(0, i);
				String after = input.substring(i + 1, input.length());
				String result = before + "<mark name=\"" + "asterisk" + startIndex + "\"/>" + asteriskToMark(after, startIndex + 1);
				return result;
			}
		}
		return input;
	}
	
	private static String atToMark(String input, int startIndex){
		for (int i = 0; i < input.length(); i++){
			if (input.charAt(i) == "@".charAt(0)){
				String before = input.substring(0, i);
				String after = input.substring(i + 1, input.length());
				String result = before + "<mark name=\"" + "at" + startIndex + "\"/>" + atToMark(after, startIndex + 1);
				return result;
			}
		}
		return input;
	}
	
	public static String preprocessText(String text){
		System.out.println("Start text:");
		System.out.println(text);
		//remove < and >
		String temp1 = text.replace("<", "");
		String temp2 = temp1.replace(">", "");
		
		//replace asterix with <mark name="asterisk#"/>
		
		String temp3 = asteriskToMark(temp2, 0);
		
		//replace @ with <mark name="at#"/>
		
		String temp4 = atToMark(temp3, 0);
		
		//append "end" mark to end of synthesis
		String result = temp4 + "<mark name=\"end\"/>";
		
		System.out.println("Result text:");
		System.out.println(result);
		return result;
	}
	
	private static void processEffects(SampledAudio input, String timingsPath){
		File timingsDir = new File(WEB_ROOT, timingsPath);
		Properties timings = new Properties();
		try {
			timings.load(new FileInputStream(timingsDir));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		double endTime = Double.parseDouble(timings.getProperty("end"));
		int sampleLength = input.samples.length;
		
		double samplesPerTime = sampleLength / endTime;
		//System.out.println("Samples per time: " + samplesPerTime);
		
		//multiply time by samplesPerTime to get sample range
		/*
		for(int i = (int) ((1.0/3.0) * samplesPerTime); i < (int) ((2.0/3.0) * samplesPerTime); i++){
			if (i >= input.samples.length){
				break;
			}
			input.samples[i] = clamp(input.samples[i] * 1000, -1, 1);
			input.samples[i] = input.samples[i] / 100;
		}
		*/
		//go through asterisk keys until not found
		ArrayList<SampleEffect> effects = new ArrayList<SampleEffect>();
		
		double startTimeTemp = 0;
		int index = 0;
		String value = "";
		while ((value = timings.getProperty("asterisk" + index)) != null){
			if ((index % 2) == 0){
				startTimeTemp = Double.parseDouble(value);
			}else{
				SampleEffect ow = new SampleEffect();
				ow.startTime = startTimeTemp;
				ow.endTime = Double.parseDouble(value);
				ow.effect = EffectName.LOUD;
				effects.add(ow);
			}
			index++;
		}
		
		for (SampleEffect effect : effects){
			switch(effect.effect){
			case LOUD:
				System.out.println("Applying loud effect from " + effect.startTime + " to " + effect.endTime);
				for(int i = (int) (effect.startTime * samplesPerTime); i < (int) (effect.endTime * samplesPerTime); i++){
					if (i >= input.samples.length){
						break;
					}
					input.samples[i] = clamp(input.samples[i] * 100, -1, 1);
					input.samples[i] = input.samples[i] / 2;
				}
				break;
			default:
				System.out.println("Effect not recognised");
				break;
			}
			
		}
		
		// insert thenword
		
		SampledAudio thenword = null;
		try {
			File thenwordDir = new File("../ttsdata/content/thenword.wav");
			thenword = TextToSpeechInterface.wavToSampledAudio(thenwordDir.getCanonicalPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		ArrayList<SampleEffect> effects2 = new ArrayList<SampleEffect>();
		
		int index2 = 0;
		String value2 = "";
		while ((value2 = timings.getProperty("at" + index2)) != null){
			SampleEffect ow = new SampleEffect();
			ow.startTime = Double.parseDouble(value2);
			ow.effect = EffectName.INSERT_THE_N_WORD;
			effects2.add(ow);
			index2++;
		}
		
		for (SampleEffect effect : effects2){
			switch(effect.effect){
			case INSERT_THE_N_WORD:
				System.out.println("Applying thenword effect from " + effect.startTime);
				SampledAudio newInput = input.insertOtherSampledAudio(effect.startTime, thenword);
				input.samples = newInput.samples;
				input.bits = newInput.bits;
				input.numChannels = newInput.numChannels;
				input.sampleRate = newInput.sampleRate;
				input.numFrames = newInput.numFrames;
				break;
			default:
				System.out.println("Effect not recognised");
				break;
			}
		}
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
			response.httpStatusMessage = "Create message ok";

			verboseOutput += "Room: " + roomName +  "updated, is now \n";
			verboseOutput += room.toString();
		}else{
			response.setCode(401);
			response.httpStatusMessage = "Room access forbidden";
		}
	}
	
	private void createRoom(HttpRequest request, HttpResponse response){
		
		
		
		String roomName = request.headers.getProperty("Room");
		
		if(ServerState.roomExists(roomName)){
			response.setCode(401);
			response.httpStatusMessage = "Room already exists";
		}else{
			ServerState.createRoom(roomName);
			
			//optional settings
			Room room = ServerState.retrieveRoom(roomName);
			
			String accessByURLString = request.headers.getProperty("Room-Access-By-URL");
			boolean accessByURL = Boolean.parseBoolean(accessByURLString);
			room.setAccessByURL(accessByURL);
			
			String accessByPasswordString = request.headers.getProperty("Room-Access-By-Password");
			boolean accessByPassword = Boolean.parseBoolean(accessByPasswordString);
			room.setAccessByPassword(accessByPassword);
			
			String password = request.headers.getProperty("Room-Password");
			room.setPassword(password);
			
			response.setCode(200);
			response.httpStatusMessage = "OK";
			response.addHeader("Room", roomName);
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
		case "createRoom":
			createRoom(request, response);
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
			//System.out.println(verboseOutput);
			//System.out.println();
			System.out.flush();
		}
	}

}
