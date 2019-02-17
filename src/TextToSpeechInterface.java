package src;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import javax.sound.sampled.AudioInputStream;

import marytts.LocalMaryInterface;
import marytts.MaryInterface;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;
import marytts.util.data.audio.MaryAudioUtils;

public class TextToSpeechInterface {
	
	public static void createNecessaryDirectories(String filePath){
		File file = new File(filePath);
		File directory = new File(file, "..");
		if (!(directory.isDirectory())){
			directory.mkdirs();
		}
	}
	
	public static void maryTTSbasic(String path, String text){
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
		double[] output = new double[2*samples.length];
		for (int i = 0; i < samples.length; i++){
			//output[2*i] = samples[i];
			//output[2*i+1] = samples[i];
			if (i > samples.length/2){
				//double sign = Math.signum(samples[i]);
				//samples[i] *= sign * Math.sqrt(Math.abs(samples[i])) * 2;
			}
		}
		createNecessaryDirectories(path);
		try {
			MaryAudioUtils.writeWavFile(samples, path, audio.getFormat());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String filePathToDirectoryPath(String path){
		File filePath = new File(path);
		return filePath.getParent();
	}
	
	public static void gTTSbasic(String path, String text){
		try {
			Process tts = Runtime.getRuntime().exec("gtts-cli -l en-gh \"" + text + "\" --output " + path);
			tts.waitFor();
			Process mp3towav = Runtime.getRuntime().exec("python py/mp3towav.py " + path);
			mp3towav.waitFor();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/* outputs a wav file */
	public static void textToSpeechFile(String path, String text){
		gTTSbasic(path, text);
	}
	
	public static void timestampedTextToSpeechFile(String audioPath, String timingsPath, String text){
		IBMTextToSpeech tts = new IBMTextToSpeech();
		tts.timestampedTtsToWav(audioPath, timingsPath, text);
	}
	
	public static SampledAudio wavToSampledAudio(String path){
		//System.out.println("Trying to open " + path);
		try
	      {
	         // Open the wav file specified as the first argument
	         WavFile wavFile = WavFile.openWavFile(new File(path));
	         wavFile.display();
	         int numChannels = wavFile.getNumChannels();
	         int numFrames = (int) wavFile.getNumFrames() * numChannels;
	         //System.out.println("numFrames: " + numFrames);

	         double[] buffer = new double[numFrames];

	         wavFile.readFrames(buffer, numFrames);
	         
	         SampledAudio audio = new SampledAudio();
	         audio.bits = wavFile.getValidBits();
	         audio.numChannels = numChannels;
	         audio.numFrames = (int)wavFile.getNumFrames();
	         audio.sampleRate = (int)wavFile.getSampleRate();
	         audio.samples = buffer;
	         return audio;
	      }
	      catch (Exception e)
	      {
	         e.printStackTrace();
	         return new SampledAudio();
	      }
	}
	
	public static void sampledAudioToWav(String path, SampledAudio audio){
		int numFrames = audio.numFrames;
		try {
			WavFile wavFile = WavFile.newWavFile(new File(path), audio.numChannels, numFrames, audio.bits, audio.sampleRate);
			wavFile.writeFrames(audio.samples, numFrames);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static MarkedAudio textToSpeechMarkedAudio(String text) throws Exception {
		IBMTextToSpeech ibmtts = new IBMTextToSpeech();
		MarkedAudio result = null;
		try {
			result = ibmtts.timestampedTtsToMarkedAudio(text);
		} catch (Exception e) {
			throw e;
		}
		return result;
	}
}