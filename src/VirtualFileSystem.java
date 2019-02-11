package src;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class VirtualFileSystem {
	private final static String[] allowedReadPrefixPaths = {"../ttsmessenger/, ../ttsdata/"};
	private final static String[] allowedWritePrefixPaths = {"../ttsdata/"};
	private final static String[] whitelistPaths = {};
	private final static String[] blacklistPaths = {};
	
	private static final String webToInternalPathMapLocation = "";
	private static Properties webToInternalPathMap = new Properties();
	
	static{
		try {
			webToInternalPathMap.load(new FileInputStream(new File(webToInternalPathMapLocation)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//make opposite hash map
	}
	
	public static String webPathToInternalPath(String webPath){
		return webPath;
	}
	
	public static void createNecessaryDirectories(String filePath){
		File file = new File(filePath);
		File directory = new File(file, "..");
		if (!(directory.isDirectory())){
			directory.mkdirs();
		}
	}
	
	public boolean okToReadInternal(String path){
		File file = new File(path);
		return okToReadInternal(file);
	}
	
	public boolean okToReadInternal(File file){
		return true;
	}
}
