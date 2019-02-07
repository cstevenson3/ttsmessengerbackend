package src;

import java.io.File;

public class SafeFileSystem {
	private final String[] allowedReadPrefixPaths = {"../ttsmessenger/, ../ttsdata/"};
	private final String[] allowedWritePrefixPaths = {"../ttsdata/"};
	private final String[] whitelistPaths = {};
	private final String[] blacklistPaths = {};
	
	public static String webPathToServerPath(String webPath){
		return webPath;
	}
}
