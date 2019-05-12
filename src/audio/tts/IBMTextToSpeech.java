package src.audio.tts;

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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Semaphore;

import org.apache.commons.io.FileUtils;

import okhttp3.WebSocket;
import src.audio.MarkedAudio;
import src.audio.SampledAudio;
import src.audio.util.wavIO;

import com.ibm.watson.developer_cloud.service.security.IamOptions;
import com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.developer_cloud.text_to_speech.v1.model.MarkTiming;
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
	private static final int SAMPLE_RATE = 22050;
	
	private static IamOptions connectionOptions;
	
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
		connectionOptions = new IamOptions.Builder().apiKey(apiKey).build();
		
	}
	
	public static void basicTtsToWav(String path, String text){
		TextToSpeech textToSpeech = new TextToSpeech(connectionOptions);
		textToSpeech = new TextToSpeech(connectionOptions);
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
		TextToSpeech textToSpeech = new TextToSpeech(connectionOptions);
		textToSpeech = new TextToSpeech(connectionOptions);
		textToSpeech.setEndPoint(url);
		List<String> list = Arrays.asList(new String[]{"words"});
		SynthesizeOptions synthesizeOptions =
		new SynthesizeOptions.Builder()
		.text(text)
		.accept("audio/wav")
		.voice("en-US_AllisonVoice")
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
				Properties props = new Properties();
				try {
					props.store(new FileOutputStream(new File(timingsPath)), "");
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
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
			public void onMarks(Marks marks) {
				Properties props = new Properties();
				try {
					props.load(new FileInputStream(new File(timingsPath)));
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				for (MarkTiming mark : marks.getMarks()){
					props.setProperty(mark.getMark(), Double.toString(mark.getTime()));
				}
				try {
					props.store(new FileOutputStream(new File(timingsPath)), "");
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
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

	
	public static ArrayList<String> textToMarkNames(String text){
		ArrayList<String> result = new ArrayList<String>();
		String markTemplateStart = "<mark name=\"";
		String markTemplateEnd = "\"";
		for(int i = 0; i<text.length()-markTemplateStart.length()-markTemplateEnd.length(); i++){
			if(markTemplateStart.equals(text.substring(i, i+markTemplateStart.length()))){
				//scan till markTemplateEnd is found
				String markName = "";
				int scanIndex = i + markTemplateStart.length();
				while(scanIndex + markTemplateEnd.length()-1<text.length()&&!(markTemplateEnd.equals(text.subSequence(scanIndex, scanIndex + markTemplateEnd.length())))){
					markName += text.substring(scanIndex, scanIndex + 1);
					scanIndex ++;
				}
				//check mark name is valid
				if(markName.equals("")){

				}else{
					result.add(markName);
				}
			}
		}
		return result;
	}

	Properties marks;
	public MarkedAudio timestampedTtsToMarkedAudio(String text) throws Exception {
		//check if text is empty w.r.t. speech synthesis
		boolean textEmpty = true;
		int angleBracketCounter = 0;
		for(char c:text.toCharArray()){
			if(c=="<".charAt(0)){
				angleBracketCounter++;
				continue;
			}
			if(c==">".charAt(0)){
				angleBracketCounter--;
				continue;
			}
			if(angleBracketCounter==0){
				textEmpty = false;
				break;
			}
		}
		

		if(textEmpty){
			//return empty single channel sound
			SampledAudio empty = new SampledAudio();
			empty.sampleRate = SAMPLE_RATE;
			empty.samples = new double[0];
			empty.bits = 16;
			empty.numChannels = 1;
			empty.numFrames = 0;

			double separation = 0.001;
			Properties zeroMarks = new Properties();
			for(String markName:textToMarkNames(text)){
				zeroMarks.put(markName, String.valueOf(separation));
				separation += 0.001; //separate each mark very slightly to maintain their order when effects are added
			}

			MarkedAudio result = new MarkedAudio();
			result.marks = zeroMarks;
			result.sa = empty;
			return result;
		}

		marks = new Properties();
		try {
			responseMutex.acquire();
		} catch (InterruptedException e2) {
			e2.printStackTrace();
			throw new Exception();
		}
		TextToSpeech textToSpeech = new TextToSpeech(connectionOptions);
		textToSpeech = new TextToSpeech(connectionOptions);
		textToSpeech.setEndPoint(url);
		SynthesizeOptions synthesizeOptions =
		new SynthesizeOptions.Builder()
		.text(text)
		.accept("audio/wav;rate=" + Integer.toString(SAMPLE_RATE)) //endianness=big-endian;
		.voice("en-US_AllisonVoice")
		.build();

		WebSocket webSocket =
		textToSpeech.synthesizeUsingWebSocket(synthesizeOptions, new SynthesizeCallback() {

			@Override
			public void onAudioStream(byte[] samples) {
				//write to byte stream
				audio.write(samples, 0, samples.length);
			}

			@Override
			public void onConnected() {

			}

			@Override
			public void onContentType(String arg0) {

			}

			@Override
			public void onDisconnected() {
				responseMutex.release();
			}

			@Override
			public void onError(Exception arg0) {
				responseMutex.release();
			}

			@Override
			public void onMarks(Marks inputMarks) {
				for (MarkTiming mark : inputMarks.getMarks()){
					marks.setProperty(mark.getMark(), Double.toString(mark.getTime()));
				}
			}

			@Override
			public void onTimings(Timings timings) {

			}

			@Override
			public void onWarning(Exception arg0) {
				responseMutex.release();
			}
		    
		});
		//wait for response callbacks to finish
		try {
			responseMutex.acquire();
		} catch (InterruptedException e2) {
			e2.printStackTrace();
		}
		System.out.println("Released");
		webSocket.close(1000, "All necessary data acquired"); //might be irrelevant
		
		byte[] audioBytes = audio.toByteArray();

		double[] samples = new double[audioBytes.length / 2];
		
		for (int i = 0; i < samples.length; i++){
			double sample = (256.0 * ((double)audioBytes[i*2 + 1]) + ((double)audioBytes[i*2])) / 65536.0;
			samples[i] = sample;
		}
		
		SampledAudio sa = new SampledAudio();
		sa.bits = 16;
		sa.numChannels = 1;
		sa.numFrames = samples.length;
		sa.sampleRate = SAMPLE_RATE;
		sa.samples = samples;
		
		MarkedAudio result = new MarkedAudio();
		result.marks = marks;
		result.sa = sa;
		return result;
	}
}
