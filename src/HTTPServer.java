package src;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.ServerSocket;
import java.util.Base64;

import javax.sound.sampled.AudioInputStream;

import org.apache.commons.io.FileUtils;

import marytts.LocalMaryInterface;
import marytts.MaryInterface;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;
import marytts.util.data.audio.AudioPlayer;
import marytts.util.data.audio.MaryAudioUtils;

public class HTTPServer {

	static final int PORT = 25565;
	
	public static void server(){
		ServerSocket serverConnect = null;
		try {
			serverConnect = new ServerSocket(PORT);
			System.out.println("Server started.\nListening for connections on port : " + PORT + " ...\n");
			
			while (true) {
				RequestHandler handler = new RequestHandler(serverConnect.accept());
				Thread thread = new Thread(handler);
				thread.start();
			}
			
		} catch (IOException e) {
			System.err.println("Server Connection error : " + e.getMessage());
		} finally {
			try {
				serverConnect.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void testReader(){
		String test = "test1\n\ntest2";
		Reader inputString = new StringReader(test);
		BufferedReader reader = new BufferedReader(inputString);
		try {
			System.out.println(reader.readLine());
			String middleLine = reader.readLine();
			if (middleLine.isEmpty()){
				System.out.println("works");
			}
			System.out.println(reader.readLine());
			System.out.println(reader.readLine());
			System.out.println(reader.readLine());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void bytesStringTest(){
		String input = "abc\ndef\rghi\r\njkl";
		byte[] inputBytes = input.getBytes();
		System.out.println(new String(inputBytes));
	}
	
	public static void maryTtsTest(){
		MaryInterface marytts = null;
		AudioInputStream audio = null;
		AudioPlayer ap = null;
		try {
			marytts = new LocalMaryInterface();
			ap = new AudioPlayer();
		} catch (MaryConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			audio = marytts.generateAudio("This is my text.");
			ap.setAudio(audio);
            ap.start();
		} catch (SynthesisException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	public static void createNecessaryDirectories(String filePath){
		File file = new File(filePath);
		File directory = new File(file, "..");
		if (!(directory.isDirectory())){
			directory.mkdirs();
		}
	}
	
	public static void textToSpeechFile(String path, String text){
		MaryInterface marytts = null;
		AudioInputStream audio = null;
		try {
			marytts = new LocalMaryInterface();
		} catch (MaryConfigurationException e) {
			e.printStackTrace();
		}
		try {
			audio = marytts.generateAudio(text);
		} catch (SynthesisException e1) {
			e1.printStackTrace();
		}
		double[] samples = MaryAudioUtils.getSamplesAsDoubleArray(audio);
		createNecessaryDirectories(path);
		try {
			MaryAudioUtils.writeWavFile(samples, path, audio.getFormat());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static double clamp(double val, double min, double max) {
	    return Math.max(min, Math.min(max, val));
	}
	
	public static void main(String[] args) {
		//textToSpeechFile("0/0.wav", "The FitnessGram™ Pacer Test is a multistage aerobic capacity test that progressively gets more difficult as it continues. The 20 meter pacer test will begin in 30 seconds. Line up at the start. The running speed starts slowly, but gets faster each minute after you hear this signal. [beep] A single lap should be completed each time you hear this sound. [ding] Remember to run in a straight line, and run as long as possible. The second time you fail to complete a lap before the sound, your test is over. The test will begin on the word start. On your mark, get ready, start.");
		//bytesStringTest();
		//testReader();
		
		//Room room = new Room();
		//room.test();
		
		//TextToSpeech.maryTTSbasic("py/pee.wav", "hello hello hello");
		
		//byte[] bytes = new byte[]{1};
		//System.out.println(bytes.length);
		
		//SampledAudio audio = TextToSpeechInterface.wavToSampledAudio("../ttsdata/tests/ibm30.wav");
		
		//for (int i = 0; i < audio.samples.length; i++){
			//audio.samples[i] = clamp(audio.samples[i] * 1000, -1, 1);
			//audio.samples[i] = audio.samples[i] / 100;
		//}
		//TextToSpeech.sampledAudioToWav("py/hellonew.wav", audio);
		
		//IBMTextToSpeech tts = new IBMTextToSpeech();
		//tts.timestampedTtsToWav("../ttsdata/tests/ibm40.wav", "../ttsdata/tests/ibm40.txt", "Hello <mark name=\"test\"/> World");
		//IBMTextToSpeech.basicTtsToWav("../ttsdata/tests/ibm20.wav", "Hello World");

		//wavIO w = new wavIO();
		//w.setPath("../ttsdata/tests/ibm20.wav");
		//w.read();
		//System.out.println(w.toString());
		
		ServerState.createRoom("welcome");
		ServerState.retrieveRoom("welcome").setAccessByURL(true);
		server();
		
		//System.out.println(RequestHandler.preprocessText("The<> name of *Kelvin* Qi Gong is *dumb* like him *woo*"));
		
		/*SampledAudio ub = TextToSpeechInterface.wavToSampledAudio("../ttsmessenger/audio/0/0.wav");
		SampledAudio thenword = TextToSpeechInterface.wavToSampledAudio("../ttsdata/content/thenword.wav");
		SampledAudio inserted = ub.insertOtherSampledAudio(1.198, thenword);
		TextToSpeechInterface.sampledAudioToWav("../ttsdata/content/ultralight2.wav", inserted);
		System.out.println(ub.getDuration());*/
		
		/*
		SampledAudio sa = TextToSpeechInterface.wavToSampledAudio("../ttsdata/content/thenword.wav");
		
		double max = 0;
		double min = 0;
		
		for (int i = 0; i < sa.samples.length; i++){
			double sample = sa.samples[i];
			if (sample > max) max = sample;
			if (sample < min) min = sample;
		}
		System.out.println("Max: " + max);
		System.out.println("Min: " + min);
		*/
		/*
		IBMTextToSpeech tts = new IBMTextToSpeech();
		MarkedAudio test = null;
		try {
			test = tts.timestampedTtsToMarkedAudio("A");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		TextToSpeechInterface.sampledAudioToWav("hello300.wav", test.sa);
		*//*
		Message message = new Message();
		message.audioPath = "audio path";
		message.basicText = "basic text";
		message.timeCreated = 123;
		
		VirtualFileSystem.store("messageTest.ser", message);
		
		Message retrieved = (Message) VirtualFileSystem.retrieve("messageTest.ser");
		System.out.println(retrieved);
		
		Message retrieved2 = (Message) VirtualFileSystem.retrieve("doesntexist.ser");
		System.out.println(retrieved2==null);
		*/
	}
}
