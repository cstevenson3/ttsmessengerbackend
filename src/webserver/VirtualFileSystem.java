package src.webserver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

import org.apache.commons.io.FileUtils;

public class VirtualFileSystem {
	private final static String[] allowedReadPrefixPaths = {"../ttsmessenger/, ../ttsdata/"};
	private final static String[] allowedWritePrefixPaths = {"../ttsdata/"};
	private final static String[] whitelistPaths = {};
	private final static String[] blacklistPaths = {};
	
	private static HashMap<String, Semaphore> fileEditMutexes;
	private static HashMap<String, Semaphore> cacheEditMutexes;
	private static HashMap<String, byte[]> fileCache;
	
	static{
		fileEditMutexes = new HashMap<String, Semaphore>();
		cacheEditMutexes = new HashMap<String, Semaphore>();
		fileCache = new HashMap<String, byte[]>();
	}
	
	public static void createNecessaryDirectories(String filePath){
		File file = new File(filePath);
		File directory = new File(file, "..");
		if (!(directory.isDirectory())){
			directory.mkdirs();
		}
	}
	
	private static void acquireMutex(HashMap<String, Semaphore> map, String directory){
		if(map.containsKey(directory)){
			
		}else{
			map.put(directory, new Semaphore(1));
		}
		try {
			map.get(directory).acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private static void releaseMutex(HashMap<String, Semaphore> map, String directory){
		map.get(directory).release();
	}
	
	private static void pushFromCacheToFile(String directory){
		acquireMutex(fileEditMutexes, directory);
		
		writeBytesToFile(directory, fileCache.get(directory));
		
		releaseMutex(fileEditMutexes, directory);
	}
	
	private static void writeBytesToFile(String directory, byte[] bytes) {
		createNecessaryDirectories(directory);
		try {
			FileUtils.writeByteArrayToFile(new File(directory), bytes);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void store(String directory, Serializable object){
		byte[] serialized = objectToBytes(object);
		
		acquireMutex(cacheEditMutexes, directory);
			
		fileCache.put(directory, serialized);
		pushFromCacheToFile(directory);

		releaseMutex(cacheEditMutexes, directory);
	}
	
	
	
	public static Object retrieve(String directory){
		byte[] file;
		
		//check cache first
		if(fileCache.containsKey(directory)){
			
		}else{
			loadFileToCache(directory);
		}
		
		file = fileCache.get(directory);
		
		Object reformed = bytesToObject(file);
		return reformed;
	}
	
	private static void loadFileToCache(String directory) {
		acquireMutex(fileEditMutexes, directory);
		
		byte[] file = fileToBytes(directory);
		
		releaseMutex(fileEditMutexes, directory);
		
		
		acquireMutex(cacheEditMutexes, directory);
		
		fileCache.put(directory, file);
		
		releaseMutex(cacheEditMutexes, directory);	
	}

	public static void deleteDirectory(String directory){
		try{
			FileUtils.deleteDirectory(new File(directory));
		}catch(IOException e){
			e.printStackTrace();
		}
		
		/*
		File index = new File(directory);
		if (index.exists()){
			String[]entries = index.list();
			for(String s: entries){
				File currentFile = new File(index.getPath(),s);
				currentFile.delete();
			}
		}
		*/
	}

	private static byte[] fileToBytes(String directory){
		try {
			return Files.readAllBytes(new File(directory).toPath());
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private static byte[] objectToBytes(Serializable object){
		byte[] result = null;
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		try {
		  out = new ObjectOutputStream(bos);   
		  out.writeObject(object);
		  out.flush();
		  result = bos.toByteArray();
		  
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
		  try {
		    bos.close();
		  } catch (IOException ex) {
		    // ignore close exception
		  }
		}
		return result;
	}
	
	private static Object bytesToObject(byte[] bytes){
		if (bytes == null){
			return null;
		}
		Object result = null;
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		ObjectInput in = null;
		try {
		  in = new ObjectInputStream(bis);
		  result = in.readObject(); 
		  
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
		  try {
		    if (in != null) {
		      in.close();
		    }
		  } catch (IOException ex) {
		  }
		}
		return result;
	}

	public static boolean fileExists(String directory) {
		return new File(directory).exists();
	}
}
