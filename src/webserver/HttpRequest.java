package src.webserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;

import javax.xml.ws.http.HTTPException;

public class HttpRequest extends HttpMessage{
	
	public static final String[] validHttpMethods = {"GET", "POST", "PUT", "DELETE", "PATCH"};
	public static final String[] supportedHttpMethods = {"GET", "POST"};
	
	public String httpMethod;
	public String fileRequested;
		
	public HttpRequest(){
		super();
		httpMethod = "";
		fileRequested = "";
	}
	
	public HttpRequest(BufferedReader in, String verboseOutput) throws Exception{
		this();

		String firstLine = "";
		try {
			firstLine = in.readLine();
			if (firstLine == null) throw new IOException();
		} catch (IOException e) {
			verboseOutput += "IOException on reading first line of message\n";
			throw e;
		}
		
		String[] splitted = firstLine.split(" ");
		
		//deduce html version
		switch(splitted.length){
		case 2:
			//Check to see if implicit version HTTP/0.9 is being used
			if (Arrays.asList(validHttpMethods).contains(splitted[0])){
				//Valid first line
				httpVersion = "HTML/0.9";
			}else{
				HTTPException e = new HTTPException(400);
				throw e;
			}
			break;
		case 3:
			if (Arrays.asList(validHttpVersions).contains(splitted[2])){
				httpVersion = splitted[2];
			}else{
				HTTPException e = new HTTPException(400);
				throw e;
			}
			break;
		default:
			//invalid request
			HTTPException e = new HTTPException(400);
			throw e;
		}
		
		//check if http version is not supported
		if (!(Arrays.asList(supportedHttpVersions).contains(httpVersion))){
			HTTPException e = new HTTPException(505);
			throw e;
		}
		//otherwise HTTP version is supported
		
		//check method
		httpMethod = splitted[0].toUpperCase();
		//check method is valid
		if (!(Arrays.asList(supportedHttpMethods).contains(httpMethod))){
			HTTPException e = new HTTPException(400);
			throw e;
		}
		//check method is supported
		if (!(Arrays.asList(supportedHttpMethods).contains(httpMethod))){
			HTTPException e = new HTTPException(501);
			throw e;
		}
		
		//file requested
		fileRequested = splitted[1].toLowerCase();
		
		//get headers
		String line = "";
		line = in.readLine();
		while (line != null && !line.isEmpty()){
			String[] pair = line.split(": ");
			if (pair.length != 2){
				verboseOutput += "Malformed header: " + line + "\n";
			}else{
				headers.setProperty(pair[0], pair[1]);
			}
			line = in.readLine();
		}
		//should be ready to read body
		if (headers.getProperty("Content-Length")==null){
			//require body length response?
		}else{
			int bodyLength = Integer.parseInt(headers.getProperty("Content-Length"));
			char[] bodyArray = new char[bodyLength];
			in.read(bodyArray, 0, bodyLength);
			body = new String(bodyArray);
		}
	}
}