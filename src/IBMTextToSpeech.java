package src;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Semaphore;

import org.apache.commons.io.FileUtils;

import okhttp3.WebSocket;

import com.ibm.watson.developer_cloud.service.security.IamOptions;
import com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.developer_cloud.text_to_speech.v1.model.Marks;
import com.ibm.watson.developer_cloud.text_to_speech.v1.model.SynthesizeOptions;
import com.ibm.watson.developer_cloud.text_to_speech.v1.model.Timings;
import com.ibm.watson.developer_cloud.text_to_speech.v1.model.WordTiming;
import com.ibm.watson.developer_cloud.text_to_speech.v1.util.WaveUtils;
import com.ibm.watson.developer_cloud.text_to_speech.v1.websocket.SynthesizeCallback;

public class IBMTextToSpeech {
	private static final String keysPath = "../ttsdata/keys.cfg";
	private static final String usagePath = "../ttsdata/IBMUsage.txt";
	private static final String url = "https://gateway-syd.watsonplatform.net/text-to-speech/api";
	
	private static IamOptions options;
	
	private static Semaphore usageMutex = new Semaphore(1);
	
	static{
		File file = new File(keysPath);
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		Properties keys = new Properties();
		try {
			keys.load(fis);
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			try {
				fis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		String apiKey = keys.getProperty("tts1apikey");
		options = new IamOptions.Builder().apiKey(apiKey).build();
		
	}
	
	public static void basicTtsToWav(String path, String text){
		TextToSpeech textToSpeech = new TextToSpeech(options);
		textToSpeech = new TextToSpeech(options);
		textToSpeech.setEndPoint(url);
		try {
			SynthesizeOptions synthesizeOptions =
			new SynthesizeOptions.Builder()
			.text(text)
			.accept("audio/wav")
			.voice("en-US_AllisonVoice")
			.build();

			InputStream inputStream =
			textToSpeech.synthesize(synthesizeOptions).execute();
			InputStream in = WaveUtils.reWriteWaveHeader(inputStream);

			OutputStream out = new FileOutputStream(path);
			byte[] buffer = new byte[1024];
			int length;
			while ((length = in.read(buffer)) > 0) {
				out.write(buffer, 0, length);
			}

			out.close();
			in.close();
			inputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//update usage stats
		try {
			usageMutex.acquire();
			
			File file = new File(usagePath);
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(file);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}
			Properties keys = new Properties();
			try {
				keys.load(fis);
			} catch (IOException e) {
				e.printStackTrace();
			}finally{
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			String usageString = keys.getProperty("chars");
			int usage = Integer.parseInt(usageString);
			usage += text.length();
			keys.setProperty("chars", Integer.toString(usage));
			File filenew = new File(usagePath);
	        OutputStream out = new FileOutputStream(filenew);
	        keys.store(out, "Usage of IBM TTS API, 10000 char/month is max for free plan");
		} catch (Exception e) {
			e.printStackTrace();
		} finally{
			usageMutex.release();
		}
	}
	
	private Semaphore responseMutex = new Semaphore(1);
	//private boolean audioProcessed = false;
	//private boolean timingsProcessed = false;
	
	private ByteArrayOutputStream audio = new ByteArrayOutputStream();
	
	public void timestampedTtsToWav(String audioPath, String timingsPath, String text){
		try {
			responseMutex.acquire();
		} catch (InterruptedException e2) {
			e2.printStackTrace();
		}
		TextToSpeech textToSpeech = new TextToSpeech(options);
		textToSpeech = new TextToSpeech(options);
		textToSpeech.setEndPoint(url);
		List<String> list = Arrays.asList(new String[]{"words"});
		SynthesizeOptions synthesizeOptions =
		new SynthesizeOptions.Builder()
		.text(text)
		.accept("audio/wav")
		.voice("en-US_AllisonVoice")
		.timings(list)
		.build();

		WebSocket webSocket =
		textToSpeech.synthesizeUsingWebSocket(synthesizeOptions, new SynthesizeCallback() {

			@Override
			public void onAudioStream(byte[] samples) {
				//write to audio path
				System.out.println("Samples length: " + samples.length);
				
				audio.write(samples, 0, samples.length);
				
				//wavIO.bytesToWav(audioPath, samples, 16, 1, 22050);
				/*
				try {
					FileUtils.writeByteArrayToFile(new File(audioPath), samples);
				} catch (IOException e) {
					e.printStackTrace();
				}
				*/
			}

			@Override
			public void onConnected() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onContentType(String arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onDisconnected() {
				// TODO Auto-generated method stub
				responseMutex.release();
			}

			@Override
			public void onError(Exception arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onMarks(Marks arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onTimings(Timings timings) {
				for (WordTiming wordTiming : timings.getWords()){
					System.out.println(wordTiming.getWord() + " played between " + wordTiming.getStartTime() + " and " + wordTiming.getEndTime());
				}
			}

			@Override
			public void onWarning(Exception arg0) {
				// TODO Auto-generated method stub
				
			}
		    
		});
		//wait for response callbacks to finish
		try {
			responseMutex.acquire();
		} catch (InterruptedException e2) {
			e2.printStackTrace();
		}
		System.out.println("released");
		webSocket.close(1000, "All necessary data acquired");
		
		byte[] audioBytes = audio.toByteArray();
		wavIO.bytesToWav(audioPath, audioBytes, 16, 1, 22050);
		
		//update usage stats
		try {
			usageMutex.acquire();
			
			File file = new File(usagePath);
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(file);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}
			Properties keys = new Properties();
			try {
				keys.load(fis);
			} catch (IOException e) {
				e.printStackTrace();
			}finally{
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			String usageString = keys.getProperty("chars");
			int usage = Integer.parseInt(usageString);
			usage += text.length();
			keys.setProperty("chars", Integer.toString(usage));
			File filenew = new File(usagePath);
	        OutputStream out = new FileOutputStream(filenew);
	        keys.store(out, "Usage of IBM TTS API, 10000 char/month is max for free plan");
		} catch (Exception e) {
			e.printStackTrace();
		} finally{
			usageMutex.release();
		}
	}
}
