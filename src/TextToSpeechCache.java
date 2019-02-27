package src;

import java.io.File;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

public class TextToSpeechCache {
	//stores the results of tts requests to minimise bandwidth and API usage
	
	public class CacheableTtsResult implements Serializable{
		String uri;
		MarkedAudio result;
	}
	
	private static HashMap<String, MarkedAudio> cache;
	private static Semaphore cacheEditMutex;
	private static final String cachePath = "../ttsdata/ttscache/";
	
	static{
		cache = new HashMap<String, MarkedAudio>();
		cacheEditMutex = new Semaphore(1);
	}
	
	public static boolean contains(String text){
		String uri = textToURI(text);
		if (cache.containsKey(uri)){
			return true;
		}else{
			String path = uriToPath(uri);
			File file = new File(path);
			if (file.exists()){
				return true;
			}else{
				return false;
			}
		}
	}
	
	public static MarkedAudio getFromCache(String text){
		if(!contains(text)){
			return null; //maybe throw error
		}
		MarkedAudio result = null;
		String uri = textToURI(text);
		acquireMutex(cacheEditMutex);
		if (cache.containsKey(uri)){
			result = cache.get(text);
		}else{
			result = (MarkedAudio) VirtualFileSystem.retrieve(uriToPath(uri));
		}
		releaseMutex(cacheEditMutex);
		return result;
	}
	
	public static void addToCache(String text, MarkedAudio result){
		String uri = textToURI(text);
		acquireMutex(cacheEditMutex);
		cache.put(uri, result);
		VirtualFileSystem.store(uriToPath(uri), result);
		releaseMutex(cacheEditMutex);
	}
	
	private static String uriToPath(String uri){
		return cachePath + uri + ".ser";
	}
	
	private static String textToURI(String s) {
	    String result;

	    try {
	        result = URLEncoder.encode(s, "UTF-8")
	                .replaceAll("\\+", "%20")
	                .replaceAll("\\%21", "!")
	                .replaceAll("\\%27", "'")
	                .replaceAll("\\%28", "(")
	                .replaceAll("\\%29", ")")
	                .replaceAll("\\%7E", "~");
	    } catch (UnsupportedEncodingException e) {
	        result = s;
	    }

	    return result;
	}
	
	private static void acquireMutex(Semaphore mutex){
		if (mutex == null){
			mutex = new Semaphore(1);
		}
		try {
			mutex.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private static void releaseMutex(Semaphore mutex){
		if (mutex == null){
			mutex = new Semaphore(1);
			return;
		}
		mutex.release();
	}
}
