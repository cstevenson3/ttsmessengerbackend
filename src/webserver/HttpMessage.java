package src.webserver;

import java.util.Properties;

public class HttpMessage {
	public String httpVersion;
	public Properties headers;
	public String body;
	
	public static final String[] validHttpVersions = {"HTTP/0.9", "HTTP/1.0", "HTTP/1.1", "HTTP/2.0"};
	public static final String[] supportedHttpVersions = {"HTTP/1.1"};
	
	public void setDefaults(){
		httpVersion = "HTTP/1.1";
		headers = new Properties();
		body = "";
	}
	
	public HttpMessage(){
		setDefaults();
	}
}