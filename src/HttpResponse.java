package src;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

public class HttpResponse extends HttpMessage{

	public int httpStatusCode;
	public String httpStatusMessage;
	public byte[] fileBody;
	public boolean useFileBody;
	
	
	public HttpResponse(){
		super();
		httpStatusCode = 500;
		httpStatusMessage = "Response not updated correctly";
		useFileBody = true;
	}
	
	public String messageFromCode(int code){
		switch (code){
		case 400:
			return "OK";
		case 403:
			return "Forbidden";
		case 500:
			return "Internal Server Error";
		default:
			return "Unknown code";
		}
	}
	
	public void addHeader(String key, String value){
		headers.setProperty(key, value);
	}
	
	public void setCode(int code){
		httpStatusCode = code;
	}
	
	public void autofillMessage(){
		httpStatusMessage = messageFromCode(httpStatusCode);
	}
	
	public void outputResponse(PrintWriter headerOut, BufferedOutputStream dataOut) throws IOException {
		headerOut.println(httpVersion + " " + Integer.toString(httpStatusCode) + " " + httpStatusMessage);
		//override certain headers
		
		byte[] bodyBytes;
		if (useFileBody){
			bodyBytes = fileBody;
		}else{
			bodyBytes = body.getBytes();

		}
		headers.setProperty("Content-Length", Integer.toString(bodyBytes.length));
		
		
		for(String key : headers.stringPropertyNames()) {
			String value = headers.getProperty(key);
			headerOut.println(key + ": " + value);
		}
		
		headerOut.println();
		headerOut.flush();
		
		dataOut.write(bodyBytes, 0, bodyBytes.length);
		dataOut.flush();
	}

}
